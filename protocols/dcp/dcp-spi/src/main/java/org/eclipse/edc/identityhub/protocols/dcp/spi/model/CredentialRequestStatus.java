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

package org.eclipse.edc.identityhub.protocols.dcp.spi.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Objects;

@JsonDeserialize(builder = CredentialRequestStatus.Builder.class)
public class CredentialRequestStatus {

    public static final String CREDENTIAL_REQUEST_TERM = "CredentialStatus";
    public static final String CREDENTIAL_REQUEST_STATUS_TERM = "status";
    public static final String CREDENTIAL_REQUEST_ISSUER_PID_TERM = "issuerPid";
    public static final String CREDENTIAL_REQUEST_HOLDER_PID_TERM = "holderPid";
    public static final String CREDENTIAL_REQUEST_STATUS_RECEIVED_TERM = "RECEIVED";
    public static final String CREDENTIAL_REQUEST_STATUS_ISSUED_TERM = "ISSUED";
    public static final String CREDENTIAL_REQUEST_STATUS_REJECTED_TERM = "REJECTED";

    private Status status;
    private String issuerPid;
    private String holderPid;

    private CredentialRequestStatus() {
    }

    public Status getStatus() {
        return status;
    }

    public String getIssuerPid() {
        return issuerPid;
    }

    public String getHolderPid() {
        return holderPid;
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

        public Builder issuerPid(String issuerPid) {
            this.credentialRequestStatus.issuerPid = issuerPid;
            return this;
        }

        public Builder holderPid(String holderPid) {
            this.credentialRequestStatus.holderPid = holderPid;
            return this;
        }

        public CredentialRequestStatus build() {
            Objects.requireNonNull(credentialRequestStatus.issuerPid, "issuerPid");
            Objects.requireNonNull(credentialRequestStatus.holderPid, "holderPid");
            Objects.requireNonNull(credentialRequestStatus.status, "status");
            return credentialRequestStatus;
        }
    }
}
