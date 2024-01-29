/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.spi.events.participant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ParticipantContextDeletedTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void verify_serDes() throws JsonProcessingException {
        var evt = ParticipantContextDeleted.Builder.newInstance()
                .participantId("test-participantId")
                .build();

        var json = mapper.writeValueAsString(evt);

        assertThat(json).isNotNull();

        assertThat(mapper.readValue(json, ParticipantContextDeleted.class)).usingRecursiveComparison().isEqualTo(evt);
    }
}