/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.core.services.query;

import org.eclipse.edc.iam.identitytrust.spi.model.PresentationQueryMessage;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialStatus;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.RevocationServiceRegistry;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.presentationdefinition.PresentationDefinition;
import org.eclipse.edc.identityhub.defaults.EdcScopeToCriterionTransformer;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.resolution.QueryFailure;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.StoreResult;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.spi.result.StoreResult.success;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CredentialQueryResolverImplTest {

    public static final String TEST_PARTICIPANT_CONTEXT_ID = "test-participant";
    private final CredentialStore storeMock = mock();
    private final RevocationServiceRegistry revocationServiceRegistry = mock();
    private final Monitor monitor = mock();
    private final CredentialQueryResolverImpl resolver = new CredentialQueryResolverImpl(storeMock, new EdcScopeToCriterionTransformer(), revocationServiceRegistry, monitor);

    @BeforeEach
    void setUp() {
        when(revocationServiceRegistry.checkValidity(any())).thenReturn(Result.success());
    }

    @Test
    void query_noResult() {
        when(storeMock.query(any())).thenAnswer(i -> success(List.of()));
        var res = resolver.query(TEST_PARTICIPANT_CONTEXT_ID,
                createPresentationQuery("org.eclipse.edc.vc.type:TestCredential:read"), List.of("org.eclipse.edc.vc.type:AnotherCredential:read"));
        assertThat(res.succeeded()).isTrue();
        assertThat(res.getContent()).isEmpty();
    }

    @Test
    void query_invalidAccessTokenScope_shouldReturnEmpty() {
        when(storeMock.query(any())).thenReturn(success(Collections.emptyList()));
        var res = resolver.query(TEST_PARTICIPANT_CONTEXT_ID, createPresentationQuery(), List.of("foobar"));
        assertThat(res.succeeded()).isFalse();
        assertThat(res.reason()).isEqualTo(QueryFailure.Reason.INVALID_SCOPE);
        assertThat(res.getFailureDetail()).contains("Scope string cannot be converted: Scope string has invalid format.");
    }

    @Test
    void query_noAccessTokenScope_noQueryScope_shouldReturnEmpty() {
        when(storeMock.query(any())).thenReturn(success(Collections.emptyList()));
        var res = resolver.query(TEST_PARTICIPANT_CONTEXT_ID, createPresentationQuery(/*empty scopes*/), List.of());
        assertThat(res.succeeded()).isTrue();
        assertThat(res.getContent()).isEmpty();
    }

    @Test
    void query_noQueryScope_shouldAllPermitted() {
        var credential = createCredentialResource("AnotherCredential");
        when(storeMock.query(any())).thenReturn(success(List.of(credential)));

        var res = resolver.query(TEST_PARTICIPANT_CONTEXT_ID, createPresentationQuery(/*empty scopes*/), List.of("org.eclipse.edc.vc.type:AnotherCredential:read"));
        assertThat(res.succeeded()).isTrue();
        assertThat(res.getContent()).usingRecursiveFieldByFieldElementComparator().containsExactly(credential.getVerifiableCredential());
    }

    @Test
    void query_noAccessTokenScope_withQueryScope_shouldReturnFailure() {
        var credential = createCredentialResource("AnotherCredential");
        when(storeMock.query(any()))
                .thenReturn(success(List.of(credential)));

        var res = resolver.query(TEST_PARTICIPANT_CONTEXT_ID, createPresentationQuery("org.eclipse.edc.vc.type:AnotherCredential:read"), List.of());
        assertThat(res.succeeded()).isFalse();
        assertThat(res.reason()).isEqualTo(QueryFailure.Reason.UNAUTHORIZED_SCOPE);
        verify(monitor).warning("Permission was not granted on any credentials (empty access token scope list), but 1 were requested.");
    }

    @Test
    void query_accessTokenScopeStringInvalid_shouldReturnFailure() {
        when(storeMock.query(any())).thenReturn(success(Collections.emptyList()));
        var res = resolver.query(TEST_PARTICIPANT_CONTEXT_ID,
                createPresentationQuery("invalid"), List.of("org.eclipse.edc.vc.type:AnotherCredential:read"));
        assertThat(res.failed()).isTrue();
        assertThat(res.reason()).isEqualTo(QueryFailure.Reason.INVALID_SCOPE);
        assertThat(res.getFailureDetail()).contains("Scope string has invalid format.");
    }

    @Test
    void query_scopeStringHasWrongOperator_shouldReturnFailure() {
        var res = resolver.query(TEST_PARTICIPANT_CONTEXT_ID, createPresentationQuery("org.eclipse.edc.vc.type:TestCredential:write"), List.of("ignored"));
        assertThat(res.failed()).isTrue();
        assertThat(res.reason()).isEqualTo(QueryFailure.Reason.INVALID_SCOPE);
        assertThat(res.getFailureDetail()).contains("Scope string cannot be converted: Scope string has invalid format.");
    }

    @Test
    void query_singleScopeString() {
        var credential = createCredentialResource("TestCredential");
        when(storeMock.query(any())).thenAnswer(i -> success(List.of(credential)));
        var res = resolver.query(TEST_PARTICIPANT_CONTEXT_ID,
                createPresentationQuery("org.eclipse.edc.vc.type:TestCredential:read"), List.of("org.eclipse.edc.vc.type:TestCredential:read"));
        assertThat(res.succeeded()).withFailMessage(res::getFailureDetail).isTrue();
        assertThat(res.getContent()).containsExactly(credential.getVerifiableCredential());
    }

    @Test
    void query_verifyDifferentObjects() {
        var credential1 = createCredentialResource(createCredential("TestCredential").build()).id("id1").build();
        var credential2 = createCredentialResource(createCredential("TestCredential").build()).id("id1").build();

        when(storeMock.query(any()))
                .thenReturn(success(List.of(credential1)))
                .thenReturn(success(List.of(credential2)));

        var res = resolver.query(TEST_PARTICIPANT_CONTEXT_ID,
                createPresentationQuery("org.eclipse.edc.vc.type:TestCredential:read"), List.of("org.eclipse.edc.vc.type:TestCredential:read"));

        assertThat(res.succeeded()).withFailMessage(res::getFailureDetail).isTrue();
        assertThat(res.getContent()).usingRecursiveFieldByFieldElementComparator().containsExactly(credential2.getVerifiableCredential());
    }

    @Test
    void query_whenParticipantIdMismatch_expectEmptyResult() {
        when(storeMock.query(any())).thenAnswer(i -> success(List.of()));

        var res = resolver.query("another_participant_context_id",
                createPresentationQuery("org.eclipse.edc.vc.type:TestCredential:read"), List.of("org.eclipse.edc.vc.type:TestCredential:read"));
        assertThat(res.succeeded()).isTrue();
        assertThat(res.getContent()).isEmpty();
    }

    @Test
    void query_multipleScopeStrings() {
        var credential1 = createCredentialResource("TestCredential");
        var credential2 = createCredentialResource("AnotherCredential");
        var mapping = Map.of("TestCredential", credential1, "AnotherCredential", credential2);

        when(storeMock.query(any())).thenAnswer(i -> {
            QuerySpec querySpec = i.getArgument(0);
            return success(List.of(mapping.get(querySpec.getFilterExpression().get(0).getOperandRight().toString())));
        });

        var res = resolver.query(TEST_PARTICIPANT_CONTEXT_ID,
                createPresentationQuery("org.eclipse.edc.vc.type:TestCredential:read",
                        "org.eclipse.edc.vc.type:AnotherCredential:read"), List.of("org.eclipse.edc.vc.type:TestCredential:read", "org.eclipse.edc.vc.type:AnotherCredential:read"));
        assertThat(res.succeeded()).withFailMessage(res::getFailureDetail).isTrue();
        assertThat(res.getContent()).containsExactlyInAnyOrder(credential1.getVerifiableCredential(), credential2.getVerifiableCredential());
    }

    @Test
    void query_presentationDefinition_unsupported() {
        var q = PresentationQueryMessage.Builder.newinstance().presentationDefinition(PresentationDefinition.Builder.newInstance().id("test-pd").build()).build();
        assertThatThrownBy(() -> resolver.query(TEST_PARTICIPANT_CONTEXT_ID, q, List.of("org.eclipse.edc.vc.type:SomeCredential:read")))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Querying with a DIF Presentation Exchange definition is not yet supported.");
    }

    @Test
    void query_requestsTooManyCredentials_shouldReturnFailure() {
        var credential1 = createCredentialResource("TestCredential");
        var credential2 = createCredentialResource("AnotherCredential");
        when(storeMock.query(any()))
                .thenReturn(success(List.of(credential1)))
                .thenReturn(success(List.of(credential2)))
                .thenReturn(success(List.of(credential1)));

        var res = resolver.query(TEST_PARTICIPANT_CONTEXT_ID,
                createPresentationQuery("org.eclipse.edc.vc.type:TestCredential:read",
                        "org.eclipse.edc.vc.type:AnotherCredential:read"), List.of("org.eclipse.edc.vc.type:TestCredential:read"));

        assertThat(res.succeeded()).isFalse();
        assertThat(res.reason()).isEqualTo(QueryFailure.Reason.UNAUTHORIZED_SCOPE);
        assertThat(res.getFailureDetail()).isEqualTo("Invalid query: requested Credentials outside of scope.");
    }

    @Test
    void query_moreCredentialsAllowed_shouldReturnOnlyRequested() {
        var credential1 = createCredentialResource("TestCredential");
        when(storeMock.query(any())).thenAnswer(i -> success(List.of(credential1)));

        var res = resolver.query(TEST_PARTICIPANT_CONTEXT_ID,
                createPresentationQuery("org.eclipse.edc.vc.type:TestCredential:read"), List.of("org.eclipse.edc.vc.type:TestCredential:read", "org.eclipse.edc.vc.type:AnotherCredential:read"));

        assertThat(res.succeeded()).isTrue();
        assertThat(res.getContent()).containsOnly(credential1.getVerifiableCredential());
    }

    @Test
    void query_exactMatchAllowedAndRequestedCredentials() {
        var credential1 = createCredentialResource("TestCredential");
        when(storeMock.query(any())).thenAnswer(i -> success(List.of(credential1)));

        var res = resolver.query(TEST_PARTICIPANT_CONTEXT_ID,
                createPresentationQuery("org.eclipse.edc.vc.type:TestCredential:read"), List.of("org.eclipse.edc.vc.type:TestCredential:read"));

        assertThat(res.succeeded()).isTrue();
        assertThat(res.getContent()).containsOnly(credential1.getVerifiableCredential());
    }

    @Test
    void query_requestedCredentialNotAllowed() {
        var credential1 = createCredentialResource("TestCredential");
        var credential2 = createCredentialResource("AnotherCredential");
        when(storeMock.query(any())).thenAnswer(i -> success(List.of(credential1)))
                .thenAnswer(i -> success(List.of(credential2)));

        var res = resolver.query(TEST_PARTICIPANT_CONTEXT_ID,
                createPresentationQuery("org.eclipse.edc.vc.type:TestCredential:read"), List.of("org.eclipse.edc.vc.type:AnotherCredential:read"));

        assertThat(res.failed()).isTrue();
        assertThat(res.reason()).isEqualTo(QueryFailure.Reason.UNAUTHORIZED_SCOPE);
        assertThat(res.getFailureDetail()).isEqualTo("Invalid query: requested Credentials outside of scope.");
    }

    @Test
    void query_sameSizeDifferentScope() {
        var credential1 = createCredentialResource("TestCredential");
        var credential2 = createCredentialResource("AnotherCredential");
        var credential3 = createCredentialResource("FooCredential");
        var credential4 = createCredentialResource("BarCredential");
        when(storeMock.query(any()))
                .thenReturn(success(List.of(credential1)))
                .thenReturn(success(List.of(credential2)))
                .thenReturn(success(List.of(credential3)))
                .thenReturn(success(List.of(credential4)));

        var res = resolver.query(TEST_PARTICIPANT_CONTEXT_ID,
                createPresentationQuery("org.eclipse.edc.vc.type:TestCredential:read", "org.eclipse.edc.vc.type:AnotherCredential:read"),
                List.of("org.eclipse.edc.vc.type:FooCredential:read", "org.eclipse.edc.vc.type:BarCredential:read"));

        assertThat(res.succeeded()).isFalse();
        assertThat(res.reason()).isEqualTo(QueryFailure.Reason.UNAUTHORIZED_SCOPE);
        assertThat(res.getFailureDetail()).isEqualTo("Invalid query: requested Credentials outside of scope.");
    }

    @Test
    void query_storeReturnsFailure() {
        var credential1 = createCredentialResource("TestCredential");
        when(storeMock.query(any())).thenReturn(StoreResult.notFound("test-failure"));

        var res = resolver.query(TEST_PARTICIPANT_CONTEXT_ID,
                createPresentationQuery("org.eclipse.edc.vc.type:TestCredential:read"), List.of("org.eclipse.edc.vc.type:AnotherCredential:read"));

        assertThat(res.failed()).isTrue();
        assertThat(res.reason()).isEqualTo(QueryFailure.Reason.STORAGE_FAILURE);
        assertThat(res.getFailureDetail()).isEqualTo("test-failure");
    }

    @Test
    void query_whenExpiredCredential_doesNotInclude() {
        var credential = createCredential("TestCredential")
                .expirationDate(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();
        var resource = createCredentialResource(credential).build();

        when(storeMock.query(any())).thenAnswer(i -> success(List.of(resource)));
        var res = resolver.query(TEST_PARTICIPANT_CONTEXT_ID,
                createPresentationQuery("org.eclipse.edc.vc.type:TestCredential:read"), List.of("org.eclipse.edc.vc.type:TestCredential:read"));

        assertThat(res.succeeded()).withFailMessage(res::getFailureDetail).isTrue();
        assertThat(res.getContent()).isEmpty();
        verify(monitor).warning(eq("Credential '%s' is expired.".formatted(credential.getId())));
    }

    @Test
    void query_whenNotYetValidCredential_doesNotInclude() {
        var credential = createCredential("TestCredential")
                .issuanceDate(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();
        var resource = createCredentialResource(credential).build();

        when(storeMock.query(any())).thenAnswer(i -> success(List.of(resource)));
        var res = resolver.query(TEST_PARTICIPANT_CONTEXT_ID,
                createPresentationQuery("org.eclipse.edc.vc.type:TestCredential:read"), List.of("org.eclipse.edc.vc.type:TestCredential:read"));

        assertThat(res.succeeded()).withFailMessage(res::getFailureDetail).isTrue();
        assertThat(res.getContent()).isEmpty();
        verify(monitor).warning(eq("Credential '%s' is not yet valid.".formatted(credential.getId())));
    }

    @Test
    void query_whenRevokedCredential_doesNotInclude() {
        when(revocationServiceRegistry.checkValidity(any())).thenReturn(Result.failure("revoked"));
        var credential = createCredential("TestCredential")
                .credentialStatus(new CredentialStatus("test-cred-stat-id", "StatusList2021Entry",
                        Map.of("statusListCredential", "https://university.example/credentials/status/3",
                                "statusPurpose", "suspension",
                                "statusListIndex", 69)))
                .build();
        var resource = createCredentialResource(credential).build();
        when(storeMock.query(any())).thenAnswer(i -> success(List.of(resource)));
        var res = resolver.query(TEST_PARTICIPANT_CONTEXT_ID,
                createPresentationQuery("org.eclipse.edc.vc.type:TestCredential:read"), List.of("org.eclipse.edc.vc.type:TestCredential:read"));

        assertThat(res.succeeded()).withFailMessage(res::getFailureDetail).isTrue();
        assertThat(res.getContent()).isEmpty();
        verify(monitor).warning(eq("Credential '%s' not valid: revoked".formatted(credential.getId())));
    }

    private VerifiableCredentialResource.Builder createCredentialResource(VerifiableCredential cred) {
        return VerifiableCredentialResource.Builder.newInstance()
                .credential(new VerifiableCredentialContainer("foobar", CredentialFormat.VC1_0_LD, cred))
                .holderId("test-holder")
                .issuerId("test-issuer")
                .participantContextId(TEST_PARTICIPANT_CONTEXT_ID);
    }

    private VerifiableCredential.Builder createCredential(String... type) {
        return VerifiableCredential.Builder.newInstance()
                .types(Arrays.asList(type))
                .id(UUID.randomUUID().toString())
                .issuer(new Issuer("test-issuer", Map.of()))
                .issuanceDate(Instant.now())
                .credentialSubject(CredentialSubject.Builder.newInstance().id("test-cred-id").claim("test-claim", "test-value").build());
    }

    private PresentationQueryMessage createPresentationQuery(@Nullable String... scope) {
        var scopes = new ArrayList<>(Arrays.asList(scope));
        return PresentationQueryMessage.Builder.newinstance().scopes(scopes).build();
    }

    private VerifiableCredentialResource createCredentialResource(String... type) {
        var cred = createCredential(type).build();
        return createCredentialResource(cred).build();
    }
}