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

package org.eclipse.edc.issuerservice.spi.issuance.model;

import org.eclipse.edc.identityhub.spi.participantcontext.model.AbstractParticipantResource;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Defines an attestation that is used to evaluate an issuance request.
 */
public class AttestationDefinition extends AbstractParticipantResource {

    private String id;
    private String attestationType;
    private Map<String, Object> configuration = new HashMap<>();

    private AttestationDefinition() {
    }

    public String getId() {
        return id;
    }

    public String getAttestationType() {
        return attestationType;
    }

    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    public static final class Builder extends AbstractParticipantResource.Builder<AttestationDefinition, Builder> {

        private Builder() {
            super(new AttestationDefinition());
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder id(String id) {
            this.entity.id = id;
            return this;
        }

        public Builder attestationType(String attestationType) {
            this.entity.attestationType = attestationType;
            return this;
        }

        public Builder configuration(Map<String, Object> configuration) {
            this.entity.configuration = configuration;
            return this;
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public AttestationDefinition build() {
            Objects.requireNonNull(entity.id, "Must have an ID");
            Objects.requireNonNull(entity.attestationType, "Must have an attestation type");
            Objects.requireNonNull(entity.configuration, "Must have an configuration");
            Objects.requireNonNull(entity.participantContextId, "Must have an participantContextId");
            return super.build();
        }

    }
}
