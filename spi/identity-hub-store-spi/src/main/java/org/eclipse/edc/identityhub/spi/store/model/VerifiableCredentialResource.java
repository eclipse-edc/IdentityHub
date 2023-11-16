/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.spi.store.model;

import org.eclipse.edc.identitytrust.model.VerifiableCredentialContainer;
import org.eclipse.edc.policy.model.Policy;

/**
 * Represents a Verifiable Credential Resource.
 * The Verifiable Credential Resource extends the Identity Resource class and adds additional properties specific to verifiable credentials,
 * specifically the issuance and re-issuance policies as well as a representation of the VC
 */
public class VerifiableCredentialResource extends IdentityResource {
    private VcState state;
    private Policy issuancePolicy;
    private Policy reissuancePolicy;
    private VerifiableCredentialContainer verifiableCredential;

    private VerifiableCredentialResource() {

    }

    public VcState getState() {
        return state;
    }

    public Policy getIssuancePolicy() {
        return issuancePolicy;
    }

    public Policy getReissuancePolicy() {
        return reissuancePolicy;
    }

    public VerifiableCredentialContainer getVerifiableCredential() {
        return verifiableCredential;
    }

    public static class Builder extends IdentityResource.Builder<VerifiableCredentialResource, Builder> {

        protected Builder(VerifiableCredentialResource resource) {
            super(resource);
        }

        public static Builder newInstance() {
            return new Builder(new VerifiableCredentialResource());
        }

        public Builder state(VcState state) {
            resource.state = state;
            return self();
        }

        public Builder issuancePolicy(Policy issuancePolicy) {
            resource.issuancePolicy = issuancePolicy;
            return self();
        }

        public Builder reissuancePolicy(Policy reissuancePolicy) {
            resource.reissuancePolicy = reissuancePolicy;
            return self();
        }

        public Builder credential(VerifiableCredentialContainer credential) {
            resource.verifiableCredential = credential;
            return self();
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public VerifiableCredentialResource build() {
            if (resource.state == null) {
                resource.state = VcState.INITIAL;
            }
            return super.build();
        }
    }
}
