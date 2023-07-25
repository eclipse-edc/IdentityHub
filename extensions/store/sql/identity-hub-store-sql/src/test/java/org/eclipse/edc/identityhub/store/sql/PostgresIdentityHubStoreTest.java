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

import org.eclipse.edc.identityhub.store.spi.IdentityHubStore;
import org.eclipse.edc.identityhub.store.spi.IdentityHubStoreTestBase;
import org.eclipse.edc.identityhub.store.sql.schema.BaseSqlIdentityHubStatements;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.testfixtures.PostgresqlStoreSetupExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@ComponentTest
@ExtendWith(PostgresqlStoreSetupExtension.class)
public class PostgresIdentityHubStoreTest extends IdentityHubStoreTestBase {
    private SqlIdentityHubStore store;

    @BeforeEach
    void setUp(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) throws IOException {
        var statements = new BaseSqlIdentityHubStatements();
        var typeManager = new TypeManager();

        store = new SqlIdentityHubStore(extension.getDataSourceRegistry(), extension.getDatasourceName(),
                extension.getTransactionContext(), statements, typeManager.getMapper(), queryExecutor);

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
}
