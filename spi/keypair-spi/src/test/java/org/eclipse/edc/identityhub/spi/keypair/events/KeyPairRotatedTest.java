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

package org.eclipse.edc.identityhub.spi.keypair.events;

import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairResource;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class KeyPairRotatedTest {

    private final TypeManager typeManager = new JacksonTypeManager();

    @Test
    void verify_serDes() {
        var evt = KeyPairRotated.Builder.newInstance()
                .keyPairResource(KeyPairResource.Builder.newInstance().id(UUID.randomUUID().toString()).build())
                .keyId("key-id")
                .participantContextId("participant-id")
                .build();

        var json = typeManager.writeValueAsString(evt);
        assertThat(json).isNotNull();

        assertThat(typeManager.readValue(json, KeyPairRotated.class)).usingRecursiveComparison().isEqualTo(evt);
    }
}