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
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;

/**
 * Event that signals that a {@link ParticipantContext} was created
 */
@JsonDeserialize(builder = ParticipantContextCreated.Builder.class)
public class ParticipantContextCreated extends ParticipantContextEvent {
    private ParticipantManifest manifest;

    private ParticipantContextCreated() {
    }

    @Override
    public String name() {
        return "participantcontext.created";
    }

    public ParticipantManifest getManifest() {
        return manifest;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ParticipantContextEvent.Builder<ParticipantContextCreated, Builder> {

        private Builder() {
            super(new ParticipantContextCreated());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        @Override
        public Builder self() {
            return this;
        }

        public Builder manifest(ParticipantManifest manifest) {
            this.event.manifest = manifest;
            return this;
        }
    }
}
