/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.identityhub.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

/**
 * See <a href="https://identity.foundation/decentralized-web-node/spec/#messages">Message documentation</a>
 */
@JsonDeserialize(builder = MessageRequestObject.Builder.class)
public class MessageRequestObject {
    private Descriptor descriptor;
    private byte[] data;

    private MessageRequestObject() {
    }

    public Descriptor getDescriptor() {
        return descriptor;
    }

    @Schema(description = "Optional base64Url encoded string of the message data")
    public byte[] getData() {
        return data;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final MessageRequestObject messageRequestObject;

        private Builder() {
            this(new MessageRequestObject());
        }

        private Builder(MessageRequestObject messageRequestObject) {
            this.messageRequestObject = messageRequestObject;
        }

        @JsonCreator()
        public static MessageRequestObject.Builder newInstance() {
            return new MessageRequestObject.Builder();
        }

        public MessageRequestObject.Builder descriptor(Descriptor descriptor) {
            messageRequestObject.descriptor = descriptor;
            return this;
        }

        public MessageRequestObject.Builder data(byte[] data) {
            messageRequestObject.data = data == null ? null : data.clone();
            return this;
        }


        public MessageRequestObject build() {
            Objects.requireNonNull(messageRequestObject.getDescriptor(), "MessageRequestObject must contain a descriptor property.");
            return messageRequestObject;
        }
    }
}
