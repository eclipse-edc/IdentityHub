/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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

package org.eclipse.edc.issuerservice.store.sql.holder;

import org.eclipse.edc.issuerservice.spi.holder.model.Holder;
import org.eclipse.edc.issuerservice.store.sql.holder.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreFailure;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.testfixtures.PostgresqlStoreSetupExtension;
import org.eclipse.edc.transaction.local.LocalDataSourceRegistry;
import org.eclipse.edc.transaction.local.LocalTransactionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

/**
 * Verifies that a create that races with another one for the same holder fails with {@code ALREADY_EXISTS}, rather than
 * with a primary key violation.
 * <p>
 * This cannot use the transaction context of {@link PostgresqlStoreSetupExtension}: it auto-commits every statement, so the
 * first holder would be visible to the second create before it inserts.
 */
@PostgresqlIntegrationTest
@ExtendWith(PostgresqlStoreSetupExtension.class)
class SqlHolderStoreConcurrencyTest {

    private static final String HOLDER_ID = "holder-id";
    private final HolderStoreStatements statements = new PostgresDialectStatements();
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    private LocalTransactionContext transactionContext;
    private SqlHolderStore store;

    @BeforeEach
    void setup(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) {
        transactionContext = new LocalTransactionContext(new ConsoleMonitor());
        var registry = new LocalDataSourceRegistry(transactionContext);
        // DefaultDataSourceRegistry.resolve() hands back the raw DataSource, which the local registry then enlists
        registry.register(extension.getDatasourceName(), extension.getDataSourceRegistry().resolve(extension.getDatasourceName()));

        store = new SqlHolderStore(registry, extension.getDatasourceName(), transactionContext,
                new JacksonTypeManager()::getMapper, queryExecutor, statements);

        extension.runQuery(TestUtils.getResourceFileContentAsString("holder-schema.sql"));
    }

    @AfterEach
    void tearDown(PostgresqlStoreSetupExtension extension) throws InterruptedException {
        executor.shutdownNow();
        assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        extension.runQuery("DROP TABLE " + statements.getHoldersTable() + " CASCADE");
    }

    @Test
    void create_whenOtherTransactionCreatesSameHolder_shouldReturnAlreadyExists() throws Exception {
        var created = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        try {
            var first = executor.submit(() -> transactionContext.execute(() -> {
                var result = store.create(createHolder("did:web:first"));
                created.countDown();
                awaitUninterruptibly(release);
                return result;
            }));
            assertThat(created.await(30, TimeUnit.SECONDS)).isTrue();

            // the first holder is not committed yet, so the second create does not find it, and its insert waits for the
            // first transaction to complete
            var second = executor.submit(() -> transactionContext.execute(() -> store.create(createHolder("did:web:second"))));
            assertThatThrownBy(() -> second.get(1, TimeUnit.SECONDS)).isInstanceOf(TimeoutException.class);

            release.countDown();
            assertThat(first.get(30, TimeUnit.SECONDS)).isSucceeded();
            assertThat(second.get(30, TimeUnit.SECONDS)).isFailed()
                    .satisfies(f -> assertThat(f.getReason()).isEqualTo(StoreFailure.Reason.ALREADY_EXISTS));
        } finally {
            release.countDown();
        }

        var holders = transactionContext.execute(() -> store.query(QuerySpec.max()).getContent());
        assertThat(holders).singleElement().satisfies(holder -> assertThat(holder.getDid()).isEqualTo("did:web:first"));
    }

    private Holder createHolder(String did) {
        return Holder.Builder.newInstance()
                .participantContextId("participant-context-id")
                .holderId(HOLDER_ID)
                .did(did)
                .holderName("holder-name")
                .build();
    }

    private void awaitUninterruptibly(CountDownLatch latch) {
        try {
            if (!latch.await(30, TimeUnit.SECONDS)) {
                throw new IllegalStateException("timed out waiting for latch");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
