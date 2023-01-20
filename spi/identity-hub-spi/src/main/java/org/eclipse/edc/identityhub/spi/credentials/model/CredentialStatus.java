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
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Credential status as defined in <a href="https://www.w3.org/TR/vc-data-model/#status">W3C specification</a>.
 */
public class CredentialStatus {

    private String id;
    private String type;

    private CredentialStatus() {
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public String getType() {
        return type;
    }


    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        CredentialStatus status;

        private Builder() {
            status = new CredentialStatus();
        }

        @JsonCreator
        public static CredentialStatus.Builder newInstance() {
            return new CredentialStatus.Builder();
        }

        public CredentialStatus.Builder id(String id) {
            status.id = id;
            return this;
        }

        public CredentialStatus.Builder type(String type) {
            status.type = type;
            return this;
        }

        public CredentialStatus build() {
            Objects.requireNonNull(status.id, "CredentialStatus must contain `id` property.");
            Objects.requireNonNull(status.type, "CredentialStatus must contain `type` property.");
            return status;
        }
    }
}
