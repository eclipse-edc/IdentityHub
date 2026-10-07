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

package org.eclipse.edc.identityhub.store;

import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Goes through all entities that match a query, page by page. Without a limit of its own, a query only returns the first
 * page of the default size, so all entities beyond that would never be looked at.
 * <p>
 * The pages are ordered by entity ID, and each one starts after the last ID of the previous page, rather than at an
 * offset: an entity that stops matching the query while the pages are processed, e.g. because its state changes, would
 * otherwise shift the following entities to an earlier page, and they would be skipped.
 */
public final class QueryPages {

    private static final String ID = "id";

    private QueryPages() {
    }

    /**
     * Processes all entities that match the query, one page at a time.
     *
     * @param query    the query, whose filter applies to every page. Its sort order, offset and limit are ignored.
     * @param pageSize the number of entities per page
     * @param fetch    fetches the entities of one page
     * @param idOf     the ID of an entity
     * @param process  processes the entities of one page
     * @param <T>      the type of the entities
     */
    public static <T> void forEachPage(QuerySpec query, int pageSize, Function<QuerySpec, Collection<T>> fetch,
                                       Function<T, String> idOf, Consumer<Collection<T>> process) {
        String lastId = null;
        while (true) {
            var page = fetch.apply(pageAfter(query, lastId, pageSize));
            if (page.isEmpty()) {
                return;
            }
            process.accept(page);
            if (page.size() < pageSize) {
                return;
            }
            for (var entity : page) {
                lastId = idOf.apply(entity);
            }
        }
    }

    private static QuerySpec pageAfter(QuerySpec query, @Nullable String lastId, int pageSize) {
        var filter = new ArrayList<>(query.getFilterExpression());
        if (lastId != null) {
            filter.add(Criterion.criterion(ID, ">", lastId));
        }
        return QuerySpec.Builder.newInstance()
                .filter(filter)
                .sortField(ID)
                .sortOrder(SortOrder.ASC)
                .limit(pageSize)
                .build();
    }
}
