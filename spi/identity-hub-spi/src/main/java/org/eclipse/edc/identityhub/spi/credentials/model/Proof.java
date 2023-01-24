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

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Proof as designed in <a href="https://www.w3.org/TR/vc-data-integrity/#proofs">W3C specification</a>.
 */
@JsonDeserialize(builder = Proof.Builder.class)
public class Proof {

    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    private String type;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_FORMAT)
    private Date created;

    private String verificationMethod;

    private String proofPurpose;

    private final Map<String, Object> extensions = new HashMap<>();

    private Proof() {
    }

    @NotNull
    public String getType() {
        return type;
    }

    @NotNull
    public Date getCreated() {
        return created;
    }

    @NotNull
    public String getVerificationMethod() {
        return verificationMethod;
    }

    @NotNull
    public String getProofPurpose() {
        return proofPurpose;
    }

    @JsonAnyGetter
    public Map<String, Object> getExtensions() {
        return extensions;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        Proof proof;

        private Builder() {
            proof = new Proof();
        }

        @JsonCreator
        public static Proof.Builder newInstance() {
            return new Proof.Builder();
        }

        public Proof.Builder type(String type) {
            proof.type = type;
            return this;
        }

        public Proof.Builder created(Date created) {
            proof.created = created;
            return this;
        }

        public Proof.Builder verificationMethod(String verificationMethod) {
            proof.verificationMethod = verificationMethod;
            return this;
        }

        public Proof.Builder proofPurpose(String proofPurpose) {
            proof.proofPurpose = proofPurpose;
            return this;
        }

        @JsonAnySetter
        public Proof.Builder extension(String key, String value) {
            proof.extensions.put(key, value);
            return this;
        }

        public Proof build() {
            Objects.requireNonNull(proof.type, "Proof must contain `type` property.");
            Objects.requireNonNull(proof.created, "Proof must contain `created` property.");
            Objects.requireNonNull(proof.verificationMethod, "Proof must contain `verificationMethod` property.");
            Objects.requireNonNull(proof.proofPurpose, "Proof must contain `proofPurpose` property.");
            return proof;
        }
    }
}
