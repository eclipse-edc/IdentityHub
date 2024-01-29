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

package org.eclipse.edc.identityhub.spi.events;

import org.eclipse.edc.identityhub.spi.model.participant.ParticipantContext;
import org.eclipse.edc.spi.observe.Observable;

/**
 * Interface implemented by listeners registered to observe participant context changes via {@link Observable#registerListener}.
 * The listener must be called after the state changes are persisted.
 */
public interface ParticipantContextListener {

    default void created(ParticipantContext newContext) {

    }

    default void updated(ParticipantContext updatedContext) {

    }

    default void deleted(ParticipantContext deletedContext) {

    }
}
