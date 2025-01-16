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

package org.eclipse.edc.identityhub.api.verifiablecredential.v1.unstable.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeyDescriptorTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void verify_serdes() throws JsonProcessingException {
        var descriptor = KeyDescriptor.Builder.newInstance()
                .keyId("key-id")
                .privateKeyAlias("alias")
                .publicKeyJwk(Map.of("foo", "bar"))
                .publicKeyPem("pem formatted text")
                .keyGeneratorParams(Map.of("bar", "baz"))
                .build();

        var json = mapper.writeValueAsString(descriptor);
        assertThat(json).isNotNull();

        var deser = mapper.readValue(json, KeyDescriptor.class);
        assertThat(deser).usingRecursiveComparison().isEqualTo(descriptor);
    }
}