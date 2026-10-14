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

package org.eclipse.edc.iam.decentralizedclaims.sts.store;

import org.eclipse.edc.iam.decentralizedclaims.sts.store.schema.BaseSqlDialectStatements;
import org.eclipse.edc.iam.decentralizedclaims.sts.store.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.annotations.ComponentTest;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.iam.decentralizedclaims.sts.spi.store.fixtures.TestFunctions.createClient;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

/**
 * Verifies that a create that races with another one for the same account, or for another account with the same client
 * ID, fails with {@code ALREADY_EXISTS}, rather than with a primary key or unique violation.
 * <p>
 * This cannot use the transaction context of {@link PostgresqlStoreSetupExtension}: it auto-commits every statement, so the
 * first account would be visible to the second create before it inserts.
 */
@ComponentTest
@ExtendWith(PostgresqlStoreSetupExtension.class)
class SqlStsAccountStoreConcurrencyTest {

    private static final String CLIENT_ID = "client-id";
    private final BaseSqlDialectStatements statements = new PostgresDialectStatements();
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    private LocalTransactionContext transactionContext;
    private SqlStsAccountStore store;

    @BeforeEach
    void setup(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) {
        transactionContext = new LocalTransactionContext(new ConsoleMonitor());
        var registry = new LocalDataSourceRegistry(transactionContext);
        // DefaultDataSourceRegistry.resolve() hands back the raw DataSource, which the local registry then enlists
        registry.register(extension.getDatasourceName(), extension.getDataSourceRegistry().resolve(extension.getDatasourceName()));

        store = new SqlStsAccountStore(registry, extension.getDatasourceName(), transactionContext,
                new JacksonTypeManager()::getMapper, statements, queryExecutor);

        extension.runQuery(TestUtils.getResourceFileContentAsString("sts-client-schema.sql"));
    }

    @AfterEach
    void tearDown(PostgresqlStoreSetupExtension extension) throws InterruptedException {
        executor.shutdownNow();
        assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        extension.runQuery("DROP TABLE " + statements.getStsClientTable() + " CASCADE");
    }

    @ParameterizedTest
    @ValueSource(strings = { "first-id", "second-id" })
    void create_whenOtherTransactionCreatesConflictingAccount_shouldReturnAlreadyExists(String secondId) throws Exception {
        var created = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        try {
            var first = executor.submit(() -> transactionContext.execute(() -> {
                var result = store.create(createClient("first-id", "first-alias", CLIENT_ID));
                created.countDown();
                awaitUninterruptibly(release);
                return result;
            }));
            assertThat(created.await(30, TimeUnit.SECONDS)).isTrue();

            // the first account is not committed yet, so the second create does not find it, and its insert waits for
            // the first transaction to complete
            var second = executor.submit(() -> transactionContext.execute(() -> store.create(createClient(secondId, "second-alias", CLIENT_ID))));
            assertThatThrownBy(() -> second.get(1, TimeUnit.SECONDS)).isInstanceOf(TimeoutException.class);

            release.countDown();
            assertThat(first.get(30, TimeUnit.SECONDS)).isSucceeded();
            assertThat(second.get(30, TimeUnit.SECONDS)).isFailed()
                    .satisfies(f -> assertThat(f.getReason()).isEqualTo(StoreFailure.Reason.ALREADY_EXISTS));
        } finally {
            release.countDown();
        }

        var accounts = transactionContext.execute(() -> store.findAll(QuerySpec.max()).toList());
        assertThat(accounts).singleElement().satisfies(account -> assertThat(account.getId()).isEqualTo("first-id"));
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
