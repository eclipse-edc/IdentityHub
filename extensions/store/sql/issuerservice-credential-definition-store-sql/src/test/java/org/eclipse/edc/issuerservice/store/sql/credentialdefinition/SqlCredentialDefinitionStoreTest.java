/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.issuerservice.store.sql.credentialdefinition;

import org.eclipse.edc.issuerservice.spi.credentialdefinition.store.CredentialDefinitionStore;
import org.eclipse.edc.issuerservice.spi.credentialdefinition.store.CredentialDefinitionStoreTestBase;
import org.eclipse.edc.issuerservice.store.sql.credentialdefinition.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.testfixtures.PostgresqlStoreSetupExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Clock;

@PostgresqlIntegrationTest
@ExtendWith(PostgresqlStoreSetupExtension.class)
class SqlCredentialDefinitionStoreTest extends CredentialDefinitionStoreTestBase {

    private final CredentialDefinitionStoreStatements statements = new PostgresDialectStatements();
    private SqlCredentialDefinitionStore store;

    @BeforeEach
    void setup(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) {
        var typeManager = new JacksonTypeManager();
        store = new SqlCredentialDefinitionStore(extension.getDataSourceRegistry(), extension.getDatasourceName(),
                extension.getTransactionContext(), typeManager.getMapper(), queryExecutor, statements, Clock.systemUTC());

        var schema = TestUtils.getResourceFileContentAsString("credential-definition-schema.sql");
        extension.runQuery(schema);
    }

    @AfterEach
    void tearDown(PostgresqlStoreSetupExtension extension) {
        extension.runQuery("DROP TABLE " + statements.getCredentialDefinitionTable() + " CASCADE");
    }

    @Override
    protected CredentialDefinitionStore getStore() {
        return store;
    }
}