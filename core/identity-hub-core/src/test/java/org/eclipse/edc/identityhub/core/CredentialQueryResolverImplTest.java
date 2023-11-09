/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.core;


import org.eclipse.edc.identityhub.defaults.EdcScopeToCriterionTransformer;
import org.eclipse.edc.identityhub.spi.model.PresentationQuery;
import org.eclipse.edc.identityhub.spi.model.presentationdefinition.PresentationDefinition;
import org.eclipse.edc.identityhub.spi.resolution.QueryFailure;
import org.eclipse.edc.identityhub.spi.store.CredentialStore;
import org.eclipse.edc.identityhub.spi.store.model.VerifiableCredentialResource;
import org.eclipse.edc.identitytrust.model.CredentialFormat;
import org.eclipse.edc.identitytrust.model.CredentialSubject;
import org.eclipse.edc.identitytrust.model.Issuer;
import org.eclipse.edc.identitytrust.model.VerifiableCredential;
import org.eclipse.edc.identitytrust.model.VerifiableCredentialContainer;
import org.eclipse.edc.spi.result.StoreResult;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.spi.result.StoreResult.success;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CredentialQueryResolverImplTest {

    private final CredentialStore storeMock = mock();
    private final CredentialQueryResolverImpl resolver = new CredentialQueryResolverImpl(storeMock, new EdcScopeToCriterionTransformer());

    @Test
    void query_noResult() {
        when(storeMock.query(any())).thenReturn(success(Stream.empty()));
        var res = resolver.query(createPresentationQuery("org.eclipse.edc.vc.type:TestCredential:read"),
                List.of("org.eclipse.edc.vc.type:AnotherCredential:read"));
        assertThat(res.succeeded()).isTrue();
        assertThat(res.getContent()).isEmpty();
    }

    @Test
    void query_noProverScope_shouldReturnEmpty() {
        when(storeMock.query(any())).thenReturn(success(Stream.empty()));
        var res = resolver.query(createPresentationQuery(), List.of("foobar"));
        assertThat(res.succeeded()).isFalse();
        assertThat(res.reason()).isEqualTo(QueryFailure.Reason.INVALID_SCOPE);
        assertThat(res.getFailureDetail()).contains("Invalid query: must contain at least one scope.");
    }

    @Test
    void query_proverScopeStringInvalid_shouldReturnFailure() {
        when(storeMock.query(any())).thenReturn(success(Stream.empty()));
        var res = resolver.query(createPresentationQuery("invalid"),
                List.of("org.eclipse.edc.vc.type:AnotherCredential:read"));
        assertThat(res.failed()).isTrue();
        assertThat(res.reason()).isEqualTo(QueryFailure.Reason.INVALID_SCOPE);
        assertThat(res.getFailureDetail()).contains("Scope string has invalid format.");
    }

    @Test
    void query_scopeStringHasWrongOperator_shouldReturnFailure() {
        var res = resolver.query(createPresentationQuery("org.eclipse.edc.vc.type:TestCredential:write"), List.of("ignored"));
        assertThat(res.failed()).isTrue();
        assertThat(res.reason()).isEqualTo(QueryFailure.Reason.INVALID_SCOPE);
        assertThat(res.getFailureDetail()).contains("Invalid scope operation: write");
    }

    @Test
    void query_singleScopeString() {
        var credential = createCredentialResource("TestCredential");
        when(storeMock.query(any())).thenReturn(success(Stream.of(credential)));
        var res = resolver.query(createPresentationQuery("org.eclipse.edc.vc.type:TestCredential:read"),
                List.of("org.eclipse.edc.vc.type:TestCredential:read"));
        assertThat(res.succeeded()).withFailMessage(res::getFailureDetail).isTrue();
        assertThat(res.getContent()).containsExactly(credential.getVerifiableCredential());
    }

    @Test
    void query_multipleScopeStrings() {
        var credential1 = createCredentialResource("TestCredential");
        var credential2 = createCredentialResource("AnotherCredential");
        when(storeMock.query(any())).thenReturn(success(Stream.of(credential1, credential2)));

        var res = resolver.query(createPresentationQuery("org.eclipse.edc.vc.type:TestCredential:read",
                        "org.eclipse.edc.vc.type:AnotherCredential:read"),
                List.of("org.eclipse.edc.vc.type:TestCredential:read", "org.eclipse.edc.vc.type:AnotherCredential:read"));
        assertThat(res.succeeded()).withFailMessage(res::getFailureDetail).isTrue();
        assertThat(res.getContent()).containsExactlyInAnyOrder(credential1.getVerifiableCredential(), credential2.getVerifiableCredential());
    }

    @Test
    void query_presentationDefinition_unsupported() {
        var q = PresentationQuery.Builder.newinstance().presentationDefinition(PresentationDefinition.Builder.newInstance().id("test-pd").build()).build();
        assertThatThrownBy(() -> resolver.query(q, List.of("org.eclipse.edc.vc.type:SomeCredential:read")))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Querying with a DIF Presentation Exchange definition is not yet supported.");
    }

    @Test
    void query_requestsTooManyCredentials_shouldReturnFailure() {
        var credential1 = createCredentialResource("TestCredential");
        var credential2 = createCredentialResource("AnotherCredential");
        when(storeMock.query(any())).thenReturn(success(Stream.of(credential1, credential2)));

        var res = resolver.query(createPresentationQuery("org.eclipse.edc.vc.type:TestCredential:read",
                        "org.eclipse.edc.vc.type:AnotherCredential:read"),
                List.of("org.eclipse.edc.vc.type:TestCredential:read"));

        assertThat(res.failed()).isTrue();
        assertThat(res.reason()).isEqualTo(QueryFailure.Reason.UNAUTHORIZED_SCOPE);
        assertThat(res.getFailureDetail()).isEqualTo("Invalid query: requested Credentials outside of scope.");
    }

    @Test
    void query_moreCredentialsAllowed_shouldReturnOnlyRequested() {
        var credential1 = createCredentialResource("TestCredential");
        when(storeMock.query(any())).thenReturn(success(Stream.of(credential1)));

        var res = resolver.query(createPresentationQuery("org.eclipse.edc.vc.type:TestCredential:read"),
                List.of("org.eclipse.edc.vc.type:TestCredential:read", "org.eclipse.edc.vc.type:AnotherCredential:read"));

        assertThat(res.succeeded()).isTrue();
        assertThat(res.getContent()).containsOnly(credential1.getVerifiableCredential());
    }

    @Test
    void query_exactMatchAllowedAndRequestedCredentials() {
        var credential1 = createCredentialResource("TestCredential");
        when(storeMock.query(any())).thenReturn(success(Stream.of(credential1)));

        var res = resolver.query(createPresentationQuery("org.eclipse.edc.vc.type:TestCredential:read"),
                List.of("org.eclipse.edc.vc.type:TestCredential:read"));

        assertThat(res.succeeded()).isTrue();
        assertThat(res.getContent()).containsOnly(credential1.getVerifiableCredential());
    }

    @Test
    void query_requestedCredentialNotAllowed() {
        var credential1 = createCredentialResource("TestCredential");
        when(storeMock.query(any())).thenReturn(success(Stream.of(credential1)));

        var res = resolver.query(createPresentationQuery("org.eclipse.edc.vc.type:TestCredential:read"),
                List.of("org.eclipse.edc.vc.type:AnotherCredential:read"));

        assertThat(res.failed()).isTrue();
        assertThat(res.reason()).isEqualTo(QueryFailure.Reason.UNAUTHORIZED_SCOPE);
        assertThat(res.getFailureDetail()).isEqualTo("Invalid query: requested Credentials outside of scope.");
    }

    @Test
    void query_sameSizeDifferentScope() {
        var credential1 = createCredentialResource("TestCredential");
        var credential2 = createCredentialResource("AnotherCredential");
        when(storeMock.query(any())).thenReturn(success(Stream.of(credential1)));

        var res = resolver.query(createPresentationQuery("org.eclipse.edc.vc.type:TestCredential:read", "org.eclipse.edc.vc.type:AnotherCredential:read"),
                List.of("org.eclipse.edc.vc.type:FooCredential:read", "org.eclipse.edc.vc.type:BarCredential:read"));

        assertThat(res.failed()).isTrue();
        assertThat(res.reason()).isEqualTo(QueryFailure.Reason.UNAUTHORIZED_SCOPE);
        assertThat(res.getFailureDetail()).isEqualTo("Invalid query: requested Credentials outside of scope.");
    }

    @Test
    void query_storeReturnsFailure() {
        var credential1 = createCredentialResource("TestCredential");
        when(storeMock.query(any())).thenReturn(StoreResult.notFound("test-failure"));

        var res = resolver.query(createPresentationQuery("org.eclipse.edc.vc.type:TestCredential:read"),
                List.of("org.eclipse.edc.vc.type:AnotherCredential:read"));

        assertThat(res.failed()).isTrue();
        assertThat(res.reason()).isEqualTo(QueryFailure.Reason.STORAGE_FAILURE);
        assertThat(res.getFailureDetail()).isEqualTo("test-failure");
    }

    private PresentationQuery createPresentationQuery(@Nullable String... scope) {
        var scopes = new ArrayList<>(Arrays.asList(scope));
        return PresentationQuery.Builder.newinstance().scopes(scopes).build();
    }

    private VerifiableCredentialResource createCredentialResource(String... type) {
        var cred = VerifiableCredential.Builder.newInstance()
                .types(Arrays.asList(type))
                .issuer(new Issuer("test-issuer", Map.of()))
                .issuanceDate(Instant.now())
                .credentialSubject(CredentialSubject.Builder.newInstance().id("test-cred-id").claim("test-claim", "test-value").build())
                .build();
        return VerifiableCredentialResource.Builder.newInstance()
                .credential(new VerifiableCredentialContainer("foobar", CredentialFormat.JSON_LD, cred))
                .holderId("test-holder")
                .issuerId("test-issuer")
                .build();
    }
}