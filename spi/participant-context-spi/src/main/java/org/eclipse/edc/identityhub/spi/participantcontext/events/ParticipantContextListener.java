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

package org.eclipse.edc.identityhub.spi.participantcontext.events;

import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.spi.observe.Observable;

/**
 * Interface implemented by listeners registered to observe participant context changes via {@link Observable#registerListener}.
 * The listener must be called after the state changes are persisted.
 */
public interface ParticipantContextListener {

    /**
     * Notifies about the fact that a new {@link ParticipantContext} has been created, and further action, such as creating keypairs or updating DID documents
     * can now happen.
     *
     * @param newContext The newly created (already persisted) participant context
     * @param manifest   The original manifest based on which the context was created
     */
    default void created(ParticipantContext newContext, ParticipantManifest manifest) {

    }

    /**
     * Notifies about the fact that a {@link ParticipantContext} has been updated, and further action, such as creating keypairs or updating DID documents
     * can now happen.
     *
     * @param updatedContext The updated (already persisted) participant context
     */
    default void updated(ParticipantContext updatedContext) {

    }

    /**
     * Notifies about the fact that the deletion of a {@link ParticipantContext} is imminent. This is useful if resources like keypairs,
     * DID documents etc. should be cleaned up <em>before the deletion of the {@link ParticipantContext}</em>.
     *
     * @param deletedContext The ParticipantContext that is going to be deleted.
     */
    default void deleting(ParticipantContext deletedContext) {

    }

    /**
     * Notifies about the fact that a {@link ParticipantContext} and all associated data (key-pairs, DID documents, etc) have been deleted.
     *
     * @param deletedContext The deleted participant context
     */
    default void deleted(ParticipantContext deletedContext) {

    }
}
