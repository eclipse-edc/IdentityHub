/*
 *  Copyright (c) 2023 GAIA-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       GAIA-X - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.credentials.jwt;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.spi.EdcException;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY;

/**
 * Represents a presentation in JWT format defined at <a href="https://www.w3.org/TR/vc-data-model/#json-web-token">W3C specification</a>.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonDeserialize(builder = JwtPresentation.Builder.class)
public class JwtPresentation {

    private static final String JSON_PROP_TYPES = "type";
    public static final String DEFAULT_TYPE = "VerifiablePresentation";
    private static final String JSON_PROP_VERIFIABLE_CREDENTIAL = "verifiableCredential";
    private static final String JSON_PROP_CONTEXTS = "@context";

    private String id;

    @JsonProperty(JSON_PROP_CONTEXTS)
    @JsonFormat(with = ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private final List<String> contexts = new ArrayList<>();

    @JsonProperty(JSON_PROP_TYPES)
    @JsonFormat(with = ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private final List<String> types = new ArrayList<>();

    @JsonProperty(JSON_PROP_VERIFIABLE_CREDENTIAL)
    @JsonFormat(with = ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<String> signedCredentials = new ArrayList<>();

    protected JwtPresentation() {
    }

    /**
     * Returns the credential id.
     *
     * @return id
     * @see <a href="https://www.w3.org/TR/vc-data-model/#identifiers">Identifier</a>
     */
    @NotNull
    public String getId() {
        return id;
    }

    /**
     * Returns the contexts.
     *
     * @return contexts
     * @see <a href="https://www.w3.org/TR/vc-data-model/#contexts">Contexts</a>
     */
    @NotNull
    public List<String> getContexts() {
        return contexts;
    }

    /**
     * Returns the types.
     *
     * @return types
     * @see <a href="https://www.w3.org/TR/vc-data-model/#types">Types</a>
     */
    @NotNull
    public List<String> getTypes() {
        return types;
    }

    /**
     * Returns the list of verifiable credentials in JWT format
     *
     * @return verifiable credentials in JWT format
     * @see <a href="https://www.w3.org/TR/vc-data-model/#verifiableCredentials">Verifiable Credentials</a>
     */
    @NotNull
    public List<String> getSignedCredentials() {
        return signedCredentials;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final JwtPresentation presentation;

        private Builder(JwtPresentation presentation) {
            this.presentation = presentation;
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder(new JwtPresentation());
        }


        public Builder id(String id) {
            presentation.id = id;
            return this;
        }

        public Builder context(String context) {
            presentation.contexts.add(context);
            return this;
        }

        @JsonProperty(JSON_PROP_CONTEXTS)
        public Builder contexts(List<String> contexts) {
            presentation.contexts.addAll(contexts);
            return this;
        }

        @JsonProperty(JSON_PROP_TYPES)
        public Builder types(List<String> types) {
            presentation.types.addAll(types);
            return this;
        }

        public Builder type(String type) {
            presentation.types.add(type);
            return this;
        }

        @JsonProperty(JSON_PROP_VERIFIABLE_CREDENTIAL)
        @JsonFormat(with = ACCEPT_SINGLE_VALUE_AS_ARRAY)
        public Builder signedCredentials(List<String> signedCredentials) {
            presentation.signedCredentials.addAll(signedCredentials);
            return this;
        }

        public JwtPresentation build() {
            if (presentation.types.isEmpty()) {
                throw new EdcException("Presentation must contain `type` property.");
            }

            if (presentation.signedCredentials.isEmpty()) {
                throw new EdcException("Presentation must contain `verifiableCredential` property.");
            }
            return presentation;
        }
    }
}
