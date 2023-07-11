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
import org.eclipse.edc.identityhub.store.sql.schema.BaseSqlIdentityHubStatements;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transaction.datasource.spi.DefaultDataSourceRegistry;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.lang.reflect.Method;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SqlIdentityHubStoreTest {

    @Mock
    private ResultSet resultSet;

    private SqlIdentityHubStore sqlIdentityHubStore;

    @BeforeEach
    void setUp() throws IOException {
        resultSet = mock(ResultSet.class);
        var statements = new BaseSqlIdentityHubStatements();
        var typeManager = new TypeManager();
        var dataSourceRegistry = new DefaultDataSourceRegistry();
        var transactionContext = new NoopTransactionContext();

        sqlIdentityHubStore = new SqlIdentityHubStore(dataSourceRegistry, "dataSourceName", transactionContext, statements, typeManager.getMapper());
    }



    @Test
    public void testParse() throws Exception {
        when(resultSet.getString("id")).thenReturn("123");
        when(resultSet.getString("payloadFormat")).thenReturn("json");
        when(resultSet.getString("payload")).thenReturn("{\"key\": \"value\"}");
        when(resultSet.getLong("created_at")).thenReturn(1625385600000L);

        Method parseMethod = SqlIdentityHubStore.class.getDeclaredMethod("parse", ResultSet.class);
        parseMethod.setAccessible(true);

        var result = (IdentityHubRecord) parseMethod.invoke(sqlIdentityHubStore, resultSet);

        assertThat("123").isEqualTo(result.getId());
        assertThat("json").isEqualTo(result.getPayloadFormat());
        assertThat("{\"key\": \"value\"}").isEqualTo(new String(result.getPayload()));
        assertThat(1625385600000L).isEqualTo(result.getCreatedAt());
    }

}
