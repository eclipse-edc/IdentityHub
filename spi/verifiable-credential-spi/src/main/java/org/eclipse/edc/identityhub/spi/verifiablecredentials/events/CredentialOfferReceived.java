/*
 *  Copyright (c) 2025 Metaform Systems Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.spi.verifiablecredentials.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = CredentialOfferReceived.Builder.class)
public class CredentialOfferReceived extends CredentialOfferEvent {
    @Override
    public String name() {
        return "credentialoffer.received";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends CredentialOfferEvent.Builder<CredentialOfferReceived, Builder> {

        private Builder() {
            super(new CredentialOfferReceived());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        @Override
        public Builder self() {
            return this;
        }
    }
}
