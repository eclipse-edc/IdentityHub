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
import org.eclipse.edc.iam.did.spi.document.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Optional.ofNullable;

/**
 * Manifest (=recipe) for creating the {@link ParticipantContext}.
 */
@JsonDeserialize(builder = ParticipantManifest.Builder.class)
public class ParticipantManifest {
    private Map<String, Object> additionalProperties = new HashMap<>();
    private List<String> roles = new ArrayList<>();
    private Set<Service> serviceEndpoints = new HashSet<>();
    private boolean isActive;
    private String participantId;
    private String did;
    private KeyDescriptor key;

    private ParticipantManifest() {
    }

    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
    }

    public String clientSecretAlias() {
        return ofNullable(additionalProperties.get("clientSecret")).map(Object::toString).orElseGet(() -> participantId + "-sts-client-secret");
    }

    /**
     * An optional list of service endpoints that should get published in the DID document, e.g. resolution endpoints, storage endpoints, etc.
     */
    public Set<Service> getServiceEndpoints() {
        return serviceEndpoints;
    }

    /**
     * Indicates whether the participant context should immediately transition to the {@link ParticipantContextState#ACTIVATED} state. This will include
     * publishing the generated DID document.
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * The DSP {@code participantId} of this participant. This must be a unique and stable ID.
     */
    public String getParticipantId() {
        return participantId;
    }

    /**
     * Key material that is to be associated with this participant. May not be null.
     */
    public KeyDescriptor getKey() {
        return key;
    }

    /**
     * The DID that is to be used when publishing the participant DID document. This must be specified because there is no reliable way to determine the DID
     * automatically.
     */
    public String getDid() {
        return did;
    }

    public List<String> getRoles() {
        return roles;
    }

    public Object getProperty(String key) {
        return additionalProperties.get(key);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {

        private final ParticipantManifest manifest;

        private Builder() {
            manifest = new ParticipantManifest();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
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

        public Builder roles(List<String> roles) {
            manifest.roles = roles;
            return this;
        }

        public Builder did(String did) {
            manifest.did = did;
            return this;
        }

        public Builder property(String key, Object value) {
            manifest.additionalProperties.put(key, value);
            return this;
        }

        public Builder additionalProperties(Map<String, Object> properties) {
            manifest.additionalProperties = properties;
            return this;
        }

        public ParticipantManifest build() {
            return manifest;
        }
    }
}
