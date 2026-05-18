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
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;

import java.util.Collection;

public class CredentialDelivered extends IssuanceEvent {
    private Collection<VerifiableCredentialContainer> credentials;

    public Collection<VerifiableCredentialContainer> getCredentials() {
        return credentials;
    }

    @Override
    public String name() {
        return "issuance.credential.delivered";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends IssuanceEvent.Builder<CredentialDelivered, Builder> {

        protected Builder() {
            super(new CredentialDelivered());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder credentials(Collection<VerifiableCredentialContainer> credentials) {
            event.credentials = credentials;
            return self();
        }

        @Override
        public Builder self() {
            return this;
        }
    }
}
