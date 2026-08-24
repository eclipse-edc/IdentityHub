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
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.assertj.core.api.Assertions;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestMessage;
import org.eclipse.edc.identityhub.spi.authentication.ParticipantSecureTokenService;
import org.eclipse.edc.identityhub.spi.credential.request.model.HolderCredentialRequest;
import org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState;
import org.eclipse.edc.identityhub.spi.credential.request.model.RequestedCredential;
import org.eclipse.edc.identityhub.spi.credential.request.store.HolderCredentialRequestStore;
import org.eclipse.edc.identityhub.spi.participantcontext.IdentityHubParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.IdentityHubParticipantContext;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.DcpConstants.DCP_SCOPE_V_1_0;
import static org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState.ERROR;
import static org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState.REQUESTED;
import static org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState.REQUESTING;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.persistence.StateEntityStore.hasState;
import static org.eclipse.edc.spi.persistence.StateEntityStore.isNotPending;
import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class CredentialRequestManagerImplTest {

    public static final String ISSUER_DID = "did:web:issuer";
    private final HolderCredentialRequestStore store = mock();
    private final DidResolverRegistry resolver = mock();
    private final TypeTransformerRegistry transformerRegistry = mock();
    private final EdcHttpClient httpClient = mock();
    private final ParticipantSecureTokenService sts = mock();
    private final IdentityHubParticipantContextService participantContextService = mock();
    private final JsonLd jsonLd = mock();
    private final CredentialRequestManagerImpl credentialRequestService = CredentialRequestManagerImpl.Builder.newInstance()
            .store(store)
            .didResolverRegistry(resolver)
            .typeTransformerRegistry(transformerRegistry)
            .jsonLd(jsonLd)
            .httpClient(httpClient)
            .secureTokenService(sts)
            .participantContextService(participantContextService)
            .transactionContext(new NoopTransactionContext())
            .monitor(mock())
            .waitStrategy(() -> 500L)
            .build();

    @BeforeEach
    void setUp() {
        when(transformerRegistry.transform(any(CredentialRequestMessage.class), eq(JsonObject.class)))
                .thenReturn(success(Json.createObjectBuilder().build()));
        when(jsonLd.compact(any(), eq(DCP_SCOPE_V_1_0))).thenReturn(success(Json.createObjectBuilder().build()));
        when(sts.createToken(anyString(), anyMap(), ArgumentMatchers.isNull())).thenReturn(success(TokenRepresentation.Builder.newInstance().build()));
        when(participantContextService.getParticipantContext(anyString())).thenReturn(ServiceResult.success(participantContext()));
        when(store.findById(anyString())).thenReturn(null);
        when(store.save(any())).thenReturn(StoreResult.success());
    }

    private IdentityHubParticipantContext participantContext() {
        return IdentityHubParticipantContext.Builder.newInstance()
                .participantContextId("participantId")
                .did("did:web:test")
                .apiTokenAlias("alias")
                .build();
    }

    private DidDocument didDocument() {
        return DidDocument.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .service(List.of(new Service(UUID.randomUUID().toString(), "IssuerService", "http://issuer.com/issuance")))
                .build();
    }

    private Response response(int code, String message, String body) {
        return new Response.Builder()
                .request(new Request.Builder().url("http://issuer.com/issuance/credentials").build())
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message(message)
                .body(ResponseBody.create(body, MediaType.parse("application/json")))
                .build();
    }

    @Nested
    class Initiate {
        @Test
        void initiateRequest() {
            var result = credentialRequestService.initiateRequest("test-participant", ISSUER_DID, "test-holder-request-id", List.of(new RequestedCredential("test-id", "TestCredential", CredentialFormat.VC1_0_JWT.toString())));
            assertThat(result)
                    .isSucceeded()
                    .isEqualTo("test-holder-request-id");

            verify(store).findById(anyString());
            verify(store).save(any());
            verifyNoMoreInteractions(store, resolver, transformerRegistry, httpClient);
        }

        @Test
        void initiateRequest_whenStorageFailure() {
            doThrow(new EdcPersistenceException("foo")).when(store).save(any());
            var result = credentialRequestService.initiateRequest("test-participant", ISSUER_DID, "test-holder-request-id", List.of(new RequestedCredential("test-id", "TestCredential", CredentialFormat.VC1_0_JWT.toString())));
            assertThat(result)
                    .isFailed()
                    .detail().isEqualTo("foo");

            verify(store).findById(anyString());
            verify(store).save(any());
            verifyNoMoreInteractions(store, resolver, transformerRegistry, httpClient);
        }

        @Test
        void initiateRequest_whenAlreadyExists_expectConflict() {
            var holderPid = "test-holder-request-id";
            when(store.findById(anyString())).thenReturn(HolderCredentialRequest.Builder.newInstance()
                    .issuerDid(ISSUER_DID)
                    .participantContextId("test-participant")
                    .requestedCredential("test-id", "TestCredential", CredentialFormat.VC1_0_JWT.toString())
                    .build());
            var result = credentialRequestService.initiateRequest("test-participant", ISSUER_DID, holderPid, List.of(new RequestedCredential("test-id", "TestCredential", CredentialFormat.VC1_0_JWT.toString())));
            assertThat(result)
                    .isFailed();

            verify(store, never()).save(any());
            verify(store).findById(eq(holderPid));
            verifyNoMoreInteractions(store, resolver, transformerRegistry, httpClient);
        }

    }

    @Nested
    class StateMachine {
        private static final Duration MAX_DURATION = Duration.ofSeconds(5);

        @ParameterizedTest(name = "state = {0}")
        @ValueSource(strings = { "CREATED", "REQUESTING" })
        void processInitial_shouldSendRequest(String stateString) {
            var state = HolderRequestState.valueOf(stateString);
            when(resolver.resolve(eq(ISSUER_DID))).thenReturn(success(didDocument()));
            when(httpClient.execute(any(), (Function<Response, Result<String>>) any()))
                    .thenReturn(success("test-issuance-process-id"));

            var rq = createRequest()
                    .state(state.code())
                    .build();
            when(store.nextNotLeased(anyInt(), stateIs(state.code())))
                    .thenReturn(List.of(rq));

            credentialRequestService.start();

            await().atMost(MAX_DURATION).untilAsserted(() -> {
                var inOrder = inOrder(resolver, store, httpClient, sts, jsonLd);
                inOrder.verify(resolver).resolve(eq(ISSUER_DID));
                inOrder.verify(store).save(argThat(r -> r.getState() == REQUESTING.code()));
                inOrder.verify(sts).createToken(anyString(), anyMap(), ArgumentMatchers.isNull());
                inOrder.verify(jsonLd).compact(any(), eq(DCP_SCOPE_V_1_0));
                inOrder.verify(httpClient).execute(any(), (Function<Response, Result<String>>) any());
                inOrder.verify(store).save(argThat(r -> r.getState() == REQUESTED.code() && r.getIssuerPid() != null));
            });
        }

        @ParameterizedTest(name = "state = {0}")
        @ValueSource(strings = { "CREATED", "REQUESTING" })
        void processInitial_whenDidNotResolvable_shouldTransitionToError(String stateString) {
            var state = HolderRequestState.valueOf(stateString);

            when(resolver.resolve(eq(ISSUER_DID))).thenReturn(Result.failure("foobar"));
            var rq = createRequest()
                    .state(state.code())
                    .build();
            when(store.nextNotLeased(anyInt(), stateIs(state.code())))
                    .thenReturn(List.of(rq))
                    .thenReturn(List.of());

            credentialRequestService.start();

            await().atMost(MAX_DURATION).untilAsserted(() -> {
                var inOrder = inOrder(resolver, store);
                inOrder.verify(resolver).resolve(eq(ISSUER_DID));
                inOrder.verify(store, times(1)).save(argThat(r -> r.getState() == ERROR.code() && r.getErrorDetail().equals("foobar")));
                verifyNoMoreInteractions(resolver, sts, httpClient);
            });
        }

        @ParameterizedTest(name = "state = {0}")
        @ValueSource(strings = { "CREATED", "REQUESTING" })
        void processInitial_whenDidDoesNotContainEndpoint_shouldTransitionToError(String stateString) {
            var state = HolderRequestState.valueOf(stateString);

            when(resolver.resolve(eq(ISSUER_DID))).thenReturn(success(DidDocument.Builder.newInstance()
                    .id(UUID.randomUUID().toString())
                    // missing: endpoint
                    .build()));

            var rq = createRequest()
                    .state(state.code())
                    .build();
            when(store.nextNotLeased(anyInt(), stateIs(state.code())))
                    .thenReturn(List.of(rq))
                    .thenReturn(List.of());

            credentialRequestService.start();

            await().atMost(MAX_DURATION).untilAsserted(() -> {
                var inOrder = inOrder(resolver, store);
                inOrder.verify(resolver).resolve(eq(ISSUER_DID));
                inOrder.verify(store, times(1)).save(argThat(r -> r.getState() == ERROR.code() && r.getErrorDetail().contains("DID Document does not contain any 'IssuerService' endpoint")));
                verifyNoMoreInteractions(resolver, sts, httpClient);
            });
        }

        @ParameterizedTest(name = "state = {0}")
        @ValueSource(strings = { "CREATED", "REQUESTING" })
        void processInitial_whenStsFails_shouldTransitionToError(String stateString) {
            var state = HolderRequestState.valueOf(stateString);

            when(resolver.resolve(eq(ISSUER_DID))).thenReturn(success(didDocument()));
            when(sts.createToken(anyString(), anyMap(), ArgumentMatchers.isNull())).thenReturn(Result.failure("sts-failure"));

            var rq = createRequest()
                    .state(state.code())
                    .build();
            when(store.nextNotLeased(anyInt(), stateIs(state.code())))
                    .thenReturn(List.of(rq))
                    .thenReturn(List.of());

            credentialRequestService.start();

            await().atMost(MAX_DURATION).untilAsserted(() -> {
                var inOrder = inOrder(resolver, store, httpClient, sts);
                inOrder.verify(resolver).resolve(eq(ISSUER_DID));
                inOrder.verify(sts).createToken(anyString(), anyMap(), ArgumentMatchers.isNull());
                inOrder.verify(store, times(1)).save(argThat(r -> r.getState() == ERROR.code() && r.getErrorDetail().equals("sts-failure")));
            });
        }

        @ParameterizedTest(name = "state = {0}")
        @ValueSource(strings = { "CREATED", "REQUESTING" })
        void processInitial_whenIssuerReturnsError_shouldTransitionToError(String stateString) {
            var state = HolderRequestState.valueOf(stateString);
            when(resolver.resolve(eq(ISSUER_DID))).thenReturn(success(didDocument()));
            when(httpClient.execute(any(), (Function<Response, Result<String>>) any())).thenReturn(failure("issuer failure bad request"));

            var rq = createRequest()
                    .state(state.code())
                    .build();
            when(store.nextNotLeased(anyInt(), stateIs(state.code())))
                    .thenReturn(List.of(rq))
                    .thenReturn(List.of());

            credentialRequestService.start();

            await().atMost(MAX_DURATION).untilAsserted(() -> {
                var inOrder = inOrder(resolver, store, httpClient, sts);
                inOrder.verify(resolver).resolve(eq(ISSUER_DID));
                inOrder.verify(sts).createToken(anyString(), anyMap(), ArgumentMatchers.isNull());
                inOrder.verify(httpClient).execute(any(), (Function<Response, Result<String>>) any());
                inOrder.verify(store, times(1)).save(argThat(r -> r.getState() == ERROR.code() && r.getErrorDetail().equals("issuer failure bad request")));
            });
        }

        // A2.7: re-processing a request in REQUESTING (after crash/restart) re-sends the DCP request idempotently: same holderPid reused, an issuer 409/duplicate response must not transition the request to ERROR
        @Test
        @DisplayName("A2.7: re-processing a REQUESTING request reuses the holderPid, and an issuer duplicate response does not transition it to ERROR")
        void processRequesting_whenIssuerReportsDuplicate_shouldNotTransitionToError() {
            // a request that was already sent once (state REQUESTING); the issuer answers with 409/duplicate
            when(resolver.resolve(eq(ISSUER_DID))).thenReturn(success(didDocument()));
            when(httpClient.execute(any(), (Function<Response, Result<String>>) any()))
                    .thenAnswer(i -> i.getArgument(1, Function.class).apply(response(409, "Conflict",
                            "[{\"message\":\"An issuance process with holderPid 'test-request' already exists\"}]")));

            var rq = createRequest()
                    .state(REQUESTING.code())
                    .build();
            when(store.nextNotLeased(anyInt(), stateIs(REQUESTING.code())))
                    .thenReturn(List.of(rq))
                    .thenReturn(List.of());

            credentialRequestService.start();

            // the re-sent request reuses the same holderPid, and the issuer's 409 is treated as "already known", not as an error
            await().atMost(MAX_DURATION).untilAsserted(() -> {
                verify(transformerRegistry).transform(argThat((CredentialRequestMessage m) -> "test-request".equals(m.getHolderPid())), eq(JsonObject.class));
                verify(store, never()).save(argThat(r -> r.getState() == ERROR.code()));
                Assertions.assertThat(rq.getState()).isEqualTo(REQUESTED.code());
                Assertions.assertThat(rq.getErrorDetail()).isNull();
            });
        }

        private HolderCredentialRequest.Builder createRequest() {
            return HolderCredentialRequest.Builder.newInstance()
                    .requestedCredential("foo-credential-id", "FooCredential", CredentialFormat.VC1_0_JWT.toString())
                    .state(REQUESTED.code())
                    .id("test-request")
                    .issuerDid(ISSUER_DID)
                    .participantContextId("test-participant");
        }


        private Criterion[] stateIs(int state) {
            return aryEq(new Criterion[]{ hasState(state), isNotPending() });
        }

    }

}
