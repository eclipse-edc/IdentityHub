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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@PostgresqlDbIntegrationTest
@ExtendWith(PostgresqlStoreSetupExtension.class)
public class PostgresParticipantStoreTest extends IdentityHubStoreTestBase {
    private SqlIdentityHubStore store;
    @Mock
    private ResultSet resultSet;

    @BeforeEach
    void setUp(PostgresqlStoreSetupExtension extension) throws IOException {
        var statements = new BaseSqlIdentityHubStatements();
        var typeManager = new TypeManager();

        store = new SqlIdentityHubStore(extension.getDataSourceRegistry(), extension.getDatasourceName(), extension.getTransactionContext(), statements, typeManager.getMapper());

        var schema = Files.readString(Paths.get("docs/schema.sql"));
        extension.runQuery(schema);
    }

    @Test
    public void testParse() throws SQLException {
        // Mock the necessary values in the ResultSet
        when(resultSet.getString("idColumn")).thenReturn("123");
        when(resultSet.getString("payloadFormatColumn")).thenReturn("json");
        when(resultSet.getString("payloadColumn")).thenReturn("{\"key\": \"value\"}");
        when(resultSet.getLong("createdAtColumn")).thenReturn(1625385600000L);

        // Call the parse method
        IdentityHubRecord result = store.parse(resultSet);

        // Verify the parsed IdentityHubRecord
        assertEquals("123", result.getId());
        assertEquals("json", result.getPayloadFormat());
        assertEquals("{\"key\": \"value\"}", new String(result.getPayload()));
        assertEquals(1625385600000L, result.getCreatedAt());
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
}
