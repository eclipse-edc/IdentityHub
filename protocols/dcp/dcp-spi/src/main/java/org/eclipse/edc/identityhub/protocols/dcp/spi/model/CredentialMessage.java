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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Objects.requireNonNull;

public class CredentialMessage {
    public static final String TYPE_TERM = "type";
    public static final String CREDENTIALS_TERM = "credentials";
    public static final String ISSUER_PID_TERM = "issuerPid";
    public static final String HOLDER_PID_TERM = "holderPid";
    public static final String STATUS_TERM = "status";
    public static final String CREDENTIAL_MESSAGE_TERM = "CredentialMessage";
    private static final List<String> ALLOWED_STATUS = List.of("ISSUED", "REJECTED");

    private Collection<CredentialContainer> credentials = new ArrayList<>();
    private String issuerPid;
    private String holderPid;
    private String status;

    public String getStatus() {
        return status;
    }

    public Collection<CredentialContainer> getCredentials() {
        return credentials;
    }

    public String getIssuerPid() {
        return issuerPid;
    }

    public String getHolderPid() {
        return holderPid;
    }

    public static final class Builder {
        private final CredentialMessage credentialMessage;

        private Builder(CredentialMessage credentialMessage) {
            this.credentialMessage = credentialMessage;
        }

        public static Builder newInstance() {
            return new Builder(new CredentialMessage());
        }

        public Builder credentials(Collection<CredentialContainer> credentials) {
            this.credentialMessage.credentials = credentials;
            return this;
        }

        public Builder credential(CredentialContainer credential) {
            this.credentialMessage.credentials.add(credential);
            return this;
        }

        public Builder issuerPid(String issuerPid) {
            this.credentialMessage.issuerPid = issuerPid;
            return this;
        }

        public Builder holderPid(String holderPid) {
            this.credentialMessage.holderPid = holderPid;
            return this;
        }

        public Builder status(String status) {
            this.credentialMessage.status = status;
            return this;
        }

        public CredentialMessage build() {
            requireNonNull(credentialMessage.issuerPid, "issuerPid");
            requireNonNull(credentialMessage.holderPid, "holderPid");
            requireNonNull(credentialMessage.status, "status");

            if (!ALLOWED_STATUS.contains(credentialMessage.status.toUpperCase())) {
                throw new IllegalArgumentException("Invalid status value! Expected %s but got %s".formatted(ALLOWED_STATUS, credentialMessage.status));
            }
            return credentialMessage;
        }
    }
}
