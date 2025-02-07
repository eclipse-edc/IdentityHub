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
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.spi.result.StoreResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BitstringStatusListFactoryTest {

    private final CredentialStore credentialStore = mock();
    private final BitstringStatusListFactory factory = new BitstringStatusListFactory(credentialStore);


    @Test
    void create_success() {
        var status = new CredentialStatus("id", "BitstringStatusListEntry", Map.of("statusPurpose", "revocation",
                "statusListIndex", "1234",
                "statusListCredential", "https://example.com/credentials/status/1234"));

        when(credentialStore.findById(any())).thenReturn(StoreResult.success(null));

        var result = factory.create(status);
        assertThat(result).isSucceeded();
        assertThat(result.getContent()).isInstanceOf(BitstringStatusInfo.class);
    }

    @Test
    void create_whenNoIndex_expectFailure() {
        var status = new CredentialStatus("id", "BitstringStatusListEntry", Map.of("statusPurpose", "revocation",
                "statusListCredential", "https://example.com/credentials/status/1234"));

        when(credentialStore.findById(any())).thenReturn(StoreResult.success(null));

        var result = factory.create(status);
        assertThat(result).isFailed().detail().contains("the 'statusListIndex' field is missing");
    }

    @Test
    void create_whenNoCredential_expectFailure() {
        var status = new CredentialStatus("id", "BitstringStatusListEntry", Map.of("statusPurpose", "revocation",
                "statusListIndex", "1234"));

        when(credentialStore.findById(any())).thenReturn(StoreResult.success(null));

        var result = factory.create(status);
        assertThat(result).isFailed().detail().contains("the 'statusListCredential' field is missing");
    }

    @Test
    void create_whenRevocationCredentialNotFound_expectFailure() {
        var status = new CredentialStatus("id", "BitstringStatusListEntry", Map.of("statusPurpose", "revocation",
                "statusListIndex", "1234",
                "statusListCredential", "https://example.com/credentials/status/1234"));

        when(credentialStore.findById(any())).thenReturn(StoreResult.notFound("foo"));

        var result = factory.create(status);
        assertThat(result).isFailed().detail().isEqualTo("foo");
    }
}