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

import dev.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.eclipse.edc.http.client.EdcHttpClientImpl;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.retry.ExponentialWaitStrategy;
import org.eclipse.edc.statemachine.retry.EntityRetryProcessConfiguration;

import java.time.Duration;

/**
 * Configures a state machine with the same settings as the Connector's
 * {@link org.eclipse.edc.statemachine.StateMachineConfiguration}, plus a maximum delay between retries. The n-th retry of
 * an operation waits the base delay times 2 to the power of n, but at most the maximum delay.
 * <p>
 * By default, an operation is retried for about six hours: the retries are 2 s, 4 s, ..., 256 s apart, and 5 min after
 * that, so that an outage of the counterparty is bridged, and the next attempt is not far off once it ends.
 * <p>
 * The state machine leases a batch of entities at once, and processes them one by one. The lease must outlast the
 * processing of the whole batch, otherwise another runtime may lease and process the same entities in the meantime. By
 * default, a message is sent within 15 s, so a batch of 10 entities fits into the lease of 5 min.
 */
@Settings
public record IdentityHubStateMachineConfiguration(

        @Setting(
                description = "The iteration wait time in milliseconds in the state machine.",
                key = "state-machine.iteration-wait-millis",
                defaultValue = DEFAULT_ITERATION_WAIT + "")
        long iterationWaitMillis,

        @Setting(
                description = "The number of entities to be processed on every iteration. They are leased together, so the lease must outlast the processing of all of them.",
                key = "state-machine.batch-size",
                defaultValue = DEFAULT_BATCH_SIZE + "")
        int batchSize,

        @Setting(
                description = "How long the entities of a batch are leased, in milliseconds. It must be longer than the processing of a whole batch, " +
                        "i.e. longer than the batch size times the call timeout, otherwise another runtime may process the same entities.",
                key = "state-machine.lease-duration-millis",
                defaultValue = DEFAULT_LEASE_DURATION + "")
        long leaseDurationMillis,

        @Setting(
                description = "How many times a failed operation is retried before it fails with an error. With the default delays, the retries take about six hours.",
                key = "send.retry.limit",
                defaultValue = DEFAULT_SEND_RETRY_LIMIT + "")
        int sendRetryLimit,

        @Setting(
                description = "The base delay of retries in milliseconds. The n-th retry waits the base delay times 2 to the power of n, but at most the maximum delay.",
                key = "send.retry.base-delay.ms",
                defaultValue = DEFAULT_SEND_RETRY_BASE_DELAY + "")
        long sendRetryBaseDelayMs,

        @Setting(
                description = "The maximum delay between two retries in milliseconds.",
                key = "send.retry.max-delay.ms",
                defaultValue = DEFAULT_SEND_RETRY_MAX_DELAY + "")
        long sendRetryMaxDelayMs,

        @Setting(
                description = "How long it may take to connect to the counterparty when a message is sent, in milliseconds.",
                key = "send.connect-timeout.ms",
                defaultValue = DEFAULT_SEND_CONNECT_TIMEOUT + "")
        long sendConnectTimeoutMs,

        @Setting(
                description = "How long it may take to send a message and receive the response, in milliseconds. A message that times out is not retried right away, but by the state machine, like any other failure.",
                key = "send.call-timeout.ms",
                defaultValue = DEFAULT_SEND_CALL_TIMEOUT + "")
        long sendCallTimeoutMs
) {

    public static final long DEFAULT_ITERATION_WAIT = 1000;
    public static final int DEFAULT_BATCH_SIZE = 10;
    public static final long DEFAULT_LEASE_DURATION = 5 * 60 * 1000;
    public static final int DEFAULT_SEND_RETRY_LIMIT = 80;
    public static final long DEFAULT_SEND_RETRY_BASE_DELAY = 1000;
    public static final long DEFAULT_SEND_RETRY_MAX_DELAY = 5 * 60 * 1000;
    public static final long DEFAULT_SEND_CONNECT_TIMEOUT = 5 * 1000;
    public static final long DEFAULT_SEND_CALL_TIMEOUT = 15 * 1000;

    public ExponentialWaitStrategy iterationWaitExponentialWaitStrategy() {
        return new ExponentialWaitStrategy(iterationWaitMillis);
    }

    public EntityRetryProcessConfiguration entityRetryProcessConfiguration() {
        return new EntityRetryProcessConfiguration(sendRetryLimit, () -> new BoundedExponentialWaitStrategy(sendRetryBaseDelayMs, sendRetryMaxDelayMs));
    }

    /**
     * How long the entities of a batch are leased.
     */
    public Duration leaseDuration() {
        return Duration.ofMillis(leaseDurationMillis);
    }

    /**
     * An HTTP client for the messages that the state machine sends while it holds the lease. It does not retry a failed
     * message, as the state machine retries the operation anyway, and it gives up on a message after the call timeout,
     * so that a batch fits into the lease.
     *
     * @param okHttpClient the runtime's HTTP client, whose other settings are kept
     * @param monitor      the monitor of the HTTP client
     * @return the HTTP client
     */
    public EdcHttpClient createNoRetryClient(OkHttpClient okHttpClient, Monitor monitor) {
        var client = okHttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(sendConnectTimeoutMs))
                .callTimeout(Duration.ofMillis(sendCallTimeoutMs))
                .build();
        return new EdcHttpClientImpl(client, RetryPolicy.<Response>builder().withMaxRetries(0).build(), monitor);
    }

    /**
     * Logs a warning if a batch may take longer to process than its lease lasts, i.e. if the batch size times the call
     * timeout is not less than the lease duration.
     *
     * @param monitor      the monitor to log the warning to
     * @param stateMachine the name of the state machine, for the warning
     */
    public void warnIfBatchMayOutlastLease(Monitor monitor, String stateMachine) {
        var batchDuration = Duration.ofMillis(sendCallTimeoutMs).multipliedBy(batchSize);
        if (batchDuration.compareTo(leaseDuration()) >= 0) {
            monitor.warning(("The %s state machine may take %d s to process a batch of %d entities, but leases them for only %d s. " +
                    "Another runtime may then process the same entities, so raise the lease duration, or lower the batch size or the call timeout")
                    .formatted(stateMachine, batchDuration.toSeconds(), batchSize, leaseDuration().toSeconds()));
        }
    }
}
