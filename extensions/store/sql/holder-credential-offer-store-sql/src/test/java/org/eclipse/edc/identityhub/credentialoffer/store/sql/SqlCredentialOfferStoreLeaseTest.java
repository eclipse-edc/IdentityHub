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

package org.eclipse.edc.identityhub.credentialoffer.store.sql;

import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialOffer;
import org.eclipse.edc.identityhub.store.sql.credentialoffer.schema.CredentialOfferStoreStatements;
import org.eclipse.edc.identityhub.store.sql.credentialoffer.schema.SqlCredentialOfferStore;
import org.eclipse.edc.identityhub.store.sql.credentialoffer.schema.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
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

import java.time.Clock;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialOfferStatus.PROCESSED;
import static org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialOfferStatus.RECEIVED;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.persistence.StateEntityStore.hasState;

/**
 * Verifies that a leased {@link CredentialOffer} contains the changes of another transaction that held its lease
 * until it completed.
 * <p>
 * This cannot use the transaction context of {@link PostgresqlStoreSetupExtension}: it auto-commits every statement, so a
 * lease would never be held by an open transaction. Each store has its own lease holder, like two replicas.
 */
@PostgresqlIntegrationTest
@ExtendWith(PostgresqlStoreSetupExtension.class)
class SqlCredentialOfferStoreLeaseTest {

    private static final String OFFER_ID = "offer-id";
    private static final String RUNTIME_ID = "runtime-id";
    private final Clock clock = Clock.systemUTC();
    private final LeaseStatements leaseStatements = new BaseSqlLeaseStatements();
    private final CredentialOfferStoreStatements statements = new PostgresDialectStatements(leaseStatements, clock);
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    private LocalTransactionContext transactionContext;
    private SqlCredentialOfferStore store;
    private SqlCredentialOfferStore otherStore;
    private LeaseUtil leaseUtil;

    @BeforeEach
    void setup(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) {
        transactionContext = new LocalTransactionContext(new ConsoleMonitor());
        var registry = new LocalDataSourceRegistry(transactionContext);
        // DefaultDataSourceRegistry.resolve() hands back the raw DataSource, which the local registry then enlists
        registry.register(extension.getDatasourceName(), extension.getDataSourceRegistry().resolve(extension.getDatasourceName()));

        store = createStore(registry, extension.getDatasourceName(), queryExecutor, RUNTIME_ID);
        otherStore = createStore(registry, extension.getDatasourceName(), queryExecutor, "other-runtime-id");
        leaseUtil = new LeaseUtil(transactionContext, extension::getConnection, statements.getCredentialOffersTable(), leaseStatements, clock);

        extension.runQuery(TestUtils.getResourceFileContentAsString("credential-offer-schema.sql"));
    }

    @AfterEach
    void tearDown(PostgresqlStoreSetupExtension extension) throws InterruptedException {
        executor.shutdownNow();
        assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        extension.runQuery("DROP TABLE " + statements.getCredentialOffersTable() + " CASCADE");
        extension.runQuery("DROP TABLE " + leaseStatements.getLeaseTableName() + " CASCADE");
    }

    @Test
    void findByIdAndLease_whenOtherTransactionHoldsLease_shouldReturnItsChanges() throws Exception {
        var release = changeInOtherTransaction(offer -> offer.transition(PROCESSED));
        try {
            var result = executor.submit(() -> transactionContext.execute(() -> store.findByIdAndLease(OFFER_ID)));

            assertWaitsForLease(result);
            release.countDown();
            assertThat(result.get(30, TimeUnit.SECONDS)).isSucceeded()
                    .satisfies(offer -> assertThat(offer.getStateAsEnum()).isEqualTo(PROCESSED));
        } finally {
            release.countDown();
        }
    }

    @Test
    void nextNotLeased_whenOtherTransactionChangesEntityToNotMatch_shouldNotReturnIt() throws Exception {
        var release = changeInOtherTransaction(offer -> offer.transition(PROCESSED));
        try {
            var result = executor.submit(() -> transactionContext.execute(() -> store.nextNotLeased(10, hasState(RECEIVED.code()))));

            assertWaitsForLease(result);
            release.countDown();
            assertThat(result.get(30, TimeUnit.SECONDS)).isEmpty();
            // the lease is broken again, so that the offer is not blocked until the lease expires
            assertThat(leaseUtil.isLeased(OFFER_ID, RUNTIME_ID)).isFalse();
        } finally {
            release.countDown();
        }
    }

    @Test
    void nextNotLeased_whenOtherTransactionChangesEntity_shouldReturnItsChanges() throws Exception {
        var release = changeInOtherTransaction(offer -> offer.setErrorDetail("changed"));
        try {
            var result = executor.submit(() -> transactionContext.execute(() -> store.nextNotLeased(10, hasState(RECEIVED.code()))));

            assertWaitsForLease(result);
            release.countDown();
            assertThat(result.get(30, TimeUnit.SECONDS)).singleElement()
                    .satisfies(offer -> assertThat(offer.getErrorDetail()).isEqualTo("changed"));
            assertThat(leaseUtil.isLeased(OFFER_ID, RUNTIME_ID)).isTrue();
        } finally {
            release.countDown();
        }
    }

    /**
     * Stores an offer in state RECEIVED, then leases, changes and saves it in another transaction, which stays open until
     * the returned latch is counted down.
     */
    private CountDownLatch changeInOtherTransaction(Consumer<CredentialOffer> change) throws InterruptedException {
        transactionContext.execute(() -> otherStore.save(CredentialOffer.Builder.newInstance()
                .id(OFFER_ID)
                .state(RECEIVED.code())
                .participantContextId("test-participant")
                .issuer("did:web:issuer")
                .build()));

        var changed = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        executor.submit(() -> transactionContext.execute(() -> {
            var offer = otherStore.findByIdAndLease(OFFER_ID).getContent();
            change.accept(offer);
            otherStore.save(offer);
            changed.countDown();
            awaitUninterruptibly(release);
        }));
        assertThat(changed.await(30, TimeUnit.SECONDS)).isTrue();
        return release;
    }

    private void assertWaitsForLease(Future<?> result) {
        assertThatThrownBy(() -> result.get(1, TimeUnit.SECONDS)).isInstanceOf(TimeoutException.class);
    }

    private SqlCredentialOfferStore createStore(LocalDataSourceRegistry registry, String datasourceName, QueryExecutor queryExecutor, String leaseHolder) {
        var leaseContextBuilder = SqlLeaseContextBuilderImpl.with(transactionContext, leaseHolder, statements.getCredentialOffersTable(), leaseStatements, clock, queryExecutor);
        return new SqlCredentialOfferStore(registry, datasourceName, transactionContext, new JacksonTypeManager()::getMapper,
                queryExecutor, statements, leaseContextBuilder);
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
