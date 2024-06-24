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

package org.eclipse.edc.identityhub.spi.keypair.events;

import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairResource;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.spi.event.Event;

import java.util.Objects;

/**
 * Base class for all events that relate to state changes or actions regarding KeyPairs
 */
public abstract class KeyPairEvent extends Event {
    protected String participantId;
    protected String keyPairResourceId;
    protected String keyId;

    /**
     * The ID of the {@link KeyPairResource}. This is the internal database ID.
     */
    public String getKeyPairResourceId() {
        return keyPairResourceId;
    }

    /**
     * The Key ID. For example, this is what would go into the {@code kid} header of a JWT.
     */
    public String getKeyId() {
        return keyId;
    }

    /**
     * The ID of the {@link ParticipantContext} that owns the KeyPair resource.
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

        public B keyPairResourceId(String keyPairResourceId) {
            event.keyPairResourceId = keyPairResourceId;
            return self();
        }

        public T build() {
            Objects.requireNonNull((event.participantId));
            return event;
        }
    }
}
