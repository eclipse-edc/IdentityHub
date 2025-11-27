/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
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

package org.eclipse.edc.identityhub.spi.participantcontext.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.participantcontext.spi.types.AbstractParticipantResource;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContextState;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * Representation of a participant in Identity Hub.
 */
@JsonDeserialize(builder = IdentityHubParticipantContext.Builder.class)
public class IdentityHubParticipantContext extends ParticipantContext {

    public static final String API_TOKEN_ALIAS = "apiTokenAlias";
    public static final String ROLES = "roles";

    private IdentityHubParticipantContext() {
    }

    public String clientSecretAlias() {
        return ofNullable(properties.get("clientSecret")).map(Object::toString).orElseGet(() -> participantContextId + "-sts-client-secret");
    }


    /**
     * Get the alias, under which the API token for this {@link IdentityHubParticipantContext} is stored in the {@link org.eclipse.edc.spi.security.Vault}.
     * Note that API tokens should <strong>never</strong> be stored in the database, much less so unencrypted.
     */
    public String getApiTokenAlias() {
        return (String) properties.get(API_TOKEN_ALIAS);
    }


    public String getDid() {
        return getIdentity();
    }

    @SuppressWarnings("unchecked")
    public List<String> getRoles() {
        return Collections.unmodifiableList((List<? extends String>) properties.get(ROLES));
    }

    public void setRoles(List<String> roles) {
        properties.put(ROLES, roles);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder extends AbstractParticipantResource.Builder<IdentityHubParticipantContext, Builder> {

        private Builder() {
            super(new IdentityHubParticipantContext());
            entity.createdAt = Instant.now().toEpochMilli();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder createdAt(long createdAt) {
            this.entity.createdAt = createdAt;
            return this;
        }

        public Builder lastModified(long lastModified) {
            this.entity.lastModified = lastModified;
            return this;
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public Builder participantContextId(String participantContextId) {
            this.entity.participantContextId = participantContextId;
            return this;
        }

        @Override
        public IdentityHubParticipantContext build() {
            Objects.requireNonNull(entity.participantContextId, "Participant ID cannot be null");
            Objects.requireNonNull(entity.getApiTokenAlias(), "API Token Alias cannot be null");

            if (entity.getLastModified() == 0L) {
                entity.lastModified = entity.getCreatedAt();
            }
            return super.build();
        }

        public Builder state(ParticipantContextState state) {
            this.entity.state = state.ordinal();
            return this;
        }

        public Builder roles(List<String> roles) {
            this.entity.properties.put(ROLES, roles);
            return this;
        }

        public Builder apiTokenAlias(String apiToken) {
            this.entity.properties.put(API_TOKEN_ALIAS, apiToken);
            return this;
        }

        public Builder property(String key, Object value) {
            entity.properties.put(key, value);
            return this;
        }

        public Builder properties(Map<String, Object> properties) {
            entity.properties = properties;
            return this;
        }

        public Builder did(String did) {
            this.entity.identity = did;
            return this;
        }
    }
}