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

package org.eclipse.edc.issuerservice.spi.holder.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.identityhub.spi.participantcontext.model.AbstractParticipantResource;

import java.util.Objects;

/**
 * Represents a single user account in the issuer service. Members of a dataspace that would like this issuer service to issue them
 * credentials, will need such an account with an issuer service.
 */
public class Holder extends AbstractParticipantResource {

    private String holderId;
    private String did;
    private String holderName;

    private Holder() {
    }

    public String getDid() {
        return did;
    }

    public String getHolderId() {
        return holderId;
    }

    public String getHolderName() {
        return holderName;
    }


    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder extends AbstractParticipantResource.Builder<Holder, Builder> {

        private Builder() {
            super(new Holder());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder holderId(String holderId) {
            entity.holderId = holderId;
            return this;
        }

        public Builder did(String did) {
            entity.did = did;
            return this;
        }

        public Builder holderName(String holderName) {
            entity.holderName = holderName;
            return this;
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public Holder build() {
            Objects.requireNonNull(entity.holderId, "Holder ID must not be null");
            Objects.requireNonNull(entity.did, "DID must not be null");
            Objects.requireNonNull(entity.participantContextId, "Participant context ID must not be null");
            return super.build();
        }
    }
}
