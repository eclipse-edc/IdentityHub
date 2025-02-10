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

package org.eclipse.edc.issuerservice.api.admin.participant.v1.unstable.model;


import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.edc.issuerservice.spi.participant.model.Participant;

public record ParticipantDto(@JsonProperty(value = "participantId", required = true) String id,
                             @JsonProperty(value = "did", required = true) String did,
                             @JsonProperty("name") String name) {

    public static ParticipantDto from(Participant participant) {
        return new ParticipantDto(participant.participantId(), participant.did(), participant.participantName());
    }

    public Participant toParticipant() {
        return new Participant(id, did, name);
    }
}


