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

package org.eclips.dataspaceconnector.identityhub.store.sql;

import org.eclipse.dataspaceconnector.common.util.junit.annotations.PostgresqlDbIntegrationTest;
import org.eclipse.dataspaceconnector.identityhub.store.spi.IdentityHubStore;
import org.eclipse.dataspaceconnector.identityhub.store.spi.IdentityHubStoreTestBase;
import org.eclipse.dataspaceconnector.identityhub.store.sql.SqlIdentityHubStore;
import org.eclipse.dataspaceconnector.identityhub.store.sql.schema.BaseSqlIdentityHubStatements;
import org.eclipse.dataspaceconnector.spi.transaction.NoopTransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.datasource.DataSourceRegistry;
import org.eclipse.dataspaceconnector.sql.PostgresqlLocalInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;

import static org.eclipse.dataspaceconnector.sql.SqlQueryExecutor.executeQuery;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@PostgresqlDbIntegrationTest
public class PostgresParticipantStoreTest extends IdentityHubStoreTestBase {

    protected static final String DATASOURCE_NAME = "identityhub";
    private static final String POSTGRES_USER = "postgres";
    private static final String POSTGRES_PASSWORD = "password";
    private static final String POSTGRES_DATABASE = "itest";

    private TransactionContext transactionContext;
    private Connection connection;
    private SqlIdentityHubStore store;

    @BeforeAll
    static void prepare() {
        PostgresqlLocalInstance.createDatabase(POSTGRES_DATABASE);
    }

    @BeforeEach
    void setUp() throws SQLException, IOException {
        transactionContext = new NoopTransactionContext();
        var dataSourceRegistry = mock(DataSourceRegistry.class);

        var statements = new BaseSqlIdentityHubStatements();

        var ds = new PGSimpleDataSource();
        ds.setServerNames(new String[] {"localhost"});
        ds.setPortNumbers(new int[] {5432});
        ds.setUser(POSTGRES_USER);
        ds.setPassword(POSTGRES_PASSWORD);
        ds.setDatabaseName(POSTGRES_DATABASE);

        // do not actually close
        connection = spy(ds.getConnection());
        doNothing().when(connection).close();

        var datasourceMock = mock(DataSource.class);
        when(datasourceMock.getConnection()).thenReturn(connection);
        when(dataSourceRegistry.resolve(DATASOURCE_NAME)).thenReturn(datasourceMock);

        store = new SqlIdentityHubStore(dataSourceRegistry, DATASOURCE_NAME, transactionContext, statements);

        var schema = Files.readString(Paths.get("docs/schema.sql"));
        try {
            transactionContext.execute(() -> {
                executeQuery(connection, schema);
                return null;
            });
        } catch (Exception exc) {
            fail(exc);
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        transactionContext.execute(() -> {
            var dialect = new BaseSqlIdentityHubStatements();
            executeQuery(connection, "DROP TABLE " + dialect.getIdentityHubTable());
        });
        doCallRealMethod().when(connection).close();
        connection.close();
    }

    @Override
    protected IdentityHubStore getStore() {
        return store;
    }
}
