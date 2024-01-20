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

package org.eclipse.edc.identityhub.spi.model.participant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.time.Instant;
import java.util.Objects;

/**
 * Representation of a participant in Identity Hub.
 */
@JsonDeserialize(builder = ParticipantContext.Builder.class)
public class ParticipantContext {
    private String participantId;
    private long createdAt;
    private long lastModified;
    private int state; // CREATED, ACTIVATED, DEACTIVATED
    private String apiTokenAlias; // or apiTokenAlias

    private ParticipantContext() {
    }

    /**
     * Participant IDs must be stable and globally unique (i.e. per dataspace). They will be visible in contracts, negotiations, etc.
     */
    public String getParticipantId() {
        return participantId;
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

    public void activate() {
        this.state = ParticipantContextState.ACTIVATED.ordinal();
    }

    public void deactivate() {
        this.state = ParticipantContextState.DEACTIVATED.ordinal();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private final ParticipantContext participantContext;

        private Builder() {
            participantContext = new ParticipantContext();
            participantContext.createdAt = Instant.now().toEpochMilli();
        }

        public Builder createdAt(long createdAt) {
            this.participantContext.createdAt = createdAt;
            return this;
        }

        public Builder lastModified(long lastModified) {
            this.participantContext.lastModified = lastModified;
            return this;
        }

        public Builder participantId(String participantId) {
            this.participantContext.participantId = participantId;
            return this;
        }

        public Builder state(ParticipantContextState state) {
            this.participantContext.state = state.ordinal();
            return this;
        }

        public Builder apiTokenAlias(String apiToken) {
            this.participantContext.apiTokenAlias = apiToken;
            return this;
        }

        public ParticipantContext build() {
            Objects.requireNonNull(participantContext.participantId, "Participant ID cannot be null");
            Objects.requireNonNull(participantContext.apiTokenAlias, "API Token Alias cannot be null");

            if (participantContext.getLastModified() == 0L) {
                participantContext.lastModified = participantContext.getCreatedAt();
            }
            return participantContext;
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }
    }
}