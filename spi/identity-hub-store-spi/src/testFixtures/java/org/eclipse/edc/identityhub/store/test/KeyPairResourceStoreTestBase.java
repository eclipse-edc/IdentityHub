/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.store.test;

import org.assertj.core.api.Assertions;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairResource;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairState;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantResource;
import org.eclipse.edc.identityhub.spi.store.KeyPairResourceStore;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static java.util.stream.IntStream.range;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

public abstract class KeyPairResourceStoreTestBase {
    @Test
    void create() {
        var result = getStore().create(createKeyPairResource().build());
        assertThat(result).isSucceeded();
    }

    @Test
    void create_whenExists_shouldReturnFailure() {
        var keyPairResource = createKeyPairResource().build();
        var result = getStore().create(keyPairResource);
        assertThat(result).isSucceeded();
        var result2 = getStore().create(keyPairResource);

        assertThat(result2).isFailed().detail().contains("already exists");
    }

    @Test
    void query_byId() {
        range(0, 5)
                .mapToObj(i -> createKeyPairResource().id("id" + i).build())
                .forEach(getStore()::create);

        var query = QuerySpec.Builder.newInstance()
                .filter(new Criterion("id", "=", "id2"))
                .build();

        assertThat(getStore().query(query)).isSucceeded()
                .satisfies(str -> Assertions.assertThat(str).hasSize(1));
    }

    @Test
    void query_byProperty() {
        var keyPairResource = createKeyPairResource().state(KeyPairState.CREATED).build();
        getStore().create(keyPairResource);

        var query = QuerySpec.Builder.newInstance()
                .filter(new Criterion("state", "=", KeyPairState.CREATED.code()))
                .build();

        assertThat(getStore().query(query)).isSucceeded()
                .satisfies(str -> Assertions.assertThat(str)
                        .hasSize(1)
                        .usingRecursiveFieldByFieldElementComparator()
                        .containsExactly(keyPairResource));
    }

    @Test
    void query_noQuerySpec() {
        var resources = range(0, 5)
                .mapToObj(i -> createKeyPairResource().participantId("id" + i).build())
                .toList();

        resources.forEach(getStore()::create);

        var res = getStore().query(QuerySpec.none());
        assertThat(res).isSucceeded();
        Assertions.assertThat(res.getContent())
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(resources.toArray(new KeyPairResource[0]));
    }

    @Test
    void query_whenNotFound() {
        var resources = range(0, 5)
                .mapToObj(i -> createKeyPairResource()
                        .participantId("id" + i)
                        .build())
                .toList();

        resources.forEach(getStore()::create);

        var query = ParticipantResource.queryByParticipantId("id7")
                .build();
        var res = getStore().query(query);
        assertThat(res).isSucceeded();
        Assertions.assertThat(res.getContent()).isEmpty();
    }

    @Test
    void query_byInvalidField_shouldReturnEmptyList() {
        var resources = range(0, 5)
                .mapToObj(i -> createKeyPairResource()
                        .participantId("id" + i)
                        .build())
                .toList();

        resources.forEach(getStore()::create);

        var query = QuerySpec.Builder.newInstance()
                .filter(new Criterion("invalidField", "=", "test-value"))
                .build();
        var res = getStore().query(query);
        assertThat(res).isSucceeded();
        Assertions.assertThat(res.getContent()).isNotNull().isEmpty();
    }

    @Test
    void query_byIdAndState() {
        var kp1 = createKeyPairResource().id("id1").state(KeyPairState.ACTIVATED).build();
        var kp2 = createKeyPairResource().id("id2").state(KeyPairState.CREATED).build();
        var kp3 = createKeyPairResource().id("id3").state(KeyPairState.REVOKED).build();
        var kp4 = createKeyPairResource().id("id4").state(KeyPairState.ROTATED).build();

        List.of(kp1, kp2, kp3, kp4).forEach(getStore()::create);

        var query = QuerySpec.Builder.newInstance()
                .filter(new Criterion("id", "=", "id3"))
                .filter(new Criterion("state", "=", KeyPairState.REVOKED.code()))
                .build();

        assertThat(getStore().query(query))
                .isSucceeded()
                .satisfies(keyPairResources -> Assertions.assertThat(keyPairResources)
                        .usingRecursiveFieldByFieldElementComparator()
                        .containsExactly(kp3));
    }

    @Test
    void update() {
        var keyPairResource = createKeyPairResource();
        var result = getStore().create(keyPairResource.build());
        assertThat(result).isSucceeded();

        var updateRes = getStore().update(keyPairResource.state(KeyPairState.REVOKED).build());
        assertThat(updateRes).isSucceeded();
    }

    @Test
    void update_whenIdChanges_fails() {
        var keyPairResource = createKeyPairResource();
        getStore().create(keyPairResource.build());

        var updateRes = getStore().update(keyPairResource.state(KeyPairState.ROTATED).id("another-id").build());
        assertThat(updateRes).isFailed().detail().contains("with ID 'another-id' does not exist.");
    }

    @Test
    void update_whenNotExists() {
        var context = createKeyPairResource();
        var updateRes = getStore().update(context.state(KeyPairState.ROTATED).participantId("another-id").build());
        assertThat(updateRes).isFailed().detail().matches(".* with ID .* does not exist.");
    }

    @Test
    void delete() {
        var keyPairResource = createKeyPairResource().build();
        getStore().create(keyPairResource);

        var deleteRes = getStore().deleteById(keyPairResource.getId());
        assertThat(deleteRes).isSucceeded();
    }

    @Test
    void delete_whenNotExists() {
        assertThat(getStore().deleteById("not-exist")).isFailed()
                .detail().contains("with ID 'not-exist' does not exist.");
    }

    protected abstract KeyPairResourceStore getStore();

    private KeyPairResource.Builder createKeyPairResource() {
        return KeyPairResource.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .keyId("test-key-1")
                .privateKeyAlias("private-key-alias")
                .participantId("test-participant")
                .serializedPublicKey("this-is-a-pem-string")
                .keyContext("JsonWebKey2020")
                .useDuration(Duration.ofDays(6).toMillis());
    }
}
