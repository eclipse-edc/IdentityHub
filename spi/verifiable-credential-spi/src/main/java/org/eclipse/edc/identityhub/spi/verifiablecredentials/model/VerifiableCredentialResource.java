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

package org.eclipse.edc.identityhub.spi.verifiablecredentials.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.identityhub.spi.participantcontext.model.IdentityResource;
import org.eclipse.edc.policy.model.Policy;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus.EXPIRED;
import static org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus.REVOKED;
import static org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus.SUSPENDED;

/**
 * Represents a Verifiable Credential Resource.
 * The Verifiable Credential Resource extends the Identity Resource class and adds additional properties specific to verifiable credentials,
 * specifically the issuance and re-issuance policies as well as a representation of the VC
 */
public class VerifiableCredentialResource extends IdentityResource {
    private Map<String, Object> metadata = new HashMap<>();
    private int state;
    private Instant timeOfLastStatusUpdate;
    private Policy issuancePolicy;
    private Policy reissuancePolicy;
    private VerifiableCredentialContainer verifiableCredential;
    private CredentialUsage usage = CredentialUsage.Holder;

    private VerifiableCredentialResource() {

    }

    /**
     * Holds metadata about a credential, for example could hold data if the credential is a status list credential
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public int getState() {
        return state;
    }

    @JsonIgnore
    public VcStatus getStateAsEnum() {
        return VcStatus.from(state);
    }

    @Deprecated(since = "0.11.0")
    public Policy getIssuancePolicy() {
        return issuancePolicy;
    }

    @Deprecated(since = "0.11.0")
    public Policy getReissuancePolicy() {
        return reissuancePolicy;
    }

    @JsonIgnore
    public boolean isExpired() {
        return EXPIRED.code() == state;
    }

    @JsonIgnore
    public boolean isRevoked() {
        return REVOKED.code() == state;
    }

    @JsonIgnore
    public boolean isSuspended() {
        return SUSPENDED.code() == state;
    }

    public void suspend() {
        setCredentialStatus(SUSPENDED);
    }

    public void setExpired() {
        setCredentialStatus(EXPIRED);
    }

    public void revoke() {
        setCredentialStatus(REVOKED);
    }

    public VerifiableCredentialContainer getVerifiableCredential() {
        return verifiableCredential;
    }

    public Instant getTimeOfLastStatusUpdate() {
        return timeOfLastStatusUpdate;
    }


    public void setCredentialStatus(VcStatus status) {
        state = status.code();
        timeOfLastStatusUpdate = Instant.now();
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public CredentialUsage getUsage() {
        return usage;
    }

    @Override
    public String toString() {
        return "VerifiableCredentialResource{" +
                "usage=" + usage +
                ", state=" + state +
                ", timeOfLastStatusUpdate=" + timeOfLastStatusUpdate +
                ", issuancePolicy=" + issuancePolicy +
                ", reissuancePolicy=" + reissuancePolicy +
                ", verifiableCredential=" + verifiableCredential +
                ", metadata=" + metadata +
                '}';
    }

    public static class Builder extends IdentityResource.Builder<VerifiableCredentialResource, Builder> {

        protected Builder(VerifiableCredentialResource resource) {
            super(resource);
        }

        public static Builder newInstance() {
            return new Builder(new VerifiableCredentialResource());
        }

        /**
         * Initializes a new {@link VerifiableCredentialResource} intended for use as {@link CredentialUsage#Holder}.
         */
        public static Builder newHolder() {
            var resource = new VerifiableCredentialResource();
            resource.usage = CredentialUsage.Holder;
            return new Builder(resource);
        }

        /**
         * Initializes a new {@link VerifiableCredentialResource} intended for use as {@link CredentialUsage#StatusList}.
         */
        public static Builder newStatusList() {
            var resource = new VerifiableCredentialResource();
            resource.usage = CredentialUsage.StatusList;
            return new Builder(resource);
        }

        /**
         * Initializes a new {@link VerifiableCredentialResource} intended for use as {@link CredentialUsage#IssuanceTracking}.
         */
        public static Builder newIssuanceTracker() {
            var resource = new VerifiableCredentialResource();
            resource.usage = CredentialUsage.IssuanceTracking;
            return new Builder(resource);
        }

        public Builder state(VcStatus state) {
            entity.state = state.code();
            return self();
        }

        public Builder issuancePolicy(Policy issuancePolicy) {
            entity.issuancePolicy = issuancePolicy;
            return self();
        }

        public Builder reissuancePolicy(Policy reissuancePolicy) {
            entity.reissuancePolicy = reissuancePolicy;
            return self();
        }

        public Builder metadata(Map<String, Object> metadata) {
            entity.metadata = metadata;
            return self();
        }

        public Builder metadata(String key, Object value) {
            entity.metadata.put(key, value);
            return self();
        }

        public Builder credential(VerifiableCredentialContainer credential) {
            entity.verifiableCredential = credential;
            return self();
        }

        public Builder usage(CredentialUsage usage) {
            entity.usage = usage;
            return self();
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public VerifiableCredentialResource build() {
            if (entity.state == 0) {
                entity.state = VcStatus.INITIAL.code();
            }
            return super.build();
        }
    }
}
