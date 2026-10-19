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
import org.eclipse.edc.identityhub.store.InMemoryParticipantResourceStore;
import org.eclipse.edc.spi.query.QueryResolver;
import org.eclipse.edc.store.ReflectionBasedQueryResolver;

/**
 * In-memory variant of the {@link CredentialStore} that is thread-safe.
 */
public class InMemoryCredentialStore extends InMemoryParticipantResourceStore<VerifiableCredentialResource> implements CredentialStore {

    @Override
    protected QueryResolver<VerifiableCredentialResource> createQueryResolver() {
        criterionOperatorRegistry.registerPropertyLookup(new CredentialResourceLookup());
        return new ReflectionBasedQueryResolver<>(VerifiableCredentialResource.class, criterionOperatorRegistry);
    }

    @Override
    protected String getResourceId(VerifiableCredentialResource entity) {
        return entity.getId();
    }

    @Override
    protected String notFoundMessage(String id) {
        return notFoundErrorMessage(id);
    }

    @Override
    protected String alreadyExistsMessage(String id) {
        return alreadyExistsErrorMessage(id);
    }
}
