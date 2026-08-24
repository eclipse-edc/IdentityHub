/*
 *  Copyright (c) 2025 Metaform Systems Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.api.credentialoffer;

import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.IssuerMetadata;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.junit.assertions.AbstractResultAssert;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CredentialObjectResolverTest {

    private static final String ISSUER_DID = "did:web:issuer";

    private final DidResolverRegistry didResolverRegistry = mock();
    private final EdcHttpClient httpClient = mock();
    private final JsonLd jsonLd = mock();
    private final TypeTransformerRegistry transformerRegistry = mock();

    private final CredentialObjectResolver resolver = new CredentialObjectResolver(didResolverRegistry, httpClient, jsonLd, transformerRegistry);

    @Test
    void resolve_whenNoSparseEntries_shouldReturnUnchanged() {
        var credentialObject = CredentialObject.Builder.newInstance()
                .id("credential-object-id")
                .credentialType("MembershipCredential")
                .profile("vc20-bssl/jwt")
                .build();

        var result = resolver.resolve(ISSUER_DID, List.of(credentialObject));

        AbstractResultAssert.assertThat(result).isSucceeded()
                .satisfies(credentialObjects -> assertThat(credentialObjects).containsExactly(credentialObject));
        // no metadata is fetched when nothing is sparse
        verifyNoInteractions(didResolverRegistry, httpClient);
    }

    @Test
    void resolve_whenSparseEntry_shouldCompleteFromIssuerMetadata() {
        var supported = CredentialObject.Builder.newInstance()
                .id("credential-object-id")
                .credentialType("MembershipCredential")
                .profile("vc20-bssl/jwt")
                .build();
        stubIssuerMetadata(IssuerMetadata.Builder.newInstance().issuer(ISSUER_DID).credentialSupported(supported).build());

        var sparse = CredentialObject.Builder.newInstance().id("credential-object-id").build();

        var result = resolver.resolve(ISSUER_DID, List.of(sparse));

        AbstractResultAssert.assertThat(result).isSucceeded()
                .satisfies(credentialObjects -> assertThat(credentialObjects).containsExactly(supported));
    }

    @Test
    void resolve_whenSparseEntryNotInIssuerMetadata_shouldFail() {
        stubIssuerMetadata(IssuerMetadata.Builder.newInstance().issuer(ISSUER_DID).build());

        var sparse = CredentialObject.Builder.newInstance().id("credential-object-id").build();

        var result = resolver.resolve(ISSUER_DID, List.of(sparse));

        AbstractResultAssert.assertThat(result).isFailed()
                .detail().contains("credential-object-id");
    }

    @Test
    void resolve_whenDidNotResolvable_shouldFail() {
        when(didResolverRegistry.resolve(anyString())).thenReturn(Result.failure("DID not resolvable"));

        var sparse = CredentialObject.Builder.newInstance().id("credential-object-id").build();

        AbstractResultAssert.assertThat(resolver.resolve(ISSUER_DID, List.of(sparse))).isFailed();
        verifyNoInteractions(httpClient);
    }

    @Test
    void resolve_whenNoIssuerServiceEndpoint_shouldFail() {
        when(didResolverRegistry.resolve(anyString())).thenReturn(Result.success(DidDocument.Builder.newInstance()
                .id(ISSUER_DID)
                .service(List.of(new Service("id", "SomeOtherService", "http://localhost:1234")))
                .build()));

        var sparse = CredentialObject.Builder.newInstance().id("credential-object-id").build();

        AbstractResultAssert.assertThat(resolver.resolve(ISSUER_DID, List.of(sparse))).isFailed()
                .detail().contains("IssuerService");
        verifyNoInteractions(httpClient);
    }

    @Test
    @SuppressWarnings("unchecked")
    void resolve_whenMetadataRequestFails_shouldFail() {
        stubDidDocument();
        when(httpClient.execute(any(), any(Function.class))).thenReturn(Result.failure("Error fetching Issuer metadata"));

        var sparse = CredentialObject.Builder.newInstance().id("credential-object-id").build();

        AbstractResultAssert.assertThat(resolver.resolve(ISSUER_DID, List.of(sparse))).isFailed();
    }

    @SuppressWarnings("unchecked")
    private void stubIssuerMetadata(IssuerMetadata metadata) {
        stubDidDocument();
        when(httpClient.execute(any(), any(Function.class))).thenReturn(Result.success(metadata));
    }

    private void stubDidDocument() {
        when(didResolverRegistry.resolve(anyString())).thenReturn(Result.success(DidDocument.Builder.newInstance()
                .id(ISSUER_DID)
                .service(List.of(new Service("id", "IssuerService", "http://localhost:1234/api/issuance")))
                .build()));
    }
}
