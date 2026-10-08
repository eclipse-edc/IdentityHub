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

import org.assertj.core.api.Assertions;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcess;
import org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcessStates;
import org.eclipse.edc.issuerservice.spi.issuance.process.store.IssuanceProcessStore;
import org.eclipse.edc.issuerservice.spi.issuance.process.store.IssuanceProcessStoreTestBase;
import org.eclipse.edc.issuerservice.store.sql.issuanceprocess.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.StoreFailure;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.lease.BaseSqlLeaseStatements;
import org.eclipse.edc.sql.lease.SqlLeaseContextBuilderImpl;
import org.eclipse.edc.sql.lease.spi.LeaseStatements;
import org.eclipse.edc.sql.testfixtures.LeaseUtil;
import org.eclipse.edc.sql.testfixtures.PostgresqlStoreSetupExtension;
import org.eclipse.edc.transaction.local.LocalDataSourceRegistry;
import org.eclipse.edc.transaction.local.LocalTransactionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.Mockito.mock;

@PostgresqlIntegrationTest
@ExtendWith(PostgresqlStoreSetupExtension.class)
class SqlIssuanceProcessStoreTest extends IssuanceProcessStoreTestBase {
    private final LeaseStatements leaseStatements = new BaseSqlLeaseStatements();
    private final JacksonTypeManager typeManager = new JacksonTypeManager();

    private IssuanceProcessStoreStatements statements;
    private SqlIssuanceProcessStore store;
    private LeaseUtil leaseUtil;

    @BeforeEach
    void setup(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) {
        statements = new PostgresDialectStatements(leaseStatements, clock);
        var leaseContextBuilder = SqlLeaseContextBuilderImpl.with(extension.getTransactionContext(), RUNTIME_ID, statements.getIssuanceProcessTable(), leaseStatements, clock, queryExecutor);

        store = new SqlIssuanceProcessStore(extension.getDataSourceRegistry(), extension.getDatasourceName(),
                extension.getTransactionContext(), typeManager::getMapper, queryExecutor, statements, leaseContextBuilder);

        leaseUtil = new LeaseUtil(extension.getTransactionContext(), extension::getConnection, statements.getIssuanceProcessTable(), leaseStatements, clock);

        var schema = TestUtils.getResourceFileContentAsString("issuance-process-schema.sql");
        extension.runQuery(schema);
    }

    @AfterEach
    void tearDown(PostgresqlStoreSetupExtension extension) {
        extension.runQuery("DROP TABLE " + statements.getIssuanceProcessTable() + " CASCADE");
        extension.runQuery("DROP TABLE " + leaseStatements.getLeaseTableName() + " CASCADE");
    }

    @Test
    void save_whenHolderPidUsed_shouldNotAbortTransaction(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) {
        var transactionContext = new LocalTransactionContext(mock(Monitor.class));
        var transactionalStore = createTransactionalStore(extension, queryExecutor, transactionContext);
        transactionalStore.save(createIssuanceProcess("holder-pid"));
        var duplicate = createIssuanceProcess("holder-pid");
        var other = createIssuanceProcess("other-holder-pid");

        transactionContext.execute(() -> {
            assertThat(transactionalStore.save(duplicate)).isFailed()
                    .extracting(StoreFailure::getReason)
                    .isEqualTo(StoreFailure.Reason.ALREADY_EXISTS);
            // a failed statement would abort the transaction, and with it everything else that is written in it
            assertThat(transactionalStore.save(other)).isSucceeded();
        });

        Assertions.assertThat(transactionalStore.findById(other.getId())).isNotNull();
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

    /**
     * The extension's transaction context does not hold a connection across statements, so the test needs real transactions.
     */
    private SqlIssuanceProcessStore createTransactionalStore(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor,
                                                             LocalTransactionContext transactionContext) {
        var dataSourceRegistry = new LocalDataSourceRegistry(transactionContext);
        dataSourceRegistry.register(extension.getDatasourceName(), extension.getDataSourceRegistry().resolve(extension.getDatasourceName()));
        var leaseContextBuilder = SqlLeaseContextBuilderImpl.with(transactionContext, RUNTIME_ID, statements.getIssuanceProcessTable(), leaseStatements, clock, queryExecutor);
        return new SqlIssuanceProcessStore(dataSourceRegistry, extension.getDatasourceName(), transactionContext, typeManager::getMapper,
                queryExecutor, statements, leaseContextBuilder);
    }

    private IssuanceProcess createIssuanceProcess(String holderPid) {
        return IssuanceProcess.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .participantContextId("participant")
                .holderId("holder")
                .credentialFormats(Map.of("format", CredentialFormat.VC1_0_JWT))
                .holderPid(holderPid)
                .state(IssuanceProcessStates.APPROVED.code())
                .build();
    }
}