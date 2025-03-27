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
import java.util.List;

public class CredentialOfferMessage {
    public static final String CREDENTIAL_OFFER_MESSAGE_TERM = "CredentialOfferMessage";
    public static final String CREDENTIAL_ISSUER_TERM = "issuer";
    public static final String CREDENTIALS_TERM = "credentials";
    private List<CredentialObject> credentials = new ArrayList<>();
    private String issuer;

    private CredentialOfferMessage() {

    }

    public String getIssuer() {
        return issuer;
    }

    public List<CredentialObject> getCredentials() {
        return credentials;
    }


    public static final class Builder {
        private final CredentialOfferMessage instance;

        private Builder(CredentialOfferMessage instance) {
            this.instance = instance;
        }

        public static Builder newInstance() {
            return new Builder(new CredentialOfferMessage());
        }

        public Builder issuer(String issuer) {
            this.instance.issuer = issuer;
            return this;
        }

        public Builder credentials(List<CredentialObject> credentials) {
            this.instance.credentials = credentials;
            return this;
        }

        public Builder credential(CredentialObject credential) {
            this.instance.credentials.add(credential);
            return this;
        }

        public CredentialOfferMessage build() {
            return instance;
        }

    }
}
