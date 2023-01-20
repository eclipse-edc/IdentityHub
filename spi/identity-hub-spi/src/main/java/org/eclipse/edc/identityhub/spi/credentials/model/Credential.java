/*
 *  Copyright (c) 2023 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.spi.credentials.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.spi.EdcException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY;

/**
 * Represents a credential defined at <a href="https://www.w3.org/TR/vc-data-model/#credentials">W3C specification</a>.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonDeserialize(builder = Credential.Builder.class)
public class Credential {

    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    private static final String JSON_PROP_CONTEXTS = "@context";
    private static final String JSON_PROP_TYPES = "type";

    @JsonProperty(JSON_PROP_CONTEXTS)
    @JsonFormat(with = ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private final List<String> contexts = new ArrayList<>();

    private String id;

    @JsonProperty(JSON_PROP_TYPES)
    @JsonFormat(with = ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private final List<String> types = new ArrayList<>();

    private String issuer;

    private CredentialSubject credentialSubject;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_FORMAT)
    private Date issuanceDate;

    @Nullable
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_FORMAT)
    private Date expirationDate;

    private CredentialStatus credentialStatus;

    protected Credential() {
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
     * Returns the issuer.
     *
     * @return issuer
     * @see <a href="https://www.w3.org/TR/vc-data-model/#issuer">Issuer</a>
     */
    @NotNull
    public String getIssuer() {
        return issuer;
    }

    /**
     * Returns the credential claims.
     *
     * @return credential subject
     * @see <a href="https://www.w3.org/TR/vc-data-model/#credential-subject">Credential subject</a>
     */
    @NotNull
    public CredentialSubject getCredentialSubject() {
        return credentialSubject;
    }

    /**
     * Returns the issuance date of the credentia.
     *
     * @return issuance date
     * @see <a href="https://www.w3.org/TR/vc-data-model/#issuance-date">Issuance date</a>
     */
    @NotNull
    public Date getIssuanceDate() {
        return issuanceDate;
    }

    /**
     * Returns the expiration date of the credential.
     *
     * @return expiration date
     * @see <a href="https://www.w3.org/TR/vc-data-model/#expiration">Expiration</a>
     */
    @Nullable
    public Date getExpirationDate() {
        return expirationDate;
    }

    /**
     * Returns the status of the credential.
     *
     * @return credential status
     * @see <a href="https://www.w3.org/TR/vc-data-model/#status">Credential status</a>
     */
    @Nullable
    public CredentialStatus getCredentialStatus() {
        return credentialStatus;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final Credential credential;

        private Builder(Credential credential) {
            this.credential = credential;
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder(new Credential());
        }

        public Builder context(String context) {
            credential.contexts.add(context);
            return this;
        }

        @JsonProperty(JSON_PROP_CONTEXTS)
        public Builder contexts(List<String> contexts) {
            credential.contexts.addAll(contexts);
            return this;
        }

        public Builder id(String id) {
            credential.id = id;
            return this;
        }

        @JsonProperty(JSON_PROP_TYPES)
        public Builder types(List<String> types) {
            credential.types.addAll(types);
            return this;
        }

        public Builder type(String type) {
            credential.types.add(type);
            return this;
        }

        public Builder issuer(String issuer) {
            credential.issuer = issuer;
            return this;
        }

        public Builder credentialSubject(CredentialSubject credentialSubject) {
            credential.credentialSubject = credentialSubject;
            return this;
        }

        public Builder issuanceDate(Date issuanceDate) {
            credential.issuanceDate = issuanceDate;
            return this;
        }

        public Builder expirationDate(Date expirationDate) {
            credential.expirationDate = expirationDate;
            return this;
        }

        public Builder credentialStatus(CredentialStatus credentialStatus) {
            credential.credentialStatus = credentialStatus;
            return this;
        }

        public Credential build() {
            if (credential.contexts.isEmpty()) {
                throw new EdcException("Credential must have at least one context.");
            }
            Objects.requireNonNull(credential.issuer, "Credential must contain `issuer` property.");
            Objects.requireNonNull(credential.id, "Credential must contain `id` property.");
            Objects.requireNonNull(credential.credentialSubject, "Credential must contain `credentialSubject` property.");
            Objects.requireNonNull(credential.issuanceDate, "Credential must contain `issuanceDate` property.");
            return credential;
        }
    }
}
