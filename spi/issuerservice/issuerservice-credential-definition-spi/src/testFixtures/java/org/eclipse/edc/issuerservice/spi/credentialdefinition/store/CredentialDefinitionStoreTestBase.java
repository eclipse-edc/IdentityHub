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

package org.eclipse.edc.issuerservice.spi.credentialdefinition.store;

import org.assertj.core.api.Assertions;
import org.eclipse.edc.identityhub.spi.issuance.credentials.model.CredentialDefinition;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static java.util.stream.IntStream.range;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

public abstract class CredentialDefinitionStoreTestBase {

    @Test
    void create() {
        var credentialDefinition = createCredentialDefinition();
        var result = getStore().create(credentialDefinition);
        assertThat(result).isSucceeded();
        var query = getStore().query(QuerySpec.max());
        assertThat(query).isSucceeded();
        Assertions.assertThat(query.getContent()).usingRecursiveFieldByFieldElementComparator().containsExactly(credentialDefinition);
    }

    @Test
    void create_whenExists_shouldReturnFailure() {
        var credentialDefinition = createCredentialDefinition();
        var result = getStore().create(credentialDefinition);
        assertThat(result).isSucceeded();
        var result2 = getStore().create(credentialDefinition);

        assertThat(result2).isFailed().detail().contains("already exists");
    }

    @Test
    void create_whenTypeExists_shouldReturnFailure() {
        var credentialDefinition = createCredentialDefinition();

        var result = getStore().create(credentialDefinition);

        var newCredentialDefinition = createCredentialDefinition(UUID.randomUUID().toString(), credentialDefinition.getCredentialType());
        assertThat(result).isSucceeded();
        var result2 = getStore().create(newCredentialDefinition);

        assertThat(result2).isFailed().detail().contains("already exists");
    }

    @Test
    void query_byId() {
        range(0, 5)
                .mapToObj(i -> createCredentialDefinition("id" + i, "Membership" + i))
                .forEach(getStore()::create);

        var q = QuerySpec.Builder.newInstance()
                .filter(new Criterion("credentialType", "=", "Membership4"))
                .build();

        assertThat(getStore().query(q)).isSucceeded()
                .satisfies(str -> Assertions.assertThat(str).hasSize(1));
    }


    @Test
    void query_noQuerySpec() {
        var resources = range(0, 5)
                .mapToObj(i -> createCredentialDefinition("id" + i, "Membership" + i))
                .toList();

        resources.forEach(getStore()::create);

        var res = getStore().query(QuerySpec.none());
        assertThat(res).isSucceeded();
        Assertions.assertThat(res.getContent())
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(resources.toArray(new CredentialDefinition[0]));
    }

    @Test
    void query_whenNotFound() {
        var resources = range(0, 5)
                .mapToObj(i -> createCredentialDefinition("id" + i, "Membership" + i))
                .toList();

        resources.forEach(getStore()::create);

        var query = QuerySpec.Builder.newInstance()
                .filter(new Criterion("credentialType", "=", "Membership99"))
                .build();
        var res = getStore().query(query);
        assertThat(res).isSucceeded();
        Assertions.assertThat(res.getContent()).isEmpty();
    }

    @Test
    void query_byInvalidField_shouldReturnEmptyList() {
        var resources = range(0, 5)
                .mapToObj(i -> createCredentialDefinition("id" + i, "Membership" + i))
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
    void update() {
        var credentialDefinition = createCredentialDefinition();
        var result = getStore().create(credentialDefinition);
        assertThat(result).isSucceeded();

        var updated = createCredentialDefinition(credentialDefinition.getId(), credentialDefinition.getCredentialType());
        var updateRes = getStore().update(updated);
        assertThat(updateRes).isSucceeded();
    }

    @Test
    void update_whenNotExists() {
        var credentialDefinition = createCredentialDefinition();

        var updateRes = getStore().update(credentialDefinition);
        assertThat(updateRes).isFailed().detail().contains("ID '%s' does not exist.".formatted(credentialDefinition.getId()));
    }

    @Test
    void update_whenTypeExists_fails() {
        var credentialDefinition = createCredentialDefinition();
        var credentialDefinition1 = createCredentialDefinition(UUID.randomUUID().toString(), "Membership1");
        var result = getStore().create(credentialDefinition);
        var result1 = getStore().create(credentialDefinition1);
        assertThat(result).isSucceeded();
        assertThat(result1).isSucceeded();

        credentialDefinition = createCredentialDefinition(credentialDefinition.getId(), "Membership1");

        var updateRes = getStore().update(credentialDefinition);
        assertThat(updateRes).isFailed();
    }

    @Test
    void update_whenChangingType() {
        var credentialDefinition = createCredentialDefinition();
        var result = getStore().create(credentialDefinition);
        assertThat(result).isSucceeded();

        credentialDefinition = createCredentialDefinition(credentialDefinition.getId(), "Membership1");

        var updateRes = getStore().update(credentialDefinition);
        assertThat(updateRes).isSucceeded();
    }

    @Test
    void delete() {
        var context = createCredentialDefinition();
        getStore().create(context);

        var deleteRes = getStore().deleteById(context.getId());
        assertThat(deleteRes).isSucceeded();
    }

    @Test
    void delete_whenNotExists() {
        assertThat(getStore().deleteById("not-exist")).isFailed()
                .detail().contains("does not exist.");
    }

    protected abstract CredentialDefinitionStore getStore();

    private CredentialDefinition createCredentialDefinition() {
        return createCredentialDefinition(UUID.randomUUID().toString(), "Membership");
    }

    private CredentialDefinition createCredentialDefinition(String id, String type) {
        return CredentialDefinition.Builder.newInstance().id(id).jsonSchema("")
                .credentialType(type)
                .jsonSchemaUrl("http://example.com/schema")
                .build();
    }

}
