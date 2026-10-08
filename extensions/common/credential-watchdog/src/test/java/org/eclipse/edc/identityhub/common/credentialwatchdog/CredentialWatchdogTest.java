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
import org.eclipse.edc.identityhub.transaction.TrackingTransactionContext;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat.VC1_0_JWT;
import static org.eclipse.edc.identityhub.common.credentialwatchdog.CredentialWatchdog.ALLOWED_STATES;
import static org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus.EXPIRED;
import static org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus.ISSUED;
import static org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus.REQUESTED;
import static org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus.REVOKED;
import static org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource.METADATA_RENEWAL_ERROR;
import static org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource.METADATA_RENEWAL_REQUEST_ID;
import static org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource.METADATA_SUPERSEDED_BY;
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
    private final Map<String, VerifiableCredentialResource> storedCredentials = new HashMap<>();

    @BeforeEach
    void setUp() {
        // the watchdog reads every credential anew, locked, in the transactions that change it
        when(credentialStore.queryForUpdate(any())).thenAnswer(i -> StoreResult.success(byId(i.getArgument(0))));
        when(credentialStatusCheckService.checkStatus(any())).thenReturn(Result.success(VcStatus.ISSUED));
        when(credentialRequestManager.initiateRequest(anyString(), anyString(), anyString(), anyList())).thenAnswer(i -> ServiceResult.success(i.getArgument(2)));
    }

    @Test
    void run_whenNonRequiresUpdate() {
        storeHolds(createCredentialBuilder().build(), createCredentialBuilder().build());

        watchdog.run();

        // verify the store was queried with the proper filter expressions
        verify(credentialStore).query(argThat(querySpec ->
                querySpec.getFilterExpression().size() == 2 &&
                        querySpec.getFilterExpression().get(0).toString().equals("state in " + ALLOWED_STATES)));
    }

    @Test
    void run_whenNoCredentials() {
        storeHolds();

        watchdog.run();

        verifyNoInteractions(credentialStatusCheckService);
        verify(credentialStore, never()).update(any());
    }

    @Test
    void run_whenRequiresUpdate() {
        var cred1 = createCredentialBuilder().build();
        var cred2 = createCredentialBuilder().build();

        storeHolds(cred1, cred2);
        when(credentialStatusCheckService.checkStatus(any()))
                .thenReturn(Result.success(REVOKED))
                .thenReturn(Result.success(ISSUED));

        watchdog.run();

        verify(credentialStore).query(any());
        verify(credentialStore, times(4)).queryForUpdate(any());
        verify(credentialStore).update(argThat(vcr -> vcr.getId().equals(cred1.getId())));
        verifyNoMoreInteractions(credentialStore);
        verify(credentialStatusCheckService, times(2)).checkStatus(any());
        verifyNoMoreInteractions(credentialStatusCheckService);
    }

    @Test
    void run_whenCheckServiceFails_shouldTransitionError() {
        storeHolds(createCredentialBuilder().build(), createCredentialBuilder().build());

        when(credentialStatusCheckService.checkStatus(any()))
                .thenReturn(Result.failure("test failure"))
                .thenReturn(Result.success(ISSUED));
        watchdog.run();

        verify(credentialStore).query(any());
        verify(credentialStore, times(4)).queryForUpdate(any());
        verify(credentialStore).update(argThat(vcr -> vcr.getStateAsEnum() == VcStatus.ERROR));
        verifyNoMoreInteractions(credentialStore);
        verify(credentialStatusCheckService, times(2)).checkStatus(any());
    }

    @Test
    void run_whenCredentialInError_andCheckSucceeds_shouldRecover() {
        var cred = createCredentialBuilder().state(VcStatus.ERROR).build();
        storeHolds(cred);

        watchdog.run();

        verify(credentialStore).query(argThat(querySpec ->
                querySpec.getFilterExpression().get(0).getOperandRight() instanceof Collection<?> states && states.contains(VcStatus.ERROR.code())));
        verify(credentialStatusCheckService).checkStatus(cred);
        verify(credentialStore).update(argThat(vcr -> vcr.getId().equals(cred.getId()) && vcr.getStateAsEnum() == ISSUED));
    }

    @Test
    void run_whenCredentialInError_andCheckFailsAgain_shouldNotUpdate() {
        var cred = createCredentialBuilder().state(VcStatus.ERROR).build();
        storeHolds(cred);
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
        storeHolds(cred);
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
        storeHolds(cred);
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
        storeHolds(cred);
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
        storeHolds(cred);
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
        storeHolds(cred);
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
        storeHolds(cred);
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
        storeHolds(cred);
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
        storeHolds(cred);
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
        storeHolds(cred);
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
        storeHolds(cred);
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
        storeHolds(cred);
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
        storeHolds(withoutExpiry, expiring);
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
        storeHolds(failing, revoked);
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
        storeHolds(createCredentialBuilder().build(), createCredentialBuilder().build());
        when(credentialStatusCheckService.checkStatus(any())).thenReturn(Result.success(REVOKED));
        when(credentialStore.update(any())).thenAnswer(i -> {
            transactionsOfUpdates.add(transactionContext.currentTransaction());
            return StoreResult.success();
        });

        watchdog.run();

        // the credentials are fetched in the first transaction. Each one is reconciled in a transaction, and updated in
        // another one of its own: if they shared a transaction, a failure on one of them would roll back the updates of
        // all the others
        assertThat(transactionsOfUpdates).containsExactly(3, 5);
    }

    @Test
    void run_shouldDetermineStatusOutsideTransaction() {
        var transactionContext = new TrackingTransactionContext();
        var watchdog = new CredentialWatchdog(credentialStore, credentialStatusCheckService, monitor, transactionContext,
                Duration.ofSeconds(GRACE_PERIOD), credentialRequestManager);
        var transactionsOfChecks = new ArrayList<Integer>();
        storeHolds(createCredentialBuilder().build());
        when(credentialStatusCheckService.checkStatus(any())).thenAnswer(i -> {
            transactionsOfChecks.add(transactionContext.currentTransaction());
            return Result.success(REVOKED);
        });

        watchdog.run();

        // determining the status may download the status list, which must not hold a database connection, nor any locks
        assertThat(transactionsOfChecks).containsExactly(0);
        verify(credentialStore).update(argThat(vc -> vc.getStateAsEnum() == REVOKED));
    }

    @Test
    void run_shouldLockCredentialInTransactionsThatChangeIt() {
        var transactionContext = new TrackingTransactionContext();
        var watchdog = new CredentialWatchdog(credentialStore, credentialStatusCheckService, monitor, transactionContext,
                Duration.ofSeconds(GRACE_PERIOD), credentialRequestManager);
        var credential = createCredentialBuilder().build();
        storeHolds(credential);
        var transactionsOfLocks = new ArrayList<Integer>();
        var transactionsOfUpdates = new ArrayList<Integer>();
        when(credentialStore.queryForUpdate(any())).thenAnswer(i -> {
            transactionsOfLocks.add(transactionContext.currentTransaction());
            return StoreResult.success(byId(i.getArgument(0)));
        });
        when(credentialStore.update(any())).thenAnswer(i -> {
            transactionsOfUpdates.add(transactionContext.currentTransaction());
            return StoreResult.success();
        });
        when(credentialStatusCheckService.checkStatus(any())).thenReturn(Result.success(REVOKED));

        watchdog.run();

        // the credential is read anew and locked in both of its transactions, the second of which updates it
        assertThat(transactionsOfLocks).containsExactly(2, 3);
        assertThat(transactionsOfUpdates).containsExactly(3);
        verify(credentialStore, times(2)).queryForUpdate(argThat(query -> query.getFilterExpression().contains(new Criterion("id", "=", credential.getId()))));
    }

    @Test
    void run_whenCredentialSupersededWhileChecked_shouldNotOverwriteIt() {
        var credential = createExpiringCredential().id("credential-id").build();
        storeHolds(credential);
        // a delivery supersedes the credential while its status is determined
        var superseded = createExpiringCredential().id("credential-id")
                .state(EXPIRED)
                .metadata(METADATA_SUPERSEDED_BY, "new-credential-id")
                .build();
        when(credentialStore.queryForUpdate(any()))
                .thenReturn(StoreResult.success(List.of(credential)))
                .thenReturn(StoreResult.success(List.of(superseded)));

        watchdog.run();

        // writing the copy that was checked would make the superseded credential usable, and renew it again
        verify(credentialStore, never()).update(any());
        verify(credentialRequestManager, never()).initiateRequest(anyString(), anyString(), anyString(), anyList());
    }

    @Test
    void run_whenRenewalStartedByAnotherRuntimeWhileChecked_shouldNotRenewAgain() {
        var credential = createExpiringCredential().id("credential-id").build();
        storeHolds(credential);
        // another runtime checks the same credential, and starts its renewal first
        var renewing = createExpiringCredential().id("credential-id")
                .state(REQUESTED)
                .metadata(METADATA_RENEWAL_REQUEST_ID, "renewal-request")
                .build();
        when(credentialStore.queryForUpdate(any()))
                .thenReturn(StoreResult.success(List.of(credential)))
                .thenReturn(StoreResult.success(List.of(renewing)));

        watchdog.run();

        verify(credentialRequestManager, never()).initiateRequest(anyString(), anyString(), anyString(), anyList());
        verify(credentialStore, never()).update(any());
    }

    @Test
    void run_whenRenewalStartedByAnotherRuntimeBeforeCheck_shouldLeaveCredentialUntouched() {
        var credential = createExpiringCredential().id("credential-id").build();
        storeHolds(credential);
        // the credential was found before another runtime started its renewal
        var renewing = createExpiringCredential().id("credential-id")
                .state(REQUESTED)
                .metadata(METADATA_RENEWAL_REQUEST_ID, "renewal-request")
                .build();
        when(credentialStore.queryForUpdate(any())).thenReturn(StoreResult.success(List.of(renewing)));
        when(credentialRequestManager.findById("renewal-request")).thenReturn(renewalRequest(HolderRequestState.REQUESTED, null));

        watchdog.run();

        verifyNoInteractions(credentialStatusCheckService);
        verify(credentialRequestManager, never()).initiateRequest(anyString(), anyString(), anyString(), anyList());
        verify(credentialStore, never()).update(any());
    }

    @Test
    void run_shouldCheckCredentialsBeyondTheFirstPage() {
        var credentials = IntStream.range(0, 250)
                .mapToObj(i -> createCredentialBuilder().id("credential-%03d".formatted(i)).build())
                .toList();
        credentials.forEach(credential -> storedCredentials.put(credential.getId(), credential));
        when(credentialStore.query(any())).thenAnswer(i -> StoreResult.success(page(credentials, i.getArgument(0))));

        watchdog.run();

        verify(credentialStatusCheckService, times(250)).checkStatus(any());
    }

    /**
     * The page of the credentials that a store returns for the query: those matching its ID criterion, ordered by ID, up to
     * its limit.
     */
    private List<VerifiableCredentialResource> page(List<VerifiableCredentialResource> credentials, QuerySpec query) {
        var after = query.getFilterExpression().stream()
                .filter(criterion -> criterion.getOperandLeft().equals("id") && criterion.getOperator().equals(">"))
                .map(criterion -> criterion.getOperandRight().toString())
                .findFirst()
                .orElse("");
        return credentials.stream()
                .filter(credential -> credential.getId().compareTo(after) > 0)
                .sorted(Comparator.comparing(VerifiableCredentialResource::getId))
                .limit(query.getLimit())
                .toList();
    }

    /**
     * Lets the store hold the given credentials: the watchdog finds them with its query, and reads each one anew by its ID.
     */
    private void storeHolds(VerifiableCredentialResource... credentials) {
        Arrays.stream(credentials).forEach(credential -> storedCredentials.put(credential.getId(), credential));
        when(credentialStore.query(any())).thenReturn(StoreResult.success(List.of(credentials)));
    }

    private List<VerifiableCredentialResource> byId(QuerySpec query) {
        // Mockito passes null when a test stubs queryForUpdate again
        if (query == null) {
            return List.of();
        }
        return query.getFilterExpression().stream()
                .filter(criterion -> criterion.getOperandLeft().equals("id") && criterion.getOperator().equals("="))
                .map(criterion -> storedCredentials.get(criterion.getOperandRight().toString()))
                .filter(Objects::nonNull)
                .toList();
    }

    private VerifiableCredentialResource.Builder createExpiringCredential() {
        return createCredentialBuilder()
                .metadata(VerifiableCredentialResource.METADATA_CREDENTIAL_OBJECT_ID, "credential-object-id")
                .credential(new VerifiableCredentialContainer("raw-vc-content", VC1_0_JWT, createVerifiableCredential()
                        .expirationDate(Instant.now().plusSeconds(GRACE_PERIOD / 2))
                        .build()));
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
}
