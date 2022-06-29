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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Objects;

/**
 * See <a href="https://identity.foundation/decentralized-web-node/spec/#response-objects">response objects documentation</a>
 * and <a href="https://identity.foundation/decentralized-web-node/spec/#message-level-status-coding">status doc</a>.
 */
@JsonDeserialize(builder = MessageStatus.Builder.class)
public class MessageStatus extends Status {
    public static final MessageStatus OK = new MessageStatus(200, "The message was successfully processed");
    public static final MessageStatus MALFORMED_MESSAGE = new MessageStatus(400, "The message was malformed or improperly constructed");
    public static final MessageStatus INTERFACE_NOT_IMPLEMENTED = new MessageStatus(501, "The interface method is not implemented");

    private MessageStatus(int code, String detail) {
        super(code, detail);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private Integer code;
        private String detail;

        private Builder() {
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder code(int code) {
            this.code = code;
            return this;
        }

        public Builder detail(String detail) {
            this.detail = detail;
            return this;
        }

        public MessageStatus build() {
            Objects.requireNonNull(code, "MessageStatus must contain code property.");
            Objects.requireNonNull(detail, "MessageStatus must contain detail property.");
            return new MessageStatus(code, detail);
        }
    }
}
