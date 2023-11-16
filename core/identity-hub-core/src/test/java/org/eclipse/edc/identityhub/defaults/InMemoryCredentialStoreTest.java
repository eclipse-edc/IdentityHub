/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.defaults;

import org.assertj.core.api.Assertions;
import org.eclipse.edc.identityhub.spi.store.model.VcState;
import org.eclipse.edc.identityhub.spi.store.model.VerifiableCredentialResource;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.junit.jupiter.api.Test;

import static java.util.stream.IntStream.range;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

class InMemoryCredentialStoreTest {

    private final InMemoryCredentialStore store = new InMemoryCredentialStore();

    @Test
    void create() {
        var result = store.create(createCredential());
        assertThat(result).isSucceeded();

    }

    @Test
    void create_whenExists_shouldReturnFailure() {
        var credential = createCredential();
        var result = store.create(credential);
        assertThat(result).isSucceeded();
        var result2 = store.create(credential);

        assertThat(result2).isFailed().detail().contains("already exists");
    }

    @Test
    void query() {
        range(0, 5)
                .mapToObj(i -> createCredentialBuilder().id("id" + i).build())
                .forEach(store::create);

    }

    @Test
    void query_noQuerySpec() {
        var resources = range(0, 5)
                .mapToObj(i -> createCredentialBuilder().id("id" + i).build())
                .toList();

        resources.forEach(store::create);

        var res = store.query(QuerySpec.max());
        assertThat(res).isSucceeded();
        Assertions.assertThat(res.getContent()).hasSize(5).containsAll(resources);
    }


    @Test
    void query_whenNotFound() {
        var resources = range(0, 5)
                .mapToObj(i -> createCredentialBuilder()
                        .id("id" + i)
                        .build())
                .toList();

        resources.forEach(store::create);

        var query = QuerySpec.Builder.newInstance()
                .filter(new Criterion("holderId", "=", "some-holder"))
                .build();
        var res = store.query(query);
        assertThat(res).isSucceeded();
        Assertions.assertThat(res.getContent()).isEmpty();
    }

    @Test
    void query_byInvalidField_shouldReturnEmptyList() {
        var resources = range(0, 5)
                .mapToObj(i -> createCredentialBuilder()
                        .id("id" + i)
                        .build())
                .toList();

        resources.forEach(store::create);

        var query = QuerySpec.Builder.newInstance()
                .filter(new Criterion("invalidField", "=", "test-value"))
                .build();
        var res = store.query(query);
        assertThat(res).isSucceeded();
        Assertions.assertThat(res.getContent()).isNotNull().isEmpty();
    }

    @Test
    void update() {
        var credential = createCredentialBuilder();
        var result = store.create(credential.build());
        assertThat(result).isSucceeded();

        var updateRes = store.update(credential.state(VcState.ISSUED).build());
        assertThat(updateRes).isSucceeded();
    }

    @Test
    void update_whenIdChanges_fails() {
        var credential = createCredentialBuilder();
        var result = store.create(credential.build());

        var updateRes = store.update(credential.state(VcState.ISSUED).id("another-id").build());
        assertThat(updateRes).isFailed().detail().contains("with ID another-id was not found");
    }

    @Test
    void update_whenNotExists() {
        var credential = createCredentialBuilder();
        var updateRes = store.update(credential.state(VcState.ISSUED).id("another-id").build());
        assertThat(updateRes).isFailed().detail().contains("with ID another-id was not found");
    }

    @Test
    void delete() {
        var credential = createCredential();
        store.create(credential);

        var deleteRes = store.delete(credential.getId());
        assertThat(deleteRes).isSucceeded();
    }

    @Test
    void delete_whenNotExists() {
        assertThat(store.delete("not-exist")).isFailed()
                .detail().contains("with ID not-exist was not found");
    }

    private VerifiableCredentialResource createCredential() {
        return createCredentialBuilder().build();
    }

    private VerifiableCredentialResource.Builder createCredentialBuilder() {
        return VerifiableCredentialResource.Builder.newInstance()
                .issuerId("test-issuer")
                .holderId("test-holder")
                .id("test-id");
    }
}