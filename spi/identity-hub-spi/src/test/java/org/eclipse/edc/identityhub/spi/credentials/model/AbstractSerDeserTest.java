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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

abstract class AbstractSerDeserTest<T> {

    private static final ObjectMapper MAPPER = new TypeManager().getMapper();

    @Test
    void verifySerDes() throws JsonProcessingException {
        var vc = getEntity();

        var json = MAPPER.writeValueAsString(vc);

        assertNotNull(json);

        var deser = MAPPER.readValue(json, getClazz());
        assertThat(deser).usingRecursiveComparison().isEqualTo(vc);
    }

    protected ObjectMapper getMapper() {
        return MAPPER;
    }

    protected abstract Class<T> getClazz();

    protected abstract T getEntity();
}
