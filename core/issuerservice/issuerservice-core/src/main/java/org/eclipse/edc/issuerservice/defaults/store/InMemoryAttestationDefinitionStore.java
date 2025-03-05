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
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionStore;
import org.eclipse.edc.issuerservice.spi.issuance.model.AttestationDefinition;
import org.eclipse.edc.spi.query.QueryResolver;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.store.ReflectionBasedQueryResolver;
import org.jetbrains.annotations.Nullable;

public class InMemoryAttestationDefinitionStore extends InMemoryEntityStore<AttestationDefinition> implements AttestationDefinitionStore {

    @Override
    public @Nullable AttestationDefinition resolveDefinition(String id) {
        return store.get(id);
    }

    @Override
    public StoreResult<Void> deleteById(String id) {
        return super.deleteById(id);
    }

    @Override
    protected String getId(AttestationDefinition newObject) {
        return newObject.getId();
    }

    @Override
    protected QueryResolver<AttestationDefinition> createQueryResolver() {
        return new ReflectionBasedQueryResolver<>(AttestationDefinition.class, criterionOperatorRegistry);
    }
}
