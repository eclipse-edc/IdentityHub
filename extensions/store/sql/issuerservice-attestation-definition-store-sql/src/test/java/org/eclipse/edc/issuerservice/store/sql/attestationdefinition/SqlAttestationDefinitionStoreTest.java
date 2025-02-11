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

package org.eclipse.edc.issuerservice.store.sql.attestationdefinition;

import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionStore;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.store.AttestationDefinitionStoreTestBase;
import org.eclipse.edc.issuerservice.store.sql.attestationdefinition.schema.postgres.PostgresDialectStatements;
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
class SqlAttestationDefinitionStoreTest extends AttestationDefinitionStoreTestBase {

    private final AttestationDefinitionStoreStatements statements = new PostgresDialectStatements();
    private SqlAttestationDefinitionStore store;

    @BeforeEach
    void setup(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) {
        var typeManager = new JacksonTypeManager();
        store = new SqlAttestationDefinitionStore(extension.getDataSourceRegistry(), extension.getDatasourceName(),
                extension.getTransactionContext(), typeManager.getMapper(), queryExecutor, statements, Clock.systemUTC());

        var schema = TestUtils.getResourceFileContentAsString("attestation-definition-schema.sql");
        extension.runQuery(schema);
    }

    @AfterEach
    void tearDown(PostgresqlStoreSetupExtension extension) {
        extension.runQuery("DROP TABLE " + statements.getAttestationDefinitionTable() + " CASCADE");
    }

    @Override
    protected AttestationDefinitionStore getStore() {
        return store;
    }
}