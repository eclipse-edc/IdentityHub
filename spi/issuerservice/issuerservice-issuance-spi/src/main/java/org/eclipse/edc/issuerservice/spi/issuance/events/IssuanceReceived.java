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

import java.util.Map;

public class IssuanceReceived extends IssuanceEvent {

    private Map<String, CredentialFormat> requestedFormats;

    @Override
    public String name() {
        return "issuance.received";
    }

    public Map<String, CredentialFormat> getRequestedFormats() {
        return requestedFormats;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends IssuanceEvent.Builder<IssuanceReceived, IssuanceReceived.Builder> {

        protected Builder() {
            super(new IssuanceReceived());
        }

        @JsonCreator
        public static IssuanceReceived.Builder newInstance() {
            return new IssuanceReceived.Builder();
        }


        @Override
        public IssuanceReceived.Builder self() {
            return this;
        }

        @Override
        public IssuanceReceived build() {
            return super.build();
        }

        public Builder requestedFormats(Map<String, CredentialFormat> formats) {
            this.event.requestedFormats = formats;
            return self();
        }
    }
}
