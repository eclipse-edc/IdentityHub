/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.identithub.did.spi.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.eclipse.edc.iam.did.spi.document.DidDocument;

import java.time.Clock;
import java.util.Objects;

/**
 * This class wraps a {@link org.eclipse.edc.iam.did.spi.document.DidDocument} and represents its lifecycle in the identity hub.
 */
public class DidResource {
    @JsonIgnore
    private Clock clock = Clock.systemUTC();
    private String did;
    private int state = DidState.INITIAL.code();
    private long stateTimestamp;
    private long createTimestamp;
    private DidDocument document;
    // todo: what is this?
    // private List<VerificationRelationship> verificationRelationships;

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

    public static final class Builder {
        private final DidResource resource;

        private Builder() {
            resource = new DidResource();
        }

        public Builder did(String did) {
            this.resource.did = did;
            return this;
        }

        public Builder state(DidState state) {
            this.resource.state = state.code();
            return this;
        }

        public Builder stateTimeStamp(long timestamp) {
            this.resource.stateTimestamp = timestamp;
            return this;
        }

        public Builder clock(Clock clock) {
            this.resource.clock = clock;
            return this;
        }

        public Builder document(DidDocument document) {
            this.resource.document = document;
            return this;
        }

        public Builder createTimestamp(long createdAt) {
            this.resource.createTimestamp = createdAt;
            return this;
        }

        public DidResource build() {
            Objects.requireNonNull(resource.did, "Must have an identifier");
            Objects.requireNonNull(resource.state, "Must have a state");

            if (resource.stateTimestamp <= 0) {
                resource.stateTimestamp = resource.clock.millis();
            }

            return resource;
        }

        public Builder state(int code) {
            this.resource.state = code;
            return this;
        }

        public static Builder newInstance() {
            return new Builder();
        }


    }
}
