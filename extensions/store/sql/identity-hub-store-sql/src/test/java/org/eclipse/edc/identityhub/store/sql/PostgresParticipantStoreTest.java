/*
 *  Copyright (c) 2020 - 2023 Amadeus
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

package org.eclipse.edc.identityhub.store.sql;

import org.eclipse.edc.identityhub.store.spi.IdentityHubRecord;
import org.eclipse.edc.identityhub.store.spi.IdentityHubStore;
import org.eclipse.edc.identityhub.store.spi.IdentityHubStoreTestBase;
import org.eclipse.edc.identityhub.store.sql.schema.BaseSqlIdentityHubStatements;
import org.eclipse.edc.junit.annotations.PostgresqlDbIntegrationTest;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.sql.testfixtures.PostgresqlStoreSetupExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@PostgresqlDbIntegrationTest
@ExtendWith(PostgresqlStoreSetupExtension.class)
public class PostgresParticipantStoreTest extends IdentityHubStoreTestBase {
    private SqlIdentityHubStore store;

    @BeforeEach
    void setUp(PostgresqlStoreSetupExtension extension) throws IOException {
        var statements = new BaseSqlIdentityHubStatements();
        var typeManager = new TypeManager();

        store = new SqlIdentityHubStore(extension.getDataSourceRegistry(), extension.getDatasourceName(), extension.getTransactionContext(), statements, typeManager.getMapper());

        var schema = Files.readString(Paths.get("docs/schema.sql"));
        extension.runQuery(schema);
    }

    @AfterEach
    void tearDown(PostgresqlStoreSetupExtension extension) {
        var dialect = new BaseSqlIdentityHubStatements();
        extension.runQuery("DROP TABLE " + dialect.getTable());
    }

    @Override
    protected IdentityHubStore getStore() {
        return store;
    }

    @Test
    @DisplayName("Verify creation of an IdentityHubRecord object")
    void addIdentityHubRecord() {
        IdentityHubRecord record = IdentityHubRecord.Builder.newInstance()
                .id("id1")
                .payloadFormat("format1")
                .payload("payload1".getBytes())
                .createdAt(System.currentTimeMillis())
                .build();

        store.add(record);

        var records = store.getAll().collect(Collectors.toList());

        assertThat(records).containsExactly(record);
    }

    @Test
    @DisplayName("Verify getAll method returns all added records")
    void getAllIdentityHubRecords() {
        IdentityHubRecord record1 = IdentityHubRecord.Builder.newInstance()
                .id("id2")
                .payloadFormat("format2")
                .payload("payload2".getBytes())
                .createdAt(System.currentTimeMillis())
                .build();

        IdentityHubRecord record2 = IdentityHubRecord.Builder.newInstance()
                .id("id3")
                .payloadFormat("format3")
                .payload("payload3".getBytes())
                .createdAt(System.currentTimeMillis())
                .build();

        store.add(record1);
        store.add(record2);

        var records = store.getAll().collect(Collectors.toList());

        assertThat(records).containsExactlyInAnyOrder(record1, record2);
    }
}
