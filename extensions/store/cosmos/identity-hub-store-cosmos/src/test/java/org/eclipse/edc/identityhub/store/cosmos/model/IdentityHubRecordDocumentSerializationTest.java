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

package org.eclipse.edc.identityhub.store.cosmos.model;

import org.eclipse.edc.identityhub.store.spi.IdentityHubRecord;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IdentityHubRecordDocumentSerializationTest {

    private TypeManager typeManager;


    @BeforeEach
    void setup() {
        typeManager = new TypeManager();
        typeManager.registerTypes(IdentityHubRecordDocument.class, IdentityHubRecord.class);
    }

    @Test
    void testSerialization() {
        var record = createRecord();

        var document = new IdentityHubRecordDocument(record, "partitionkey-test");

        var s = typeManager.writeValueAsString(document);

        assertThat(s).isNotNull()
                .contains("\"partitionKey\":\"partitionkey-test\"")
                .contains("\"id\":\"id-test\"")
                .contains("\"payloadFormat\":\"format-test\"")
                .contains("\"payload\":");
    }

    @Test
    void testDeserialization() {
        var record = createRecord();

        var document = new IdentityHubRecordDocument(record, "partitionkey-test");
        var json = typeManager.writeValueAsString(document);

        var deserialized = typeManager.readValue(json, IdentityHubRecordDocument.class);
        assertThat(deserialized.getWrappedInstance()).usingRecursiveComparison().isEqualTo(document.getWrappedInstance());
    }

    private static IdentityHubRecord createRecord() {
        return IdentityHubRecord.Builder.newInstance()
                .id("id-test")
                .payloadFormat("format-test")
                .payload("test".getBytes())
                .build();
    }
}
