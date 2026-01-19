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

package org.eclipse.edc.issuerservice.credentials.statuslist.bitstring;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialStatus;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.spi.result.ServiceFailure;
import org.eclipse.edc.spi.result.StoreResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.NOT_FOUND;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BitstringStatusListFactoryTest {

    private final CredentialStore credentialStore = mock();
    private final BitstringStatusListFactory factory = new BitstringStatusListFactory(credentialStore);

    @Test
    void shouldCreateStatusInfo() {
        var status = new CredentialStatus("id", "BitstringStatusListEntry", Map.of(
                "statusPurpose", "revocation",
                "statusListIndex", "1234",
                "statusListCredential", "https://example.com/credentials/status/credentialId"));
        var statusList = VerifiableCredentialResource.Builder.newStatusList().issuerId("issuer").holderId("holder").build();
        when(credentialStore.query(any())).thenReturn(StoreResult.success(List.of(statusList)));

        var result = factory.create(status);

        assertThat(result).isSucceeded().isInstanceOfSatisfying(BitstringStatusInfo.class, info -> {
            assertThat(info.index()).isEqualTo(1234);
            assertThat(info.statusListCredential()).isSameAs(statusList);
        });
        verify(credentialStore).query(argThat(querySpec -> querySpec.getFilterExpression()
                .contains(criterion("verifiableCredential.credential.id", "=", "credentialId"))));
    }

    @Test
    void shouldFail_whenNoIndex() {
        var status = new CredentialStatus("id", "BitstringStatusListEntry", Map.of(
                "statusPurpose", "revocation",
                "statusListCredential", "https://example.com/credentials/status/1234"));

        var result = factory.create(status);

        assertThat(result).isFailed().detail().contains("the 'statusListIndex' field is missing");
    }

    @Test
    void shouldFail_whenNoStatusListCredential() {
        var status = new CredentialStatus("id", "BitstringStatusListEntry", Map.of(
                "statusPurpose", "revocation",
                "statusListIndex", "1234"));

        var result = factory.create(status);

        assertThat(result).isFailed().detail().contains("the 'statusListCredential' field is missing");
    }

    @Test
    void shouldFail_whenCredentialNotFound() {
        var status = new CredentialStatus("id", "BitstringStatusListEntry", Map.of("statusPurpose", "revocation",
                "statusListIndex", "1234",
                "statusListCredential", "https://example.com/credentials/status/1234"));
        when(credentialStore.query(any())).thenReturn(StoreResult.success(emptyList()));

        var result = factory.create(status);

        assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(NOT_FOUND);
    }

    @Test
    void shouldFail_whenQueryFails() {
        var status = new CredentialStatus("id", "BitstringStatusListEntry", Map.of("statusPurpose", "revocation",
                "statusListIndex", "1234",
                "statusListCredential", "https://example.com/credentials/status/1234"));
        when(credentialStore.query(any())).thenReturn(StoreResult.generalError("failure"));

        var result = factory.create(status);

        assertThat(result).isFailed().messages().contains("failure");
    }
}
