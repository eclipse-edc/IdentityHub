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

package org.eclipse.edc.identityhub.core.services.verifiablecredential;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.RevocationServiceRegistry;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class CredentialStatusCheckServiceImplTest {

    private final RevocationServiceRegistry revocationServiceRegistry = mock();
    private final Clock clock = Clock.systemUTC();
    private final CredentialStatusCheckServiceImpl service = new CredentialStatusCheckServiceImpl(revocationServiceRegistry, clock);

    @BeforeEach
    void setUp() {
        when(revocationServiceRegistry.getRevocationStatus(any())).thenReturn(Result.success(null));
    }

    @Test
    void checkStatus_whenExpired() {
        var credential = createVerifiableCredential()
                .expirationDate(Instant.now(clock).minus(10, ChronoUnit.MINUTES))
                .build();
        assertThat(service.checkStatus(createCredentialBuilder(credential).build()))
                .isSucceeded()
                .isEqualTo(VcStatus.EXPIRED);
    }

    @Test
    void checkStatus_notYetValid_becomesValid() {
        var now = Instant.now();
        var tenSecondsAgo = now.minus(10, ChronoUnit.SECONDS);

        var credential = createVerifiableCredential()
                .issuanceDate(tenSecondsAgo)
                .build();

        var result = service.checkStatus(createCredentialBuilder(credential)
                .state(VcStatus.NOT_YET_VALID)
                .build());
        assertThat(result).isSucceeded()
                .isEqualTo(VcStatus.ISSUED);
    }

    @Test
    void checkStatus_suspended_becomesSuspended() {
        when(revocationServiceRegistry.getRevocationStatus(any())).thenReturn(Result.success("suspension"));
        var credential = createVerifiableCredential()
                .build();

        var result = service.checkStatus(createCredentialBuilder(credential)
                .state(VcStatus.SUSPENDED)
                .build());
        assertThat(result).isSucceeded()
                .isEqualTo(VcStatus.SUSPENDED);
    }

    @Test
    void checkStatus_suspended_becomesNotSuspended() {
        when(revocationServiceRegistry.getRevocationStatus(any())).thenReturn(Result.success(null));
        var credential = createVerifiableCredential()
                .build();

        var result = service.checkStatus(createCredentialBuilder(credential)
                .state(VcStatus.SUSPENDED)
                .build());
        assertThat(result).isSucceeded()
                .isEqualTo(VcStatus.ISSUED);
    }

    @Test
    void checkStatus_whenRevoked() {
        when(revocationServiceRegistry.getRevocationStatus(any())).thenReturn(Result.success("revocation"));
        var credential = createCredentialBuilder(createVerifiableCredential().build()).build();

        assertThat(service.checkStatus(credential))
                .isSucceeded()
                .isEqualTo(VcStatus.REVOKED);
    }

    @Test
    void checkStatus_whenSuspended() {
        when(revocationServiceRegistry.getRevocationStatus(any())).thenReturn(Result.success("suspension"));
        var credential = createCredentialBuilder(createVerifiableCredential().build()).build();

        assertThat(service.checkStatus(credential))
                .isSucceeded()
                .isEqualTo(VcStatus.SUSPENDED);
    }

    @Test
    void checkStatus_whenUnknownRevocationStatus() {
        when(revocationServiceRegistry.getRevocationStatus(any())).thenReturn(Result.success("foo-status"));
        var credential = createCredentialBuilder(createVerifiableCredential().build()).build();

        assertThat(service.checkStatus(credential))
                .isSucceeded()
                .isEqualTo(VcStatus.ISSUED);
    }

    @Test
    void checkStatus_whenMultipleRules() {
        when(revocationServiceRegistry.getRevocationStatus(any())).thenReturn(Result.success("revocation"));
        var credential = createVerifiableCredential()
                .expirationDate(Instant.now(clock).minus(10, ChronoUnit.MINUTES))
                .build();
        assertThat(service.checkStatus(createCredentialBuilder(credential).build()))
                .isSucceeded()
                .isEqualTo(VcStatus.REVOKED);
    }

    @Test
    void checkStatus_revocationCheckThrows() {
        when(revocationServiceRegistry.getRevocationStatus(any())).thenReturn(Result.failure("failed"));
        var credential = createCredentialBuilder(createVerifiableCredential().build()).build();

        assertThat(service.checkStatus(credential))
                .isFailed()
                .detail().isEqualTo("failed");
    }

    @Test
    void checkStatus_suspended_becomesExpired() {
        var credential = createVerifiableCredential()
                .expirationDate(Instant.now(clock).minus(10, ChronoUnit.MINUTES))
                .build();
        assertThat(service.checkStatus(createCredentialBuilder(credential).state(VcStatus.SUSPENDED).build()))
                .isSucceeded()
                .isEqualTo(VcStatus.EXPIRED);
    }

    @Test
    void checkStatus_notYetValid_becomesSuspended() {
        when(revocationServiceRegistry.getRevocationStatus(any())).thenReturn(Result.success("suspension"));
        var now = Instant.now();
        var tenSecondsAgo = now.minus(10, ChronoUnit.SECONDS);

        var credential = createVerifiableCredential()
                .issuanceDate(tenSecondsAgo)
                .build();

        var result = service.checkStatus(createCredentialBuilder(credential)
                .state(VcStatus.NOT_YET_VALID)
                .build());
        assertThat(result).isSucceeded()
                .isEqualTo(VcStatus.SUSPENDED);
    }

    @Test
    void checkStatus_notYetValid_becomesNotYetValid() {
        var now = Instant.now();
        var inTenSeconds = now.plus(10, ChronoUnit.SECONDS);

        var credential = createVerifiableCredential()
                .issuanceDate(inTenSeconds)
                .build();

        var result = service.checkStatus(createCredentialBuilder(credential)
                .state(VcStatus.NOT_YET_VALID)
                .build());
        assertThat(result).isSucceeded()
                .isEqualTo(VcStatus.NOT_YET_VALID);
    }

    @Test
    void checkStatus_notYetValid_becomesExpired() {
        var now = Instant.now();
        var tenSecondsAgo = now.minus(10, ChronoUnit.SECONDS);
        var fiveSecondsAgo = now.minus(5, ChronoUnit.SECONDS);

        var credential = createVerifiableCredential()
                .issuanceDate(tenSecondsAgo)
                .expirationDate(fiveSecondsAgo)
                .build();

        var result = service.checkStatus(createCredentialBuilder(credential)
                .state(VcStatus.NOT_YET_VALID)
                .build());
        assertThat(result).isSucceeded()
                .isEqualTo(VcStatus.EXPIRED);
    }


    @Test
    void checkStatus_suspended_becomesRevoked() {
        when(revocationServiceRegistry.getRevocationStatus(any())).thenReturn(Result.success("revocation"));
        var credential = createVerifiableCredential()
                .build();

        assertThat(service.checkStatus(createCredentialBuilder(credential).state(VcStatus.SUSPENDED).build()))
                .isSucceeded()
                .isEqualTo(VcStatus.REVOKED);
    }

    @Test
    void checkStatus_expired_becomesRevoked() {
        when(revocationServiceRegistry.getRevocationStatus(any())).thenReturn(Result.success("revocation"));
        var credential = createVerifiableCredential()
                .expirationDate(Instant.now(clock).minus(10, ChronoUnit.MINUTES))
                .build();

        assertThat(service.checkStatus(createCredentialBuilder(credential).state(VcStatus.ISSUED).build()))
                .isSucceeded()
                .isEqualTo(VcStatus.REVOKED);
        verify(revocationServiceRegistry).getRevocationStatus(credential);
        verifyNoMoreInteractions(revocationServiceRegistry);
    }

    @Test
    void checkStatus_expired_becomesSuspended() {
        when(revocationServiceRegistry.getRevocationStatus(any())).thenReturn(Result.success("suspension"));
        var credential = createVerifiableCredential()
                .expirationDate(Instant.now(clock).minus(10, ChronoUnit.MINUTES))
                .build();

        assertThat(service.checkStatus(createCredentialBuilder(credential).state(VcStatus.ISSUED).build()))
                .isSucceeded()
                .isEqualTo(VcStatus.SUSPENDED);
        verify(revocationServiceRegistry, times(2)).getRevocationStatus(credential);
        verifyNoMoreInteractions(revocationServiceRegistry);
    }

    private VerifiableCredentialResource.Builder createCredentialBuilder(VerifiableCredential credential) {

        return VerifiableCredentialResource.Builder.newInstance()
                .issuerId("test-issuer")
                .holderId("test-holder")
                .state(VcStatus.ISSUED)
                .participantContextId("participant-id")
                .credential(new VerifiableCredentialContainer("raw-vc-content", CredentialFormat.JSON_LD, credential))
                .id(UUID.randomUUID().toString());
    }

    private VerifiableCredential.Builder createVerifiableCredential() {
        return VerifiableCredential.Builder.newInstance()
                .credentialSubject(CredentialSubject.Builder.newInstance().id("test-subject").claim("test-key", "test-val").build())
                .issuanceDate(Instant.now(clock).minus(10, ChronoUnit.DAYS))
                .type("VerifiableCredential")
                .issuer(new Issuer("test-issuer", Map.of()))
                .id("did:web:test-credential");
    }
}