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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * See <a href="https://identity.foundation/decentralized-web-node/spec/#response-objects">Response Object documentation</a>
 */
@JsonDeserialize(builder = ResponseObject.Builder.class)
public class ResponseObject {
    private RequestStatus status;
    private Collection<MessageResponseObject> replies = new ArrayList<>();

    private ResponseObject() {
    }

    public RequestStatus getStatus() {
        return status;
    }

    public Collection<MessageResponseObject> getReplies() {
        return replies;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        ResponseObject responseObject;

        private Builder() {
            responseObject = new ResponseObject();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }


        public Builder status(RequestStatus status) {
            responseObject.status = status;
            return this;
        }

        public Builder replies(List<MessageResponseObject> replies) {
            responseObject.replies = Collections.unmodifiableCollection(replies);
            return this;
        }

        public ResponseObject build() {
            return responseObject;
        }
    }
}
