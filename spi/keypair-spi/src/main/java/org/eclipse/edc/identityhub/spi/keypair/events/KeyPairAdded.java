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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Event that signals that a key pair was added for a particular participant
 */
@JsonDeserialize(builder = KeyPairAdded.Builder.class)
public class KeyPairAdded extends KeyPairEvent {
    private String publicKeySerialized;
    private String type;

    @Override
    public String name() {
        return "keypair.added";
    }

    public String getPublicKeySerialized() {
        return publicKeySerialized;
    }

    public String getType() {
        return type;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends KeyPairEvent.Builder<KeyPairAdded, Builder> {

        private Builder() {
            super(new KeyPairAdded());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new KeyPairAdded.Builder();
        }

        @Override
        public KeyPairAdded.Builder self() {
            return this;
        }

        public Builder publicKey(String publicKey, String type) {
            event.publicKeySerialized = publicKey;
            event.type = type;
            return this;
        }
    }
}
