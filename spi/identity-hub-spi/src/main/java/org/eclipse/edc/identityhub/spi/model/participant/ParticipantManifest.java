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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.iam.did.spi.document.Service;

import java.util.HashSet;
import java.util.Set;

/**
 * Manifest (=recipe) for creating the {@link ParticipantContext}.
 */
@JsonDeserialize(builder = ParticipantManifest.Builder.class)
public class ParticipantManifest {
    private Set<Service> serviceEndpoints = new HashSet<>();
    private boolean isActive;
    private String participantId;
    private String did;
    private KeyDescriptor key;

    private ParticipantManifest() {
    }

    public Set<Service> getServiceEndpoints() {
        return serviceEndpoints;
    }

    public boolean isActive() {
        return isActive;
    }

    public String getParticipantId() {
        return participantId;
    }

    public KeyDescriptor getKey() {
        return key;
    }

    public String getDid() {
        return did;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {

        private final ParticipantManifest manifest;

        private Builder() {
            manifest = new ParticipantManifest();
        }

        public Builder serviceEndpoints(Set<Service> serviceEndpoints) {
            manifest.serviceEndpoints = serviceEndpoints;
            return this;
        }

        public Builder serviceEndpoint(Service serviceEndpoint) {
            manifest.serviceEndpoints.add(serviceEndpoint);
            return this;
        }

        public Builder active(boolean isActive) {
            manifest.isActive = isActive;
            return this;
        }

        public Builder participantId(String participantId) {
            manifest.participantId = participantId;
            return this;
        }

        public Builder key(KeyDescriptor key) {
            manifest.key = key;
            return this;
        }

        public Builder did(String did) {
            manifest.did = did;
            return this;
        }

        public ParticipantManifest build() {
            return manifest;
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }
    }
}
