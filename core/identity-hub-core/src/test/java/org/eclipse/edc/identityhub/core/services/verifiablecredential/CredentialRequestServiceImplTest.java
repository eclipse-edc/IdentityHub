/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.core.services.verifiablecredential;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import okhttp3.Response;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.iam.identitytrust.spi.SecureTokenService;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestMessage;
import org.eclipse.edc.identityhub.spi.credential.request.store.HolderCredentialRequestStore;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState.CREATED;
import static org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState.ERROR;
import static org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState.REQUESTING;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class CredentialRequestServiceImplTest {

    public static final String ISSUER_DID = "did:web:issuer";
    public static final String OWN_DID = "did:web:holder";
    private final HolderCredentialRequestStore store = mock();
    private final DidResolverRegistry resolver = mock();
    private final TypeTransformerRegistry transformerRegistry = mock();
    private final EdcHttpClient httpClient = mock();
    private final SecureTokenService sts = mock();
    private final CredentialRequestServiceImpl credentialRequestService = new CredentialRequestServiceImpl(store, resolver, transformerRegistry, httpClient, sts, OWN_DID);

    @BeforeEach
    void setUp() {
        when(transformerRegistry.transform(any(CredentialRequestMessage.class), eq(JsonObject.class)))
                .thenReturn(success(Json.createObjectBuilder().build()));
        when(sts.createToken(anyMap(), ArgumentMatchers.isNull())).thenReturn(success(TokenRepresentation.Builder.newInstance().build()));
    }

    @Test
    void initiateRequest() {
        when(resolver.resolve(eq(ISSUER_DID))).thenReturn(success(didDocument()));
        when(httpClient.execute(any(), (Function<Response, Result<String>>) any()))
                .thenReturn(success("test-issuance-process-id"));

        var result = credentialRequestService.initiateRequest("test-participant", ISSUER_DID, UUID.randomUUID().toString(), Map.of("TestCredential", CredentialFormat.VC1_0_JWT.toString()));
        assertThat(result)
                .isSucceeded()
                .isEqualTo("test-issuance-process-id");

        verify(store, times(3)).save(any());


        verify(resolver).resolve(eq(ISSUER_DID));
        verify(httpClient).execute(any(), (Function<Response, Result<String>>) any());
        verify(transformerRegistry).transform(any(CredentialRequestMessage.class), eq(JsonObject.class));
        verifyNoMoreInteractions(store, resolver, transformerRegistry, httpClient);
    }

    @Test
    void initiateRequest_whenDidNotResolvable() {
        when(resolver.resolve(eq(ISSUER_DID))).thenReturn(Result.failure("foo-failure"));

        var result = credentialRequestService.initiateRequest("test-participant", ISSUER_DID, UUID.randomUUID().toString(), Map.of("TestCredential", CredentialFormat.VC1_0_JWT.toString()));
        assertThat(result)
                .isFailed()
                .detail().containsSequence("foo-failure");

        verify(store).save(argThat(rq -> rq.getErrorDetail().equals("foo-failure")));
        verify(resolver).resolve(eq(ISSUER_DID));
        verifyNoMoreInteractions(store, resolver, transformerRegistry, httpClient);
    }

    @Test
    void initiateRequest_whenDidDoesNotContainEndpoint() {
        when(resolver.resolve(eq(ISSUER_DID))).thenReturn(success(DidDocument.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                // missing: endpoint
                .build()));

        var result = credentialRequestService.initiateRequest("test-participant", ISSUER_DID, UUID.randomUUID().toString(), Map.of("TestCredential", CredentialFormat.VC1_0_JWT.toString()));
        assertThat(result)
                .isFailed()
                .detail().containsSequence("does not contain any CredentialRequest endpoint");

        verify(store).save(argThat(rq -> rq.getErrorDetail().contains("does not contain any CredentialRequest endpoint")));
        verify(resolver).resolve(eq(ISSUER_DID));
        verifyNoMoreInteractions(store, resolver, transformerRegistry, httpClient);
    }

    @Test
    void initiateRequest_whenStsFails() {
        when(resolver.resolve(eq(ISSUER_DID))).thenReturn(success(didDocument()));
        when(sts.createToken(anyMap(), ArgumentMatchers.isNull())).thenReturn(failure("sts-failure"));

        var result = credentialRequestService.initiateRequest("test-participant", ISSUER_DID, UUID.randomUUID().toString(), Map.of("TestCredential", CredentialFormat.VC1_0_JWT.toString()));
        assertThat(result)
                .isFailed()
                .detail().isEqualTo("sts-failure");

        verify(store, times(1)).save(argThat(rq -> rq.getState() == CREATED.code()));
        verify(store, times(1)).save(argThat(rq -> rq.getState() == REQUESTING.code()));
        verify(store, times(1)).save(argThat(rq -> "sts-failure".equals(rq.getErrorDetail()) && rq.getState() == ERROR.code()));

        verify(resolver).resolve(eq(ISSUER_DID));
        verify(sts).createToken(anyMap(), ArgumentMatchers.isNull());
        verifyNoMoreInteractions(store, resolver, transformerRegistry, httpClient);
    }

    @Test
    void initiateRequest_whenIssuerReturnsError() {
        when(resolver.resolve(eq(ISSUER_DID))).thenReturn(success(didDocument()));
        when(httpClient.execute(any(), (Function<Response, Result<String>>) any()))
                .thenReturn(failure("issuer failure bad request"));

        var result = credentialRequestService.initiateRequest("test-participant", ISSUER_DID, UUID.randomUUID().toString(), Map.of("TestCredential", CredentialFormat.VC1_0_JWT.toString()));
        assertThat(result)
                .isFailed()
                .detail().isEqualTo("issuer failure bad request");

        verify(store, times(2)).save(argThat(rq -> rq.getErrorDetail() == null && rq.getState() != ERROR.code()));
        verify(store, times(1)).save(argThat(rq -> "issuer failure bad request".equals(rq.getErrorDetail()) && rq.getState() == ERROR.code()));

        verify(resolver).resolve(eq(ISSUER_DID));
        verify(transformerRegistry).transform(any(CredentialRequestMessage.class), eq(JsonObject.class));
        verify(httpClient).execute(any(), (Function<Response, Result<String>>) any());
        verifyNoMoreInteractions(store, resolver, transformerRegistry, httpClient);
    }

    private DidDocument didDocument() {
        return DidDocument.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .service(List.of(new Service(UUID.randomUUID().toString(), "CredentialRequest", "http://issuer.com/issuance")))
                .build();
    }

}