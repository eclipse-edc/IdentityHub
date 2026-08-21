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
import org.eclipse.edc.statemachine.retry.EntityRetryProcessConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

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
    private final IssuanceObservable issuanceObservable = new IssuanceObservableImpl();
    private final IssuanceEventListener listener = mock();
    private IssuanceProcessManager issuanceProcessManager;

    @BeforeEach
    void setup() {
        issuanceObservable.registerListener(listener);
        var entityRetryProcessConfiguration = new EntityRetryProcessConfiguration(1, () -> new ExponentialWaitStrategy(0L));
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
                .monitor(monitor)
                .clock(clock)
                .build();
    }


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
        });
    }

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

    // B3.3: failure in the "add credentials to status list" step is a FATAL_ERROR: the process transitions to ERRORED immediately, without retry
    @Disabled("TODO: implement (catalog B3.3)")
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

        var process = IssuanceProcess.Builder.newInstance().state(APPROVED.code())
                .holderId("holderId")
                .participantContextId("participantContextId")
                .holderPid("holderPid")
                .credentialFormats(Map.of(credentialDefinition.getId(), VC1_0_JWT))
                .build();

        when(issuanceProcessStore.nextNotLeased(anyInt(), stateIs(APPROVED.code()))).thenReturn(List.of(process)).thenReturn(emptyList());
        when(credentialDefinitionStore.query(any())).thenReturn(StoreResult.success(List.of(credentialDefinition)));
        when(credentialGenerator.generateCredentials(any(), any(), any(), any())).thenReturn(Result.success(List.of(credential)));
        when(issuanceProcessStore.save(any())).thenReturn(StoreResult.success());
        // the status-list step fails
        when(credentialStatusService.addCredential(any(), any())).thenReturn(ServiceResult.unexpected("status list failure"));

        issuanceProcessManager.start();

        await().untilAsserted(() -> {
            verify(issuanceProcessStore).save(argThat(p -> p.getState() == ERRORED.code()));
            // a FATAL_ERROR must skip the retry path: transitionToApproved() is never called
            verify(issuanceProcessStore, never()).save(argThat(p -> p.getState() == APPROVED.code()));
            verify(credentialStorageClient, never()).deliverCredentials(any(), any());
            // TODO: assert the errorDetail contains "Failed to add credential to status list"
            // TODO: verify(listener).errored(eq(process), any());
        });
    }

    // B3.4: delivery failure -> retry (transitionToApproved, stateCount incremented); once the retry limit is exhausted -> ERRORED with error detail, errored event fired
    @Disabled("TODO: implement (catalog B3.4)")
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

        // stateCount(2) exceeds the configured retry limit of 1, so the failure becomes final
        var process = IssuanceProcess.Builder.newInstance().state(APPROVED.code())
                .holderId("holderId")
                .participantContextId("participantContextId")
                .holderPid("holderPid")
                .credentialFormats(Map.of(credentialDefinition.getId(), VC1_0_JWT))
                .stateCount(2)
                .build();

        when(issuanceProcessStore.nextNotLeased(anyInt(), stateIs(APPROVED.code()))).thenReturn(List.of(process)).thenReturn(emptyList());
        when(credentialDefinitionStore.query(any())).thenReturn(StoreResult.success(List.of(credentialDefinition)));
        when(credentialGenerator.generateCredentials(any(), any(), any(), any())).thenReturn(Result.success(List.of(credential)));
        when(credentialStatusService.addCredential(any(), any())).thenReturn(ServiceResult.success(credential.credential()));
        when(credentialGenerator.signCredential(any(), any(), any())).thenReturn(Result.success(credential));
        when(issuanceProcessStore.save(any())).thenReturn(StoreResult.success());
        // holder unreachable / non-2xx from the Storage API
        when(credentialStorageClient.deliverCredentials(any(), any())).thenReturn(Result.failure("holder unreachable"));

        issuanceProcessManager.start();

        await().untilAsserted(() -> {
            verify(issuanceProcessStore).save(argThat(p -> p.getState() == ERRORED.code()));
            // TODO: assert the errorDetail carries the delivery failure - NOTE: deliverCredentials() currently maps the
            //  failure to StatusResult.failure(ERROR_RETRY) WITHOUT the failure detail, so the detail is lost today
            // TODO: verify(listener).errored(eq(process), any());
        });
        // TODO: add a variant with stateCount below the retry limit asserting the process is saved back in APPROVED
        //  with an incremented stateCount (transitionToApproved), i.e. the delivery is retried on the next tick
    }

    // B3.5: credential store failure AFTER successful delivery -> the process retries and RE-DELIVERS on the next tick (duplicate delivery)
    @Disabled("TODO: implement (catalog B3.5)")
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
                .build();

        // the process is picked up twice: original pass + retry pass after the store failure
        when(issuanceProcessStore.nextNotLeased(anyInt(), stateIs(APPROVED.code())))
                .thenReturn(List.of(process))
                .thenReturn(List.of(process))
                .thenReturn(emptyList());
        when(credentialDefinitionStore.query(any())).thenReturn(StoreResult.success(List.of(credentialDefinition)));
        when(credentialGenerator.generateCredentials(any(), any(), any(), any())).thenReturn(Result.success(List.of(credential)));
        when(credentialStatusService.addCredential(any(), any())).thenReturn(ServiceResult.success(credential.credential()));
        when(credentialGenerator.signCredential(any(), any(), any())).thenReturn(Result.success(credential));
        when(issuanceProcessStore.save(any())).thenReturn(StoreResult.success());
        // delivery succeeds ...
        when(credentialStorageClient.deliverCredentials(any(), any())).thenReturn(Result.success());
        // ... but persisting the issuance-tracking resource fails afterwards
        when(credentialStore.create(any())).thenReturn(StoreResult.generalError("store failure"));

        issuanceProcessManager.start();

        await().untilAsserted(() -> {
            // the holder received the same credentials twice
            verify(credentialStorageClient, times(2)).deliverCredentials(any(), any());
            // TODO: assert the process was saved back in APPROVED with an incremented stateCount between the two deliveries
        });
    }

    // B3.7: the state machine honors the retry/backoff configuration from the 'edc.issuer.issuance' settings context (batch size, retry limit)
    @Disabled("TODO: implement (catalog B3.7)")
    @Test
    void shouldHonorRetryAndBatchConfiguration() {
        // TODO: build an IssuanceProcessManagerImpl with an explicit batchSize and EntityRetryProcessConfiguration
        //  (retry limit, backoff strategy) mirroring the values of the 'edc.issuer.issuance.*' settings
        // TODO: assert store.nextNotLeased() is invoked with the configured batch size
        // TODO: assert a failing process is retried exactly 'retryLimit' times before transitioning to ERRORED
        // TODO: assert the configured wait/backoff strategy is applied between state-machine iterations
    }

    private Criterion[] stateIs(int state) {
        return aryEq(new Criterion[]{ hasState(state), isNotPending() });
    }
}
