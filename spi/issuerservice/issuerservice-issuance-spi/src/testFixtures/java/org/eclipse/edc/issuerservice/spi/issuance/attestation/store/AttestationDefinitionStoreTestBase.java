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

package org.eclipse.edc.issuerservice.spi.issuance.attestation.store;

import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionStore;
import org.eclipse.edc.issuerservice.spi.issuance.model.AttestationDefinition;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreFailure;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

public abstract class AttestationDefinitionStoreTestBase {

    @Test
    void create() {
        var res = getStore().create(new AttestationDefinition("test-id", "test-type", Map.of("foo", "bar")));
        assertThat(res).isSucceeded();
    }

    @Test
    void create_whenExists_expectFailure() {
        getStore().create(new AttestationDefinition("test-id", "test-type", Map.of("foo", "bar")));
        var res = getStore().create(new AttestationDefinition("test-id", "test-type", Map.of("foo", "bar")));
        assertThat(res).isFailed().satisfies(f -> assertThat(f.getReason()).isEqualTo(StoreFailure.Reason.ALREADY_EXISTS));
    }

    @Test
    void resolveDefinition() {
        var att = new AttestationDefinition("test-id", "test-type", Map.of("foo", "bar"));
        getStore().create(att);

        assertThat(getStore().resolveDefinition("test-id")).usingRecursiveComparison().isEqualTo(att);
    }

    @Test
    void resolveDefinition_whenNotExists() {
        assertThat(getStore().resolveDefinition("test-id")).isNull();
    }

    @Test
    void update() {
        var att = new AttestationDefinition("test-id", "test-type", Map.of("foo", "bar"));
        getStore().create(att);

        var updatedAtt = new AttestationDefinition("test-id", "test-type", Map.of("bar", "baz"));
        assertThat(getStore().update(updatedAtt)).isSucceeded();

        var dbAtt = getStore().resolveDefinition("test-id");
        assertThat(dbAtt).isNotNull()
                .extracting(AttestationDefinition::configuration)
                .satisfies(config -> assertThat(config).hasSize(1).containsEntry("bar", "baz"));
    }

    @Test
    void update_whenNotExists_expectFailure() {
        var updatedAtt = new AttestationDefinition("test-id", "test-type", Map.of("bar", "baz"));
        assertThat(getStore().update(updatedAtt)).isFailed()
                .detail().contains("does not exist.");
    }

    @Test
    void deleteById() {
        var att = new AttestationDefinition("test-id", "test-type", Map.of("foo", "bar"));
        getStore().create(att);

        assertThat(getStore().deleteById("test-id")).isSucceeded();
        assertThat(getStore().query(QuerySpec.max())).isSucceeded().satisfies(list -> assertThat(list).isEmpty());
    }

    @Test
    void deleteById_whenNotExists() {
        assertThat(getStore().deleteById("test-id")).isFailed().detail().contains("does not exist.");
    }

    @Test
    void query_byId() {
        var att = new AttestationDefinition("test-id", "test-type", Map.of("foo", "bar"));
        getStore().create(att);

        var query = QuerySpec.Builder.newInstance().filter(new Criterion("id", "=", "test-id")).build();

        assertThat(getStore().query(query)).isSucceeded()
                .satisfies(list -> assertThat(list)
                        .hasSize(1)
                        .usingRecursiveFieldByFieldElementComparator()
                        .containsExactly(att));
    }

    @Test
    void query_byType() {
        var att = new AttestationDefinition("test-id", "test-type", Map.of("foo", "bar"));
        getStore().create(att);

        var query = QuerySpec.Builder.newInstance().filter(new Criterion("attestationType", "=", "test-type")).build();

        assertThat(getStore().query(query)).isSucceeded()
                .satisfies(list -> assertThat(list)
                        .hasSize(1)
                        .usingRecursiveFieldByFieldElementComparator()
                        .containsExactly(att));
    }

    @Test
    void query_byConfiguration() {
        var att = new AttestationDefinition("test-id", "test-type", Map.of("foo", "bar"));
        getStore().create(att);

        var query = QuerySpec.Builder.newInstance().filter(new Criterion("configuration.foo", "=", "bar")).build();

        assertThat(getStore().query(query)).isSucceeded()
                .satisfies(list -> assertThat(list)
                        .hasSize(1)
                        .usingRecursiveFieldByFieldElementComparator()
                        .containsExactly(att));
    }

    @Test
    void query_byConfiguration_whenInvalidField_expectFailure() {
        var att = new AttestationDefinition("test-id", "test-type", Map.of("foo", "bar"));
        getStore().create(att);

        var query = QuerySpec.Builder.newInstance().filter(new Criterion("configuration.notexist", "=", "notexist")).build();

        assertThat(getStore().query(query)).isSucceeded()
                .satisfies(list -> assertThat(list).isEmpty());
    }


    protected abstract AttestationDefinitionStore getStore();
}
