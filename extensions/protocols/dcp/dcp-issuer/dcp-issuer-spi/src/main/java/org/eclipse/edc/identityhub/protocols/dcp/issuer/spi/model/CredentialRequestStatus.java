/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.protocols.dcp.issuer.spi.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Objects;

@JsonDeserialize(builder = CredentialRequestStatus.Builder.class)
public class CredentialRequestStatus {

    public static final String CREDENTIAL_REQUEST_TERM = "CredentialStatus";
    public static final String CREDENTIAL_REQUEST_STATUS_TERM = "status";
    public static final String CREDENTIAL_REQUEST_REQUEST_ID_TERM = "requestId";

    private Status status;
    private String requestId;

    private CredentialRequestStatus() {
    }

    public Status getStatus() {
        return status;
    }

    public String getRequestId() {
        return requestId;
    }

    public enum Status {
        RECEIVED,
        ISSUED,
        REJECTED
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private final CredentialRequestStatus credentialRequestStatus;

        private Builder() {
            credentialRequestStatus = new CredentialRequestStatus();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder status(Status status) {
            this.credentialRequestStatus.status = status;
            return this;
        }

        public Builder requestId(String requestId) {
            this.credentialRequestStatus.requestId = requestId;
            return this;
        }

        public CredentialRequestStatus build() {
            Objects.requireNonNull(credentialRequestStatus.requestId, "requestId");
            Objects.requireNonNull(credentialRequestStatus.status, "status");
            return credentialRequestStatus;
        }
    }
}
