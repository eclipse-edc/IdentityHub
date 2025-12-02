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

package org.eclipse.edc.identityhub.spi.participantcontext.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.eclipse.edc.participantcontext.spi.types.AbstractParticipantResource;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

/**
 * Abstract class representing an Identity Resource.
 * Identity resources have an ID, a timestamp, an issuer ID, a holder ID, and a clock.
 * They can be extended with custom properties and behaviors.
 */
public abstract class IdentityResource extends AbstractParticipantResource {
    protected long timestamp;
    protected String issuerId;
    protected String holderId;

    @JsonIgnore
    public Clock getClock() {
        return clock;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getIssuerId() {
        return issuerId;
    }

    public String getHolderId() {
        return holderId;
    }

    public abstract static class Builder<T extends IdentityResource, B extends Builder<T, B>> extends AbstractParticipantResource.Builder<T, B> {

        protected Builder(T entity) {
            super(entity);
        }

        public B timestamp(long timestamp) {
            entity.timestamp = timestamp;
            return self();
        }

        public B issuerId(String issuerId) {
            entity.issuerId = issuerId;
            return self();
        }

        public B holderId(String holderId) {
            entity.holderId = holderId;
            return self();
        }

        @Override
        protected T build() {
            super.build();
            Objects.requireNonNull(entity.issuerId, "Must have an issuer.");
            Objects.requireNonNull(entity.holderId, "Must have a holder.");

            if (entity.id == null) {
                entity.id = UUID.randomUUID().toString();
            }

            if (entity.timestamp == 0) {
                entity.timestamp = entity.clock.millis();
            }
            return entity;
        }
    }
}
