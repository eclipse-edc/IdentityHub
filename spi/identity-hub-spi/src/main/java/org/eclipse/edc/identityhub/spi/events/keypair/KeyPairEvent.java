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

package org.eclipse.edc.identityhub.spi.events.keypair;

import org.eclipse.edc.spi.event.Event;

import java.util.Objects;

/**
 * Base class for all events that relate to state changes or actions regarding KeyPairs
 */
public abstract class KeyPairEvent extends Event {
    protected String participantId;
    protected String keyId;

    /**
     * The ID of the Key stored in the {@link org.eclipse.edc.identityhub.spi.model.KeyPairResource}
     */
    public String getKeyId() {
        return keyId;
    }

    /**
     * The ID of the {@link org.eclipse.edc.identityhub.spi.model.participant.ParticipantContext} that owns the KeyPair resource.
     */
    public String getParticipantId() {
        return participantId;
    }

    public abstract static class Builder<T extends KeyPairEvent, B extends KeyPairEvent.Builder<T, B>> {

        protected final T event;

        protected Builder(T event) {
            this.event = event;
        }

        public abstract B self();

        public B participantId(String assetId) {
            event.participantId = assetId;
            return self();
        }

        public B keyId(String keyId) {
            event.keyId = keyId;
            return self();
        }

        public T build() {
            Objects.requireNonNull((event.participantId));
            return event;
        }
    }
}
