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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class BoundedExponentialWaitStrategyTest {

    @ParameterizedTest(name = "{0} failures: {1} ms")
    @CsvSource({ "0, 1000", "1, 2000", "2, 4000", "8, 256000", "9, 300000", "10, 300000" })
    void retryInMillis_shouldDoubleUpToMaximum(int failures, long expectedDelay) {
        var strategy = new BoundedExponentialWaitStrategy(1000, 300_000);

        strategy.failures(failures);

        assertThat(strategy.retryInMillis()).isEqualTo(expectedDelay);
    }

    @Test
    void retryInMillis_whenManyFailures_shouldNotOverflow() {
        var strategy = new BoundedExponentialWaitStrategy(1000, 300_000);

        // an unbounded exponential delay would overflow, c.f. ExponentialWaitStrategy
        strategy.failures(1000);

        assertThat(strategy.retryInMillis()).isEqualTo(300_000);
    }

    @Test
    void retryInMillis_shouldCountItsOwnCalls() {
        var strategy = new BoundedExponentialWaitStrategy(1000, 300_000);

        assertThat(strategy.retryInMillis()).isEqualTo(1000);
        assertThat(strategy.retryInMillis()).isEqualTo(2000);
        assertThat(strategy.retryInMillis()).isEqualTo(4000);
    }

    @Test
    void success_shouldResetDelay() {
        var strategy = new BoundedExponentialWaitStrategy(1000, 300_000);
        strategy.failures(5);

        strategy.success();

        assertThat(strategy.retryInMillis()).isEqualTo(1000);
    }

    @Test
    void waitForMillis_shouldReturnBaseDelay() {
        assertThat(new BoundedExponentialWaitStrategy(1000, 300_000).waitForMillis()).isEqualTo(1000);
    }
}
