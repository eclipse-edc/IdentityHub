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

package org.eclipse.edc.identityhub.transaction;

import org.eclipse.edc.transaction.spi.NoopTransactionContext;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Executes blocks like a {@link NoopTransactionContext}, and numbers the transactions, so that a test can tell whether
 * something happens in a transaction, whether two things happen in the same one, and whether a transaction would be rolled
 * back. Like a real transaction context, it tracks the transaction per thread, so that a test with several threads, e.g.
 * those of a state machine, can rely on it.
 */
public class TrackingTransactionContext extends NoopTransactionContext {
    private final AtomicInteger transactions = new AtomicInteger();
    private final ThreadLocal<Integer> depth = ThreadLocal.withInitial(() -> 0);
    private final ThreadLocal<Integer> current = ThreadLocal.withInitial(() -> 0);
    private final Set<Integer> rolledBack = ConcurrentHashMap.newKeySet();

    /**
     * The number of the transaction the calling thread is in, starting at 1, or 0 outside a transaction.
     */
    public int currentTransaction() {
        return depth.get() > 0 ? current.get() : 0;
    }

    /**
     * Whether an exception was thrown through any block of the given transaction, which rolls a real transaction back, even
     * if the exception is caught outside of the block.
     */
    public boolean isRolledBack(int transaction) {
        return rolledBack.contains(transaction);
    }

    @Override
    public void execute(TransactionBlock block) {
        track(() -> {
            super.execute(block);
            return null;
        });
    }

    @Override
    public <T> T execute(ResultTransactionBlock<T> block) {
        return track(() -> super.execute(block));
    }

    private <T> T track(Supplier<T> block) {
        // a nested block joins the surrounding transaction
        if (depth.get() == 0) {
            current.set(transactions.incrementAndGet());
        }
        depth.set(depth.get() + 1);
        try {
            return block.get();
        } catch (RuntimeException e) {
            rolledBack.add(current.get());
            throw e;
        } finally {
            depth.set(depth.get() - 1);
        }
    }
}
