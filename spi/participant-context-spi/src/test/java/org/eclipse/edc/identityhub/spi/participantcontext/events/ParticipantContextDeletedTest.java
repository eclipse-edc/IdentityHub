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

package org.eclipse.edc.identityhub.spi.participantcontext.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ParticipantContextDeletedTest {

    private final TypeManager manager = new JacksonTypeManager();

    @Test
    void verify_serDes() throws JsonProcessingException {
        var evt = ParticipantContextDeleted.Builder.newInstance()
                .participantContextId("test-participantId")
                .build();

        var json = manager.writeValueAsString(evt);

        assertThat(json).isNotNull();

        assertThat(manager.readValue(json, ParticipantContextDeleted.class)).usingRecursiveComparison().isEqualTo(evt);
    }
}