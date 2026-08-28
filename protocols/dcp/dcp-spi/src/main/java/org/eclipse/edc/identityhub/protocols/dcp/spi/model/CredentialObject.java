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

package org.eclipse.edc.identityhub.protocols.dcp.spi.model;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.presentationdefinition.PresentationDefinition;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

public class CredentialObject {

    public static final String CREDENTIAL_OBJECT_TERM = "CredentialObject";
    public static final String CREDENTIAL_OBJECT_CREDENTIAL_TYPE_TERM = "credentialType";
    public static final String CREDENTIAL_OBJECT_CREDENTIAL_SCHEMA_TERM = "credentialSchema";
    public static final String CREDENTIAL_OBJECT_OFFER_REASON_TERM = "offerReason";
    /**
     * The credential is offered again before the one the Holder has expires.
     */
    public static final String OFFER_REASON_REISSUE = "reissue";
    /**
     * The credential is offered again because the key its proof was made with is no longer valid.
     */
    public static final String OFFER_REASON_PROOF_KEY_REVOCATION = "proof-key-revocation";
    public static final String CREDENTIAL_OBJECT_PROFILE_TERM = "profile";
    public static final String CREDENTIAL_OBJECT_BINDING_METHODS_TERM = "bindingMethods";
    public static final String CREDENTIAL_OBJECT_ISSUANCE_POLICY_TERM = "issuancePolicy";
    private String profile;
    private String id;
    private String credentialType;
    private String credentialSchema;
    private String offerReason;
    private List<String> bindingMethods = new ArrayList<>();
    private PresentationDefinition issuancePolicy;

    public String getId() {
        return id;
    }

    public String getCredentialType() {
        return credentialType;
    }

    /**
     * URL of the schema the issued credential's {@code credentialSubject} adheres to.
     */
    public String getCredentialSchema() {
        return credentialSchema;
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

    public static class Builder {
        private final CredentialObject credentialObject;

        private Builder() {
            this.credentialObject = new CredentialObject();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder id(String id) {
            credentialObject.id = id;
            return this;
        }

        public Builder credentialType(String credentialType) {
            credentialObject.credentialType = credentialType;
            return this;
        }

        public Builder credentialSchema(String credentialSchema) {
            credentialObject.credentialSchema = credentialSchema;
            return this;
        }

        public Builder offerReason(String offerReason) {
            credentialObject.offerReason = offerReason;
            return this;
        }

        public Builder profile(String profile) {
            credentialObject.profile = profile;
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

        public CredentialObject build() {
            requireNonNull(credentialObject.id, "id must not be null");
            return credentialObject;
        }
    }

}
