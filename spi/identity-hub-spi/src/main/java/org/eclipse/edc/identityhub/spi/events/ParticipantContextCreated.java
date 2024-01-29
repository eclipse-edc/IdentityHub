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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

public class ParticipantContextCreated extends ParticipantContextEvent {
    @Override
    public String name() {
        return "participantcontext.created";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ParticipantContextEvent.Builder<ParticipantContextCreated, Builder> {

        private Builder() {
            super(new ParticipantContextCreated());
        }

        @Override
        public Builder self() {
            return this;
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }
    }
}
