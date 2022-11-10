/*
 *  Copyright (c) 2020 - 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.store.spi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.identityhub.spi.credentials.model.VerifiableCredential;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public abstract class IdentityHubStoreTestBase {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Random RAND = new Random();

    private static byte[] toByteArray(VerifiableCredential vc) {
        try {
            return MAPPER.writeValueAsBytes(vc);
        } catch (JsonProcessingException e) {
            throw new AssertionError(e);
        }
    }

    private static List<IdentityHubRecord> createIdentityHubRecords() {
        var credentialsCount = RAND.nextInt(10) + 1;
        return IntStream.range(0, credentialsCount)
                .mapToObj(i -> createIdentityHubRecord())
                .collect(Collectors.toList());
    }

    private static IdentityHubRecord createIdentityHubRecord() {
        var vc = VerifiableCredential.Builder.newInstance().id(UUID.randomUUID().toString()).build();
        return IdentityHubRecord.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .payload(toByteArray(vc))
                .build();
    }

    @Test
    void saveSameVerifiableCredentialTwice_shouldThrows() {
        var record = createIdentityHubRecord();

        getStore().add(record);

        assertThatExceptionOfType(EdcPersistenceException.class).isThrownBy(() -> getStore().add(record));
    }

    @Test
    void saveAndListVerifiableCredentials() {
        // Arrange
        var records = createIdentityHubRecords();

        // Act
        records.forEach(a -> getStore().add(a));

        // Assert
        try (var stream = getStore().getAll()) {
            var stored = stream.collect(Collectors.toList());
            assertThat(stored).hasSize(records.size());
            records.forEach(expected -> assertThat(stored).anySatisfy(r -> {
                assertThat(r.getId()).isEqualTo(expected.getId());
                assertThat(r.getPayload()).isEqualTo(expected.getPayload());
            }));
        }

    }

    protected abstract IdentityHubStore getStore();
}
