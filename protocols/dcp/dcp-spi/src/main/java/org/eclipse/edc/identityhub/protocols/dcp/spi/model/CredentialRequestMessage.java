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

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a request made by a credential holder to a credential issuer to request the issuance of one or several
 * Verifiable Credentials
 */
@JsonDeserialize(builder = CredentialRequestMessage.Builder.class)
public class CredentialRequestMessage {

    public static final String CREDENTIAL_REQUEST_MESSAGE_TERM = "CredentialRequestMessage";
    public static final String CREDENTIAL_REQUEST_MESSAGE_CREDENTIALS_TERM = "credentials";
    public static final String CREDENTIAL_REQUEST_MESSAGE_HOLDER_PID_TERM = "holderPid";

    private List<CredentialRequestSpecifier> credentials = new ArrayList<>();
    private String holderPid;

    public List<CredentialRequestSpecifier> getCredentials() {
        return credentials;
    }

    public String getHolderPid() {
        return holderPid;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {

        private final CredentialRequestMessage instance;

        private Builder() {
            instance = new CredentialRequestMessage();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }


        public Builder credentials(List<CredentialRequestSpecifier> credentials) {
            this.instance.credentials = credentials;
            return this;
        }

        public Builder credential(CredentialRequestSpecifier credential) {
            this.instance.credentials.add(credential);
            return this;
        }

        public Builder holderPid(String holderPid) {
            this.instance.holderPid = holderPid;
            return this;
        }

        public CredentialRequestMessage build() {
            return instance;
        }
    }

    /**
     * Represents a response for a {@link CredentialRequestMessage}
     */
    public record Response(String requestId) {

    }
}
