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

package org.eclipse.edc.identityhub.spi.issuance.credentials.model;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.DataModelVersion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Defines credential type that can be issued, its schema, and requirements for issuance.
 */
public class CredentialDefinition {

    private final List<String> attestations = new ArrayList<>();
    private final List<CredentialRuleDefinition> rules = new ArrayList<>();
    private final List<MappingDefinition> mappings = new ArrayList<>();
    private String credentialType;
    private String schema;
    private long validity;
    private DataModelVersion dataModel = DataModelVersion.V_1_1;

    private CredentialDefinition() {
    }

    public String getCredentialType() {
        return credentialType;
    }

    public DataModelVersion getDataModel() {
        return dataModel;
    }

    public String getSchema() {
        return schema;
    }

    public long getValidity() {
        return validity;
    }

    public List<String> getAttestations() {
        return attestations;
    }

    public List<CredentialRuleDefinition> getRules() {
        return rules;
    }

    public List<MappingDefinition> getMappings() {
        return mappings;
    }

    public static final class Builder {
        private final CredentialDefinition definition;

        private Builder() {
            definition = new CredentialDefinition();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder credentialType(String credentialType) {
            this.definition.credentialType = credentialType;
            return this;
        }

        public Builder schema(String schema) {
            this.definition.schema = schema;
            return this;
        }

        public Builder validity(long validity) {
            this.definition.validity = validity;
            return this;
        }

        public Builder dataModel(DataModelVersion dataModel) {
            this.definition.dataModel = dataModel;
            return this;
        }

        public Builder attestations(Collection<String> attestations) {
            this.definition.attestations.addAll(attestations);
            return this;
        }

        public Builder attestation(String attestation) {
            this.definition.attestations.add(attestation);
            return this;
        }

        public Builder rules(Collection<CredentialRuleDefinition> rules) {
            this.definition.rules.addAll(rules);
            return this;
        }

        public Builder rule(CredentialRuleDefinition rule) {
            this.definition.rules.add(rule);
            return this;
        }

        public Builder mappings(Collection<MappingDefinition> rules) {
            this.definition.mappings.addAll(rules);
            return this;
        }

        public Builder mappings(MappingDefinition mapping) {
            this.definition.mappings.add(mapping);
            return this;
        }

        public CredentialDefinition build() {
            requireNonNull(definition.credentialType, "credentialType");
            requireNonNull(definition.schema, "schema");
            return definition;
        }

    }

}
