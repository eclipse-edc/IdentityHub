/*
 *  Copyright (c) 2020 - 2022 Amadeus
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.identityhub.spi.model.Record;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RecordTest {

    @Test
    void verifySerDes() throws JsonProcessingException {
        var mapper = new ObjectMapper();
        var record = Record.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .data(new byte[]{})
                .createdAt(System.currentTimeMillis())
                .dataFormat("application/json")
                .build();

        var json = mapper.writeValueAsString(record);

        assertNotNull(json);

        var deser = mapper.readValue(json, Record.class);
        assertThat(deser).usingRecursiveComparison().isEqualTo(record);
    }
}
