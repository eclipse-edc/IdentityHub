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

package org.eclipse.edc.identityhub.spi.participantcontext;

import org.eclipse.edc.identityhub.spi.participantcontext.model.CreateParticipantContextResponse;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * Handles {@link ParticipantContext} objects, their lifecycle and their authentication tokens.
 */
public interface ParticipantContextService {

    /**
     * Creates a new participant context from a manifest. If one with the same ID exists, a failure is returned.
     *
     * @param manifest The new participant context
     * @return success if created, or a failure if already exists.
     */
    ServiceResult<CreateParticipantContextResponse> createParticipantContext(ParticipantManifest manifest);

    /**
     * Fetches the {@link ParticipantContext} by ID.
     *
     * @param participantContextId the ID to look for.
     * @return The participant context, or a failure if not found.
     */
    ServiceResult<ParticipantContext> getParticipantContext(String participantContextId);

    /**
     * Deletes the {@link ParticipantContext} by ID.
     *
     * @param participantContextId the ID to delete.
     * @return Success if deleted, or a failure if not found.
     */
    ServiceResult<Void> deleteParticipantContext(String participantContextId);

    /**
     * Re-generates the API token for a particular participant context. The API token will be overwritten in the vault using
     * the same alias as before.
     * Note that API tokens are <strong>never</strong> stored in the database.
     *
     * @param participantContextId The participant ID to regenerate the API token for.
     * @return the new API token, or a failure
     */
    ServiceResult<String> regenerateApiToken(String participantContextId);

    /**
     * Applies a modification function to the {@link ParticipantContext} and persists the changed object in the database.
     *
     * @param participantContextId The ID of the participant to modify
     * @param modificationFunction A modification function that is applied to the participant context
     * @return success if the update could be performed, a failure otherwise
     */
    ServiceResult<Void> updateParticipant(String participantContextId, Consumer<ParticipantContext> modificationFunction);

    /**
     * Returns a collection of {@link ParticipantContext} objects that match the specified query.
     */
    ServiceResult<Collection<ParticipantContext>> query(QuerySpec querySpec);
}
