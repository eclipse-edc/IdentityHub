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

import org.eclipse.edc.spi.retry.ExponentialWaitStrategy;
import org.eclipse.edc.spi.retry.WaitStrategy;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Doubles the delay with every failure, like the {@link ExponentialWaitStrategy}, but never exceeds a maximum delay. That
 * keeps the delay between two attempts reasonable after a long outage, and it cannot overflow.
 */
public class BoundedExponentialWaitStrategy implements WaitStrategy {
    private final long baseDelayMillis;
    private final long maxDelayMillis;
    private final AtomicInteger errorCount = new AtomicInteger();

    public BoundedExponentialWaitStrategy(long baseDelayMillis, long maxDelayMillis) {
        this.baseDelayMillis = baseDelayMillis;
        this.maxDelayMillis = maxDelayMillis;
    }

    @Override
    public long waitForMillis() {
        return baseDelayMillis;
    }

    @Override
    public void success() {
        errorCount.set(0);
    }

    @Override
    public void failures(int numberOfFailures) {
        errorCount.addAndGet(numberOfFailures);
    }

    /**
     * Returns the base delay times 2 to the power of the number of failures, but at most the maximum delay.
     */
    @Override
    public long retryInMillis() {
        var failures = errorCount.getAndIncrement();
        var delay = baseDelayMillis;
        // doubling stops at the maximum, so that the delay cannot overflow
        for (var i = 0; i < failures && delay < maxDelayMillis; i++) {
            delay *= 2;
        }
        return Math.min(delay, maxDelayMillis);
    }
}
