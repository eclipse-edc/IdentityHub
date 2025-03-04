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

package org.eclipse.edc.issuerservice.spi.holder.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a single user account in the issuer service. Members of a dataspace that would like this issuer service to issue them
 * credentials, will need such an account with an issuer service.
 *
 * @param holderId     The ID of the holder.
 * @param did          The Decentralized Identifier of the holder.
 * @param holderName   A human-readable name of the holder.
 * @param attestations A list of Strings, each of which contains the ID of an {@code AttestationDefinition}, that is enabled for this holder
 */
public record Holder(String holderId, String did, String holderName, List<String> attestations) {

    public Holder(String holderId, String did, String holderName) {
        this(holderId, did, holderName, List.of());
    }

    public Holder(String holderId, String did, String holderName, List<String> attestations) {
        this.holderId = holderId;
        this.did = did;
        this.holderName = holderName;
        this.attestations = new ArrayList<>(attestations); //ensure mutability
    }

    public void addAttestation(String attestationId) {
        attestations.add(attestationId);
    }

    @Override
    public List<String> attestations() {
        return Collections.unmodifiableList(attestations); // force mutation through method
    }

    public boolean removeAttestation(String attestationId) {
        return attestations.remove(attestationId);
    }
}
