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

import static org.eclipse.edc.iam.verifiablecredentials.spi.VcConstants.VC_PREFIX;

public class IssuerMetadata {

    public static final String ISSUER_METADATA_TERM = "IssuerMetadata";
    public static final String ISSUER_METADATA_ISSUER_IRI = VC_PREFIX + "issuer";
    public static final String ISSUER_METADATA_CREDENTIALS_SUPPORTED_TERM = "credentialsSupported";

    private String issuer;
    private List<CredentialObject> credentialsSupported = new ArrayList<>();

    public List<CredentialObject> getCredentialsSupported() {
        return credentialsSupported;
    }

    public String getIssuer() {
        return issuer;
    }

    public static class Builder {
        private final IssuerMetadata issuerMetadata;

        private Builder() {
            this.issuerMetadata = new IssuerMetadata();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder issuer(String issuer) {
            this.issuerMetadata.issuer = issuer;
            return this;
        }

        public Builder credentialsSupported(List<CredentialObject> credentialsSupported) {
            this.issuerMetadata.credentialsSupported = credentialsSupported;
            return this;
        }

        public Builder credentialSupported(CredentialObject credentialSupported) {
            this.issuerMetadata.credentialsSupported.add(credentialSupported);
            return this;
        }

        public IssuerMetadata build() {
            return this.issuerMetadata;
        }
    }
}
