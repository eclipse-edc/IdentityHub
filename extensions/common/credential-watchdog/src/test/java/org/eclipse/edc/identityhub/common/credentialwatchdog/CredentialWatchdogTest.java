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

package org.eclipse.edc.identityhub.common.credentialwatchdog;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.identityhub.spi.credential.request.model.HolderCredentialRequest;
import org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.CredentialRequestManager;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.CredentialStatusCheckService;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat.VC1_0_JWT;
import static org.eclipse.edc.identityhub.common.credentialwatchdog.CredentialWatchdog.ALLOWED_STATES;
import static org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus.ISSUED;
import static org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus.REQUESTED;
import static org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus.REVOKED;
import static org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource.METADATA_RENEWAL_ERROR;
import static org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource.METADATA_RENEWAL_REQUEST_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class CredentialWatchdogTest {

    private static final long GRACE_PERIOD = 20;
    private final CredentialStore credentialStore = mock();
    private final CredentialStatusCheckService credentialStatusCheckService = mock();
    private final CredentialRequestManager credentialRequestManager = mock();
    private final Monitor monitor = mock();
    private final CredentialWatchdog watchdog = new CredentialWatchdog(credentialStore, credentialStatusCheckService, monitor, new NoopTransactionContext(),
            Duration.ofSeconds(GRACE_PERIOD), credentialRequestManager);

    @BeforeEach
    void setUp() {
        when(credentialStatusCheckService.checkStatus(any())).thenReturn(Result.success(VcStatus.ISSUED));
        when(credentialRequestManager.initiateRequest(anyString(), anyString(), anyString(), anyList())).thenAnswer(i -> ServiceResult.success(i.getArgument(2)));
    }

    @Test
    void run_whenNonRequiresUpdate() {
        when(credentialStore.query(any()))
                .thenReturn(StoreResult.success(List.of(createCredentialBuilder().build(), createCredentialBuilder().build())));

        watchdog.run();

        // verify the store was queried with the proper filter expressions
        verify(credentialStore).query(argThat(querySpec ->
                querySpec.getFilterExpression().size() == 2 &&
                        querySpec.getFilterExpression().get(0).toString().equals("state in " + ALLOWED_STATES)));
    }

    @Test
    void run_whenNoCredentials() {
        when(credentialStore.query(any())).thenReturn(StoreResult.success(Collections.emptyList()));

        watchdog.run();

        verifyNoInteractions(credentialStatusCheckService);
        verify(credentialStore, never()).update(any());
    }

    @Test
    void run_whenRequiresUpdate() {
        var cred1 = createCredentialBuilder().build();
        var cred2 = createCredentialBuilder().build();

        when(credentialStore.query(any()))
                .thenReturn(StoreResult.success(List.of(cred1, cred2)));
        when(credentialStatusCheckService.checkStatus(any()))
                .thenReturn(Result.success(REVOKED))
                .thenReturn(Result.success(ISSUED));

        watchdog.run();

        verify(credentialStore).query(any());
        verify(credentialStore).update(argThat(vcr -> vcr.getId().equals(cred1.getId())));
        verifyNoMoreInteractions(credentialStore);
        verify(credentialStatusCheckService, times(2)).checkStatus(any());
        verifyNoMoreInteractions(credentialStatusCheckService);
    }

    @Test
    void run_whenCheckServiceFails_shouldTransitionError() {
        when(credentialStore.query(any()))
                .thenReturn(StoreResult.success(List.of(createCredentialBuilder().build(), createCredentialBuilder().build())));

        when(credentialStatusCheckService.checkStatus(any()))
                .thenReturn(Result.failure("test failure"))
                .thenReturn(Result.success(ISSUED));
        watchdog.run();

        verify(credentialStore).query(any());
        verify(credentialStore).update(argThat(vcr -> vcr.getStateAsEnum() == VcStatus.ERROR));
        verifyNoMoreInteractions(credentialStore);
        verify(credentialStatusCheckService, times(2)).checkStatus(any());
    }

    @Test
    void run_whenCredentialInError_andCheckSucceeds_shouldRecover() {
        var cred = createCredentialBuilder().state(VcStatus.ERROR).build();
        when(credentialStore.query(any())).thenReturn(StoreResult.success(List.of(cred)));

        watchdog.run();

        verify(credentialStore).query(argThat(querySpec ->
                querySpec.getFilterExpression().get(0).getOperandRight() instanceof Collection<?> states && states.contains(VcStatus.ERROR.code())));
        verify(credentialStatusCheckService).checkStatus(cred);
        verify(credentialStore).update(argThat(vcr -> vcr.getId().equals(cred.getId()) && vcr.getStateAsEnum() == ISSUED));
    }

    @Test
    void run_whenCredentialInError_andCheckFailsAgain_shouldNotUpdate() {
        var cred = createCredentialBuilder().state(VcStatus.ERROR).build();
        when(credentialStore.query(any())).thenReturn(StoreResult.success(List.of(cred)));
        when(credentialStatusCheckService.checkStatus(any())).thenReturn(Result.failure("status list unreachable"));

        watchdog.run();

        verify(credentialStatusCheckService).checkStatus(cred);
        verify(credentialStore, never()).update(any());
        assertThat(cred.getStateAsEnum()).isEqualTo(VcStatus.ERROR);
    }

    @Test
    void run_whenCredentialInErrorIsExpiring_shouldInitiateRenewal() {
        var cred = createCredentialBuilder()
                .state(VcStatus.ERROR)
                .metadata(VerifiableCredentialResource.METADATA_CREDENTIAL_OBJECT_ID, "cred-object-id")
                .credential(new VerifiableCredentialContainer("raw-vc-content", VC1_0_JWT, createVerifiableCredential()
                        .expirationDate(Instant.now().plusSeconds(GRACE_PERIOD / 2))
                        .build()))
                .build();
        when(credentialStore.query(any())).thenReturn(StoreResult.success(List.of(cred)));
        when(credentialStore.update(any())).thenReturn(StoreResult.success());
        when(credentialStatusCheckService.checkStatus(any())).thenReturn(Result.failure("status list unreachable"));

        watchdog.run();

        // the status is still unknown, but the credential is about to expire, so it is renewed nonetheless
        verify(credentialRequestManager).initiateRequest(eq(cred.getParticipantContextId()), eq(cred.getIssuerId()), anyString(), argThat(list ->
                list.size() == 1 && list.get(0).id().equals("cred-object-id")));
        verify(credentialStore).update(argThat(vc -> vc.getStateAsEnum() == REQUESTED && vc.getMetadata().get(METADATA_RENEWAL_REQUEST_ID) != null));
    }

    @Test
    void run_whenCredentialIsExpiring_shouldInitiateRenewal() {
        var cred = createCredentialBuilder()
                .metadata("credentialObjectId", "cred-object-id")
                .credential(new VerifiableCredentialContainer("raw-vc-content", VC1_0_JWT, createVerifiableCredential()
                        .expirationDate(Instant.now().plusSeconds(GRACE_PERIOD / 2))
                        .build()))
                .build();
        when(credentialStore.query(any())).thenReturn(StoreResult.success(List.of(cred)));
        when(credentialStore.update(any())).thenReturn(StoreResult.success());

        watchdog.run();

        // verify the store was queried with the proper filter expressions
        verify(credentialStore).query(argThat(querySpec ->
                querySpec.getFilterExpression().size() == 2 &&
                        querySpec.getFilterExpression().get(0).toString().equals("state in " + ALLOWED_STATES)));

        verify(credentialRequestManager)
                .initiateRequest(eq(cred.getParticipantContextId()), eq(cred.getIssuerId()), anyString(), argThat(list ->
                        list.size() == 1 &&
                                list.get(0).credentialType().equalsIgnoreCase("DemoCredential") &&
                                list.get(0).format().equals(VC1_0_JWT.name())));

        verify(credentialStore).update(argThat(vc -> vc.getStateAsEnum() == REQUESTED && vc.getMetadata().get(METADATA_RENEWAL_REQUEST_ID) != null));
    }


    @Test
    void run_whenCredentialIsExpiring_renewalFails() {
        var cred = createCredentialBuilder()
                .metadata("credentialObjectId", "cred-object-id")
                .credential(new VerifiableCredentialContainer("raw-vc-content", VC1_0_JWT, createVerifiableCredential()
                        .expirationDate(Instant.now().plusSeconds(GRACE_PERIOD / 2))
                        .build()))
                .build();
        when(credentialStore.query(any())).thenReturn(StoreResult.success(List.of(cred)));
        when(credentialRequestManager.initiateRequest(anyString(), anyString(), anyString(), anyList()))
                .thenReturn(ServiceResult.badRequest("foobarbaz"));

        watchdog.run();

        // verify the store was queried with the proper filter expressions
        verify(credentialStore).query(argThat(querySpec ->
                querySpec.getFilterExpression().size() == 2 &&
                        querySpec.getFilterExpression().get(0).toString().equals("state in " + ALLOWED_STATES)));

        verify(credentialRequestManager)
                .initiateRequest(eq(cred.getParticipantContextId()), eq(cred.getIssuerId()), anyString(), argThat(list ->
                        list.size() == 1 &&
                                list.get(0).credentialType().equalsIgnoreCase("DemoCredential") &&
                                list.get(0).format().equals(VC1_0_JWT.name()))
                );

        verify(monitor).warning(contains("foobarbaz"));
    }

    @Test
    void run_whenCredentialIsSuperseded_shouldNotInitiateRenewal() {
        var cred = createCredentialBuilder()
                .state(VcStatus.EXPIRED)
                .metadata(VerifiableCredentialResource.METADATA_CREDENTIAL_OBJECT_ID, "cred-object-id")
                .metadata(VerifiableCredentialResource.METADATA_SUPERSEDED_BY, "new-credential-id")
                .credential(new VerifiableCredentialContainer("raw-vc-content", VC1_0_JWT, createVerifiableCredential()
                        .expirationDate(Instant.now().plusSeconds(GRACE_PERIOD / 2))
                        .build()))
                .build();
        when(credentialStore.query(any())).thenReturn(StoreResult.success(List.of(cred)));
        when(credentialStatusCheckService.checkStatus(any())).thenReturn(Result.success(VcStatus.EXPIRED));

        watchdog.run();

        verifyNoInteractions(credentialRequestManager);
        verify(credentialStore, never()).update(any());
    }

    @Test
    void run_whenCredentialExpiredWithoutReplacement_shouldInitiateRenewal() {
        var cred = createCredentialBuilder()
                .state(VcStatus.EXPIRED)
                .metadata(VerifiableCredentialResource.METADATA_CREDENTIAL_OBJECT_ID, "cred-object-id")
                .credential(new VerifiableCredentialContainer("raw-vc-content", VC1_0_JWT, createVerifiableCredential()
                        .expirationDate(Instant.now().minusSeconds(10))
                        .build()))
                .build();
        when(credentialStore.query(any())).thenReturn(StoreResult.success(List.of(cred)));
        when(credentialStatusCheckService.checkStatus(any())).thenReturn(Result.success(VcStatus.EXPIRED));
        when(credentialStore.update(any())).thenReturn(StoreResult.success());

        watchdog.run();

        verify(credentialRequestManager).initiateRequest(eq(cred.getParticipantContextId()), eq(cred.getIssuerId()), anyString(), argThat(list ->
                list.size() == 1 && list.get(0).id().equals("cred-object-id")));
        verify(credentialStore).update(argThat(vc -> vc.getStateAsEnum() == REQUESTED && vc.getMetadata().get(METADATA_RENEWAL_REQUEST_ID) != null));
    }

    @Test
    void run_whenCredentialIsExpiring_noObjectIdPresent() {
        var cred = createCredentialBuilder()
                // .metadata("credentialObjectId", "cred-object-id") missing!
                .credential(new VerifiableCredentialContainer("raw-vc-content", VC1_0_JWT, createVerifiableCredential()
                        .expirationDate(Instant.now().plusSeconds(GRACE_PERIOD / 2))
                        .build()))
                .build();
        when(credentialStore.query(any())).thenReturn(StoreResult.success(List.of(cred)));
        when(credentialRequestManager.initiateRequest(anyString(), anyString(), anyString(), anyList()))
                .thenReturn(ServiceResult.badRequest("foobarbaz"));

        watchdog.run();

        // verify the store was queried with the proper filter expressions
        verify(credentialStore).query(argThat(querySpec ->
                querySpec.getFilterExpression().size() == 2 &&
                        querySpec.getFilterExpression().get(0).toString().equals("state in " + ALLOWED_STATES)));

        verify(credentialRequestManager, never())
                .initiateRequest(anyString(), anyString(), anyString(), anyList());

        verify(monitor).warning(contains("No CredentialObjectId found"));
    }

    @Test
    void run_whenRenewalInFlight_shouldLeaveCredentialUntouched() {
        var cred = createCredentialBuilder()
                .state(REQUESTED)
                .metadata(METADATA_RENEWAL_REQUEST_ID, "renewal-request")
                .build();
        when(credentialStore.query(any())).thenReturn(StoreResult.success(List.of(cred)));
        when(credentialRequestManager.findById("renewal-request")).thenReturn(renewalRequest(HolderRequestState.REQUESTING, null));

        watchdog.run();

        verify(credentialStore, never()).update(any());
        verifyNoInteractions(credentialStatusCheckService);
        verify(credentialRequestManager, never()).initiateRequest(anyString(), anyString(), anyString(), anyList());
    }

    @Test
    void run_whenRenewalFailed_shouldReleaseCredential() {
        var cred = createCredentialBuilder()
                .state(REQUESTED)
                .metadata(METADATA_RENEWAL_REQUEST_ID, "renewal-request")
                .build();
        when(credentialStore.query(any())).thenReturn(StoreResult.success(List.of(cred)));
        when(credentialRequestManager.findById("renewal-request")).thenReturn(renewalRequest(HolderRequestState.ERROR, "issuer unreachable"));

        watchdog.run();

        // the credential is tracked again: its status is what its own validity implies, and the failure is on record
        verify(credentialStatusCheckService).checkStatus(cred);
        verify(credentialStore, atLeastOnce()).update(cred);
        assertThat(cred.getStateAsEnum()).isEqualTo(ISSUED);
        assertThat(cred.getMetadata())
                .doesNotContainKey(METADATA_RENEWAL_REQUEST_ID)
                .containsEntry(METADATA_RENEWAL_ERROR, "issuer unreachable");
        // it is not near expiry, so no new renewal is due yet
        verify(credentialRequestManager, never()).initiateRequest(anyString(), anyString(), anyString(), anyList());
    }

    @Test
    void run_whenRenewalRequestUnknown_shouldReleaseCredential() {
        var cred = createCredentialBuilder()
                .state(REQUESTED)
                .metadata(METADATA_RENEWAL_REQUEST_ID, "renewal-request")
                .build();
        when(credentialStore.query(any())).thenReturn(StoreResult.success(List.of(cred)));
        when(credentialRequestManager.findById("renewal-request")).thenReturn(null);

        watchdog.run();

        assertThat(cred.getStateAsEnum()).isEqualTo(ISSUED);
        assertThat(cred.getMetadata()).doesNotContainKey(METADATA_RENEWAL_REQUEST_ID);
        assertThat(cred.getMetadata().get(METADATA_RENEWAL_ERROR)).asString().contains("renewal-request");
    }

    @Test
    void run_whenRenewalDeliveredButNotSuperseded_shouldReleaseCredentialWithoutError() {
        var cred = createCredentialBuilder()
                .state(REQUESTED)
                .metadata(METADATA_RENEWAL_REQUEST_ID, "renewal-request")
                .metadata(METADATA_RENEWAL_ERROR, "an earlier failure")
                .build();
        when(credentialStore.query(any())).thenReturn(StoreResult.success(List.of(cred)));
        when(credentialRequestManager.findById("renewal-request")).thenReturn(renewalRequest(HolderRequestState.ISSUED, null));

        watchdog.run();

        assertThat(cred.getStateAsEnum()).isEqualTo(ISSUED);
        assertThat(cred.getMetadata())
                .doesNotContainKey(METADATA_RENEWAL_REQUEST_ID)
                .doesNotContainKey(METADATA_RENEWAL_ERROR);
    }

    @Test
    void run_whenReleasedCredentialIsExpiring_shouldRenewAgain() {
        var cred = createCredentialBuilder()
                .state(REQUESTED)
                .metadata(METADATA_RENEWAL_REQUEST_ID, "renewal-request")
                .metadata(VerifiableCredentialResource.METADATA_CREDENTIAL_OBJECT_ID, "credential-object-id")
                .credential(new VerifiableCredentialContainer("raw-vc-content", VC1_0_JWT, createVerifiableCredential()
                        .expirationDate(Instant.now().plusSeconds(GRACE_PERIOD / 2))
                        .build()))
                .build();
        when(credentialStore.query(any())).thenReturn(StoreResult.success(List.of(cred)));
        when(credentialStore.update(any())).thenReturn(StoreResult.success());
        when(credentialRequestManager.findById("renewal-request")).thenReturn(renewalRequest(HolderRequestState.ERROR, "issuer unreachable"));

        watchdog.run();

        // released in this run, and renewed anew in the same run, since it is still about to expire
        verify(credentialRequestManager).initiateRequest(eq(cred.getParticipantContextId()), eq(cred.getIssuerId()), anyString(), anyList());
        assertThat(cred.getStateAsEnum()).isEqualTo(REQUESTED);
        assertThat(cred.getMetadata())
                .doesNotContainKey(METADATA_RENEWAL_ERROR)
                .hasEntrySatisfying(METADATA_RENEWAL_REQUEST_ID, id -> assertThat(id).isNotEqualTo("renewal-request"));
    }

    @Test
    void run_whenCredentialHasNoExpirationDate_shouldCheckItWithoutRenewing() {
        var withoutExpiry = createCredentialBuilder()
                .metadata("credentialObjectId", "cred-object-id")
                .credential(new VerifiableCredentialContainer("raw-vc-content", VC1_0_JWT, createVerifiableCredential()
                        .expirationDate(null)
                        .build()))
                .build();
        var expiring = createCredentialBuilder()
                .metadata("credentialObjectId", "cred-object-id")
                .credential(new VerifiableCredentialContainer("raw-vc-content", VC1_0_JWT, createVerifiableCredential()
                        .expirationDate(Instant.now().plusSeconds(GRACE_PERIOD / 2))
                        .build()))
                .build();
        when(credentialStore.query(any())).thenReturn(StoreResult.success(List.of(withoutExpiry, expiring)));
        when(credentialStore.update(any())).thenReturn(StoreResult.success());

        watchdog.run();

        verify(credentialStatusCheckService, times(2)).checkStatus(any());
        // only the credential that does expire is renewed
        verify(credentialRequestManager).initiateRequest(any(), any(), any(), any());
        verify(credentialStore).update(argThat(vc -> vc.getId().equals(expiring.getId()) && vc.getStateAsEnum() == REQUESTED));
    }

    @Test
    void run_whenCheckingOneCredentialFails_shouldCheckTheOthers() {
        var failing = createCredentialBuilder().build();
        var revoked = createCredentialBuilder().build();
        when(credentialStore.query(any())).thenReturn(StoreResult.success(List.of(failing, revoked)));
        when(credentialStatusCheckService.checkStatus(any()))
                .thenThrow(new EdcPersistenceException("database unavailable"))
                .thenReturn(Result.success(REVOKED));

        watchdog.run();

        verify(credentialStore).update(argThat(vc -> vc.getId().equals(revoked.getId()) && vc.getStateAsEnum() == REVOKED));
        verify(monitor).warning(contains(failing.getId()), any(EdcPersistenceException.class));
    }

    @Test
    void run_whenFetchingCredentialsThrows_shouldNotThrow() {
        when(credentialStore.query(any())).thenThrow(new EdcPersistenceException("database unavailable"));

        // an exception escaping from a run would stop all further runs of the scheduled watchdog
        assertThatNoException().isThrownBy(watchdog::run);

        verify(monitor).severe(contains("credential watchdog failed"), any(EdcPersistenceException.class));
        verifyNoInteractions(credentialStatusCheckService);
    }

    @Test
    void run_shouldCheckEachCredentialInItsOwnTransaction() {
        var transactionContext = new TrackingTransactionContext();
        var watchdog = new CredentialWatchdog(credentialStore, credentialStatusCheckService, monitor, transactionContext,
                Duration.ofSeconds(GRACE_PERIOD), credentialRequestManager);
        var transactionsOfUpdates = new ArrayList<Integer>();
        when(credentialStore.query(any())).thenReturn(StoreResult.success(List.of(createCredentialBuilder().build(), createCredentialBuilder().build())));
        when(credentialStatusCheckService.checkStatus(any())).thenReturn(Result.success(REVOKED));
        when(credentialStore.update(any())).thenAnswer(i -> {
            transactionsOfUpdates.add(transactionContext.currentTransaction());
            return StoreResult.success();
        });

        watchdog.run();

        // the credentials are fetched in the first transaction, and each one is updated in one of its own: if they shared a
        // transaction, a failure on one of them would roll back the updates of all the others
        assertThat(transactionsOfUpdates).containsExactly(2, 3);
    }

    private HolderCredentialRequest renewalRequest(HolderRequestState state, String errorDetail) {
        return HolderCredentialRequest.Builder.newInstance()
                .id("renewal-request")
                .issuerDid("test-issuer")
                .participantContextId("participant-id")
                .requestedCredential("credential-object-id", "DemoCredential", VC1_0_JWT.toString())
                .state(state.code())
                .errorDetail(errorDetail)
                .build();
    }

    private VerifiableCredentialResource.Builder createCredentialBuilder() {

        return VerifiableCredentialResource.Builder.newHolder()
                .issuerId("test-issuer")
                .holderId("test-holder")
                .state(VcStatus.ISSUED)
                .participantContextId("participant-id")
                .credential(new VerifiableCredentialContainer("raw-vc-content", VC1_0_JWT, createVerifiableCredential().build()))
                .id(UUID.randomUUID().toString());
    }

    private VerifiableCredential.Builder createVerifiableCredential() {
        return VerifiableCredential.Builder.newInstance()
                .credentialSubject(CredentialSubject.Builder.newInstance().id("test-subject").claim("test-key", "test-val").build())
                .issuanceDate(Instant.now().minus(10, ChronoUnit.DAYS))
                .expirationDate(Instant.now().plus(365, ChronoUnit.DAYS))
                .type("VerifiableCredential")
                .type("DemoCredential")
                .issuer(new Issuer("test-issuer", Map.of()))
                .id("did:web:test-credential");
    }


    /**
     * Numbers the transactions it runs, so that a test can tell which transaction a call happened in.
     */
    private static class TrackingTransactionContext extends NoopTransactionContext {
        private int transactions;
        private int depth;

        @Override
        public void execute(TransactionBlock block) {
            execute(() -> {
                block.execute();
                return null;
            });
        }

        @Override
        public <T> T execute(ResultTransactionBlock<T> block) {
            if (depth++ == 0) {
                transactions++;
            }
            try {
                return super.execute(block);
            } finally {
                depth--;
            }
        }

        int currentTransaction() {
            return depth > 0 ? transactions : 0;
        }
    }
}