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
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;

import java.util.List;
import java.util.Map;

public class IssuanceRequested extends IssuanceEvent {
    private List<String> credentialDefinitionIds;
    private Map<String, CredentialFormat> credentialFormats;

    public List<String> getCredentialDefinitionIds() {
        return credentialDefinitionIds;
    }

    public Map<String, CredentialFormat> getCredentialFormats() {
        return credentialFormats;
    }

    @Override
    public String name() {
        return "issuance.requested";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends IssuanceEvent.Builder<IssuanceRequested, Builder> {

        protected Builder() {
            super(new IssuanceRequested());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new IssuanceRequested.Builder();
        }

        public Builder credentialDefinitionIds(List<String> credentialDefinitionIds) {
            event.credentialDefinitionIds = credentialDefinitionIds;
            return this;
        }

        public Builder credentialFormats(Map<String, CredentialFormat> credentialFormats) {
            event.credentialFormats = credentialFormats;
            return this;
        }


        @Override
        public Builder self() {
            return this;
        }
    }
}
