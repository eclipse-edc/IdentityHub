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

import static java.util.Optional.ofNullable;

/**
 * In-memory variant of the {@link CredentialStore} that is thread-safe.
 */
public class InMemoryCredentialStore extends InMemoryEntityStore<VerifiableCredentialResource> implements CredentialStore {

    @Override
    protected String getId(VerifiableCredentialResource newObject) {
        return newObject.getId();
    }

    @Override
    protected QueryResolver<VerifiableCredentialResource> createQueryResolver() {
        criterionOperatorRegistry.registerPropertyLookup(new CredentialResourceLookup());
        return new ReflectionBasedQueryResolver<>(VerifiableCredentialResource.class, criterionOperatorRegistry);
    }

    @Override
    public StoreResult<VerifiableCredentialResource> findById(String credentialId) {
        return ofNullable(this.store.get(credentialId))
                .map(StoreResult::success)
                .orElseGet(() -> StoreResult.notFound(notFoundErrorMessage(credentialId)));
    }
}
