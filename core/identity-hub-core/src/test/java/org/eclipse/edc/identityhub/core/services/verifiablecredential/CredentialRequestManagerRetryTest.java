/*
 *  Copyright (c) 2026 Metaform Systems Inc.
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

package org.eclipse.edc.identityhub.core.services.verifiablecredential;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.identityhub.core.CredentialRequestConfiguration;
import org.eclipse.edc.identityhub.defaults.store.InMemoryHolderCredentialRequestStore;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestMessage;
import org.eclipse.edc.identityhub.spi.authentication.ParticipantSecureTokenService;
import org.eclipse.edc.identityhub.spi.credential.request.model.RequestedCredential;
import org.eclipse.edc.identityhub.spi.participantcontext.IdentityHubParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.IdentityHubParticipantContext;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.query.CriterionOperatorRegistryImpl;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.statemachine.retry.EntityRetryProcessConfiguration;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.DcpConstants.DCP_SCOPE_V_1_0;
import static org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState.ERROR;
import static org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState.REQUESTED;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.result.Result.success;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Covers how a credential request behaves when sending it to the Issuer fails: a failure that may go away on its own is
 * attempted again, within the configured limit, whereas one the Issuer would repeat is not. Runs against the real
 * in-memory store, so that the state machine picks up exactly what is actually pending.
 */
class CredentialRequestManagerRetryTest {

    private static final String ISSUER_DID = "did:web:issuer";
    private static final int RETRY_LIMIT = 2;
    private static final Duration MAX_DURATION = Duration.ofSeconds(5);

    private final InMemoryHolderCredentialRequestStore store =
            new InMemoryHolderCredentialRequestStore("runtime", Clock.systemUTC(), CriterionOperatorRegistryImpl.ofDefaults());
    private final DidResolverRegistry resolver = mock();
    private final EdcHttpClient httpClient = mock();
    private final ParticipantSecureTokenService sts = mock();
    private final TypeTransformerRegistry transformerRegistry = mock();
    private final JsonLd jsonLd = mock();
    private final IdentityHubParticipantContextService participantContextService = mock();
    private final CredentialRequestConfiguration configuration = mock();
    private final CredentialRequestManagerImpl manager = CredentialRequestManagerImpl.Builder.newInstance()
            .store(store)
            .didResolverRegistry(resolver)
            .typeTransformerRegistry(transformerRegistry)
            .jsonLd(jsonLd)
            .httpClient(httpClient)
            .secureTokenService(sts)
            .participantContextService(participantContextService)
            .transactionContext(new NoopTransactionContext())
            .monitor(mock())
            .waitStrategy(() -> 20L)
            .entityRetryProcessConfiguration(new EntityRetryProcessConfiguration(RETRY_LIMIT, () -> () -> 1L))
            .configuration(configuration)
            .build();

    @BeforeEach
    void setUp() {
        when(configuration.statusPollInterval()).thenReturn(60_000L);
        when(configuration.bearerAccessScope()).thenReturn(null);
        when(transformerRegistry.transform(any(CredentialRequestMessage.class), eq(JsonObject.class)))
                .thenReturn(success(Json.createObjectBuilder().build()));
        when(jsonLd.compact(any(), eq(DCP_SCOPE_V_1_0))).thenReturn(success(Json.createObjectBuilder().build()));
        when(participantContextService.getParticipantContext(anyString()))
                .thenReturn(ServiceResult.success(IdentityHubParticipantContext.Builder.newInstance()
                        .id("test-participant").did("did:web:holder").apiTokenAlias("alias").build()));
        when(sts.createToken(anyString(), anyMap(), isNull()))
                .thenReturn(success(TokenRepresentation.Builder.newInstance().token("token").build()));
        when(resolver.isSupported(anyString())).thenReturn(true);
        when(resolver.resolve(ISSUER_DID)).thenReturn(issuerDidDocument());
    }

    @AfterEach
    void tearDown() {
        manager.stop();
    }

    @Test
    @DisplayName("an unreachable Issuer is attempted again, and the request fails once the retry limit is exhausted")
    void unreachableIssuer_isRetriedUntilTheLimit_thenErrors() throws IOException {
        when(httpClient.execute(any(Request.class))).thenThrow(new IOException("Failed to connect to issuer.com"));
        var holderPid = initiate();

        manager.start();

        await().atMost(MAX_DURATION).untilAsserted(() -> assertThat(store.findById(holderPid).stateAsEnum()).isEqualTo(ERROR));
        // the first attempt, then one retry for each unit of the limit
        verify(httpClient, times(RETRY_LIMIT + 1)).execute(any(Request.class));
        assertThat(store.findById(holderPid).getErrorDetail()).contains("Failed to connect to issuer.com");
    }

    @Test
    @DisplayName("a request that could not be sent goes through once the Issuer is reachable again")
    void unreachableIssuer_recoversOnLaterAttempt() throws IOException {
        when(httpClient.execute(any(Request.class)))
                .thenThrow(new IOException("Failed to connect to issuer.com"))
                .thenReturn(created("https://issuer.com/api/issuance/requests/issuance-process-id"));
        var holderPid = initiate();

        manager.start();

        await().atMost(MAX_DURATION).untilAsserted(() -> {
            var request = store.findById(holderPid);
            assertThat(request.stateAsEnum()).isEqualTo(REQUESTED);
            assertThat(request.getIssuerPid()).isEqualTo("issuance-process-id");
        });
        verify(httpClient, times(2)).execute(any(Request.class));
    }

    @Test
    @DisplayName("an Issuer answering with a server error is attempted again")
    void serverError_isRetried() throws IOException {
        when(httpClient.execute(any(Request.class)))
                .thenReturn(response(503, "Service Unavailable", "maintenance"))
                .thenReturn(created("https://issuer.com/api/issuance/requests/issuance-process-id"));
        var holderPid = initiate();

        manager.start();

        await().atMost(MAX_DURATION).untilAsserted(() -> assertThat(store.findById(holderPid).stateAsEnum()).isEqualTo(REQUESTED));
        verify(httpClient, times(2)).execute(any(Request.class));
    }

    @Test
    @DisplayName("an Issuer refusing the request is not asked again")
    void clientError_isTerminal() throws IOException {
        when(httpClient.execute(any(Request.class))).thenReturn(response(400, "Bad Request", "unknown credential object"));
        var holderPid = initiate();

        manager.start();

        await().atMost(MAX_DURATION).untilAsserted(() -> assertThat(store.findById(holderPid).stateAsEnum()).isEqualTo(ERROR));
        await().during(Duration.ofMillis(200)).untilAsserted(() -> verify(httpClient, times(1)).execute(any(Request.class)));
        assertThat(store.findById(holderPid).getErrorDetail()).contains("unknown credential object");
    }

    @Test
    @DisplayName("an Issuer DID that cannot be resolved is attempted again, and the request fails once the retry limit is exhausted")
    void unresolvableDid_isRetriedUntilTheLimit_thenErrors() {
        when(resolver.resolve(ISSUER_DID)).thenReturn(Result.failure("Error resolving DID: did:web:issuer. HTTP Code was: 503"));
        var holderPid = initiate();

        manager.start();

        await().atMost(MAX_DURATION).untilAsserted(() -> assertThat(store.findById(holderPid).stateAsEnum()).isEqualTo(ERROR));
        // the first attempt, then one retry for each unit of the limit
        verify(resolver, times(RETRY_LIMIT + 1)).resolve(ISSUER_DID);
        verifyNoInteractions(httpClient);
        assertThat(store.findById(holderPid).getErrorDetail()).contains("HTTP Code was: 503");
    }

    @Test
    @DisplayName("a request whose Issuer DID could not be resolved goes through once it can be resolved again")
    void unresolvableDid_recoversOnLaterAttempt() throws IOException {
        when(resolver.resolve(ISSUER_DID))
                .thenReturn(Result.failure("Error resolving DID: did:web:issuer. HTTP Code was: 503"))
                .thenReturn(issuerDidDocument());
        when(httpClient.execute(any(Request.class))).thenReturn(created("https://issuer.com/api/issuance/requests/issuance-process-id"));
        var holderPid = initiate();

        manager.start();

        await().atMost(MAX_DURATION).untilAsserted(() -> assertThat(store.findById(holderPid).stateAsEnum()).isEqualTo(REQUESTED));
        verify(httpClient, times(1)).execute(any(Request.class));
    }

    @Test
    @DisplayName("an Issuer DID that no resolver supports is not attempted again, and nothing is sent")
    void unsupportedDid_isTerminal() {
        when(resolver.isSupported(ISSUER_DID)).thenReturn(false);
        var holderPid = initiate();

        manager.start();

        await().atMost(MAX_DURATION).untilAsserted(() -> assertThat(store.findById(holderPid).stateAsEnum()).isEqualTo(ERROR));
        await().during(Duration.ofMillis(200)).untilAsserted(() -> verify(resolver, never()).resolve(anyString()));
        verifyNoInteractions(httpClient);
        assertThat(store.findById(holderPid).getErrorDetail()).contains("is not supported by any DID resolver");
    }

    private Result<DidDocument> issuerDidDocument() {
        return success(DidDocument.Builder.newInstance()
                .id(ISSUER_DID)
                .service(List.of(new Service("id", "IssuerService", "https://issuer.com/api/issuance")))
                .build());
    }

    private String initiate() {
        var holderPid = UUID.randomUUID().toString();
        assertThat(manager.initiateRequest("test-participant", ISSUER_DID, holderPid,
                List.of(new RequestedCredential("credential-object-id", "DemoCredential", CredentialFormat.VC1_0_JWT.toString()))))
                .isSucceeded();
        return holderPid;
    }

    private Response created(String location) {
        return response(201, "Created", "").newBuilder().header("Location", location).build();
    }

    private Response response(int code, String message, String body) {
        return new Response.Builder()
                .request(new Request.Builder().url("https://issuer.com/api/issuance/credentials").build())
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message(message)
                .body(ResponseBody.create(body, MediaType.parse("application/json")))
                .build();
    }
}
