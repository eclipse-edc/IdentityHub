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
import org.eclipse.edc.identityhub.spi.verifiablecredentials.CredentialRequestManager;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.CredentialStatusCheckService;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat.VC1_0_JWT;
import static org.eclipse.edc.identityhub.common.credentialwatchdog.CredentialWatchdog.ALLOWED_STATES;
import static org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus.ISSUED;
import static org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus.REVOKED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
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
        when(credentialRequestManager.initiateRequest(anyString(), anyString(), anyString(), anyList())).thenReturn(ServiceResult.success());
    }

    @Test
    void run_whenNonRequiresUpdate() {
        when(credentialStore.query(any()))
                .thenReturn(StoreResult.success(List.of(createCredentialBuilder().build(), createCredentialBuilder().build())));

        watchdog.run();

        // verify the store was queried with the proper filter expressions
        verify(credentialStore).query(argThat(querySpec ->
                querySpec.getFilterExpression().size() == 1 &&
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
    void run_whenCredentialIsExpiring_shouldInitiateRenewal() {
        var cred = createCredentialBuilder()
                .metadata("credentialObjectId", "cred-object-id")
                .credential(new VerifiableCredentialContainer("raw-vc-content", VC1_0_JWT, createVerifiableCredential()
                        .expirationDate(Instant.now().plusSeconds(GRACE_PERIOD / 2))
                        .build()))
                .build();
        when(credentialStore.query(any())).thenReturn(StoreResult.success(List.of(cred)));

        watchdog.run();

        // verify the store was queried with the proper filter expressions
        verify(credentialStore).query(argThat(querySpec ->
                querySpec.getFilterExpression().size() == 1 &&
                        querySpec.getFilterExpression().get(0).toString().equals("state in " + ALLOWED_STATES)));

        verify(credentialRequestManager)
                .initiateRequest(eq(cred.getParticipantContextId()), eq(cred.getIssuerId()), anyString(), argThat(list ->
                        list.size() == 1 &&
                                list.get(0).credentialType().equalsIgnoreCase("DemoCredential") &&
                                list.get(0).format().equals(VC1_0_JWT.name())));

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
                querySpec.getFilterExpression().size() == 1 &&
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
                querySpec.getFilterExpression().size() == 1 &&
                        querySpec.getFilterExpression().get(0).toString().equals("state in " + ALLOWED_STATES)));

        verify(credentialRequestManager, never())
                .initiateRequest(anyString(), anyString(), anyString(), anyList());

        verify(monitor).warning(contains("No CredentialObjectId found"));
    }

    private VerifiableCredentialResource.Builder createCredentialBuilder() {

        return VerifiableCredentialResource.Builder.newInstance()
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