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

package org.eclipse.edc.issuerservice.issuance.process;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.issuerservice.issuance.events.IssuanceObservableImpl;
import org.eclipse.edc.issuerservice.spi.credentials.CredentialStatusService;
import org.eclipse.edc.issuerservice.spi.issuance.credentialdefinition.store.CredentialDefinitionStore;
import org.eclipse.edc.issuerservice.spi.issuance.delivery.CredentialStorageClient;
import org.eclipse.edc.issuerservice.spi.issuance.events.IssuanceEventListener;
import org.eclipse.edc.issuerservice.spi.issuance.events.IssuanceObservable;
import org.eclipse.edc.issuerservice.spi.issuance.generator.CredentialGenerationRequest;
import org.eclipse.edc.issuerservice.spi.issuance.generator.CredentialGeneratorRegistry;
import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialDefinition;
import org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcess;
import org.eclipse.edc.issuerservice.spi.issuance.process.IssuanceProcessManager;
import org.eclipse.edc.issuerservice.spi.issuance.process.store.IssuanceProcessStore;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.retry.ExponentialWaitStrategy;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.statemachine.retry.EntityRetryProcessConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat.VC1_0_JWT;
import static org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcessStates.APPROVED;
import static org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcessStates.DELIVERED;
import static org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcessStates.ERRORED;
import static org.eclipse.edc.spi.persistence.StateEntityStore.hasState;
import static org.eclipse.edc.spi.persistence.StateEntityStore.isNotPending;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class IssuanceProcessManagerImplTest {

    private final IssuanceProcessStore issuanceProcessStore = mock();
    private final Monitor monitor = mock();
    private final Clock clock = Clock.systemUTC();
    private final CredentialGeneratorRegistry credentialGenerator = mock();
    private final CredentialDefinitionStore credentialDefinitionStore = mock();
    private final CredentialStore credentialStore = mock();
    private final CredentialStorageClient credentialStorageClient = mock();
    private final CredentialStatusService credentialStatusService = mock();
    private final Vault vault = mock();
    private final IssuanceObservable issuanceObservable = new IssuanceObservableImpl();
    private final IssuanceEventListener listener = mock();
    private IssuanceProcessManager issuanceProcessManager;

    @BeforeEach
    void setup() {
        issuanceObservable.registerListener(listener);
        var entityRetryProcessConfiguration = new EntityRetryProcessConfiguration(1, () -> new ExponentialWaitStrategy(0L));
        when(vault.deleteSecret(any())).thenReturn(Result.success());

        issuanceProcessManager = IssuanceProcessManagerImpl.Builder.newInstance()
                .entityRetryProcessConfiguration(entityRetryProcessConfiguration)
                .store(issuanceProcessStore)
                .waitStrategy(() -> 50L)
                .credentialGeneratorRegistry(credentialGenerator)
                .credentialDefinitionStore(credentialDefinitionStore)
                .credentialStore(credentialStore)
                .credentialStorageClient(credentialStorageClient)
                .credentialStatusService(credentialStatusService)
                .observable(issuanceObservable)
                .vault(vault)
                .monitor(monitor)
                .clock(clock)
                .build();
    }


    @DisplayName("IS-DELIV-01: an approved process generates the credentials and dispatches them to the holder")
    @Test
    void approved_shouldGenerateAndDispatchCredentials() {

        var credentialDefinition = CredentialDefinition.Builder.newInstance()
                .id("membership-credential-id")
                .credentialType("MembershipCredential")
                .jsonSchemaUrl("http://example.org/schema")
                .jsonSchema("{}")
                .participantContextId("participantContextId")
                .formatFrom(VC1_0_JWT)
                .build();

        var generationRequests = new CredentialGenerationRequest(credentialDefinition, VC1_0_JWT);

        var credential = new VerifiableCredentialContainer("", VC1_0_JWT, VerifiableCredential.Builder.newInstance()
                .type("MembershipCredential")
                .issuer(new Issuer("did:example:issuer"))
                .issuanceDate(Instant.now())
                .credentialSubject(CredentialSubject.Builder.newInstance()
                        .id("did:example:holder")
                        .claims(Map.of("member", "Alice"))
                        .build())
                .build());

        var process = IssuanceProcess.Builder.newInstance().state(APPROVED.code())
                .holderId("holderId")
                .participantContextId("participantContextId")
                .holderPid("holderPid")
                .credentialFormats(Map.of(credentialDefinition.getId(), VC1_0_JWT))
                .build();

        when(issuanceProcessStore.nextNotLeased(anyInt(), stateIs(APPROVED.code()))).thenReturn(List.of(process)).thenReturn(emptyList());
        when(credentialDefinitionStore.query(any())).thenReturn(StoreResult.success(List.of(credentialDefinition)));
        when(credentialGenerator.generateCredentials("participantContextId", "holderId", List.of(generationRequests), process.getClaims())).thenReturn(Result.success(List.of(credential)));
        when(credentialStore.create(any())).thenReturn(StoreResult.success());
        when(issuanceProcessStore.save(any())).thenReturn(StoreResult.success());
        when(credentialStorageClient.deliverCredentials(process, List.of(credential))).thenReturn(Result.success());
        when(credentialStatusService.addCredential(any(), any())).thenReturn(ServiceResult.success(credential.credential()));
        when(credentialGenerator.signCredential(any(), any(), any())).thenReturn(Result.success(credential));

        issuanceProcessManager.start();

        await().untilAsserted(() -> {
            // raw vc should be null

            var captor = ArgumentCaptor.forClass(VerifiableCredentialResource.class);
            verify(credentialStore).create(captor.capture());
            var cred = captor.getValue();

            assertThat(cred.getState()).isEqualTo(VcStatus.ISSUED.code());
            assertThat(cred.getHolderId()).isEqualTo("did:example:holder");
            assertThat(cred.getIssuerId()).isEqualTo("did:example:issuer");
            assertThat(cred.getVerifiableCredential().rawVc()).isNull();
            assertThat(cred.getVerifiableCredential().format()).isEqualTo(credential.format());
            assertThat(cred.getVerifiableCredential().credential()).isEqualTo(credential.credential());

            verify(issuanceProcessStore).save(argThat(p -> p.getState() == DELIVERED.code()));

            verify(listener).approved(process);
            verify(listener).generated(eq(process), any());
            verify(listener).delivered(eq(process), any());

            // the Holder's access token has served its purpose and must not linger in the vault
            verify(vault).deleteSecret(process.getId());
        });
    }

    @DisplayName("IS-REQ-02: a generation failure after acceptance moves the process to ERRORED, reported as REJECTED")
    @Test
    void approved_shouldTransitionToErrored_whenGenerationErrors() {

        var credentialDefinition = CredentialDefinition.Builder.newInstance().credentialType("MembershipCredential")
                .jsonSchemaUrl("http://example.org/schema")
                .jsonSchema("{}")
                .participantContextId("participantContextId")
                .formatFrom(VC1_0_JWT)
                .build();

        var generationRequests = new CredentialGenerationRequest(credentialDefinition, VC1_0_JWT);

        var process = IssuanceProcess.Builder.newInstance().state(APPROVED.code())
                .holderId("holderId")
                .participantContextId("participantContextId")
                .holderPid("holderPid")
                .credentialFormats(Map.of(credentialDefinition.getCredentialType(), VC1_0_JWT))
                .stateCount(2)
                .build();

        when(issuanceProcessStore.nextNotLeased(anyInt(), stateIs(APPROVED.code()))).thenReturn(List.of(process)).thenReturn(emptyList());
        when(credentialDefinitionStore.query(any())).thenReturn(StoreResult.success(List.of(credentialDefinition)));
        when(credentialGenerator.generateCredentials("participantContextId", "holderId", List.of(generationRequests), process.getClaims())).thenReturn(Result.failure("generation failure"));

        issuanceProcessManager.start();

        await().untilAsserted(() -> {
            verify(issuanceProcessStore).save(argThat(p -> p.getState() == ERRORED.code()));
            verify(listener).approved(process);
        });
    }

    // IS-REQ-02: failure in the "add credentials to status list" step is a FATAL_ERROR: the process transitions to ERRORED immediately, without retry
    @DisplayName("IS-REQ-02: status-list failure transitions the process to ERRORED immediately, without retry")
    @Test
    void approved_shouldTransitionToErroredImmediately_whenStatusListUpdateFails() {
        var credentialDefinition = CredentialDefinition.Builder.newInstance()
                .id("membership-credential-id")
                .credentialType("MembershipCredential")
                .jsonSchemaUrl("http://example.org/schema")
                .jsonSchema("{}")
                .participantContextId("participantContextId")
                .formatFrom(VC1_0_JWT)
                .build();

        var credential = new VerifiableCredentialContainer("", VC1_0_JWT, VerifiableCredential.Builder.newInstance()
                .type("MembershipCredential")
                .issuer(new Issuer("did:example:issuer"))
                .issuanceDate(Instant.now())
                .credentialSubject(CredentialSubject.Builder.newInstance().id("did:example:holder").claims(Map.of("member", "Alice")).build())
                .build());

        // stateCount(1): retries would still be available - a FATAL_ERROR must not use them
        var process = IssuanceProcess.Builder.newInstance().state(APPROVED.code())
                .holderId("holderId")
                .participantContextId("participantContextId")
                .holderPid("holderPid")
                .credentialFormats(Map.of(credentialDefinition.getId(), VC1_0_JWT))
                .stateCount(1)
                .build();

        var savedStates = new CopyOnWriteArrayList<Integer>();
        when(issuanceProcessStore.save(any())).thenAnswer(invocation -> {
            savedStates.add(invocation.getArgument(0, IssuanceProcess.class).getState());
            return StoreResult.success();
        });
        when(issuanceProcessStore.nextNotLeased(anyInt(), stateIs(APPROVED.code()))).thenReturn(List.of(process)).thenReturn(emptyList());
        when(credentialDefinitionStore.query(any())).thenReturn(StoreResult.success(List.of(credentialDefinition)));
        when(credentialGenerator.generateCredentials(any(), any(), any(), any())).thenReturn(Result.success(List.of(credential)));
        // the status-list step fails
        when(credentialStatusService.addCredential(any(), any())).thenReturn(ServiceResult.unexpected("status list failure"));

        issuanceProcessManager.start();

        await().untilAsserted(() -> {
            // a FATAL_ERROR must skip the retry path: the only save is the transition to ERRORED, never back to APPROVED
            assertThat(savedStates).containsExactly(ERRORED.code());
            verify(credentialStorageClient, never()).deliverCredentials(any(), any());
            assertThat(process.getErrorDetail()).contains("status list failure");
            verify(listener).errored(eq(process), any());

            // the process is terminal, so the Holder's access token must not linger in the vault
            verify(vault).deleteSecret(process.getId());
        });
    }

    // IS-DELIV-05: delivery failure -> retry (transitionToApproved, stateCount incremented); once the retry limit is exhausted -> ERRORED with error detail, errored event fired
    @DisplayName("IS-DELIV-05: delivery failure is retried, then transitions to ERRORED once the retry limit is exhausted")
    @Test
    void approved_shouldRetryAndEventuallyError_whenDeliveryFails() {
        var credentialDefinition = CredentialDefinition.Builder.newInstance()
                .id("membership-credential-id")
                .credentialType("MembershipCredential")
                .jsonSchemaUrl("http://example.org/schema")
                .jsonSchema("{}")
                .participantContextId("participantContextId")
                .formatFrom(VC1_0_JWT)
                .build();

        var credential = new VerifiableCredentialContainer("", VC1_0_JWT, VerifiableCredential.Builder.newInstance()
                .type("MembershipCredential")
                .issuer(new Issuer("did:example:issuer"))
                .issuanceDate(Instant.now())
                .credentialSubject(CredentialSubject.Builder.newInstance().id("did:example:holder").claims(Map.of("member", "Alice")).build())
                .build());

        // stateCount(1) is below the retry limit of 1: first failure -> retry, second failure -> final
        var process = IssuanceProcess.Builder.newInstance().state(APPROVED.code())
                .holderId("holderId")
                .participantContextId("participantContextId")
                .holderPid("holderPid")
                .credentialFormats(Map.of(credentialDefinition.getId(), VC1_0_JWT))
                .stateCount(1)
                .build();

        var savedTransitions = new CopyOnWriteArrayList<Map.Entry<Integer, Integer>>();
        when(issuanceProcessStore.save(any())).thenAnswer(invocation -> {
            var saved = invocation.getArgument(0, IssuanceProcess.class);
            savedTransitions.add(Map.entry(saved.getState(), saved.getStateCount()));
            return StoreResult.success();
        });
        when(issuanceProcessStore.nextNotLeased(anyInt(), stateIs(APPROVED.code())))
                .thenReturn(List.of(process))
                .thenReturn(List.of(process))
                .thenReturn(emptyList());
        when(credentialDefinitionStore.query(any())).thenReturn(StoreResult.success(List.of(credentialDefinition)));
        when(credentialGenerator.generateCredentials(any(), any(), any(), any())).thenReturn(Result.success(List.of(credential)));
        when(credentialStatusService.addCredential(any(), any())).thenReturn(ServiceResult.success(credential.credential()));
        when(credentialGenerator.signCredential(any(), any(), any())).thenReturn(Result.success(credential));
        // holder unreachable / non-2xx from the Storage API
        when(credentialStorageClient.deliverCredentials(any(), any())).thenReturn(Result.failure("holder unreachable"));

        issuanceProcessManager.start();

        await().untilAsserted(() -> {
            verify(credentialStorageClient, times(2)).deliverCredentials(any(), any());
            assertThat(savedTransitions).hasSize(2);
            // first failure: retry via transitionToApproved with incremented stateCount
            assertThat(savedTransitions.get(0).getKey()).isEqualTo(APPROVED.code());
            assertThat(savedTransitions.get(0).getValue()).isEqualTo(2);
            // second failure: retry limit exhausted -> ERRORED, errored event fired
            assertThat(savedTransitions.get(1).getKey()).isEqualTo(ERRORED.code());
            // NOTE: deliverCredentials() maps the failure to StatusResult.failure(ERROR_RETRY) WITHOUT the failure
            //  detail, so 'holder unreachable' is not preserved in the errorDetail today
            assertThat(process.getErrorDetail()).isNotNull();
            verify(listener).errored(eq(process), any());
            // RT-03: the holder is told the issuance it was told had been accepted is not coming
            verify(credentialStorageClient).deliverRejection(eq(process), any());
        });
    }

    // RT-03: the rejection notice is best effort - a holder that cannot be reached must not keep the process out of its
    // terminal state, because the failure is still served by the Credential Request Status API
    @DisplayName("IS-REQ-02: a failing rejection notice does not keep the process out of ERRORED")
    @Test
    void error_whenRejectionNoticeFails_stillTransitionsToErrored() {
        var process = IssuanceProcess.Builder.newInstance().state(APPROVED.code())
                .holderId("holderId")
                .participantContextId("participantContextId")
                .holderPid("holderPid")
                .credentialFormats(Map.of("membership-credential-id", VC1_0_JWT))
                .stateCount(1)
                .build();

        var savedTransitions = new CopyOnWriteArrayList<Integer>();
        when(issuanceProcessStore.save(any())).thenAnswer(invocation -> {
            savedTransitions.add(invocation.getArgument(0, IssuanceProcess.class).getState());
            return StoreResult.success();
        });
        when(issuanceProcessStore.nextNotLeased(anyInt(), stateIs(APPROVED.code())))
                .thenReturn(List.of(process))
                .thenReturn(emptyList());
        // no credential definition -> the process fails outright
        when(credentialDefinitionStore.query(any())).thenReturn(StoreResult.generalError("no definitions"));
        when(credentialStorageClient.deliverRejection(any(), any())).thenReturn(Result.failure("holder unreachable"));

        issuanceProcessManager.start();

        await().untilAsserted(() -> {
            verify(credentialStorageClient).deliverRejection(eq(process), any());
            assertThat(savedTransitions).contains(ERRORED.code());
        });
    }

    // IS-DELIV-06: credential store failure AFTER successful delivery -> the process retries and RE-DELIVERS on the next tick (duplicate delivery)
    @DisplayName("IS-DELIV-06: a store failure after successful delivery causes the credentials to be re-delivered on the next iteration")
    @Test
    void approved_shouldRedeliver_whenStoreFailsAfterSuccessfulDelivery() {
        // NOTE: the "Deliver Credentials" step runs BEFORE "Store Credentials"; a store failure sends the process back to
        //  APPROVED, so the next state-machine pass delivers the same credentials to the holder AGAIN. This documents the
        //  duplicate-delivery behavior caused by the delivery-before-persistence ordering (pairs with catalog A3.24).
        var credentialDefinition = CredentialDefinition.Builder.newInstance()
                .id("membership-credential-id")
                .credentialType("MembershipCredential")
                .jsonSchemaUrl("http://example.org/schema")
                .jsonSchema("{}")
                .participantContextId("participantContextId")
                .formatFrom(VC1_0_JWT)
                .build();

        var credential = new VerifiableCredentialContainer("", VC1_0_JWT, VerifiableCredential.Builder.newInstance()
                .type("MembershipCredential")
                .issuer(new Issuer("did:example:issuer"))
                .issuanceDate(Instant.now())
                .credentialSubject(CredentialSubject.Builder.newInstance().id("did:example:holder").claims(Map.of("member", "Alice")).build())
                .build());

        var process = IssuanceProcess.Builder.newInstance().state(APPROVED.code())
                .holderId("holderId")
                .participantContextId("participantContextId")
                .holderPid("holderPid")
                .credentialFormats(Map.of(credentialDefinition.getId(), VC1_0_JWT))
                .stateCount(1)
                .build();

        var savedTransitions = new CopyOnWriteArrayList<Map.Entry<Integer, Integer>>();
        when(issuanceProcessStore.save(any())).thenAnswer(invocation -> {
            var saved = invocation.getArgument(0, IssuanceProcess.class);
            savedTransitions.add(Map.entry(saved.getState(), saved.getStateCount()));
            return StoreResult.success();
        });
        // the process is picked up twice: original pass + retry pass after the store failure
        when(issuanceProcessStore.nextNotLeased(anyInt(), stateIs(APPROVED.code())))
                .thenReturn(List.of(process))
                .thenReturn(List.of(process))
                .thenReturn(emptyList());
        when(credentialDefinitionStore.query(any())).thenReturn(StoreResult.success(List.of(credentialDefinition)));
        when(credentialGenerator.generateCredentials(any(), any(), any(), any())).thenReturn(Result.success(List.of(credential)));
        when(credentialStatusService.addCredential(any(), any())).thenReturn(ServiceResult.success(credential.credential()));
        when(credentialGenerator.signCredential(any(), any(), any())).thenReturn(Result.success(credential));
        // delivery succeeds ...
        when(credentialStorageClient.deliverCredentials(any(), any())).thenReturn(Result.success());
        // ... but persisting the issuance-tracking resource fails afterwards
        when(credentialStore.create(any())).thenReturn(StoreResult.generalError("store failure"));

        issuanceProcessManager.start();

        await().untilAsserted(() -> {
            // the holder received the same credentials twice
            verify(credentialStorageClient, times(2)).deliverCredentials(any(), any());
            assertThat(savedTransitions).hasSize(2);
            // the process was sent back to APPROVED with an incremented stateCount between the two deliveries
            assertThat(savedTransitions.get(0).getKey()).isEqualTo(APPROVED.code());
            assertThat(savedTransitions.get(0).getValue()).isEqualTo(2);
            assertThat(savedTransitions.get(1).getKey()).isEqualTo(ERRORED.code());
        });
    }

    // B3.7: the state machine honors the retry/backoff configuration from the 'edc.issuer.issuance' settings context (batch size, retry limit)
    @DisplayName("B3.7: the state machine honors the configured batch size and retry limit")
    @Test
    void shouldHonorRetryAndBatchConfiguration() {
        // an IssuanceProcessManagerImpl configured the way IssuanceCoreExtension does from the 'edc.issuer.issuance.*' settings
        var batchSize = 5;
        var retryLimit = 2;
        var configuredManager = IssuanceProcessManagerImpl.Builder.newInstance()
                .entityRetryProcessConfiguration(new EntityRetryProcessConfiguration(retryLimit, () -> new ExponentialWaitStrategy(0L)))
                .batchSize(batchSize)
                .store(issuanceProcessStore)
                .waitStrategy(() -> 50L)
                .credentialGeneratorRegistry(credentialGenerator)
                .credentialDefinitionStore(credentialDefinitionStore)
                .credentialStore(credentialStore)
                .credentialStorageClient(credentialStorageClient)
                .credentialStatusService(credentialStatusService)
                .observable(issuanceObservable)
                .vault(vault)
                .monitor(monitor)
                .clock(clock)
                .build();

        var credentialDefinition = CredentialDefinition.Builder.newInstance()
                .id("membership-credential-id")
                .credentialType("MembershipCredential")
                .jsonSchemaUrl("http://example.org/schema")
                .jsonSchema("{}")
                .participantContextId("participantContextId")
                .formatFrom(VC1_0_JWT)
                .build();

        var process = IssuanceProcess.Builder.newInstance().state(APPROVED.code())
                .holderId("holderId")
                .participantContextId("participantContextId")
                .holderPid("holderPid")
                .credentialFormats(Map.of(credentialDefinition.getId(), VC1_0_JWT))
                .stateCount(1)
                .build();

        var savedStates = new CopyOnWriteArrayList<Integer>();
        when(issuanceProcessStore.save(any())).thenAnswer(invocation -> {
            savedStates.add(invocation.getArgument(0, IssuanceProcess.class).getState());
            return StoreResult.success();
        });
        // entities must be fetched with the configured batch size
        when(issuanceProcessStore.nextNotLeased(eq(batchSize), stateIs(APPROVED.code())))
                .thenReturn(List.of(process))
                .thenReturn(List.of(process))
                .thenReturn(List.of(process))
                .thenReturn(emptyList());
        when(credentialDefinitionStore.query(any())).thenReturn(StoreResult.success(List.of(credentialDefinition)));
        // credential generation keeps failing with a retriable error
        when(credentialGenerator.generateCredentials(any(), any(), any(), any())).thenReturn(Result.failure("generation failure"));

        configuredManager.start();

        await().untilAsserted(() -> {
            verify(issuanceProcessStore, atLeastOnce()).nextNotLeased(eq(batchSize), stateIs(APPROVED.code()));
            // the process is retried exactly 'retryLimit' times (saves back to APPROVED) before transitioning to ERRORED
            assertThat(savedStates).containsExactly(APPROVED.code(), APPROVED.code(), ERRORED.code());
        });
    }

    private Criterion[] stateIs(int state) {
        return aryEq(new Criterion[]{ hasState(state), isNotPending() });
    }
}
