/*
 *  Copyright (c) 2026 Metaform Systems Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.statemachine;

import com.sun.net.httpserver.HttpServer;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.statemachine.retry.EntityRetryProcessConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.identityhub.statemachine.IdentityHubStateMachineConfiguration.DEFAULT_BATCH_SIZE;
import static org.eclipse.edc.identityhub.statemachine.IdentityHubStateMachineConfiguration.DEFAULT_ITERATION_WAIT;
import static org.eclipse.edc.identityhub.statemachine.IdentityHubStateMachineConfiguration.DEFAULT_LEASE_DURATION;
import static org.eclipse.edc.identityhub.statemachine.IdentityHubStateMachineConfiguration.DEFAULT_SEND_CALL_TIMEOUT;
import static org.eclipse.edc.identityhub.statemachine.IdentityHubStateMachineConfiguration.DEFAULT_SEND_CONNECT_TIMEOUT;
import static org.eclipse.edc.identityhub.statemachine.IdentityHubStateMachineConfiguration.DEFAULT_SEND_RETRY_BASE_DELAY;
import static org.eclipse.edc.identityhub.statemachine.IdentityHubStateMachineConfiguration.DEFAULT_SEND_RETRY_LIMIT;
import static org.eclipse.edc.identityhub.statemachine.IdentityHubStateMachineConfiguration.DEFAULT_SEND_RETRY_MAX_DELAY;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class IdentityHubStateMachineConfigurationTest {

    private final IdentityHubStateMachineConfiguration defaults = configuration(DEFAULT_BATCH_SIZE, DEFAULT_LEASE_DURATION, DEFAULT_SEND_CALL_TIMEOUT);
    private final Monitor monitor = mock();

    private static IdentityHubStateMachineConfiguration configuration(int batchSize, long leaseDurationMillis, long sendCallTimeoutMs) {
        return new IdentityHubStateMachineConfiguration(DEFAULT_ITERATION_WAIT, batchSize, leaseDurationMillis, DEFAULT_SEND_RETRY_LIMIT,
                DEFAULT_SEND_RETRY_BASE_DELAY, DEFAULT_SEND_RETRY_MAX_DELAY, DEFAULT_SEND_CONNECT_TIMEOUT, sendCallTimeoutMs);
    }

    @Test
    void entityRetryProcessConfiguration_withDefaults_shouldRetryForAboutSixHours() {
        var configuration = defaults.entityRetryProcessConfiguration();

        assertThat(totalDelay(configuration)).isBetween(Duration.ofMinutes(355), Duration.ofMinutes(375));
    }

    @Test
    void entityRetryProcessConfiguration_shouldNotWaitLongerThanMaximum() {
        var configuration = defaults.entityRetryProcessConfiguration();

        assertThat(IntStream.rangeClosed(1, configuration.retryLimit()).mapToLong(retry -> delayMillis(configuration, retry)))
                .allMatch(delay -> delay <= DEFAULT_SEND_RETRY_MAX_DELAY)
                .contains(DEFAULT_SEND_RETRY_MAX_DELAY);
    }

    @Test
    void entityRetryProcessConfiguration_shouldSupplyIndependentStrategies() {
        var configuration = defaults.entityRetryProcessConfiguration();

        // the strategies count failures, so that each entity needs its own
        assertThat(configuration.delayStrategySupplier().get()).isNotSameAs(configuration.delayStrategySupplier().get());
    }

    @Test
    void defaults_shouldProcessBatchWithinLease() {
        assertThat(Duration.ofMillis(DEFAULT_SEND_CALL_TIMEOUT).multipliedBy(DEFAULT_BATCH_SIZE)).isLessThan(Duration.ofMillis(DEFAULT_LEASE_DURATION));
    }

    @Test
    void warnIfBatchMayOutlastLease_withDefaults_shouldNotWarn() {
        defaults.warnIfBatchMayOutlastLease(monitor, "test");

        verifyNoInteractions(monitor);
    }

    @Test
    void warnIfBatchMayOutlastLease_whenBatchMayOutlastLease_shouldWarn() {
        // 20 messages of up to 15 s each take as long as the lease lasts
        configuration(20, 300_000, 15_000).warnIfBatchMayOutlastLease(monitor, "test");

        verify(monitor).warning(contains("lease"));
    }

    @Test
    void warnIfBatchMayOutlastLease_whenLeaseIsNotPositive_shouldWarn() {
        configuration(DEFAULT_BATCH_SIZE, 0, DEFAULT_SEND_CALL_TIMEOUT).warnIfBatchMayOutlastLease(monitor, "test");

        verify(monitor).warning(contains("lease"));
    }

    @Test
    void createNoRetryClient_whenNoResponseWithinCallTimeout_shouldFailWithoutRetry() throws IOException {
        var requests = new AtomicInteger();
        var server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            requests.incrementAndGet();
            try {
                Thread.sleep(5_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        try {
            var client = configuration(DEFAULT_BATCH_SIZE, DEFAULT_LEASE_DURATION, 500).createNoRetryClient(new OkHttpClient(), monitor);
            var request = new Request.Builder().url("http://localhost:%d/".formatted(server.getAddress().getPort())).build();
            var start = System.nanoTime();

            assertThatThrownBy(() -> client.execute(request)).isInstanceOf(IOException.class);

            assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(3));
            // the state machine retries the operation, so the HTTP client does not
            assertThat(requests).hasValue(1);
        } finally {
            server.stop(0);
        }
    }

    private Duration totalDelay(EntityRetryProcessConfiguration configuration) {
        return Duration.ofMillis(IntStream.rangeClosed(1, configuration.retryLimit()).mapToLong(retry -> delayMillis(configuration, retry)).sum());
    }

    /**
     * The delay before the given retry, computed like the Connector's {@code ProcessorImpl} does.
     */
    private long delayMillis(EntityRetryProcessConfiguration configuration, int retry) {
        var strategy = configuration.delayStrategySupplier().get();
        strategy.failures(retry);
        return strategy.retryInMillis();
    }
}
