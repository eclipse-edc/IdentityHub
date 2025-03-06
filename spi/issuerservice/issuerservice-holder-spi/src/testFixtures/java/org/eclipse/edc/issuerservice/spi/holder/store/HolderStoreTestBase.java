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

package org.eclipse.edc.issuerservice.spi.holder.store;

import org.eclipse.edc.issuerservice.spi.holder.model.Holder;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

public abstract class HolderStoreTestBase {

    @Test
    void create() {
        var holder = createHolder();
        var result = getStore().create(holder);
        assertThat(result).isSucceeded();
        var query = getStore().query(QuerySpec.max());
        assertThat(query).isSucceeded();
        assertThat(query.getContent()).usingRecursiveFieldByFieldElementComparator().containsExactly(holder);
    }

    @Test
    void create_whenExists_shouldReturnFailure() {
        var holder = createHolder();
        var result = getStore().create(holder);
        assertThat(result).isSucceeded();
        var result2 = getStore().create(holder);

        assertThat(result2).isFailed().detail().contains("already exists");
    }

    @Test
    void query_byId() {
        range(0, 5)
                .mapToObj(i -> createHolder("p" + i, "did:web:" + i, "participant" + i))
                .forEach(getStore()::create);

        var q = QuerySpec.Builder.newInstance()
                .filter(new Criterion("holderId", "=", "p4"))
                .build();

        assertThat(getStore().query(q)).isSucceeded()
                .satisfies(str -> assertThat(str).hasSize(1));
    }

    @Test
    void query_byDid() {

        var holder = createHolder();
        getStore().create(holder);

        var q = QuerySpec.Builder.newInstance()
                .filter(new Criterion("did", "=", "did:web:participant"))
                .build();

        assertThat(getStore().query(q)).isSucceeded()
                .satisfies(str -> assertThat(str)
                        .hasSize(1)
                        .usingRecursiveFieldByFieldElementComparator()
                        .containsExactly(holder));
    }

    @Test
    void query_byParticipantContextId() {

        var holder = createHolder();
        getStore().create(holder);

        var q = QuerySpec.Builder.newInstance()
                .filter(new Criterion("participantContextId", "=", holder.getParticipantContextId()))
                .build();

        assertThat(getStore().query(q)).isSucceeded()
                .satisfies(str -> assertThat(str)
                        .hasSize(1)
                        .usingRecursiveFieldByFieldElementComparator()
                        .containsExactly(holder));
    }

    @Test
    void query_noQuerySpec() {
        var resources = range(0, 5)
                .mapToObj(i -> createHolder("p" + i, "did:web:" + i, "participant" + i))
                .toList();

        resources.forEach(getStore()::create);

        var res = getStore().query(QuerySpec.none());
        assertThat(res).isSucceeded();
        assertThat(res.getContent())
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(resources.toArray(new Holder[0]));
    }

    @Test
    void query_whenNotFound() {
        var resources = range(0, 5)
                .mapToObj(i -> createHolder("p" + i, "did:web:" + i, "participant" + i))
                .toList();

        resources.forEach(getStore()::create);

        var query = QuerySpec.Builder.newInstance()
                .filter(new Criterion("id", "=", "p99"))
                .build();
        var res = getStore().query(query);
        assertThat(res).isSucceeded();
        assertThat(res.getContent()).isEmpty();
    }

    @Test
    void query_byInvalidField_shouldReturnEmptyList() {
        var resources = range(0, 5)
                .mapToObj(i -> createHolder("p" + i, "did:web:" + i, "participant" + i))
                .toList();


        resources.forEach(getStore()::create);

        var query = QuerySpec.Builder.newInstance()
                .filter(new Criterion("invalidField", "=", "test-value"))
                .build();
        var res = getStore().query(query);
        assertThat(res).isSucceeded();
        assertThat(res.getContent()).isNotNull().isEmpty();
    }

    @Test
    void update() {
        var holder = createHolder();
        var result = getStore().create(holder);
        assertThat(result).isSucceeded();

        var updated = createHolder("p-id", "did:web:participant-changed", "participant-changed");
        var updateRes = getStore().update(updated);
        assertThat(updateRes).isSucceeded();
    }

    @Test
    void update_whenIdChanges_fails() {
        var holder = createHolder();
        getStore().create(holder);

        var updated = createHolder("p16", "did:web:participant", "participant14");
        assertThat(getStore().update(updated)).isFailed().detail().contains("with ID 'p16' does not exist.");
    }

    @Test
    void update_whenNotExists() {
        var holder = createHolder();

        var updateRes = getStore().update(holder);
        assertThat(updateRes).isFailed().detail().contains("with ID 'p-id' does not exist.");
    }

    @Test
    void delete() {
        var holder = createHolder();
        getStore().create(holder);

        var deleteRes = getStore().deleteById(holder.getHolderId());
        assertThat(deleteRes).isSucceeded();
    }

    @Test
    void delete_whenNotExists() {
        assertThat(getStore().deleteById("not-exist")).isFailed()
                .detail().contains("with ID 'not-exist' does not exist.");
    }

    protected abstract HolderStore getStore();

    private Holder createHolder() {
        return createHolder("p-id", "did:web:participant", "participant display name");
    }

    private Holder createHolder(String id, String did, String name) {
        return Holder.Builder.newInstance()
                .participantContextId(UUID.randomUUID().toString())
                .holderId(id)
                .did(did)
                .holderName(name)
                .build();
    }
}
