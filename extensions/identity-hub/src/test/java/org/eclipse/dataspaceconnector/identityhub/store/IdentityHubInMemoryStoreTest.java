/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.identityhub.store;

import org.eclipse.dataspaceconnector.identityhub.credentials.model.VerifiableCredential;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

public class IdentityHubInMemoryStoreTest {

    @Test
    void addAndReadVerifiableCredential() {
        // Arrange
        var store = new IdentityHubInMemoryStore();
        var credentialsCount = new Random().nextInt(10);
        var credentials = IntStream.range(0, credentialsCount)
                .mapToObj(i -> VerifiableCredential.Builder.newInstance().id(UUID.randomUUID().toString()).build())
                .collect(Collectors.toList());

        // Act
        credentials.forEach(store::add);

        // Assert
        assertThat(store.getAll()).usingRecursiveFieldByFieldElementComparator().containsAll(credentials);
    }
}
