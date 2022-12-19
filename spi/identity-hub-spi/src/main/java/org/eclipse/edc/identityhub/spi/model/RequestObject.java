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

package org.eclipse.edc.identityhub.spi.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Collection;
import java.util.Collections;

/**
 * See <a href="https://identity.foundation/decentralized-web-node/spec/#request-objects">Request Object documentation</a>
 */
@JsonDeserialize(builder = RequestObject.Builder.class)
public class RequestObject {

    private Collection<MessageRequestObject> messages;

    private RequestObject() {
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

        public RequestObject.Builder messages(Collection<MessageRequestObject> messages) {
            requestObject.messages = Collections.unmodifiableCollection(messages);
            return this;
        }

        public RequestObject build() {
            return requestObject;
        }
    }
}
