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

package org.eclipse.edc.issuerservice.spi.participant.store;

import org.eclipse.edc.issuerservice.spi.participant.models.Participant;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;

import java.util.Collection;

/**
 * Stores {@link Participant} objects and provides basic CRUD operations
 */
public interface ParticipantStore {

    /**
     * Find a {@link Participant} by its ID
     *
     * @param id The participant's ID (NOT the DID!)
     */
    StoreResult<Participant> findById(String id);

    /**
     * Stores the participant in the database
     *
     * @param participant the {@link Participant}
     * @return success if stored, a failure if a Participant with the same ID already exists
     */
    StoreResult<Void> create(Participant participant);

    /**
     * Updates the participant with the given data. Existing data will be overwritten with the given object.
     *
     * @param participant a (fully populated) {@link Participant}
     * @return success if updated, a failure if not exist
     */
    StoreResult<Void> update(Participant participant);

    /**
     * Queries for participants
     *
     * @param querySpec the query to use.
     * @return A (potentially empty) list of participants.
     */
    StoreResult<Collection<Participant>> query(QuerySpec querySpec);

    /**
     * Deletes a participant with the given ID
     *
     * @param participantId the participant ID
     * @return success if deleted, a failure otherwise
     */
    StoreResult<Void> deleteById(String participantId);
}
