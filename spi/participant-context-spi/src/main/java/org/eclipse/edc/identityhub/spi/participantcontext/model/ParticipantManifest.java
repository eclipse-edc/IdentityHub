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
import com.fasterxml.jackson.annotation.JsonFormat;
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
    private Set<KeyDescriptor> keys = new HashSet<>();
    private Map<String, Object> additionalProperties = new HashMap<>();
    private List<String> roles = new ArrayList<>();
    private Set<Service> serviceEndpoints = new HashSet<>();
    private boolean isActive;
    private String participantContextId;
    private String did;

    private ParticipantManifest() {
    }

    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
    }

    public String clientSecretAlias() {
        return ofNullable(additionalProperties.get("clientSecret")).map(Object::toString).orElseGet(() -> participantContextId + "-sts-client-secret");
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
     * The ID of the participant context. It is different from the stable DSP - dataspace ID or a DID. This could be a random ID.
     */
    public String getParticipantContextId() {
        return participantContextId;
    }

    /**
     * Key material that is to be associated with this participant. May not be null.
     */
    public Set<KeyDescriptor> getKeys() {
        return keys;
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

        public Builder participantContextId(String participantContextId) {
            manifest.participantContextId = participantContextId;
            return this;
        }

        public Builder key(KeyDescriptor key) {
            manifest.keys.add(key);
            return this;
        }

        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        public Builder keys(Set<KeyDescriptor> keys) {
            manifest.keys = keys;
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
