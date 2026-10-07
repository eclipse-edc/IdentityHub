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

import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.identityhub.did.store.sql.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.identityhub.did.store.test.DidResourceStoreTestBase;
import org.eclipse.edc.identityhub.spi.did.model.DidResource;
import org.eclipse.edc.identityhub.spi.did.model.DidState;
import org.eclipse.edc.identityhub.spi.did.store.DidResourceStore;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
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
class SqlDidResourceStoreTest extends DidResourceStoreTestBase {

    private final DidResourceStatements statements = new PostgresDialectStatements();
    private final JacksonTypeManager typeManager = new JacksonTypeManager();
    private SqlDidResourceStore store;

    @BeforeEach
    void setup(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) {
        store = new SqlDidResourceStore(extension.getDataSourceRegistry(), extension.getDatasourceName(),
                extension.getTransactionContext(), typeManager.getMapper(), queryExecutor, statements);

        var schema = TestUtils.getResourceFileContentAsString("did-schema.sql");
        extension.runQuery(schema);
    }

    @AfterEach
    void tearDown(PostgresqlStoreSetupExtension extension) {
        extension.runQuery("DROP TABLE " + statements.getDidResourceTableName() + " CASCADE");
    }

    @Test
    void queryForUpdate_locksEntriesUntilTransactionCompletes(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) throws Exception {
        var transactionContext = new LocalTransactionContext(mock(Monitor.class));
        var transactionalStore = createTransactionalStore(extension, queryExecutor, transactionContext);

        var did = "did:web:locked";
        var didResource = DidResource.Builder.newInstance()
                .did(did)
                .participantContextId("test-participant")
                .document(DidDocument.Builder.newInstance().id(did).build())
                .state(DidState.GENERATED)
                .build();
        transactionalStore.save(didResource);
        var query = QuerySpec.Builder.newInstance().filter(new Criterion("did", "=", did)).build();

        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var lockHolder = CompletableFuture.runAsync(() -> transactionContext.execute(() -> {
            var resource = transactionalStore.queryForUpdate(query).iterator().next();
            // only committed once released, so a contender that does not wait for the lock would not see the service
            resource.getDocument().getService().add(new Service("service-id", "test-type", "https://test.com"));
            transactionalStore.update(resource);
            locked.countDown();
            awaitRelease(release);
        }));
        assertThat(locked.await(10, SECONDS)).isTrue();

        var contender = CompletableFuture.supplyAsync(() -> transactionContext.execute(() -> transactionalStore.queryForUpdate(query)));

        assertThatThrownBy(() -> contender.get(500, MILLISECONDS)).isInstanceOf(TimeoutException.class);
        release.countDown();

        assertThat(contender).succeedsWithin(Duration.ofSeconds(10))
                .satisfies(resources -> assertThat(resources)
                        .flatExtracting(resource -> resource.getDocument().getService())
                        .extracting(Service::getId)
                        .containsExactly("service-id"));
        lockHolder.get(10, SECONDS);
    }

    @Override
    protected DidResourceStore getStore() {
        return store;
    }

    /**
     * The extension's transaction context does not hold a connection across statements, so locks need real transactions.
     */
    private SqlDidResourceStore createTransactionalStore(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor,
                                                        LocalTransactionContext transactionContext) {
        var dataSourceRegistry = new LocalDataSourceRegistry(transactionContext);
        dataSourceRegistry.register(extension.getDatasourceName(), extension.getDataSourceRegistry().resolve(extension.getDatasourceName()));
        return new SqlDidResourceStore(dataSourceRegistry, extension.getDatasourceName(), transactionContext,
                typeManager.getMapper(), queryExecutor, statements);
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