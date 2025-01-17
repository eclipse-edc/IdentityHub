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

package org.eclipse.edc.identityhub.protocols.dcp.issuer.api.v1alpha.credentialrequest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.List;

/**
 * Represents a request made by a credential holder to a credential issuer to request the issuance of one or several
 * Verifiable Credentials
 */
@JsonDeserialize(builder = CredentialRequestMessage.Builder.class)
public class CredentialRequestMessage {
    private List<CredentialRequest> credentials = List.of();
    private List<String> contexts = List.of("https://w3id.org/dspace-dcp/v1.0/dcp.jsonld");
    private String type = "CredentialRequestMessage";

    public String getType() {
        return type;
    }

    public List<String> getContexts() {
        return contexts;
    }

    public List<CredentialRequest> getCredentials() {
        return credentials;
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

        public Builder contexts(List<String> contexts) {
            this.instance.contexts = contexts;
            return this;
        }

        public Builder type(String type) {
            this.instance.type = type;
            return this;
        }

        public Builder credentials(List<CredentialRequest> credentials) {
            this.instance.credentials = credentials;
            return this;
        }

        public Builder credential(CredentialRequest credential) {
            this.instance.credentials.add(credential);
            return this;
        }

        public CredentialRequestMessage build() {
            if (instance.contexts == null || instance.contexts.isEmpty()) {
                throw new IllegalArgumentException("@context object cannot be null or empty");
            }
            return instance;
        }
    }
}
