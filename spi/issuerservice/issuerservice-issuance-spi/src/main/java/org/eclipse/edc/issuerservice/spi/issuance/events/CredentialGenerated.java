/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.issuerservice.spi.issuance.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;

import java.util.Collection;
import java.util.List;

public class CredentialGenerated extends IssuanceEvent {
    private Collection<VerifiableCredential> credentials;

    public Collection<VerifiableCredential> getCredentials() {
        return credentials;
    }

    @Override
    public String name() {
        return "issuance.credential.generated";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends IssuanceEvent.Builder<CredentialGenerated, Builder> {

        protected Builder() {
            super(new CredentialGenerated());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder credentials(List<VerifiableCredential> credentials) {
            event.credentials = credentials;
            return self();
        }

        @Override
        public Builder self() {
            return this;
        }
    }
}
