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

package org.eclipse.edc.identityhub.spi.participantcontext.store;

import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;

import java.util.Collection;

import static org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantResource.queryByParticipantContextId;

/**
 * Stores {@link ParticipantContext} objects and provides basic CRUD operations.
 */
public interface ParticipantContextStore {
    /**
     * Creates a ParticipantContext resource in the store.
     *
     * @param context The ParticipantContext resource to create.
     * @return success if not exists, a failure if already exists
     */
    StoreResult<Void> create(ParticipantContext context);

    /**
     * Queries the store for ParticipantContexts based on the given query specification.
     *
     * @param querySpec The {@link QuerySpec} indicating the criteria for the query.
     * @return A {@link StoreResult} object containing a list of {@link ParticipantContext} objects that match the query. If none are found, returns an empty stream.
     */
    StoreResult<Collection<ParticipantContext>> query(QuerySpec querySpec);

    /**
     * Updates a ParticipantContext resource in the store.
     *
     * @param participantContext The ParticipantContext resource to update. Note that <em>all fields</em> are overwritten.
     * @return success if participant context exists, failure otherwise
     */
    StoreResult<Void> update(ParticipantContext participantContext);

    /**
     * Deletes a ParticipantContext resource from the store based on the given ID.
     *
     * @param id The ID of the ParticipantContext resource to delete.
     * @return success if the object could be deleted, a failure otherwise
     */
    StoreResult<Void> deleteById(String id);

    default String alreadyExistsErrorMessage(String id) {
        return "A ParticipantContext with ID '%s' already exists.".formatted(id);
    }

    default String notFoundErrorMessage(String id) {
        return "A ParticipantContext with ID '%s' does not exist.".formatted(id);
    }

    default StoreResult<ParticipantContext> findById(String participantContextId) {
        var res = query(queryByParticipantContextId(participantContextId).build());
        if (res.succeeded()) {
            return res.getContent().stream().findFirst()
                    .map(StoreResult::success)
                    .orElse(StoreResult.notFound("ParticipantContext with ID '%s' does not exist.".formatted(participantContextId)));
        }
        return StoreResult.generalError(res.getFailureDetail());
    }
}
