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
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.query.Criterion.criterion;

class QueryPagesTest {

    private static final int PAGE_SIZE = 100;
    private final QuerySpec query = QuerySpec.Builder.newInstance()
            .filter(criterion("state", "=", 100))
            .build();

    @Test
    void forEachPage_shouldProcessAllEntities() {
        var store = new FakeStore(250);
        var processed = new ArrayList<String>();
        var pageSizes = new ArrayList<Integer>();

        QueryPages.forEachPage(query, PAGE_SIZE, store::fetch, Function.identity(), page -> {
            pageSizes.add(page.size());
            processed.addAll(page);
        });

        assertThat(processed).hasSize(250).doesNotHaveDuplicates();
        assertThat(pageSizes).containsExactly(100, 100, 50);
    }

    @Test
    void forEachPage_whenEntitiesStopMatching_shouldNotSkipOthers() {
        var store = new FakeStore(250);
        var processed = new ArrayList<String>();

        // e.g. the state of each processed entity changes, so the query does not match it anymore: with an offset, the
        // remaining entities would move to earlier pages, which have been processed already
        QueryPages.forEachPage(query, PAGE_SIZE, store::fetch, Function.identity(), page -> {
            processed.addAll(page);
            store.stopMatching(page);
        });

        assertThat(processed).hasSize(250).doesNotHaveDuplicates();
    }

    @Test
    void forEachPage_shouldContinueAfterLastIdOfPreviousPage() {
        var store = new FakeStore(150);

        QueryPages.forEachPage(query, PAGE_SIZE, store::fetch, Function.identity(), page -> { });

        assertThat(store.queries).hasSize(2);
        assertThat(store.queries).allSatisfy(pageQuery -> {
            assertThat(pageQuery.getFilterExpression()).contains(criterion("state", "=", 100));
            assertThat(pageQuery.getSortField()).isEqualTo("id");
            assertThat(pageQuery.getSortOrder()).isEqualTo(SortOrder.ASC);
            assertThat(pageQuery.getLimit()).isEqualTo(PAGE_SIZE);
        });
        assertThat(store.queries.get(0).getFilterExpression()).hasSize(1);
        assertThat(store.queries.get(1).getFilterExpression()).contains(criterion("id", ">", FakeStore.idOf(99)));
    }

    @Test
    void forEachPage_whenLastPageIsFull_shouldStopAtEmptyPage() {
        var store = new FakeStore(200);
        var pages = new ArrayList<Integer>();

        QueryPages.forEachPage(query, PAGE_SIZE, store::fetch, Function.identity(), page -> pages.add(page.size()));

        assertThat(pages).containsExactly(100, 100);
        assertThat(store.queries).hasSize(3);
    }

    @Test
    void forEachPage_whenNoEntities_shouldNotProcessAnything() {
        var store = new FakeStore(0);
        var pages = new ArrayList<Integer>();

        QueryPages.forEachPage(query, PAGE_SIZE, store::fetch, Function.identity(), page -> pages.add(page.size()));

        assertThat(pages).isEmpty();
        assertThat(store.queries).hasSize(1);
    }

    /**
     * Holds entity IDs that match the query until they are told to stop, and applies a page query to them like a store does.
     */
    private static class FakeStore {
        private final TreeSet<String> matching = new TreeSet<>();
        private final List<QuerySpec> queries = new ArrayList<>();

        FakeStore(int count) {
            IntStream.range(0, count).mapToObj(FakeStore::idOf).forEach(matching::add);
        }

        static String idOf(int index) {
            return "entity-%04d".formatted(index);
        }

        Collection<String> fetch(QuerySpec pageQuery) {
            queries.add(pageQuery);
            var after = pageQuery.getFilterExpression().stream()
                    .filter(criterion -> criterion.getOperandLeft().equals("id") && criterion.getOperator().equals(">"))
                    .map(Criterion::getOperandRight)
                    .map(Object::toString)
                    .findFirst();
            var candidates = after.map(id -> matching.tailSet(id, false)).orElse(matching);
            return candidates.stream().limit(pageQuery.getLimit()).toList();
        }

        void stopMatching(Collection<String> ids) {
            matching.removeAll(ids);
        }
    }
}
