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
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ParticipantContextCreatedTest {

    private final TypeManager manager = new TypeManager();

    @Test
    void verify_serDes() throws JsonProcessingException {
        var evt = ParticipantContextCreated.Builder.newInstance()
                .participantId("test-participantId")
                .build();

        var json = manager.writeValueAsString(evt);

        assertThat(json).isNotNull();

        assertThat(manager.readValue(json, ParticipantContextCreated.class)).usingRecursiveComparison().isEqualTo(evt);
    }
}