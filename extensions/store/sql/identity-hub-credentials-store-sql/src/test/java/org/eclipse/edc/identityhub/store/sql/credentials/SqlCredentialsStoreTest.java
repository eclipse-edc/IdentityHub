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

package org.eclipse.edc.identityhub.store.sql.credentials;

import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.identityhub.store.sql.credentials.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.identityhub.verifiablecredentials.store.CredentialStoreTestBase;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.testfixtures.PostgresqlStoreSetupExtension;
import org.eclipse.edc.transaction.local.LocalDataSourceRegistry;
import org.eclipse.edc.transaction.local.LocalTransactionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@ComponentTest
@ExtendWith(PostgresqlStoreSetupExtension.class)
class SqlCredentialsStoreTest extends CredentialStoreTestBase {

    private final CredentialStoreStatements statements = new PostgresDialectStatements();
    private final JacksonTypeManager typeManager = new JacksonTypeManager();
    private SqlCredentialStore store;

    @BeforeEach
    void setup(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) {
        store = new SqlCredentialStore(extension.getDataSourceRegistry(), extension.getDatasourceName(),
                extension.getTransactionContext(), typeManager.getMapper(), queryExecutor, statements);

        var schema = TestUtils.getResourceFileContentAsString("credentials-schema.sql");
        extension.runQuery(schema);
    }

    @AfterEach
    void tearDown(PostgresqlStoreSetupExtension extension) {
        extension.runQuery("DROP TABLE " + statements.getCredentialResourceTable() + " CASCADE");
    }

    @Test
    void queryForUpdate_locksEntriesUntilTransactionCompletes(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) throws Exception {
        var transactionContext = new LocalTransactionContext(mock(Monitor.class));
        var transactionalStore = createTransactionalStore(extension, queryExecutor, transactionContext);

        var credential = createCredential();
        transactionalStore.create(credential);
        var query = byId(credential.getId());

        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var lockHolder = CompletableFuture.runAsync(() -> transactionContext.execute(() -> {
            transactionalStore.queryForUpdate(query);
            // only committed once released, so a contender that does not wait for the lock would still see ISSUED
            transactionalStore.update(credential.toBuilder().state(VcStatus.REVOKED).build());
            locked.countDown();
            awaitRelease(release);
        }));
        assertThat(locked.await(10, SECONDS)).isTrue();

        var contender = CompletableFuture.supplyAsync(() -> transactionContext.execute(() -> transactionalStore.queryForUpdate(query)));

        assertThatThrownBy(() -> contender.get(500, MILLISECONDS)).isInstanceOf(TimeoutException.class);
        release.countDown();

        assertThat(contender).succeedsWithin(Duration.ofSeconds(10))
                .satisfies(result -> assertThat(result.getContent())
                        .extracting(VerifiableCredentialResource::getState)
                        .containsExactly(VcStatus.REVOKED.code()));
        lockHolder.get(10, SECONDS);
    }

    @Test
    void queryForUpdate_locksEntriesInSortOrder(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) throws Exception {
        var transactionContext = new LocalTransactionContext(mock(Monitor.class));
        var transactionalStore = createTransactionalStore(extension, queryExecutor, transactionContext);

        // the newer entry is inserted first, so that a scan in physical order would reach it before the older one
        var newer = createCredentialBuilder().timestamp(2000).build();
        var older = createCredentialBuilder().timestamp(1000).build();
        transactionalStore.create(newer);
        transactionalStore.create(older);
        var oldestFirst = QuerySpec.Builder.newInstance()
                .sortField("timestamp")
                .sortOrder(SortOrder.ASC)
                .build();

        var olderLocked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var lockHolder = CompletableFuture.runAsync(() -> transactionContext.execute(() -> {
            transactionalStore.queryForUpdate(byId(older.getId()));
            olderLocked.countDown();
            awaitRelease(release);
        }));
        assertThat(olderLocked.await(10, SECONDS)).isTrue();

        var contender = CompletableFuture.supplyAsync(() -> transactionContext.execute(() -> transactionalStore.queryForUpdate(oldestFirst)));
        assertThatThrownBy(() -> contender.get(500, MILLISECONDS)).isInstanceOf(TimeoutException.class);

        // the contender waits for the older entry without holding the lock of the newer one, which is therefore still free
        var newerLock = CompletableFuture.supplyAsync(() -> transactionContext.execute(() -> transactionalStore.queryForUpdate(byId(newer.getId()))));
        assertThat(newerLock).succeedsWithin(Duration.ofSeconds(5));

        release.countDown();
        assertThat(contender).succeedsWithin(Duration.ofSeconds(10));
        lockHolder.get(10, SECONDS);
    }

    @Override
    protected CredentialStore getStore() {
        return store;
    }

    /**
     * The extension's transaction context does not hold a connection across statements, so locks need real transactions.
     */
    private SqlCredentialStore createTransactionalStore(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor,
                                                        LocalTransactionContext transactionContext) {
        var dataSourceRegistry = new LocalDataSourceRegistry(transactionContext);
        dataSourceRegistry.register(extension.getDatasourceName(), extension.getDataSourceRegistry().resolve(extension.getDatasourceName()));
        return new SqlCredentialStore(dataSourceRegistry, extension.getDatasourceName(), transactionContext,
                typeManager.getMapper(), queryExecutor, statements);
    }

    private QuerySpec byId(String id) {
        return QuerySpec.Builder.newInstance().filter(new Criterion("id", "=", id)).build();
    }

    private void awaitRelease(CountDownLatch release) {
        try {
            assertThat(release.await(10, SECONDS)).isTrue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
