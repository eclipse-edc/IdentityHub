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

package org.eclipse.edc.identityhub.spi.events.participant;

import org.eclipse.edc.identityhub.spi.model.participant.ParticipantContext;
import org.eclipse.edc.identityhub.spi.model.participant.ParticipantManifest;
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

    default void updated(ParticipantContext updatedContext) {

    }

    default void deleted(ParticipantContext deletedContext) {

    }
}
