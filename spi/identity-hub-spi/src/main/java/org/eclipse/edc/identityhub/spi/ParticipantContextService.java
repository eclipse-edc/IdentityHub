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

package org.eclipse.edc.identityhub.spi;

import org.eclipse.edc.identityhub.spi.model.participant.ParticipantContext;
import org.eclipse.edc.spi.result.ServiceResult;

/**
 * Handles {@link ParticipantContext} objects, their lifecycle and their authentication tokens.
 */
public interface ParticipantContextService {

    /**
     * Creates a new participant context. If one with the same ID exists, a failure is returned
     *
     * @param context The new participant context
     * @return success if created, or a failure if already exists.
     */
    ServiceResult<Void> createParticipantContext(ParticipantContext context);

    /**
     * Fetches the {@link ParticipantContext} by ID.
     *
     * @param participantId the ID to look for.
     * @return The participant context, or a failure if not found.
     */
    ServiceResult<ParticipantContext> getParticipantContext(String participantId);

    /**
     * Deletes the {@link ParticipantContext} by ID.
     *
     * @param participantId the ID to delete.
     * @return Success if deleted, or a failure if not found.
     */
    ServiceResult<Void> deleteParticipantContext(String participantId);

    /**
     * Re-generates the API token for a particular participant context. The API token will be overwritten in the vault using
     * the same alias as before.
     * Note that API tokens are <strong>never</strong> stored in the database.
     *
     * @param participantId The participant ID to regenerate the API token for.
     * @return the new API token, or a failure
     */
    ServiceResult<String> regenerateApiToken(String participantId);
}
