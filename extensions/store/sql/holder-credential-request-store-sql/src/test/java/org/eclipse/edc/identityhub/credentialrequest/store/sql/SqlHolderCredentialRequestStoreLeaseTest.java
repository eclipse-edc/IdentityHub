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

package org.eclipse.edc.identityhub.credentialrequest.store.sql;

import org.eclipse.edc.identityhub.spi.credential.request.model.HolderCredentialRequest;
import org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState;
import org.eclipse.edc.identityhub.store.sql.credentialrequest.schema.HolderCredentialRequestStoreStatements;
import org.eclipse.edc.identityhub.store.sql.credentialrequest.schema.SqlHolderCredentialRequestStore;
import org.eclipse.edc.identityhub.store.sql.credentialrequest.schema.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
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

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState.ISSUED;
import static org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState.REQUESTED;
import static org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState.REQUESTING;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.persistence.StateEntityStore.hasState;

/**
 * Verifies that a leased {@link HolderCredentialRequest} contains the changes of another transaction that held its lease
 * until it completed, and that a runtime whose lease expired cannot overwrite the changes of the runtime that leased the
 * request after it.
 * <p>
 * This cannot use the transaction context of {@link PostgresqlStoreSetupExtension}: it auto-commits every statement, so a
 * lease would never be held by an open transaction. Each store has its own lease holder, like two replicas.
 */
@PostgresqlIntegrationTest
@ExtendWith(PostgresqlStoreSetupExtension.class)
class SqlHolderCredentialRequestStoreLeaseTest {

    private static final String REQUEST_ID = "request-id";
    private static final String RUNTIME_ID = "runtime-id";
    private final Clock clock = Clock.systemUTC();
    private final LeaseStatements leaseStatements = new BaseSqlLeaseStatements();
    private final HolderCredentialRequestStoreStatements statements = new PostgresDialectStatements(leaseStatements, clock);
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    private LocalTransactionContext transactionContext;
    private SqlHolderCredentialRequestStore store;
    private SqlHolderCredentialRequestStore otherStore;
    private SqlHolderCredentialRequestStore expiredLeaseStore;
    private LeaseUtil leaseUtil;

    @BeforeEach
    void setup(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) {
        transactionContext = new LocalTransactionContext(new ConsoleMonitor());
        var registry = new LocalDataSourceRegistry(transactionContext);
        // DefaultDataSourceRegistry.resolve() hands back the raw DataSource, which the local registry then enlists
        registry.register(extension.getDatasourceName(), extension.getDataSourceRegistry().resolve(extension.getDatasourceName()));

        store = createStore(registry, extension.getDatasourceName(), queryExecutor, RUNTIME_ID, clock);
        otherStore = createStore(registry, extension.getDatasourceName(), queryExecutor, "other-runtime-id", clock);
        // its leases start in the past, so they have expired as soon as they are acquired
        expiredLeaseStore = createStore(registry, extension.getDatasourceName(), queryExecutor, "expired-runtime-id", Clock.offset(clock, Duration.ofMinutes(-2)));
        leaseUtil = new LeaseUtil(transactionContext, extension::getConnection, statements.getHolderCredentialRequestTable(), leaseStatements, clock);

        extension.runQuery(TestUtils.getResourceFileContentAsString("holder-credential-request-schema.sql"));
    }

    @AfterEach
    void tearDown(PostgresqlStoreSetupExtension extension) throws InterruptedException {
        executor.shutdownNow();
        assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        extension.runQuery("DROP TABLE " + statements.getHolderCredentialRequestTable() + " CASCADE");
        extension.runQuery("DROP TABLE " + leaseStatements.getLeaseTableName() + " CASCADE");
    }

    @Test
    void save_outsideTransaction_shouldSucceed() {
        assertThat(store.save(createRequest(REQUESTED))).isSucceeded();

        var stored = transactionContext.execute(() -> store.findById(REQUEST_ID));
        assertThat(stored).isNotNull();
        assertThat(stored.stateAsEnum()).isEqualTo(REQUESTED);
    }

    @Test
    void findByIdAndLease_whenOtherTransactionHoldsLease_shouldReturnItsChanges() throws Exception {
        var release = changeInOtherTransaction(request -> request.transitionIssued("issuer-pid"));
        try {
            var result = executor.submit(() -> transactionContext.execute(() -> store.findByIdAndLease(REQUEST_ID)));

            assertWaitsForLease(result);
            release.countDown();
            assertThat(result.get(30, TimeUnit.SECONDS)).isSucceeded()
                    .satisfies(request -> assertThat(request.stateAsEnum()).isEqualTo(ISSUED));
        } finally {
            release.countDown();
        }
    }

    @Test
    void nextNotLeased_whenOtherTransactionChangesEntityToNotMatch_shouldNotReturnIt() throws Exception {
        var release = changeInOtherTransaction(request -> request.transitionIssued("issuer-pid"));
        try {
            var result = executor.submit(() -> transactionContext.execute(() -> store.nextNotLeased(10, hasState(REQUESTED.code()))));

            assertWaitsForLease(result);
            release.countDown();
            assertThat(result.get(30, TimeUnit.SECONDS)).isEmpty();
            // the lease is broken again, so that the request is not blocked until the lease expires
            assertThat(leaseUtil.isLeased(REQUEST_ID, RUNTIME_ID)).isFalse();
        } finally {
            release.countDown();
        }
    }

    @Test
    void nextNotLeased_whenOtherTransactionChangesEntity_shouldReturnItsChanges() throws Exception {
        var release = changeInOtherTransaction(request -> request.transitionRequested("new-issuer-pid"));
        try {
            var result = executor.submit(() -> transactionContext.execute(() -> store.nextNotLeased(10, hasState(REQUESTED.code()))));

            assertWaitsForLease(result);
            release.countDown();
            assertThat(result.get(30, TimeUnit.SECONDS)).singleElement()
                    .satisfies(request -> assertThat(request.getIssuerPid()).isEqualTo("new-issuer-pid"));
            assertThat(leaseUtil.isLeased(REQUEST_ID, RUNTIME_ID)).isTrue();
        } finally {
            release.countDown();
        }
    }

    @Test
    void save_whenLeaseExpiredAndOtherRuntimeSavedRequest_shouldFail() {
        transactionContext.execute(() -> otherStore.save(createRequest(REQUESTING)));
        var outdated = leaseNext(expiredLeaseStore);
        var current = leaseNext(otherStore);
        current.transitionRequested("issuer-pid");
        assertThat(transactionContext.execute(() -> otherStore.save(current))).isSucceeded();

        outdated.transitionRetry();

        assertThat(transactionContext.execute(() -> expiredLeaseStore.save(outdated))).isFailed()
                .extracting(StoreFailure::getReason)
                .isEqualTo(StoreFailure.Reason.GENERAL_ERROR);
        assertThat(transactionContext.execute(() -> store.findById(REQUEST_ID)).stateAsEnum()).isEqualTo(REQUESTED);
    }

    @Test
    void save_whenLeaseExpiredAndNobodyElseSavedRequest_shouldSucceed() {
        transactionContext.execute(() -> otherStore.save(createRequest(REQUESTING)));
        var request = leaseNext(expiredLeaseStore);

        request.transitionRequested("issuer-pid");

        assertThat(transactionContext.execute(() -> expiredLeaseStore.save(request))).isSucceeded();
        assertThat(transactionContext.execute(() -> store.findById(REQUEST_ID)).stateAsEnum()).isEqualTo(REQUESTED);
    }

    /**
     * Stores a request in state REQUESTED, then leases, changes and saves it in another transaction, which stays open
     * until the returned latch is counted down.
     */
    private CountDownLatch changeInOtherTransaction(Consumer<HolderCredentialRequest> change) throws InterruptedException {
        transactionContext.execute(() -> otherStore.save(createRequest(REQUESTED)));

        var changed = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        executor.submit(() -> transactionContext.execute(() -> {
            var request = otherStore.findByIdAndLease(REQUEST_ID).getContent();
            change.accept(request);
            otherStore.save(request);
            changed.countDown();
            awaitUninterruptibly(release);
        }));
        assertThat(changed.await(30, TimeUnit.SECONDS)).isTrue();
        return release;
    }

    private HolderCredentialRequest createRequest(HolderRequestState state) {
        return HolderCredentialRequest.Builder.newInstance()
                .id(REQUEST_ID)
                .state(state.code())
                // older than the state timestamp of any transition in the test, which would otherwise not change it
                .stateTimestamp(clock.millis() - 1000)
                .issuerPid("issuer-pid")
                .requestedCredential("test-credential-id", "TestCredential", "VC1_0_JWT")
                .participantContextId("test-participant")
                .issuerDid("did:web:testissuer")
                .build();
    }

    private HolderCredentialRequest leaseNext(SqlHolderCredentialRequestStore runtime) {
        var leased = transactionContext.execute(() -> runtime.nextNotLeased(10, hasState(REQUESTING.code())));
        assertThat(leased).hasSize(1);
        return leased.get(0);
    }

    private void assertWaitsForLease(Future<?> result) {
        assertThatThrownBy(() -> result.get(1, TimeUnit.SECONDS)).isInstanceOf(TimeoutException.class);
    }

    private SqlHolderCredentialRequestStore createStore(LocalDataSourceRegistry registry, String datasourceName, QueryExecutor queryExecutor, String leaseHolder, Clock leaseClock) {
        var leaseContextBuilder = SqlLeaseContextBuilderImpl.with(transactionContext, leaseHolder, statements.getHolderCredentialRequestTable(), leaseStatements, leaseClock, queryExecutor);
        return new SqlHolderCredentialRequestStore(registry, datasourceName, transactionContext, new JacksonTypeManager()::getMapper,
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
