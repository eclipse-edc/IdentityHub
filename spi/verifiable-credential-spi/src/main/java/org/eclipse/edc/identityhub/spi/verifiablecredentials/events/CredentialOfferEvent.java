/*
 *  Copyright (c) 2025 Metaform Systems Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.spi.verifiablecredentials.events;

import org.eclipse.edc.spi.event.Event;

import java.util.Objects;

public abstract class CredentialOfferEvent extends Event {
    protected String participantContextId;
    protected String issuer;
    protected String id;

    public String getParticipantContextId() {
        return participantContextId;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getId() {
        return id;
    }

    public abstract static class Builder<T extends CredentialOfferEvent, B extends CredentialOfferEvent.Builder<T, B>> {

        protected final T event;

        protected Builder(T event) {
            this.event = event;
        }

        public abstract B self();

        public B participantContextId(String participantContextId) {
            event.participantContextId = participantContextId;
            return self();
        }

        public B id(String id) {
            event.id = id;
            return self();
        }

        public B issuer(String issuer) {
            event.issuer = issuer;
            return self();
        }

        public T build() {
            Objects.requireNonNull(event.participantContextId, "'participantContextId' cannot be null");
            Objects.requireNonNull(event.id, "'id' cannot be null");
            Objects.requireNonNull(event.issuer, "'issuer' cannot be null");
            return event;
        }
    }
}
