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

package org.eclipse.edc.issuerservice.defaults.store;

import org.eclipse.edc.identityhub.store.InMemoryEntityStore;
import org.eclipse.edc.issuerservice.spi.issuance.credentialdefinition.store.CredentialDefinitionStore;
import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialDefinition;
import org.eclipse.edc.spi.query.QueryResolver;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.store.ReflectionBasedQueryResolver;

import static org.eclipse.edc.spi.result.StoreResult.alreadyExists;
import static org.eclipse.edc.spi.result.StoreResult.notFound;
import static org.eclipse.edc.spi.result.StoreResult.success;

public class InMemoryCredentialDefinitionStore extends InMemoryEntityStore<CredentialDefinition> implements CredentialDefinitionStore {

    @Override
    public StoreResult<Void> create(CredentialDefinition credentialDefinition) {
        lock.writeLock().lock();
        try {
            if (store.containsKey(credentialDefinition.getId())) {
                return alreadyExists(alreadyExistsErrorMessage(credentialDefinition.getId()));
            }
            store.put(credentialDefinition.getId(), credentialDefinition);
            return success(null);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public StoreResult<Void> update(CredentialDefinition credentialDefinition) {
        lock.writeLock().lock();
        try {
            if (!store.containsKey(credentialDefinition.getId())) {
                return notFound(notFoundErrorMessage(credentialDefinition.getId()));
            }
            store.put(credentialDefinition.getId(), credentialDefinition);

            return success();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public StoreResult<Void> deleteById(String id) {
        lock.writeLock().lock();
        try {
            if (!store.containsKey(id)) {
                return notFound(notFoundErrorMessage(id));
            }
            store.remove(id);
            return success();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    protected String getId(CredentialDefinition newObject) {
        return newObject.getId();
    }

    @Override
    protected QueryResolver<CredentialDefinition> createQueryResolver() {
        criterionOperatorRegistry.registerPropertyLookup(new CredentialDefinitionLookup());

        return new ReflectionBasedQueryResolver<>(CredentialDefinition.class, criterionOperatorRegistry);
    }
}
