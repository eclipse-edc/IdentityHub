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

package org.eclipse.edc.identityhub.spi.events.diddocument;

import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


class DidDocumentUnpublishedTest {

    private final TypeManager manager = new JacksonTypeManager();

    @Test
    void verify_serDes() {

        var event = DidDocumentUnpublished.Builder.newInstance()
                .did("did:web:test")
                .participantId("test-id")
                .build();

        var json = manager.writeValueAsString(event);
        assertThat(json).isNotNull();

        assertThat(manager.readValue(json, DidDocumentUnpublished.class)).usingRecursiveComparison().isEqualTo(event);
    }
}