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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * Representation of a participant in Identity Hub.
 */
@JsonDeserialize(builder = ParticipantContext.Builder.class)
public class ParticipantContext extends AbstractParticipantResource {
    private Map<String, Object> properties = new HashMap<>();
    private List<String> roles = new ArrayList<>();
    private String did;
    private long createdAt;
    private long lastModified;
    private int state; // CREATED, ACTIVATED, DEACTIVATED
    private String apiTokenAlias;


    private ParticipantContext() {
    }

    public String clientSecretAlias() {
        return ofNullable(properties.get("clientSecret")).map(Object::toString).orElseGet(() -> participantContextId + "-sts-client-secret");
    }

    /**
     * The POSIX timestamp in ms when this entry was created. Immutable
     */
    public long getCreatedAt() {
        return createdAt;
    }

    /**
     * The POSIX timestamp in ms when this entry was last modified.
     */
    public long getLastModified() {
        return lastModified;
    }

    /**
     * The ParticipantContext's state. 0 = CREATED, 1 = ACTIVATED, 2 = DEACTIVATED
     */
    public int getState() {
        return state;
    }

    @JsonIgnore
    public ParticipantContextState getStateAsEnum() {
        return ParticipantContextState.values()[state];
    }

    /**
     * Get the alias, under which the API token for this {@link ParticipantContext} is stored in the {@link org.eclipse.edc.spi.security.Vault}.
     * Note that API tokens should <strong>never</strong> be stored in the database, much less so unencrypted.
     */
    public String getApiTokenAlias() {
        return apiTokenAlias;
    }

    /**
     * Updates the last-modified field.
     */
    public void updateLastModified() {
        this.lastModified = Instant.now().toEpochMilli();
    }

    /**
     * Transitions this participant context to the {@link ParticipantContextState#ACTIVATED} state.
     */
    public void activate() {
        this.state = ParticipantContextState.ACTIVATED.ordinal();
    }

    /**
     * Transitions this participant context to the {@link ParticipantContextState#DEACTIVATED} state.
     */
    public void deactivate() {
        this.state = ParticipantContextState.DEACTIVATED.ordinal();
    }

    public String getDid() {
        return did;
    }

    public List<String> getRoles() {
        return Collections.unmodifiableList(roles);
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder extends AbstractParticipantResource.Builder<ParticipantContext, Builder> {

        private Builder() {
            super(new ParticipantContext());
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
        public ParticipantContext build() {
            Objects.requireNonNull(entity.participantContextId, "Participant ID cannot be null");
            Objects.requireNonNull(entity.apiTokenAlias, "API Token Alias cannot be null");

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
            this.entity.roles = roles;
            return this;
        }

        public Builder apiTokenAlias(String apiToken) {
            this.entity.apiTokenAlias = apiToken;
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
            this.entity.did = did;
            return this;
        }
    }
}