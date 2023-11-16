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

package org.eclipse.edc.identityhub.spi.store.model;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

/**
 * Abstract class representing an Identity Resource.
 * Identity resources have an ID, a timestamp, an issuer ID, a holder ID, and a clock.
 * They can be extended with custom properties and behaviors.
 */
public abstract class IdentityResource {
    protected String id;
    protected long timestamp;
    protected String issuerId;
    protected String holderId;
    protected Clock clock;

    public Clock getClock() {
        return clock;
    }

    public String getId() {
        return id;
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

    public abstract static class Builder<T extends IdentityResource, B extends Builder<T, B>> {
        protected final T resource;

        protected Builder(T resource) {
            this.resource = resource;
        }

        public B id(String id) {
            resource.id = id;
            return self();
        }

        public B timestamp(long timestamp) {
            resource.timestamp = timestamp;
            return self();
        }

        public B issuerId(String issuerId) {
            resource.issuerId = issuerId;
            return self();
        }

        public B clock(Clock clock) {
            resource.clock = clock;
            return self();
        }

        public B holderId(String holderId) {
            resource.holderId = holderId;
            return self();
        }

        public abstract B self();

        protected T build() {
            Objects.requireNonNull(resource.issuerId, "Must have an issuer.");
            Objects.requireNonNull(resource.holderId, "Must have a holder.");
            resource.clock = Objects.requireNonNullElse(resource.clock, Clock.systemUTC());

            if (resource.id == null) {
                resource.id = UUID.randomUUID().toString();
            }

            if (resource.timestamp == 0) {
                resource.timestamp = resource.clock.millis();
            }
            return resource;
        }
    }
}
