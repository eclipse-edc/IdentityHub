/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.spi.did.store;

import org.eclipse.edc.identityhub.spi.did.model.DidResource;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;

import java.util.Collection;
import java.util.List;

/**
 * The DidResourceStore interface provides CRUD methods for interacting with a store of {@link DidResource} objects.
 */
@ExtensionPoint
public interface DidResourceStore {

    /**
     * Saves a {@link DidResource} object to the store. If a {@link DidResource} with the same {@link DidResource#getDid()} exists,
     * a failure will be returned.
     *
     * @param resource The {@link DidResource} object to be saved.
     * @return A {@link StoreResult} object indicating the success or failure of the operation.
     */
    StoreResult<Void> save(DidResource resource);

    /**
     * Updates a {@link DidResource} object in the store if it exists. If the {@link DidResource} does not exist, a
     * failure is returned and no further database interaction takes place.
     *
     * @param resource The {@link DidResource} object to be updated. Note that this updates the entire object, differential
     *                 updates are not supported.
     * @return A {@link StoreResult} object indicating the success or failure of the operation.
     */
    StoreResult<Void> update(DidResource resource);

    /**
     * Retrieves a {@link DidResource} object from the store for the provided DID.
     *
     * @param did The DID to search for.
     * @return The {@link DidResource} object found in the store, or null if no matching object is found.
     */
    DidResource findById(String did);

    /**
     * Retrieves all {@link DidResource} objects from the store.
     *
     * @return A {@link List} containing {@link DidResource} objects retrieved from the store.
     */
    Collection<DidResource> query(QuerySpec query);

    /**
     * Deletes a {@link DidResource} object from the store with the specified DID. If the specified DID document does not
     * exist, a failure is returned
     *
     * @param did The ID of the {@link DidResource} object to be deleted.
     * @return A {@link StoreResult} object indicating the success or failure of the deletion operation.
     */
    StoreResult<Void> deleteById(String did);

    default String alreadyExistsErrorMessage(String did) {
        return "A DidResource with ID %s already exists.".formatted(did);
    }

    default String notFoundErrorMessage(String did) {
        return "A DidResource with ID %s was not found.".formatted(did);
    }
}
