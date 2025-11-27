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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.participantcontext.spi.types.AbstractParticipantResource;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a single user account in the issuer service. Members of a dataspace that would like this issuer service to issue them
 * credentials, will need such an account with an issuer service.
 */
@JsonDeserialize(builder = Holder.Builder.class)
public class Holder extends AbstractParticipantResource {

    private String holderId;
    private String did;
    private String holderName;
    private boolean anonymous;
    private Map<String, Object> properties = new HashMap<>();
    private long lastModifiedAt;

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

    public long getLastModifiedAt() {
        return lastModifiedAt;
    }

    /**
     * Indicates whether the holder entity was created as part of a credential request, or whether it had existed before.
     * {@code AttestationSource} implementations can use this to determine whether linked data exists or not.
     */
    public boolean isAnonymous() {
        return anonymous;
    }

    public Map<String, Object> getProperties() {
        return properties;
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

        public Builder properties(Map<String, Object> properties) {
            entity.properties = properties;
            return this;
        }

        public Builder property(String key, Object value) {
            entity.properties.put(key, value);
            return this;
        }

        public Builder lastModifiedAt(long lastModified) {
            entity.lastModifiedAt = lastModified;
            return this;
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public Holder build() {
            super.build();
            Objects.requireNonNull(entity.holderId, "Holder ID must not be null");
            Objects.requireNonNull(entity.did, "DID must not be null");
            Objects.requireNonNull(entity.participantContextId, "Participant context ID must not be null");

            if (entity.getLastModifiedAt() == 0L) {
                entity.lastModifiedAt = entity.clock.millis();
            }
            return entity;
        }

        @JsonProperty("anonymous")
        public Builder isAnonymous(boolean anonymous) {
            entity.anonymous = anonymous;
            return this;
        }
    }
}
