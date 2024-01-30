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

import org.eclipse.edc.spi.event.Event;

import java.util.Objects;

/**
 * Base class for all events related to state changes and actions of {@link org.eclipse.edc.identityhub.spi.model.participant.ParticipantContext}s
 */
public abstract class ParticipantContextEvent extends Event {
    protected String participantId;

    public String getParticipantId() {
        return participantId;
    }

    public abstract static class Builder<T extends ParticipantContextEvent, B extends ParticipantContextEvent.Builder<T, B>> {

        protected final T event;

        protected Builder(T event) {
            this.event = event;
        }

        public abstract B self();

        public B participantId(String assetId) {
            event.participantId = assetId;
            return self();
        }

        public T build() {
            Objects.requireNonNull((event.participantId));
            return event;
        }
    }
}
