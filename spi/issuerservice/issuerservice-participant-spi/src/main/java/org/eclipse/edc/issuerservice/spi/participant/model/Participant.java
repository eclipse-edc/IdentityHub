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

package org.eclipse.edc.issuerservice.spi.participant.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a single user account in the issuer service. Members of a dataspace that would like this issuer service to issue them
 * credentials, will need such an account with an issuer service.
 *
 * @param participantId   The ID of the participant.
 * @param did             The Decentralized Identifier of the participant.
 * @param participantName A human-readable name of the participant.
 * @param attestations    A list of Strings, each of which contains the ID of an {@code AttestationDefinition}, that is enabled for this participant
 */
public record Participant(String participantId, String did, String participantName, List<String> attestations) {

    public Participant(String participantId, String did, String participantName) {
        this(participantId, did, participantName, List.of());
    }

    public Participant(String participantId, String did, String participantName, List<String> attestations) {
        this.participantId = participantId;
        this.did = did;
        this.participantName = participantName;
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
