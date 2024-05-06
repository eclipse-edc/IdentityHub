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
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContextState;

/**
 * Event that signals that a {@link ParticipantContext} was updated
 */
@JsonDeserialize(builder = ParticipantContextUpdated.Builder.class)
public class ParticipantContextUpdated extends ParticipantContextEvent {

    private ParticipantContextState newState;

    @Override
    public String name() {
        return "participantcontext.updated";
    }

    public ParticipantContextState getNewState() {
        return newState;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ParticipantContextEvent.Builder<ParticipantContextUpdated, Builder> {

        private Builder() {
            super(new ParticipantContextUpdated());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        @Override
        public Builder self() {
            return this;
        }

        public Builder newState(ParticipantContextState state) {
            this.event.newState = state;
            return this;
        }
    }
}
