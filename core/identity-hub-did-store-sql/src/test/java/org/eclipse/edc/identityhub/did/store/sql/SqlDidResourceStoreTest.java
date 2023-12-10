/*
 *  Copyright (c) 2023 Metaform Systems, Inc.
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

package org.eclipse.edc.identityhub.did.store.sql;

import org.eclipse.edc.identithub.did.spi.store.DidResourceStore;
import org.eclipse.edc.identityhub.did.store.sql.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.identityhub.did.store.test.DidResourceStoreTestBase;
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
class SqlDidResourceStoreTest extends DidResourceStoreTestBase {

    private final DidResourceStatements statements = new PostgresDialectStatements();
    private SqlDidResourceStore store;

    @BeforeEach
    void setup(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) throws IOException {
        var typeManager = new TypeManager();
        store = new SqlDidResourceStore(extension.getDataSourceRegistry(), extension.getDatasourceName(),
                extension.getTransactionContext(), typeManager.getMapper(), queryExecutor, statements);

        var schema = Files.readString(Paths.get("./docs/schema.sql"));
        extension.runQuery(schema);
    }

    @AfterEach
    void tearDown(PostgresqlStoreSetupExtension extension) {
        extension.runQuery("DROP TABLE " + statements.getDidResourceTableName() + " CASCADE");
    }

    @Override
    protected DidResourceStore getStore() {
        return store;
    }
}