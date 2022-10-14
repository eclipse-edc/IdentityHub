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

package org.eclipse.dataspaceconnector.identityhub.store.spi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.identityhub.credentials.model.VerifiableCredential;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class IdentityHubStoreTestBase {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Random RAND = new Random();

    @Test
    void saveAndListVerifiableCredentials() {
        // Arrange
        var credentials = createVerifiableCredentials();

        // Act
        credentials.forEach(a -> getStore().add(a));

        // Assert
        assertThat(getStore().getAll()).containsAll(credentials);
    }

    private static byte[] toByteArray(VerifiableCredential vc) {
        try {
            return MAPPER.writeValueAsBytes(vc);
        } catch (JsonProcessingException e) {
            throw new AssertionError(e);
        }
    }

    private static List<byte[]> createVerifiableCredentials() {
        var credentialsCount = RAND.nextInt(10) + 1;
        return IntStream.range(0, credentialsCount)
                .mapToObj(i -> VerifiableCredential.Builder.newInstance().id(UUID.randomUUID().toString()).build())
                .map(IdentityHubStoreTestBase::toByteArray)
                .collect(Collectors.toList());
    }

    protected abstract IdentityHubStore getStore();
}
