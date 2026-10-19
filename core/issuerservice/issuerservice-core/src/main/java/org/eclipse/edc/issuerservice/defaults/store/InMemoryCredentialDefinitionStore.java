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

import org.eclipse.edc.identityhub.store.InMemoryParticipantResourceStore;
import org.eclipse.edc.issuerservice.spi.issuance.credentialdefinition.store.CredentialDefinitionStore;
import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialDefinition;
import org.eclipse.edc.spi.query.QueryResolver;
import org.eclipse.edc.store.ReflectionBasedQueryResolver;

public class InMemoryCredentialDefinitionStore extends InMemoryParticipantResourceStore<CredentialDefinition> implements CredentialDefinitionStore {

    @Override
    protected String getResourceId(CredentialDefinition entity) {
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

    @Override
    protected QueryResolver<CredentialDefinition> createQueryResolver() {
        criterionOperatorRegistry.registerPropertyLookup(new CredentialDefinitionLookup());

        return new ReflectionBasedQueryResolver<>(CredentialDefinition.class, criterionOperatorRegistry);
    }
}
