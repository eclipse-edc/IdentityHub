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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.eclipse.edc.spi.result.StoreResult.alreadyExists;
import static org.eclipse.edc.spi.result.StoreResult.notFound;
import static org.eclipse.edc.spi.result.StoreResult.success;

public class InMemoryCredentialDefinitionStore extends InMemoryEntityStore<CredentialDefinition> implements CredentialDefinitionStore {


    private final Map<String, String> credentialTypes = new HashMap<>();

    @Override
    public StoreResult<Void> create(CredentialDefinition credentialDefinition) {
        lock.writeLock().lock();
        try {
            if (credentialTypes.containsKey(credentialDefinition.getCredentialType())) {
                return alreadyExists(alreadyExistsForTypeErrorMessage(credentialDefinition.getCredentialType()));
            }
            if (store.containsKey(credentialDefinition.getId())) {
                return alreadyExists(alreadyExistsErrorMessage(credentialDefinition.getId()));
            }
            store.put(credentialDefinition.getId(), credentialDefinition);
            credentialTypes.put(credentialDefinition.getCredentialType(), credentialDefinition.getId());
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
            var credentialId = credentialTypes.get(credentialDefinition.getCredentialType());
            if (credentialId != null && !credentialId.equals(credentialDefinition.getId())) {
                return alreadyExists(alreadyExistsForTypeErrorMessage(credentialDefinition.getCredentialType()));
            }
            var oldDefinition = store.put(credentialDefinition.getId(), credentialDefinition);

            Optional.ofNullable(oldDefinition)
                    .map(CredentialDefinition::getCredentialType)
                    .ifPresent(credentialTypes::remove);

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
            var credential = store.remove(id);
            credentialTypes.remove(credential.getCredentialType());
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
