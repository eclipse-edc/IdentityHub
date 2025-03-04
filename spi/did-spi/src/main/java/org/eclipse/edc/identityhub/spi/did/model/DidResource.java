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

package org.eclipse.edc.identityhub.spi.did.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.identityhub.spi.participantcontext.model.AbstractParticipantResource;

import java.time.Clock;
import java.util.Objects;

/**
 * This class wraps a {@link org.eclipse.edc.iam.did.spi.document.DidDocument} and represents its lifecycle in the identity hub.
 */
public class DidResource extends AbstractParticipantResource {
    @JsonIgnore
    private Clock clock = Clock.systemUTC();
    private String did;
    private int state = DidState.INITIAL.code();
    private long stateTimestamp;
    private long createTimestamp;
    private DidDocument document;

    private DidResource() {
    }

    public String getDid() {
        return did;
    }

    public int getState() {
        return state;
    }

    public DidState getStateAsEnum() {
        return DidState.from(state);
    }

    public long getStateTimestamp() {
        return stateTimestamp;
    }

    public long getCreateTimestamp() {
        return createTimestamp;
    }

    public DidDocument getDocument() {
        return document;
    }

    public void transitionState(DidState newState) {
        this.state = newState.code();
    }

    public static final class Builder extends AbstractParticipantResource.Builder<DidResource, DidResource.Builder> {

        private Builder() {
            super(new DidResource());
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder did(String did) {
            this.entity.did = did;
            return this;
        }

        public Builder state(DidState state) {
            this.entity.state = state.code();
            return this;
        }

        public Builder stateTimeStamp(long timestamp) {
            this.entity.stateTimestamp = timestamp;
            return this;
        }

        public Builder clock(Clock clock) {
            this.entity.clock = clock;
            return this;
        }

        public Builder document(DidDocument document) {
            this.entity.document = document;
            return this;
        }

        public Builder createTimestamp(long createdAt) {
            this.entity.createTimestamp = createdAt;
            return this;
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public DidResource build() {
            Objects.requireNonNull(entity.did, "Must have an identifier");
            if (entity.stateTimestamp <= 0) {
                entity.stateTimestamp = entity.clock.millis();
            }
            if (entity.createTimestamp <= 0) {
                entity.createTimestamp = entity.clock.millis();
            }
            return super.build();
        }

        public Builder state(int code) {
            this.entity.state = code;
            return this;
        }


    }
}
