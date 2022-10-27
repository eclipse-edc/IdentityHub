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

import org.eclipse.dataspaceconnector.identityhub.store.spi.IdentityHubRecord;
import org.eclipse.dataspaceconnector.identityhub.store.spi.IdentityHubStore;
import org.eclipse.dataspaceconnector.spi.persistence.EdcPersistenceException;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * In memory store of Hub Objects.
 */
public class InMemoryIdentityHubStore implements IdentityHubStore {

    private final Map<String, IdentityHubRecord> cache = new ConcurrentHashMap<>();

    @Override
    public @NotNull Stream<IdentityHubRecord> getAll() {
        return cache.values().stream();
    }

    @Override
    public void add(IdentityHubRecord record) {
        if (cache.containsKey(record.getId())) {
            throw new EdcPersistenceException("Identity Hub already contains a record with id: " + record.getId());
        }
        cache.put(record.getId(), record);
    }
}
