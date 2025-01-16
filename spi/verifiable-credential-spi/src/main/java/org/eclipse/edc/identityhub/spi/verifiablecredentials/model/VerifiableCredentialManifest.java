/*
 *  Copyright (c) 2024 Amadeus IT Group.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus IT Group - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.spi.verifiablecredentials.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.policy.model.Policy;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Manifest (=recipe) for creating the {@link org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential}.
 */
@JsonDeserialize(builder = VerifiableCredentialManifest.Builder.class)
public class VerifiableCredentialManifest {
    private String id;
    private String participantId;
    private VerifiableCredentialContainer verifiableCredentialContainer;
    private Policy issuancePolicy;
    private Policy reissuancePolicy;

    private VerifiableCredentialManifest() {
    }

    /**
     * The Verifiable Credential id.
     */
    public String getId() {
        return id;
    }

    /**
     * The participant id.
     */
    public String getParticipantContextId() {
        return participantId;
    }

    /**
     * The Verifiable Credential container.
     */
    public VerifiableCredentialContainer getVerifiableCredentialContainer() {
        return verifiableCredentialContainer;
    }

    /**
     * The issuance policy for the Verifiable Credential.
     */
    @Nullable
    public Policy getIssuancePolicy() {
        return issuancePolicy;
    }

    /**
     * The re-issuance policy for the Verifiable Credential.
     */
    @Nullable
    public Policy getReissuancePolicy() {
        return reissuancePolicy;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {

        private final VerifiableCredentialManifest manifest;

        private Builder() {
            manifest = new VerifiableCredentialManifest();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder id(String id) {
            manifest.id = id;
            return this;
        }

        public Builder participantContextId(String participantId) {
            manifest.participantId = participantId;
            return this;
        }

        public Builder verifiableCredentialContainer(VerifiableCredentialContainer verifiableCredentialContainer) {
            manifest.verifiableCredentialContainer = verifiableCredentialContainer;
            return this;
        }

        public Builder issuancePolicy(Policy issuancePolicy) {
            manifest.issuancePolicy = issuancePolicy;
            return this;
        }

        public Builder reissuancePolicy(Policy reissuancePolicy) {
            manifest.reissuancePolicy = reissuancePolicy;
            return this;
        }

        public VerifiableCredentialManifest build() {
            if (manifest.id == null) {
                manifest.id = UUID.randomUUID().toString();

            }
            return manifest;
        }
    }
}
