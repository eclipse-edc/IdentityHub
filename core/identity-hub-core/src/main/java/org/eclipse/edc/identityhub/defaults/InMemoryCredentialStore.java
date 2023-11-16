/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.defaults;

import org.eclipse.edc.connector.core.store.ReflectionBasedQueryResolver;
import org.eclipse.edc.identityhub.spi.store.CredentialStore;
import org.eclipse.edc.identityhub.spi.store.model.VerifiableCredentialResource;
import org.eclipse.edc.spi.query.QueryResolver;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

import static org.eclipse.edc.spi.result.StoreResult.alreadyExists;
import static org.eclipse.edc.spi.result.StoreResult.notFound;
import static org.eclipse.edc.spi.result.StoreResult.success;

public class InMemoryCredentialStore implements CredentialStore {
    private final Map<String, VerifiableCredentialResource> store = new HashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
    private final QueryResolver<VerifiableCredentialResource> queryResolver = new ReflectionBasedQueryResolver<>(VerifiableCredentialResource.class);

    @Override
    public StoreResult<Void> create(VerifiableCredentialResource credentialResource) {
        lock.writeLock().lock();
        var id = credentialResource.getId();
        try {
            if (store.containsKey(id)) {
                return alreadyExists("A VerifiableCredentialResource with ID %s already exists".formatted(id));
            }
            store.put(id, credentialResource);
            return success(null);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public StoreResult<Stream<VerifiableCredentialResource>> query(QuerySpec querySpec) {
        lock.readLock().lock();
        try {
            var result = queryResolver.query(store.values().stream(), querySpec);
            return success(result);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public StoreResult<Void> update(VerifiableCredentialResource credentialResource) {
        lock.writeLock().lock();
        try {
            var id = credentialResource.getId();
            if (!store.containsKey(id)) {
                return notFound("A VerifiableCredentialResource with ID %s was not found".formatted(id));
            }
            store.put(id, credentialResource);
            return success();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public StoreResult<Void> delete(String id) {
        lock.writeLock().lock();
        try {
            if (!store.containsKey(id)) {
                return notFound("A VerifiableCredentialResource with ID %s was not found".formatted(id));
            }
            store.remove(id);
            return success();
        } finally {
            lock.writeLock().unlock();
        }
    }

}
