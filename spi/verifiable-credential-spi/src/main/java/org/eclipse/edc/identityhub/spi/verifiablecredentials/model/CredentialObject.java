/*
 *  Copyright (c) 2025 Metaform Systems Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.spi.verifiablecredentials.model;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.presentationdefinition.PresentationDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * this is a POJO that is intended to be used for storing CredentialObjects as serialized JSON in the database when
 * storing {@link CredentialOffer}s.
 * <p>
 * todo: this is a 1:1 copy of the {@code org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject}. Do we really need both? Could we just use this one?
 */
public class CredentialObject {

    private String id;
    private String credentialType;
    private String offerReason;
    private String profile;
    private List<String> bindingMethods = new ArrayList<>();
    private PresentationDefinition issuancePolicy;

    public String getCredentialType() {
        return credentialType;
    }

    public List<String> getBindingMethods() {
        return bindingMethods;
    }

    public String getProfile() {
        return profile;
    }

    public PresentationDefinition getIssuancePolicy() {
        return issuancePolicy;
    }

    public String getOfferReason() {
        return offerReason;
    }

    public String getId() {
        return id;
    }

    public static class Builder {
        private final CredentialObject credentialObject;

        private Builder() {
            this.credentialObject = new CredentialObject();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder credentialType(String credentialType) {
            credentialObject.credentialType = credentialType;
            return this;
        }

        public Builder offerReason(String offerReason) {
            credentialObject.offerReason = offerReason;
            return this;
        }

        public Builder profile(String profiles) {
            credentialObject.profile = profiles;
            return this;
        }

        public Builder bindingMethods(List<String> bindingMethods) {
            credentialObject.bindingMethods = bindingMethods;
            return this;
        }

        public Builder bindingMethod(String bindingMethod) {
            credentialObject.bindingMethods.add(bindingMethod);
            return this;
        }

        public Builder issuancePolicy(PresentationDefinition issuancePolicy) {
            credentialObject.issuancePolicy = issuancePolicy;
            return this;
        }

        public Builder id(String id) {
            credentialObject.id = id;
            return this;
        }

        public CredentialObject build() {
            return credentialObject;
        }
    }

}
