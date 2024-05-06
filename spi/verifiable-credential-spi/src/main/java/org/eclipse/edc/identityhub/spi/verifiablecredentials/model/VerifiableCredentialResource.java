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

/**
 * Represents a Verifiable Credential Resource.
 * The Verifiable Credential Resource extends the Identity Resource class and adds additional properties specific to verifiable credentials,
 * specifically the issuance and re-issuance policies as well as a representation of the VC
 */
public class VerifiableCredentialResource extends IdentityResource {
    private static final String EXPIRED = "expired";
    private static final String REVOKED = "revoked";
    private static final String SUSPENDED = "suspended";
    private int state;
    private String credentialStatus;
    private Instant timeOfLastStatusUpdate;
    private Policy issuancePolicy;
    private Policy reissuancePolicy;
    private VerifiableCredentialContainer verifiableCredential;

    private VerifiableCredentialResource() {

    }

    public int getState() {
        return state;
    }

    @JsonIgnore
    public VcState getStateAsEnum() {
        return VcState.from(state);
    }

    public Policy getIssuancePolicy() {
        return issuancePolicy;
    }

    public Policy getReissuancePolicy() {
        return reissuancePolicy;
    }

    public boolean isExpired() {
        return EXPIRED.equalsIgnoreCase(credentialStatus);
    }

    public boolean isRevoked() {
        return REVOKED.equalsIgnoreCase(credentialStatus);
    }

    public boolean isSuspended() {
        return SUSPENDED.equalsIgnoreCase(credentialStatus);
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

    public void clearStatus() {
        setCredentialStatus(null);
    }

    public VerifiableCredentialContainer getVerifiableCredential() {
        return verifiableCredential;
    }

    public Instant getTimeOfLastStatusUpdate() {
        return timeOfLastStatusUpdate;
    }

    public String getCredentialStatus() {
        return credentialStatus;
    }

    public void setCredentialStatus(String status) {
        credentialStatus = status;
        timeOfLastStatusUpdate = Instant.now();
    }

    public static class Builder extends IdentityResource.Builder<VerifiableCredentialResource, Builder> {

        protected Builder(VerifiableCredentialResource resource) {
            super(resource);
        }

        public static Builder newInstance() {
            return new Builder(new VerifiableCredentialResource());
        }

        public Builder state(VcState state) {
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

        public Builder credential(VerifiableCredentialContainer credential) {
            entity.verifiableCredential = credential;
            return self();
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public VerifiableCredentialResource build() {
            if (entity.state == 0) {
                entity.state = VcState.INITIAL.code();
            }
            return super.build();
        }
    }
}
