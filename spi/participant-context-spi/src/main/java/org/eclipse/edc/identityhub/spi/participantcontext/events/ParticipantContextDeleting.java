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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;

/**
 * Event that signals that a {@link ParticipantContext} is in the process of being deleted. This event is emitted <em>before</em>
 * any storage interaction (= deletion) occurs.
 */
@JsonDeserialize(builder = ParticipantContextDeleting.Builder.class)
public class ParticipantContextDeleting extends ParticipantContextEvent {
    private ParticipantContext participantContext;

    @Override
    public String name() {
        return "participantcontext.deleting";
    }

    public ParticipantContext getParticipantContext() {
        return participantContext;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ParticipantContextEvent.Builder<ParticipantContextDeleting, Builder> {

        private Builder() {
            super(new ParticipantContextDeleting());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        @Override
        public Builder self() {
            return this;
        }

        public Builder participant(ParticipantContext deletedContext) {
            this.event.participantContext = deletedContext;
            return self();
        }
    }
}
