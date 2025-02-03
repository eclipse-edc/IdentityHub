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

package org.eclipse.edc.identityhub.spi.verifiablecredentials.store;


import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;

import java.util.Collection;

/**
 * The CredentialStore interface represents a store that manages verifiable credentials.
 * It provides methods for creating, querying, updating, and deleting credentials.
 */
public interface CredentialStore {
    /**
     * Creates a verifiable credential resource in the store.
     *
     * @param credentialResource The verifiable credential resource to create.
     * @return A StoreResult object indicating the result of the operation.
     */
    StoreResult<Void> create(VerifiableCredentialResource credentialResource);

    /**
     * Queries the store for verifiable credentials based on the given query specification.
     *
     * @param querySpec The {@link QuerySpec} indicating the criteria for the query.
     * @return A {@link StoreResult} object containing a list of {@link VerifiableCredentialResource} objects that match the query.
     */
    StoreResult<Collection<VerifiableCredentialResource>> query(QuerySpec querySpec);

    /**
     * Updates a verifiable credential resource in the store.
     *
     * @param credentialResource The verifiable credential resource to update. Note that <em>all fields</em> are overwritten.
     * @return A {@link StoreResult} object indicating the result of the operation.
     */
    StoreResult<Void> update(VerifiableCredentialResource credentialResource);

    /**
     * Deletes a verifiable credential resource from the store based on the given ID.
     *
     * @param id The ID of the verifiable credential resource to delete.
     * @return A {@link StoreResult} object indicating the result of the operation.
     */
    StoreResult<Void> deleteById(String id);

    default String alreadyExistsErrorMessage(String id) {
        return "A VerifiableCredentialResource with ID '%s' already exists.".formatted(id);
    }

    default String notFoundErrorMessage(String id) {
        return "A VerifiableCredentialResource with ID '%s' does not exist.".formatted(id);
    }

    /**
     * Obtains a single credential by its ID
     *
     * @param credentialId the credential ID
     * @return a result containing the {@link VerifiableCredentialResource}, or an error if not found etc.
     */
    StoreResult<VerifiableCredentialResource> findById(String credentialId);
}
