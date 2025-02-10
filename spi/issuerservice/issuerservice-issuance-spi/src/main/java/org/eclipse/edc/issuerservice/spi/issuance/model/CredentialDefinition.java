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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.DataModelVersion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * Defines credential type that can be issued, its schema, and requirements for issuance.
 */

@JsonDeserialize(builder = CredentialDefinition.Builder.class)
public class CredentialDefinition {

    private final List<String> attestations = new ArrayList<>();
    private final List<CredentialRuleDefinition> rules = new ArrayList<>();
    private final List<MappingDefinition> mappings = new ArrayList<>();
    private String credentialType;
    private String jsonSchema;
    private String jsonSchemaUrl;
    private long validity;
    private DataModelVersion dataModel = DataModelVersion.V_1_1;
    private String id;

    private CredentialDefinition() {
    }

    public String getId() {
        return id;
    }

    public String getCredentialType() {
        return credentialType;
    }

    public DataModelVersion getDataModel() {
        return dataModel;
    }

    public String getJsonSchema() {
        return jsonSchema;
    }

    public String getJsonSchemaUrl() {
        return jsonSchemaUrl;
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


    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private final CredentialDefinition definition;

        private Builder() {
            definition = new CredentialDefinition();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder id(String id) {
            this.definition.id = id;
            return this;
        }

        public Builder credentialType(String credentialType) {
            this.definition.credentialType = credentialType;
            return this;
        }

        public Builder jsonSchema(String jsonSchema) {
            this.definition.jsonSchema = jsonSchema;
            return this;
        }

        public Builder jsonSchemaUrl(String jsonSchemaUrl) {
            this.definition.jsonSchemaUrl = jsonSchemaUrl;
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

        @JsonIgnore
        public Builder attestation(String attestation) {
            this.definition.attestations.add(attestation);
            return this;
        }

        public Builder rules(Collection<CredentialRuleDefinition> rules) {
            this.definition.rules.addAll(rules);
            return this;
        }

        @JsonIgnore
        public Builder rule(CredentialRuleDefinition rule) {
            this.definition.rules.add(rule);
            return this;
        }

        public Builder mappings(Collection<MappingDefinition> rules) {
            this.definition.mappings.addAll(rules);
            return this;
        }

        @JsonIgnore
        public Builder mapping(MappingDefinition mapping) {
            this.definition.mappings.add(mapping);
            return this;
        }

        public CredentialDefinition build() {
            if (definition.id == null) {
                definition.id = UUID.randomUUID().toString();
            }
            requireNonNull(definition.credentialType, "credentialType");
            requireNonNull(definition.jsonSchema, "jsonSchema");
            requireNonNull(definition.jsonSchemaUrl, "jsonSchemaUrl");
            return definition;
        }

    }

}
