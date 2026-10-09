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

import org.eclipse.edc.statemachine.retry.EntityRetryProcessConfiguration;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.identityhub.statemachine.IdentityHubStateMachineConfiguration.DEFAULT_BATCH_SIZE;
import static org.eclipse.edc.identityhub.statemachine.IdentityHubStateMachineConfiguration.DEFAULT_ITERATION_WAIT;
import static org.eclipse.edc.identityhub.statemachine.IdentityHubStateMachineConfiguration.DEFAULT_SEND_RETRY_BASE_DELAY;
import static org.eclipse.edc.identityhub.statemachine.IdentityHubStateMachineConfiguration.DEFAULT_SEND_RETRY_LIMIT;
import static org.eclipse.edc.identityhub.statemachine.IdentityHubStateMachineConfiguration.DEFAULT_SEND_RETRY_MAX_DELAY;

class IdentityHubStateMachineConfigurationTest {

    private final IdentityHubStateMachineConfiguration defaults = new IdentityHubStateMachineConfiguration(DEFAULT_ITERATION_WAIT,
            DEFAULT_BATCH_SIZE, DEFAULT_SEND_RETRY_LIMIT, DEFAULT_SEND_RETRY_BASE_DELAY, DEFAULT_SEND_RETRY_MAX_DELAY);

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
