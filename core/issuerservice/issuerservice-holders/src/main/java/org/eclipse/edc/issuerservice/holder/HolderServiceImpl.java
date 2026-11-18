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
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import static org.eclipse.edc.spi.result.ServiceResult.from;
import static org.eclipse.edc.spi.result.ServiceResult.success;

public class HolderServiceImpl implements HolderService {
    private final TransactionContext transactionContext;
    private final HolderStore holderStore;

    public HolderServiceImpl(TransactionContext transactionContext, HolderStore holderStore) {
        this.transactionContext = transactionContext;
        this.holderStore = holderStore;
    }

    @Override
    public ServiceResult<Void> createHolder(Holder holder) {
        return transactionContext.execute(() -> checkDidAvailable(holder)
                .compose(v -> from(holderStore.create(holder))));
    }

    @Override
    public ServiceResult<Void> deleteHolder(String participantContextId, String holderId) {
        return transactionContext.execute(() -> from(holderStore.deleteById(participantContextId, holderId)));
    }

    @Override
    public ServiceResult<Void> updateHolder(Holder holder) {
        return transactionContext.execute(() -> checkDidAvailable(holder)
                .compose(v -> from(holderStore.update(holder))));
    }

    @Override
    public ServiceResult<Holder> patchHolder(String participantContextId, String holderId, String did, String name, Map<String, Object> properties, Collection<String> removedProperties) {
        return transactionContext.execute(() -> from(holderStore.findById(participantContextId, holderId))
                .compose(existingHolder -> {
                    if (!Objects.equals(existingHolder.getDid(), did)) {
                        return ServiceResult.badRequest("The DID '%s' does not match the DID of holder '%s'.".formatted(did, holderId));
                    }
                    if (!Objects.equals(existingHolder.getHolderName(), name)) {
                        return ServiceResult.badRequest("The name '%s' does not match the name of holder '%s'.".formatted(name, holderId));
                    }

                    //short-circuit if no changes
                    if (properties.isEmpty() && removedProperties.isEmpty()) {
                        return success(existingHolder);
                    }

                    existingHolder.updateProperties(properties, removedProperties);
                    return from(holderStore.update(existingHolder)).map(v -> existingHolder);
                }));
    }

    @Override
    public ServiceResult<Collection<Holder>> queryHolders(QuerySpec querySpec) {
        return transactionContext.execute(() -> from(holderStore.query(querySpec)));
    }

    @Override
    public ServiceResult<Holder> findById(String participantContextId, String holderId) {
        return transactionContext.execute(() -> from(holderStore.findById(participantContextId, holderId)));
    }

    /**
     * A participant context has at most one holder per DID, so that a DID identifies the holder it issues to.
     */
    private ServiceResult<Void> checkDidAvailable(Holder holder) {
        var query = QuerySpec.Builder.newInstance()
                .filter(new Criterion("participantContextId", "=", holder.getParticipantContextId()))
                .filter(new Criterion("did", "=", holder.getDid()))
                .build();
        return from(holderStore.query(query))
                .compose(holders -> holders.stream().anyMatch(existing -> !existing.getHolderId().equals(holder.getHolderId()))
                        ? ServiceResult.conflict("A Holder with DID '%s' already exists.".formatted(holder.getDid()))
                        : ServiceResult.success());
    }
}
