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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Event that signals that a KeyPair was revoked.
 */
@JsonDeserialize(builder = KeyPairRevoked.Builder.class)
public class KeyPairRevoked extends KeyPairEvent {
    @Override
    public String name() {
        return "keypair.revoked";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends KeyPairEvent.Builder<KeyPairRevoked, Builder> {

        private Builder() {
            super(new KeyPairRevoked());
        }

        @Override
        public KeyPairRevoked.Builder self() {
            return this;
        }

        @JsonCreator
        public static KeyPairRevoked.Builder newInstance() {
            return new KeyPairRevoked.Builder();
        }
    }
}
