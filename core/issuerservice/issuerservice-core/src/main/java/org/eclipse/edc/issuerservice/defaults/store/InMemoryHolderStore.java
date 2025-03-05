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

package org.eclipse.edc.issuerservice.defaults.store;

import org.eclipse.edc.identityhub.store.InMemoryEntityStore;
import org.eclipse.edc.issuerservice.spi.holder.model.Holder;
import org.eclipse.edc.issuerservice.spi.holder.store.HolderStore;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QueryResolver;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.store.ReflectionBasedQueryResolver;

/**
 * Purely in-memory implementation of the {@link Holder} store.
 */
public class InMemoryHolderStore extends InMemoryEntityStore<Holder> implements HolderStore {
    @Override
    public StoreResult<Holder> findById(String id) {
        var q = QuerySpec.Builder.newInstance()
                .filter(new Criterion("holderId", "=", id))
                .build();
        var result = query(q);
        if (result.succeeded()) {
            return result.getContent().isEmpty()
                    ? StoreResult.notFound(notFoundErrorMessage(id))
                    : StoreResult.success(result.getContent().iterator().next());
        }
        return result.mapFailure();
    }

    @Override
    protected String getId(Holder newObject) {
        return newObject.getHolderId();
    }

    @Override
    protected QueryResolver<Holder> createQueryResolver() {
        return new ReflectionBasedQueryResolver<>(Holder.class, criterionOperatorRegistry);
    }
}
