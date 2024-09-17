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
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.jetbrains.annotations.Nullable;

/**
 * Event that signals that a KeyPair was rotated.
 */
@JsonDeserialize(builder = KeyPairRotated.Builder.class)
public class KeyPairRotated extends KeyPairEvent {
    private @Nullable KeyDescriptor newKeyDescriptor;

    @Override
    public String name() {
        return "keypair.rotated";
    }

    public @Nullable KeyDescriptor getNewKeyDescriptor() {
        return newKeyDescriptor;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends KeyPairEvent.Builder<KeyPairRotated, Builder> {

        private Builder() {
            super(new KeyPairRotated());
        }

        @JsonCreator
        public static KeyPairRotated.Builder newInstance() {
            return new KeyPairRotated.Builder();
        }

        @Override
        public KeyPairRotated.Builder self() {
            return this;
        }

        public Builder newKeyDescriptor(@Nullable KeyDescriptor newKeyDesc) {
            event.newKeyDescriptor = newKeyDesc;
            return this;
        }
    }
}
