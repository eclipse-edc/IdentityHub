/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.identityhub.store.spi;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.dataspaceconnector.spi.entity.Entity;

import java.util.Objects;

@JsonDeserialize(builder = IdentityHubRecord.Builder.class)
public class IdentityHubRecord extends Entity {

    private byte[] payload;

    private IdentityHubRecord() {
    }
    
    public byte[] getPayload() {
        return payload;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends Entity.Builder<IdentityHubRecord, Builder> {

        protected Builder() {
            super(new IdentityHubRecord());
        }

        public static Builder newInstance() {
            return new Builder();
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public IdentityHubRecord build() {
            Objects.requireNonNull(entity.id);
            Objects.requireNonNull(entity.payload);
            return super.build();
        }

        public Builder payload(byte[] payload) {
            entity.payload = payload;
            return self();
        }
    }
}
