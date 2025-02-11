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

package org.eclipse.edc.issuerservice.spi.issuance.attestation;

import org.eclipse.edc.issuerservice.spi.issuance.model.AttestationDefinition;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Persists attestation definitions.
 */
public interface AttestationDefinitionStore {

    /**
     * Returns a definition for the id or null if not found.
     */
    @Nullable
    AttestationDefinition resolveDefinition(String id);

    /**
     * Persists a new attestation definition.
     */
    StoreResult<Void> create(AttestationDefinition credentialResource);

    /**
     * Updates an existing attestation definition.
     */
    StoreResult<Void> update(AttestationDefinition credentialResource);

    /**
     * Removes an attestation from persistent storage.
     */
    StoreResult<Void> deleteById(String id);

    /**
     * Queries for attestation definitions
     *
     * @param querySpec the query to use.
     * @return A (potentially empty) list of attestation definitions.
     */
    StoreResult<Collection<AttestationDefinition>> query(QuerySpec querySpec);


    default String alreadyExistsErrorMessage(String id) {
        return "An Attestation definition with ID '%s' already exists.".formatted(id);
    }

    default String notFoundErrorMessage(String id) {
        return "A Credential definition ID '%s' does not exist.".formatted(id);
    }
}
