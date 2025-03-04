/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.issuerservice.holder;

import org.eclipse.edc.issuerservice.spi.holder.HolderService;
import org.eclipse.edc.issuerservice.spi.holder.model.Holder;
import org.eclipse.edc.issuerservice.spi.holder.store.HolderStore;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.Collection;

import static org.eclipse.edc.spi.result.ServiceResult.from;

public class HolderServiceImpl implements HolderService {
    private final TransactionContext transactionContext;
    private final HolderStore holderStore;

    public HolderServiceImpl(TransactionContext transactionContext, HolderStore holderStore) {
        this.transactionContext = transactionContext;
        this.holderStore = holderStore;
    }

    @Override
    public ServiceResult<Void> createHolder(Holder holder) {
        return transactionContext.execute(() -> from(holderStore.create(holder)));
    }

    @Override
    public ServiceResult<Void> deleteHolder(String holderId) {
        return transactionContext.execute(() -> from(holderStore.deleteById(holderId)));
    }

    @Override
    public ServiceResult<Void> updateHolder(Holder holder) {
        return transactionContext.execute(() -> from(holderStore.update(holder)));
    }

    @Override
    public ServiceResult<Collection<Holder>> queryHolders(QuerySpec querySpec) {
        return transactionContext.execute(() -> from(holderStore.query(querySpec)));
    }

    @Override
    public ServiceResult<Holder> findById(String holderId) {
        return transactionContext.execute(() -> from(holderStore.findById(holderId)));
    }
}
