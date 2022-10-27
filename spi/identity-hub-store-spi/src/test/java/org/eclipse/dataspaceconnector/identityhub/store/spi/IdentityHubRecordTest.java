/*
 *  Copyright (c) 2022 Amadeus
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

package org.eclipse.dataspaceconnector.identityhub.store.spi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class IdentityHubRecordTest {

    private static final String PAYLOAD = UUID.randomUUID().toString();

    @Test
    void verifyMandatoryId() {
        var builder = IdentityHubRecord.Builder.newInstance()
                .payload(PAYLOAD.getBytes(StandardCharsets.UTF_8));

        assertThatExceptionOfType(NullPointerException.class).isThrownBy(builder::build);
    }

    @Test
    void verifyMandatoryPayload() {
        var builder = IdentityHubRecord.Builder.newInstance()
                .id(UUID.randomUUID().toString());

        assertThatExceptionOfType(NullPointerException.class).isThrownBy(builder::build);
    }

    @Test
    void verifySerDes() throws JsonProcessingException {
        var mapper = new ObjectMapper();
        var r = IdentityHubRecord.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .createdAt(Instant.now().getEpochSecond())
                .payload(PAYLOAD.getBytes(StandardCharsets.UTF_8))
                .build();

        var json = mapper.writeValueAsString(r);

        assertNotNull(json);

        var deser = mapper.readValue(json, IdentityHubRecord.class);
        assertThat(deser).usingRecursiveComparison().isEqualTo(r);
    }
}