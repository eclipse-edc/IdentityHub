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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Credential subject as defined in <a href="https://www.w3.org/TR/vc-data-model/#credential-subject">W3C specification</a>.
 */
@JsonDeserialize(builder = CredentialSubject.Builder.class)
public class CredentialSubject {

    private String id;
    private final Map<String, Object> claims = new HashMap<>();

    private CredentialSubject() {
    }

    @NotNull
    public String getId() {
        return id;
    }

    @JsonAnyGetter
    public Map<String, Object> getClaims() {
        return claims;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        CredentialSubject subject;

        private Builder() {
            subject = new CredentialSubject();
        }

        @JsonCreator
        public static CredentialSubject.Builder newInstance() {
            return new CredentialSubject.Builder();
        }

        public CredentialSubject.Builder id(String id) {
            subject.id = id;
            return this;
        }

        @JsonAnySetter
        public CredentialSubject.Builder claim(String key, Object value) {
            subject.claims.put(key, value);
            return this;
        }

        public CredentialSubject build() {
            Objects.requireNonNull(subject.id, "CredentialSubject must contain `id` property.");
            return subject;
        }
    }
}
