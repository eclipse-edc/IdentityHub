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

package org.eclipse.edc.identityhub.protocols.dcp.issuer.api.v1alpha.credentialrequeststatus;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@JsonDeserialize(builder = CredentialRequestStatus.Builder.class)
public class CredentialRequestStatus {
    private List<String> messages = new ArrayList<>();
    private String requestStatus; //todo: change this to an enum later
    private Instant timestamp;
    private String requestId;

    private CredentialRequestStatus() {
    }

    public List<String> getMessages() {
        return messages;
    }

    public String getRequestStatus() {
        return requestStatus;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getRequestId() {
        return requestId;
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

        public Builder requestStatus(String requestStatus) {
            this.credentialRequestStatus.requestStatus = requestStatus;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.credentialRequestStatus.timestamp = timestamp;
            return this;
        }

        public Builder requestId(String requestId) {
            this.credentialRequestStatus.requestId = requestId;
            return this;
        }

        public Builder messages(List<String> messages) {
            this.credentialRequestStatus.messages = messages;
            return this;
        }

        public Builder message(String message) {
            this.credentialRequestStatus.messages.add(message);
            return this;
        }

        public CredentialRequestStatus build() {
            Objects.requireNonNull(credentialRequestStatus.requestId, "requestId");
            Objects.requireNonNull(credentialRequestStatus.timestamp, "timestamp");
            Objects.requireNonNull(credentialRequestStatus.requestStatus, "requestStatus");
            return credentialRequestStatus;
        }
    }
}
