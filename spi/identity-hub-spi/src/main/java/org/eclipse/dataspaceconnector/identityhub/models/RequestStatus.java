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
 * and <a href="https://identity.foundation/decentralized-web-node/spec/#request-level-status-coding">status doc</a>.
 */
@JsonDeserialize(builder = RequestStatus.Builder.class)
public class RequestStatus extends Status {
    public static final RequestStatus OK = new RequestStatus(200, "The request was successfully processed");
    public static final RequestStatus DID_NOT_FOUND = new RequestStatus(404, "Target DID not found within the Decentralized Web Node");
    public static final RequestStatus ERROR = new RequestStatus(500, "The request could not be processed correctly");

    private RequestStatus(int code, String detail) {
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

        public RequestStatus build() {
            Objects.requireNonNull(code, "RequestStatus must contain code property.");
            Objects.requireNonNull(detail, "RequestStatus must contain detail property.");
            return new RequestStatus(code, detail);
        }
    }
}
