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
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialUsage;
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
import org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcessStates;
import org.eclipse.edc.issuerservice.spi.issuance.process.IssuanceProcessManager;
import org.eclipse.edc.issuerservice.spi.issuance.process.store.IssuanceProcessStore;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.retry.ExponentialWaitStrategy;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.statemachine.retry.EntityRetryProcessConfiguration;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat.VC1_0_JWT;
import static org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource.METADATA_ISSUANCE_PROCESS_ID;
import static org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcessStates.APPROVED;
import static org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcessStates.DELIVERED;
import static org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcessStates.DELIVERING;
import static org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcessStates.ERRORED;
import static org.eclipse.edc.spi.persistence.StateEntityStore.hasState;
import static org.eclipse.edc.spi.persistence.StateEntityStore.isNotPending;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class IssuanceProcessManagerImplTest {

    private final IssuanceProcessStore issuanceProcessStore = mock();
    private final Monitor monitor = mock();
    private final CredentialGeneratorRegistry credentialGenerator = mock();
    private final CredentialDefinitionStore credentialDefinitionStore = mock();
    private final CredentialStore credentialStore = mock();
    private final CredentialStorageClient credentialStorageClient = mock();
    private final CredentialStatusService credentialStatusService = mock();
    private final Vault vault = mock();
    private final IssuanceObservable issuanceObservable = new IssuanceObservableImpl();
    private final IssuanceEventListener listener = mock();
    private final RollbackRecordingTransactionContext transactionContext = new RollbackRecordingTransactionContext();
    private IssuanceProcessManager issuanceProcessManager;

    @BeforeEach
    void setup() {
        issuanceObservable.registerListener(listener);
        when(vault.deleteSecret(any())).thenReturn(Result.success());
        // no process is up for processing, and no credentials have been recorded
        when(issuanceProcessStore.nextNotLeased(anyInt(), stateIs(APPROVED.code()))).thenReturn(emptyList());
        when(issuanceProcessStore.nextNotLeased(anyInt(), stateIs(DELIVERING.code()))).thenReturn(emptyList());
        when(credentialStore.query(any())).thenReturn(StoreResult.success(emptyList()));

        issuanceProcessManager = createManager(Clock.systemUTC(), new EntityRetryProcessConfiguration(1, () -> new ExponentialWaitStrategy(0L)), 20);
    }

    @DisplayName("IS-DELIV-01: an approved process generates and records the credentials, and then delivers them to the holder")
    @Test
    void approved_shouldGenerateAndRecordCredentials_thenDeliverThem() {
        var credentialDefinition = membershipCredentialDefinition();
        var credential = membershipCredential();
        var process = approvedProcess(credentialDefinition);
        var generationRequest = new CredentialGenerationRequest(credentialDefinition, VC1_0_JWT);

        storeHolds(process);
        var savedStates = recordSavedStates();
        var records = recordCreatedCredentials();
        when(credentialStore.update(any())).thenReturn(StoreResult.success());
        when(credentialDefinitionStore.query(any())).thenReturn(StoreResult.success(List.of(credentialDefinition)));
        when(credentialGenerator.generateCredentials("participantContextId", "holderId", List.of(generationRequest), process.getClaims())).thenReturn(Result.success(List.of(credential)));
        when(credentialStatusService.addCredential(any(), any())).thenReturn(ServiceResult.success(credential.credential()));
        when(credentialGenerator.signCredential(any(), any(), any())).thenReturn(Result.success(credential));
        when(credentialStorageClient.deliverCredentials(process, List.of(credential))).thenReturn(Result.success());

        issuanceProcessManager.start();

        await().untilAsserted(() -> {
            assertThat(savedStates).containsExactly(DELIVERING.code(), DELIVERED.code());

            assertThat(records).singleElement().satisfies(record -> {
                assertThat(record.getState()).isEqualTo(VcStatus.ISSUED.code());
                assertThat(record.getUsage()).isEqualTo(CredentialUsage.IssuanceTracking);
                assertThat(record.getHolderId()).isEqualTo("did:example:holder");
                assertThat(record.getIssuerId()).isEqualTo("did:example:issuer");
                // the Issuer never keeps the signed credential
                assertThat(record.getVerifiableCredential().rawVc()).isNull();
                assertThat(record.getVerifiableCredential().format()).isEqualTo(credential.format());
                assertThat(record.getVerifiableCredential().credential()).isEqualTo(credential.credential());
                assertThat(record.getMetadata()).containsEntry(METADATA_ISSUANCE_PROCESS_ID, process.getId());
            });

            // recorded before delivery, so that the Issuer keeps track of the credential even if the delivery fails
            var inOrder = inOrder(credentialStore, credentialStorageClient);
            inOrder.verify(credentialStore).create(any());
            inOrder.verify(credentialStorageClient).deliverCredentials(any(), any());

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
        var credentialDefinition = membershipCredentialDefinition();
        // stateCount(2): the retry limit is already reached
        var process = approvedProcess(credentialDefinition).toBuilder().stateCount(2).build();

        storeHolds(process);
        var savedStates = recordSavedStates();
        when(credentialDefinitionStore.query(any())).thenReturn(StoreResult.success(List.of(credentialDefinition)));
        when(credentialGenerator.generateCredentials(any(), any(), any(), any())).thenReturn(Result.failure("generation failure"));

        issuanceProcessManager.start();

        await().untilAsserted(() -> {
            assertThat(savedStates).containsExactly(ERRORED.code());
            verify(listener).approved(process);
        });
    }

    @DisplayName("IS-REQ-02: status-list failure transitions the process to ERRORED immediately, without retry")
    @Test
    void approved_shouldTransitionToErroredImmediately_whenStatusListUpdateFails() {
        var credentialDefinition = membershipCredentialDefinition();
        var credential = membershipCredential();
        // stateCount(1): retries would still be available - a FATAL_ERROR must not use them
        var process = approvedProcess(credentialDefinition);

        storeHolds(process);
        var savedStates = recordSavedStates();
        when(credentialDefinitionStore.query(any())).thenReturn(StoreResult.success(List.of(credentialDefinition)));
        when(credentialGenerator.generateCredentials(any(), any(), any(), any())).thenReturn(Result.success(List.of(credential)));
        when(credentialStatusService.addCredential(any(), any())).thenReturn(ServiceResult.unexpected("status list failure"));

        issuanceProcessManager.start();

        await().untilAsserted(() -> {
            // a FATAL_ERROR must skip the retry path: the only save is the transition to ERRORED, never back to APPROVED
            assertThat(savedStates).containsExactly(ERRORED.code());
            verify(credentialStore, never()).create(any());
            verify(credentialStorageClient, never()).deliverCredentials(any(), any());
            assertThat(process.getErrorDetail()).contains("status list failure");
            verify(listener).errored(eq(process), any());

            // the process is terminal, so the Holder's access token must not linger in the vault
            verify(vault).deleteSecret(process.getId());
        });
        // no credential has been recorded, let alone delivered, so there is nothing to reconcile
        verify(monitor, never()).severe(argThat((String message) -> message.contains("Manual reconciliation")));
    }

    @DisplayName("IS-REQ-02: a failing rejection notice does not keep the process out of ERRORED")
    @Test
    void error_whenRejectionNoticeFails_stillTransitionsToErrored() {
        var process = approvedProcess(membershipCredentialDefinition());

        storeHolds(process);
        var savedStates = recordSavedStates();
        // no credential definition -> the process fails outright
        when(credentialDefinitionStore.query(any())).thenReturn(StoreResult.generalError("no definitions"));
        when(credentialStorageClient.deliverRejection(any(), any())).thenReturn(Result.failure("holder unreachable"));

        issuanceProcessManager.start();

        await().untilAsserted(() -> {
            verify(credentialStorageClient).deliverRejection(eq(process), any());
            assertThat(savedStates).contains(ERRORED.code());
        });
    }

    @DisplayName("IS-DELIV-06: credentials that cannot be recorded are not delivered, and recording them is retried")
    @Test
    void approved_shouldRetryWithoutDelivering_whenRecordingFails() {
        var credentialDefinition = membershipCredentialDefinition();
        var credential = membershipCredential();
        var process = approvedProcess(credentialDefinition);

        storeHolds(process);
        var savedTransitions = recordSavedTransitions();
        when(credentialDefinitionStore.query(any())).thenReturn(StoreResult.success(List.of(credentialDefinition)));
        when(credentialGenerator.generateCredentials(any(), any(), any(), any())).thenReturn(Result.success(List.of(credential)));
        when(credentialStatusService.addCredential(any(), any())).thenReturn(ServiceResult.success(credential.credential()));
        when(credentialStore.create(any())).thenReturn(StoreResult.generalError("store failure"));

        issuanceProcessManager.start();

        await().untilAsserted(() -> {
            assertThat(savedTransitions).extracting(Map.Entry::getKey).containsExactly(APPROVED.code(), ERRORED.code());
            assertThat(savedTransitions.get(0).getValue()).isEqualTo(2);
            // a credential the Issuer has no record of must never reach the Holder
            verify(credentialStorageClient, never()).deliverCredentials(any(), any());
        });
    }

    @DisplayName("IS-DELIV-06: an exception while recording the credentials is retried, instead of ending the process")
    @Test
    void approved_shouldRetry_whenRecordingThrows() {
        var credentialDefinition = membershipCredentialDefinition();
        var credential = membershipCredential();
        var process = approvedProcess(credentialDefinition);

        storeHolds(process);
        var savedTransitions = recordSavedTransitions();
        when(credentialDefinitionStore.query(any())).thenReturn(StoreResult.success(List.of(credentialDefinition)));
        when(credentialGenerator.generateCredentials(any(), any(), any(), any())).thenReturn(Result.success(List.of(credential)));
        when(credentialStatusService.addCredential(any(), any())).thenReturn(ServiceResult.success(credential.credential()));
        when(credentialStore.create(any())).thenThrow(new EdcPersistenceException("database unavailable"));

        issuanceProcessManager.start();

        await().untilAsserted(() -> {
            // retried first, and only given up once the retry limit is reached
            assertThat(savedTransitions).extracting(Map.Entry::getKey).containsExactly(APPROVED.code(), ERRORED.code());
            verify(credentialStorageClient, never()).deliverCredentials(any(), any());
        });
    }

    @DisplayName("IS-DELIV-06: the credentials of a process are recorded all together, or not at all")
    @Test
    void approved_shouldRecordAllCredentialsOrNone() {
        var credentialDefinition = membershipCredentialDefinition();
        var process = approvedProcess(credentialDefinition);

        storeHolds(process);
        var savedTransitions = recordSavedTransitions();
        when(credentialDefinitionStore.query(any())).thenReturn(StoreResult.success(List.of(credentialDefinition)));
        when(credentialGenerator.generateCredentials(any(), any(), any(), any())).thenReturn(Result.success(List.of(membershipCredential(), membershipCredential())));
        when(credentialStatusService.addCredential(any(), any())).thenAnswer(i -> ServiceResult.success(i.getArgument(1)));
        when(credentialStore.create(any()))
                .thenReturn(StoreResult.success())
                .thenReturn(StoreResult.generalError("store failure"));

        issuanceProcessManager.start();

        await().untilAsserted(() -> {
            assertThat(savedTransitions).extracting(Map.Entry::getKey).contains(APPROVED.code());
            // the first credential was already written, which must not be committed without the second one
            assertThat(transactionContext.rolledBack()).isTrue();
            verify(credentialStorageClient, never()).deliverCredentials(any(), any());
        });
    }

    @DisplayName("IS-DELIV-07: credentials an earlier attempt has recorded are not generated again")
    @Test
    void approved_shouldNotGenerateAgain_whenCredentialsAreRecorded() {
        var process = approvedProcess(membershipCredentialDefinition());

        storeHolds(process);
        var savedStates = recordSavedStates();
        // recorded by an attempt that was interrupted before the process was saved
        when(credentialStore.query(any())).thenReturn(StoreResult.success(List.of(recordOf(process, membershipCredential().credential()))));
        when(credentialStore.update(any())).thenReturn(StoreResult.success());
        when(credentialGenerator.signCredential(any(), any(), any())).thenAnswer(i -> Result.success(new VerifiableCredentialContainer("signed", VC1_0_JWT, i.getArgument(1))));
        when(credentialStorageClient.deliverCredentials(any(), any())).thenReturn(Result.success());

        issuanceProcessManager.start();

        await().untilAsserted(() -> {
            assertThat(savedStates).containsExactly(DELIVERING.code(), DELIVERED.code());
            verify(credentialStore, atLeastOnce()).query(argThat(query -> query.getFilterExpression().containsAll(List.of(
                    criterion("usage", "=", CredentialUsage.IssuanceTracking.toString()),
                    criterion("metadata." + METADATA_ISSUANCE_PROCESS_ID, "=", process.getId())))));
            // nothing new is generated, and no further status list index is taken
            verify(credentialGenerator, never()).generateCredentials(any(), any(), any(), any());
            verify(credentialStatusService, never()).addCredential(any(), any());
            verify(credentialStore, never()).create(any());
        });
    }

    @DisplayName("IS-DELIV-07: the recorded credentials are delivered with a validity that starts when they are signed")
    @Test
    void delivering_shouldDeliverRecordedCredentials_validFromSigning() {
        var signingTime = Instant.parse("2026-03-01T12:00:00Z");
        var manager = createManager(Clock.fixed(signingTime, ZoneOffset.UTC), new EntityRetryProcessConfiguration(1, () -> new ExponentialWaitStrategy(0L)), 20);
        var process = deliveringProcess();
        // generated two months before it is delivered, valid for a year
        var recordedCredential = membershipCredential().credential().toBuilder()
                .issuanceDate(Instant.parse("2026-01-01T12:00:00Z"))
                .expirationDate(Instant.parse("2027-01-01T12:00:00Z"))
                .build();
        var signedCredential = new VerifiableCredentialContainer("signed", VC1_0_JWT, recordedCredential);

        storeHolds(process);
        var savedStates = recordSavedStates();
        when(credentialStore.query(any())).thenReturn(StoreResult.success(List.of(recordOf(process, recordedCredential))));
        when(credentialStore.update(any())).thenReturn(StoreResult.success());
        when(credentialGenerator.signCredential(any(), any(), any())).thenReturn(Result.success(signedCredential));
        when(credentialStorageClient.deliverCredentials(any(), any())).thenReturn(Result.success());

        manager.start();

        await().untilAsserted(() -> {
            assertThat(savedStates).containsExactly(DELIVERED.code());
            verify(credentialStorageClient).deliverCredentials(process, List.of(signedCredential));
        });

        // the Holder gets the full year, starting when the credential is signed, and the record says so before delivery
        var updatedRecord = ArgumentCaptor.forClass(VerifiableCredentialResource.class);
        var inOrder = inOrder(credentialStore, credentialGenerator, credentialStorageClient);
        inOrder.verify(credentialStore).update(updatedRecord.capture());
        inOrder.verify(credentialGenerator).signCredential(eq("participantContextId"), argThat(this::isValidForOneYearFromSigning), eq(VC1_0_JWT));
        inOrder.verify(credentialStorageClient).deliverCredentials(any(), any());
        assertThat(updatedRecord.getValue().getVerifiableCredential().credential()).satisfies(credential -> {
            assertThat(credential.getIssuanceDate()).isEqualTo(signingTime);
            assertThat(credential.getExpirationDate()).isEqualTo(signingTime.plus(Duration.ofDays(365)));
        });
        // the Issuer never keeps the signed credential
        assertThat(updatedRecord.getValue().getVerifiableCredential().rawVc()).isNull();
    }

    @DisplayName("IS-DELIV-05: delivery failure is retried with the same credentials, then transitions to ERRORED once the retry limit is exhausted")
    @Test
    void delivering_shouldRetryAndEventuallyError_whenDeliveryFails() {
        var process = deliveringProcess();
        var recordedCredential = membershipCredential().credential();
        var record = recordOf(process, recordedCredential);

        storeHolds(process);
        var savedTransitions = recordSavedTransitions();
        when(credentialStore.query(any())).thenReturn(StoreResult.success(List.of(record)));
        when(credentialStore.update(any())).thenReturn(StoreResult.success());
        when(credentialGenerator.signCredential(any(), any(), any())).thenAnswer(i -> Result.success(new VerifiableCredentialContainer("signed", VC1_0_JWT, i.getArgument(1))));
        // holder unreachable / non-2xx from the Storage API
        when(credentialStorageClient.deliverCredentials(any(), any())).thenReturn(Result.failure("holder unreachable"));

        issuanceProcessManager.start();

        await().untilAsserted(() -> {
            // first failure: retry with incremented stateCount, second failure: retry limit exhausted
            assertThat(savedTransitions).containsExactly(Map.entry(DELIVERING.code(), 2), Map.entry(ERRORED.code(), 1));
            assertThat(process.getErrorDetail()).isNotNull();
            verify(listener).errored(eq(process), any());
            // RT-03: the holder is told the issuance it was told had been accepted is not coming
            verify(credentialStorageClient).deliverRejection(eq(process), any());
        });

        // the credential stays ISSUED, although it may have reached the Holder or not, so its record needs reconciliation
        verify(monitor).severe(argThat((String message) -> message.contains(record.getId()) && message.contains("Manual reconciliation is needed")));
        verify(credentialStatusService, never()).revokeCredential(any());

        // both attempts delivered the very same credential, nothing new was generated
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<VerifiableCredentialContainer>> delivered = ArgumentCaptor.forClass(Collection.class);
        verify(credentialStorageClient, times(2)).deliverCredentials(any(), delivered.capture());
        assertThat(delivered.getAllValues()).allSatisfy(credentials -> assertThat(credentials)
                .extracting(container -> container.credential().getId())
                .containsExactly(recordedCredential.getId()));
        verify(credentialGenerator, never()).generateCredentials(any(), any(), any(), any());
    }

    @DisplayName("IS-DELIV-07: a delivering process without recorded credentials transitions to ERRORED")
    @Test
    void delivering_shouldTransitionToErrored_whenNoCredentialsAreRecorded() {
        var process = deliveringProcess();

        storeHolds(process);
        var savedStates = recordSavedStates();

        issuanceProcessManager.start();

        await().untilAsserted(() -> {
            assertThat(savedStates).containsExactly(ERRORED.code());
            verify(credentialStorageClient, never()).deliverCredentials(any(), any());
        });
    }

    @DisplayName("IS-DELIV-07: credentials whose records cannot be updated are not delivered, and the delivery is retried")
    @Test
    void delivering_shouldRetryWithoutDelivering_whenRecordsCannotBeUpdated() {
        var process = deliveringProcess();

        storeHolds(process);
        var savedTransitions = recordSavedTransitions();
        when(credentialStore.query(any())).thenReturn(StoreResult.success(List.of(recordOf(process, membershipCredential().credential()))));
        when(credentialStore.update(any())).thenReturn(StoreResult.generalError("store failure"));

        issuanceProcessManager.start();

        await().untilAsserted(() -> {
            assertThat(savedTransitions).extracting(Map.Entry::getKey).containsExactly(DELIVERING.code(), ERRORED.code());
            verify(credentialGenerator, never()).signCredential(any(), any(), any());
            verify(credentialStorageClient, never()).deliverCredentials(any(), any());
        });
    }

    @DisplayName("IS-DELIV-08: the Holder's access token is kept while the process could not be saved as delivered")
    @Test
    void delivering_shouldKeepAccessToken_whenDeliveredStateCannotBeSaved() {
        var process = deliveringProcess();

        // e.g. another runtime has taken over the process in the meantime
        when(issuanceProcessStore.nextNotLeased(anyInt(), stateIs(DELIVERING.code()))).thenReturn(List.of(process)).thenReturn(emptyList());
        when(issuanceProcessStore.save(any())).thenReturn(StoreResult.alreadyLeased("leased by another runtime"));
        when(credentialStore.query(any())).thenReturn(StoreResult.success(List.of(recordOf(process, membershipCredential().credential()))));
        when(credentialStore.update(any())).thenReturn(StoreResult.success());
        when(credentialGenerator.signCredential(any(), any(), any())).thenAnswer(i -> Result.success(new VerifiableCredentialContainer("signed", VC1_0_JWT, i.getArgument(1))));
        when(credentialStorageClient.deliverCredentials(any(), any())).thenReturn(Result.success());

        issuanceProcessManager.start();

        await().untilAsserted(() -> {
            verify(issuanceProcessStore).save(argThat(p -> p.getState() == DELIVERED.code()));
            verify(monitor).warning(argThat((String message) -> message.contains("could not be saved")));
        });
        // whoever delivers the process again needs the token
        verify(vault, never()).deleteSecret(any());
    }

    @DisplayName("B3.7: the state machine honors the configured batch size and retry limit")
    @Test
    void shouldHonorRetryAndBatchConfiguration() {
        // an IssuanceProcessManagerImpl configured the way IssuanceCoreExtension does from the 'edc.issuer.issuance.*' settings
        var batchSize = 5;
        var retryLimit = 2;
        var configuredManager = createManager(Clock.systemUTC(), new EntityRetryProcessConfiguration(retryLimit, () -> new ExponentialWaitStrategy(0L)), batchSize);

        var credentialDefinition = membershipCredentialDefinition();
        var process = approvedProcess(credentialDefinition);

        var savedStates = recordSavedStates();
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

    private IssuanceProcessManager createManager(Clock clock, EntityRetryProcessConfiguration entityRetryProcessConfiguration, int batchSize) {
        return IssuanceProcessManagerImpl.Builder.newInstance()
                .entityRetryProcessConfiguration(entityRetryProcessConfiguration)
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
                .transactionContext(transactionContext)
                .monitor(monitor)
                .clock(clock)
                .build();
    }

    /**
     * Lets the state machine find the process whenever it is in a state that is processed, like a store would.
     */
    private void storeHolds(IssuanceProcess process) {
        for (var state : List.of(APPROVED, DELIVERING)) {
            when(issuanceProcessStore.nextNotLeased(anyInt(), stateIs(state.code())))
                    .thenAnswer(i -> process.getState() == state.code() ? List.of(process) : emptyList());
        }
    }

    private List<Integer> recordSavedStates() {
        var savedStates = new CopyOnWriteArrayList<Integer>();
        when(issuanceProcessStore.save(any())).thenAnswer(invocation -> {
            savedStates.add(invocation.getArgument(0, IssuanceProcess.class).getState());
            return StoreResult.success();
        });
        return savedStates;
    }

    private List<Map.Entry<Integer, Integer>> recordSavedTransitions() {
        var savedTransitions = new CopyOnWriteArrayList<Map.Entry<Integer, Integer>>();
        when(issuanceProcessStore.save(any())).thenAnswer(invocation -> {
            var saved = invocation.getArgument(0, IssuanceProcess.class);
            savedTransitions.add(Map.entry(saved.getState(), saved.getStateCount()));
            return StoreResult.success();
        });
        return savedTransitions;
    }

    /**
     * Records the credentials the manager creates, and finds them again when it looks for them, like a store would.
     */
    private List<VerifiableCredentialResource> recordCreatedCredentials() {
        var records = new CopyOnWriteArrayList<VerifiableCredentialResource>();
        when(credentialStore.create(any())).thenAnswer(invocation -> {
            records.add(invocation.getArgument(0));
            return StoreResult.success();
        });
        when(credentialStore.query(any())).thenAnswer(invocation -> StoreResult.success(List.copyOf(records)));
        return records;
    }

    private boolean isValidForOneYearFromSigning(VerifiableCredential credential) {
        return credential.getIssuanceDate().equals(Instant.parse("2026-03-01T12:00:00Z")) &&
                credential.getExpirationDate().equals(Instant.parse("2027-03-01T12:00:00Z"));
    }

    private CredentialDefinition membershipCredentialDefinition() {
        return CredentialDefinition.Builder.newInstance()
                .id("membership-credential-id")
                .credentialType("MembershipCredential")
                .jsonSchemaUrl("http://example.org/schema")
                .jsonSchema("{}")
                .participantContextId("participantContextId")
                .formatFrom(VC1_0_JWT)
                .build();
    }

    private VerifiableCredentialContainer membershipCredential() {
        return new VerifiableCredentialContainer("", VC1_0_JWT, VerifiableCredential.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .type("MembershipCredential")
                .issuer(new Issuer("did:example:issuer"))
                .issuanceDate(Instant.now())
                .credentialSubject(CredentialSubject.Builder.newInstance().id("did:example:holder").claims(Map.of("member", "Alice")).build())
                .build());
    }

    private VerifiableCredentialResource recordOf(IssuanceProcess process, VerifiableCredential credential) {
        return VerifiableCredentialResource.Builder.newIssuanceTracker()
                .issuerId("did:example:issuer")
                .holderId("did:example:holder")
                .participantContextId(process.getParticipantContextId())
                .state(VcStatus.ISSUED)
                .metadata(METADATA_ISSUANCE_PROCESS_ID, process.getId())
                .credential(new VerifiableCredentialContainer(null, VC1_0_JWT, credential))
                .build();
    }

    // stateCount(1) is below the retry limit of 1: first failure -> retry, second failure -> final
    private IssuanceProcess approvedProcess(CredentialDefinition credentialDefinition) {
        return processIn(APPROVED).credentialFormats(Map.of(credentialDefinition.getId(), VC1_0_JWT)).build();
    }

    private IssuanceProcess deliveringProcess() {
        return processIn(DELIVERING).build();
    }

    private IssuanceProcess.Builder processIn(IssuanceProcessStates state) {
        return IssuanceProcess.Builder.newInstance().state(state.code())
                .holderId("holderId")
                .participantContextId("participantContextId")
                .holderPid("holderPid")
                .stateCount(1);
    }

    private Criterion[] stateIs(int state) {
        return aryEq(new Criterion[]{ hasState(state), isNotPending() });
    }

    /**
     * Records whether a transaction was rolled back, i.e. whether its block threw an exception.
     */
    private static class RollbackRecordingTransactionContext extends NoopTransactionContext {
        private volatile boolean rolledBack;

        @Override
        public void execute(TransactionBlock block) {
            try {
                super.execute(block);
            } catch (RuntimeException e) {
                rolledBack = true;
                throw e;
            }
        }

        @Override
        public <T> T execute(ResultTransactionBlock<T> block) {
            try {
                return super.execute(block);
            } catch (RuntimeException e) {
                rolledBack = true;
                throw e;
            }
        }

        boolean rolledBack() {
            return rolledBack;
        }
    }
}
