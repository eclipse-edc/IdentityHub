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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * Abstraction over a cryptographically-verifiable object.
 */
public abstract class Verifiable<T> {

    @JsonUnwrapped
    protected final T item;

    protected final Proof proof;

    protected Verifiable(T item, Proof proof) {
        this.proof = proof;
        this.item = item;
    }

    @JsonCreator
    protected Verifiable(@JsonProperty(value = "proof", required = true) Proof proof) {
        this(null, proof);
    }

    /**
     * Returns the verifiable item.
     *
     * @return the verifiable item.
     */
    public T getItem() {
        return item;
    }

    /**
     * Returns the cryptographic proof.
     *
     * @return proof
     * @see <a href="https://www.w3.org/TR/vc-data-model/#proofs-signatures">Proof</a>
     */
    public Proof getProof() {
        return proof;
    }
}
