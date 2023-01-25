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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Represents a verifiable credential as defined in <a href="https://www.w3.org/TR/vc-data-model">W3C specification</a>.
 */
public class VerifiableCredential extends Verifiable<Credential> {

    public static final String DEFAULT_CONTEXT = "https://www.w3.org/2018/credentials/v1";
    public static final String DEFAULT_TYPE = "VerifiableCredential";

    @JsonCreator
    public VerifiableCredential(@JsonProperty("proof") Proof proof) {
        super(proof);
    }

    public VerifiableCredential(Credential credential, Proof proof) {
        super(credential, proof);
    }

    @JsonIgnore
    public boolean isValid() {
        Objects.requireNonNull(item, "Credential cannot be null.");
        return item.getContexts().contains(DEFAULT_CONTEXT) && item.getTypes().contains(DEFAULT_TYPE);
    }
}
