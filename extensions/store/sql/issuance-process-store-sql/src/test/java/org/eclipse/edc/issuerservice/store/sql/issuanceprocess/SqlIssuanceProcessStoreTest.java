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

package org.eclipse.edc.issuerservice.store.sql.issuanceprocess;

import org.eclipse.edc.issuerservice.spi.issuance.process.store.IssuanceProcessStore;
import org.eclipse.edc.issuerservice.spi.issuance.process.store.IssuanceProcessStoreTestBase;
import org.eclipse.edc.issuerservice.store.sql.issuanceprocess.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.testfixtures.LeaseUtil;
import org.eclipse.edc.sql.testfixtures.PostgresqlStoreSetupExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Clock;
import java.time.Duration;

@PostgresqlIntegrationTest
@ExtendWith(PostgresqlStoreSetupExtension.class)
class SqlIssuanceProcessStoreTest extends IssuanceProcessStoreTestBase {

    private IssuanceProcessStoreStatements statements;
    private SqlIssuanceProcessStore store;
    private LeaseUtil leaseUtil;

    @BeforeEach
    void setup(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) {
        var typeManager = new JacksonTypeManager();

        statements = new PostgresDialectStatements();
        store = new SqlIssuanceProcessStore(extension.getDataSourceRegistry(), extension.getDatasourceName(),
                extension.getTransactionContext(), typeManager.getMapper(), queryExecutor, statements, RUNTIME_ID, Clock.systemUTC());

        leaseUtil = new LeaseUtil(extension.getTransactionContext(), extension::getConnection, statements, clock);

        var schema = TestUtils.getResourceFileContentAsString("issuance-process-schema.sql");
        extension.runQuery(schema);
    }

    @AfterEach
    void tearDown(PostgresqlStoreSetupExtension extension) {
        extension.runQuery("DROP TABLE " + statements.getIssuanceProcessTable() + " CASCADE");
        extension.runQuery("DROP TABLE " + statements.getLeaseTableName() + " CASCADE");
    }

    @Override
    protected IssuanceProcessStore getStore() {
        return store;
    }

    @Override
    protected void leaseEntity(String issuanceId, String owner, Duration duration) {
        leaseUtil.leaseEntity(issuanceId, owner, duration);
    }

    @Override
    protected boolean isLeasedBy(String issuanceId, String owner) {
        return leaseUtil.isLeased(issuanceId, owner);
    }
}