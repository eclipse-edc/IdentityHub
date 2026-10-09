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

import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;
import org.eclipse.edc.spi.retry.ExponentialWaitStrategy;
import org.eclipse.edc.statemachine.retry.EntityRetryProcessConfiguration;

/**
 * Configures a state machine with the same settings as the Connector's
 * {@link org.eclipse.edc.statemachine.StateMachineConfiguration}, plus a maximum delay between retries. The n-th retry of
 * an operation waits the base delay times 2 to the power of n, but at most the maximum delay.
 * <p>
 * By default, an operation is retried for about six hours: the retries are 2 s, 4 s, ..., 256 s apart, and 5 min after
 * that, so that an outage of the counterparty is bridged, and the next attempt is not far off once it ends.
 */
@Settings
public record IdentityHubStateMachineConfiguration(

        @Setting(
                description = "The iteration wait time in milliseconds in the state machine.",
                key = "state-machine.iteration-wait-millis",
                defaultValue = DEFAULT_ITERATION_WAIT + "")
        long iterationWaitMillis,

        @Setting(
                description = "The number of entities to be processed on every iteration.",
                key = "state-machine.batch-size",
                defaultValue = DEFAULT_BATCH_SIZE + "")
        int batchSize,

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
        long sendRetryMaxDelayMs
) {

    public static final long DEFAULT_ITERATION_WAIT = 1000;
    public static final int DEFAULT_BATCH_SIZE = 20;
    public static final int DEFAULT_SEND_RETRY_LIMIT = 80;
    public static final long DEFAULT_SEND_RETRY_BASE_DELAY = 1000;
    public static final long DEFAULT_SEND_RETRY_MAX_DELAY = 5 * 60 * 1000;

    public ExponentialWaitStrategy iterationWaitExponentialWaitStrategy() {
        return new ExponentialWaitStrategy(iterationWaitMillis);
    }

    public EntityRetryProcessConfiguration entityRetryProcessConfiguration() {
        return new EntityRetryProcessConfiguration(sendRetryLimit, () -> new BoundedExponentialWaitStrategy(sendRetryBaseDelayMs, sendRetryMaxDelayMs));
    }
}
