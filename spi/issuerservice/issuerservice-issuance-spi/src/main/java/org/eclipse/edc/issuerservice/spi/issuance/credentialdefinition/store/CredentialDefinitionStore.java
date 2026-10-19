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

package org.eclipse.edc.issuerservice.spi.issuance.credentialdefinition.store;

import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialDefinition;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;

import java.util.Collection;

/**
 * Stores {@link CredentialDefinition} objects and provides basic CRUD operations. Credential definition IDs are only
 * unique within a participant context, so a credential definition is identified by its participant context ID and its ID.
 */
@ExtensionPoint
public interface CredentialDefinitionStore {

    /**
     * Find a {@link CredentialDefinition} by its ID
     *
     * @param participantContextId   The ID of the participant context that owns the credential definition
     * @param credentialDefinitionId The credential definition ID
     */
    StoreResult<CredentialDefinition> findById(String participantContextId, String credentialDefinitionId);

    /**
     * Stores the credential definition in the database
     *
     * @param credentialDefinition the {@link CredentialDefinition}
     * @return success if stored, a failure if the participant context already has a Credential Definition with the same ID
     */
    StoreResult<Void> create(CredentialDefinition credentialDefinition);

    /**
     * Updates the credential definition with the given data. Existing data will be overwritten with the given object,
     * except the ID and the participant context ID, which identify the credential definition.
     *
     * @param credentialDefinition a (fully populated) {@link CredentialDefinition}
     * @return success if updated, a failure if not exist
     */
    StoreResult<Void> update(CredentialDefinition credentialDefinition);

    /**
     * Queries for credential definitions
     *
     * @param querySpec the query to use.
     * @return A (potentially empty) list of credential definitions.
     */
    StoreResult<Collection<CredentialDefinition>> query(QuerySpec querySpec);

    /**
     * Deletes a credential definition with the given ID
     *
     * @param participantContextId   the ID of the participant context that owns the credential definition
     * @param credentialDefinitionId the credential definition ID
     * @return success if deleted, a failure otherwise
     */
    StoreResult<Void> deleteById(String participantContextId, String credentialDefinitionId);

    default String alreadyExistsErrorMessage(String id) {
        return "A Credential definition with ID '%s' already exists.".formatted(id);
    }

    default String notFoundErrorMessage(String id) {
        return "A Credential definition ID '%s' does not exist.".formatted(id);
    }
}
