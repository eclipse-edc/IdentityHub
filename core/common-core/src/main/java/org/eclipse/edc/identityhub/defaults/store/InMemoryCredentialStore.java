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

package org.eclipse.edc.identityhub.defaults.store;

import org.eclipse.edc.identityhub.defaults.CredentialResourceLookup;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.identityhub.store.InMemoryEntityStore;
import org.eclipse.edc.spi.query.QueryResolver;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.store.ReflectionBasedQueryResolver;


/**
 * In-memory variant of the {@link CredentialStore} that is thread-safe.
 */
public class InMemoryCredentialStore extends InMemoryEntityStore<VerifiableCredentialResource> implements CredentialStore {

    @Override
    protected String getId(VerifiableCredentialResource newObject) {
        return participantScopedKey(newObject.getParticipantContextId(), newObject.getId());
    }

    @Override
    protected QueryResolver<VerifiableCredentialResource> createQueryResolver() {
        criterionOperatorRegistry.registerPropertyLookup(new CredentialResourceLookup());
        return new ReflectionBasedQueryResolver<>(VerifiableCredentialResource.class, criterionOperatorRegistry);
    }

    @Override
    public StoreResult<VerifiableCredentialResource> findById(String participantContextId, String credentialId) {
        var result = findById(participantScopedKey(participantContextId, credentialId));
        return result.succeeded() ? result : StoreResult.notFound(notFoundErrorMessage(credentialId));
    }

    @Override
    public StoreResult<Void> create(VerifiableCredentialResource credentialResource) {
        var result = super.create(credentialResource);
        return result.succeeded() ? result : StoreResult.alreadyExists(alreadyExistsErrorMessage(credentialResource.getId()));
    }

    @Override
    public StoreResult<Void> update(VerifiableCredentialResource credentialResource) {
        var result = super.update(credentialResource);
        return result.succeeded() ? result : StoreResult.notFound(notFoundErrorMessage(credentialResource.getId()));
    }

    @Override
    public StoreResult<Void> deleteById(String participantContextId, String id) {
        var result = deleteById(participantScopedKey(participantContextId, id));
        return result.succeeded() ? result : StoreResult.notFound(notFoundErrorMessage(id));
    }
}
