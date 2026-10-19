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

package org.eclipse.edc.issuerservice.spi.holder.store;

import org.eclipse.edc.issuerservice.spi.holder.model.Holder;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;

import java.util.Collection;

/**
 * Stores {@link Holder} objects and provides basic CRUD operations. Holder IDs are only unique within a participant
 * context, so a holder is identified by its participant context ID and its ID.
 */
public interface HolderStore {

    /**
     * Find a {@link Holder} by its ID
     *
     * @param participantContextId The ID of the participant context that owns the holder
     * @param id                   The holder's ID (NOT the DID!)
     */
    StoreResult<Holder> findById(String participantContextId, String id);

    /**
     * Stores the holder in the database
     *
     * @param holder the {@link Holder}
     * @return success if stored, a failure if the participant context already has a Holder with the same ID
     */
    StoreResult<Void> create(Holder holder);

    /**
     * Updates the holder with the given data. Existing data will be overwritten with the given object, except the ID and
     * the participant context ID, which identify the holder.
     *
     * @param holder a (fully populated) {@link Holder}
     * @return success if updated, a failure if not exist
     */
    StoreResult<Void> update(Holder holder);

    /**
     * Queries for holders
     *
     * @param querySpec the query to use.
     * @return A (potentially empty) list of holders.
     */
    StoreResult<Collection<Holder>> query(QuerySpec querySpec);

    /**
     * Deletes a holder with the given ID
     *
     * @param participantContextId the ID of the participant context that owns the holder
     * @param holderId             the holder ID
     * @return success if deleted, a failure otherwise
     */
    StoreResult<Void> deleteById(String participantContextId, String holderId);

    default String alreadyExistsErrorMessage(String id) {
        return "A Holder with ID '%s' already exists.".formatted(id);
    }

    default String notFoundErrorMessage(String id) {
        return "A Holder with ID '%s' does not exist.".formatted(id);
    }
}
