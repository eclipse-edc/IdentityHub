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
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * See <a href="https://identity.foundation/decentralized-web-node/spec/#request-objects">Request Object documentation</a>
 */
public class RequestObject {

    private String requestId;
    private String target;
    private Collection<MessageRequestObject> messages;

    private RequestObject() {
    }

    public String getRequestId() {
        return requestId;
    }

    @Schema(description = "[UNSUPPORTED] Decentralized Identifier base URI of the DID-relative URL")
    public String getTarget() {
        return target;
    }

    public Collection<MessageRequestObject> getMessages() {
        return messages;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final RequestObject requestObject;

        private Builder() {
            this(new RequestObject());
        }

        private Builder(RequestObject requestObject) {
            this.requestObject = requestObject;
        }

        @JsonCreator()
        public static RequestObject.Builder newInstance() {
            return new RequestObject.Builder();
        }

        public RequestObject.Builder requestId(String requestId) {
            requestObject.requestId = requestId;
            return this;
        }

        public RequestObject.Builder target(String target) {
            requestObject.target = target;
            return this;
        }

        public RequestObject.Builder messages(Collection<MessageRequestObject> messages) {
            requestObject.messages = Collections.unmodifiableCollection(messages);
            return this;
        }

        public RequestObject build() {
            Objects.requireNonNull(requestObject.getRequestId(), "RequestObject must contain requestId property.");
            Objects.requireNonNull(requestObject.getTarget(), "RequestObject must contain target property.");
            return requestObject;
        }
    }
}
