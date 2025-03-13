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

package org.eclipse.edc.issuerservice.api.admin.credentialdefinition.v1.unstable.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialDefinition;
import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialRuleDefinition;
import org.eclipse.edc.issuerservice.spi.issuance.model.MappingDefinition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * DTO for {@link CredentialDefinition}.
 */

@JsonDeserialize(builder = CredentialDefinitionDto.Builder.class)
public class CredentialDefinitionDto {

    private final List<String> attestations = new ArrayList<>();
    private final List<CredentialRuleDefinition> rules = new ArrayList<>();
    private final List<MappingDefinition> mappings = new ArrayList<>();
    private final Set<String> formats = new HashSet<>();
    private String credentialType;
    private String jsonSchema;
    private String jsonSchemaUrl;
    private long validity;
    private String id;

    private CredentialDefinitionDto() {
    }

    public String getId() {
        return id;
    }

    public String getCredentialType() {
        return credentialType;
    }

    public Set<String> getFormats() {
        return formats;
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

    public CredentialDefinition toCredentialDefinition(String participantContextId) {
        return CredentialDefinition.Builder.newInstance()
                .id(id)
                .credentialType(credentialType)
                .jsonSchema(jsonSchema)
                .jsonSchemaUrl(jsonSchemaUrl)
                .validity(validity)
                .formats(formats.stream().map(String::toUpperCase).map(CredentialFormat::valueOf).collect(Collectors.toSet()))
                .attestations(attestations)
                .rules(rules)
                .mappings(mappings)
                .participantContextId(participantContextId)
                .build();
    }


    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private final CredentialDefinitionDto entity;

        private Builder() {
            entity = new CredentialDefinitionDto();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder id(String id) {
            this.entity.id = id;
            return this;
        }

        public Builder credentialType(String credentialType) {
            this.entity.credentialType = credentialType;
            return this;
        }

        public Builder jsonSchema(String jsonSchema) {
            this.entity.jsonSchema = jsonSchema;
            return this;
        }

        public Builder jsonSchemaUrl(String jsonSchemaUrl) {
            this.entity.jsonSchemaUrl = jsonSchemaUrl;
            return this;
        }

        public Builder validity(long validity) {
            this.entity.validity = validity;
            return this;
        }

        public Builder formats(Collection<String> formats) {
            this.entity.formats.addAll(formats);
            return this;
        }

        public Builder attestations(Collection<String> attestations) {
            this.entity.attestations.addAll(attestations);
            return this;
        }

        @JsonIgnore
        public Builder attestation(String attestation) {
            this.entity.attestations.add(attestation);
            return this;
        }

        public Builder rules(Collection<CredentialRuleDefinition> rules) {
            this.entity.rules.addAll(rules);
            return this;
        }

        @JsonIgnore
        public Builder rule(CredentialRuleDefinition rule) {
            this.entity.rules.add(rule);
            return this;
        }

        public Builder mappings(Collection<MappingDefinition> rules) {
            this.entity.mappings.addAll(rules);
            return this;
        }

        @JsonIgnore
        public Builder mapping(MappingDefinition mapping) {
            this.entity.mappings.add(mapping);
            return this;
        }

        public CredentialDefinitionDto build() {
            requireNonNull(entity.credentialType, "credentialType");
            if (entity.jsonSchema == null && entity.jsonSchemaUrl == null) {
                throw new IllegalStateException("Either jsonSchema or jsonSchemaUrl must be non-null");
            }
            return entity;
        }

    }

}
