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
import org.eclipse.edc.sql.testfixtures.PostgresqlStoreSetupExtension;
import org.eclipse.edc.sql.testfixtures.RowLeaseUtil;
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
    private final JacksonTypeManager typeManager = new JacksonTypeManager();

    private IssuanceProcessStoreStatements statements;
    private SqlIssuanceProcessStore store;
    private RowLeaseUtil leaseUtil;

    @BeforeEach
    void setup(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) {
        statements = new PostgresDialectStatements(clock);

        store = new SqlIssuanceProcessStore(extension.getDataSourceRegistry(), extension.getDatasourceName(),
                extension.getTransactionContext(), typeManager::getMapper, queryExecutor, statements, RUNTIME_ID, clock);

        leaseUtil = new RowLeaseUtil(extension.getTransactionContext(), extension::getConnection, statements, statements.getIssuanceProcessTable(), clock);

        var schema = TestUtils.getResourceFileContentAsString("issuance-process-schema.sql");
        extension.runQuery(schema);
    }

    @AfterEach
    void tearDown(PostgresqlStoreSetupExtension extension) {
        extension.runQuery("DROP TABLE " + statements.getIssuanceProcessTable() + " CASCADE");
    }

    @Test
    void schema_shouldAddLeaseAndVersionColumnsToExistingTable(PostgresqlStoreSetupExtension extension) {
        extension.runQuery("ALTER TABLE %s DROP COLUMN leased_by, DROP COLUMN leased_at, DROP COLUMN lease_duration, DROP COLUMN version"
                .formatted(statements.getIssuanceProcessTable()));

        extension.runQuery(TestUtils.getResourceFileContentAsString("issuance-process-schema.sql"));

        var process = createIssuanceProcess("holder-pid");
        assertThat(store.save(process)).isSucceeded();
        Assertions.assertThat(store.nextNotLeased(1)).extracting(IssuanceProcess::getId).containsExactly(process.getId());
        Assertions.assertThat(leaseUtil.isLeased(process.getId(), RUNTIME_ID)).isTrue();
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

    @Test
    void save_whenChangedSinceRead_shouldFail() {
        var process = createIssuanceProcess("holder-pid");
        store.save(process);
        var outdated = store.findById(process.getId());
        var current = store.findById(process.getId());
        current.transitionToDelivering();
        assertThat(store.save(current)).isSucceeded();

        outdated.transitionToApproved();

        assertThat(store.save(outdated)).isFailed()
                .extracting(StoreFailure::getReason)
                .isEqualTo(StoreFailure.Reason.CONFLICT);
        Assertions.assertThat(store.findById(process.getId()).getState()).isEqualTo(IssuanceProcessStates.DELIVERING.code());
    }

    @Test
    void save_whenSavedAgain_shouldUpdate() {
        var process = createIssuanceProcess("holder-pid");
        store.save(process);
        var read = store.findById(process.getId());
        read.transitionToDelivering();
        assertThat(store.save(read)).isSucceeded();

        read.transitionToDelivered();

        assertThat(store.save(read)).isSucceeded();
        Assertions.assertThat(store.findById(process.getId()).getState()).isEqualTo(IssuanceProcessStates.DELIVERED.code());
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
        return new SqlIssuanceProcessStore(dataSourceRegistry, extension.getDatasourceName(), transactionContext, typeManager::getMapper,
                queryExecutor, statements, RUNTIME_ID, clock);
    }

    private IssuanceProcess createIssuanceProcess(String holderPid) {
        return IssuanceProcess.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .participantContextId("participant")
                .holderId("holder")
                .credentialFormats(Map.of("format", CredentialFormat.VC1_0_JWT))
                .holderPid(holderPid)
                .state(IssuanceProcessStates.APPROVED.code())
                // older than the state timestamp of any transition in the test, which would otherwise not change it
                .stateTimestamp(clock.millis() - 1000)
                .build();
    }
}