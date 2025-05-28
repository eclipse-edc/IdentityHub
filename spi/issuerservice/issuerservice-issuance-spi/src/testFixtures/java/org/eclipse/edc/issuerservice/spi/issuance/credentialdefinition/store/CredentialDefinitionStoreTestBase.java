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

package org.eclipse.edc.issuerservice.spi.issuance.credentialdefinition.store;

import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialDefinition;
import org.eclipse.edc.issuerservice.spi.issuance.model.MappingDefinition;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat.VC1_0_JWT;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

public abstract class CredentialDefinitionStoreTestBase {


    @Test
    void create() {
        var credentialDefinition = createCredentialDefinition();
        var result = getStore().create(credentialDefinition);
        assertThat(result).isSucceeded();
        var query = getStore().query(QuerySpec.max());
        assertThat(query).isSucceeded();
        assertThat(query.getContent()).usingRecursiveFieldByFieldElementComparator().containsExactly(credentialDefinition);
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

    protected CredentialDefinition.Builder createCredentialDefinitionBuilder(String id, String type) {
        return CredentialDefinition.Builder.newInstance()
                .id(id)
                .participantContextId(UUID.randomUUID().toString())
                .credentialType(type)
                .formatFrom(VC1_0_JWT)
                .jsonSchemaUrl("http://example.com/schema");
    }

    private CredentialDefinition createCredentialDefinition() {
        return createCredentialDefinition(UUID.randomUUID().toString(), "Membership");
    }

    private CredentialDefinition createCredentialDefinition(String id, String type) {
        return createCredentialDefinitionBuilder(id, type)
                .build();
    }

    @Nested
    class Query {
        @Test
        void byId() {
            range(0, 5)
                    .mapToObj(i -> createCredentialDefinition("id" + i, "Membership" + i))
                    .forEach(getStore()::create);

            var q = QuerySpec.Builder.newInstance()
                    .filter(new Criterion("credentialType", "=", "Membership4"))
                    .build();

            assertThat(getStore().query(q)).isSucceeded()
                    .satisfies(str -> assertThat(str).hasSize(1));
        }


        @Test
        void noQuerySpec() {
            var resources = range(0, 5)
                    .mapToObj(i -> createCredentialDefinition("id" + i, "Membership" + i))
                    .toList();

            resources.forEach(getStore()::create);

            var res = getStore().query(QuerySpec.none());
            assertThat(res).isSucceeded();
            assertThat(res.getContent())
                    .usingRecursiveFieldByFieldElementComparator()
                    .containsExactlyInAnyOrder(resources.toArray(new CredentialDefinition[0]));
        }

        @Test
        void whenNotFound() {
            var resources = range(0, 5)
                    .mapToObj(i -> createCredentialDefinition("id" + i, "Membership" + i))
                    .toList();

            resources.forEach(getStore()::create);

            var query = QuerySpec.Builder.newInstance()
                    .filter(new Criterion("credentialType", "=", "Membership99"))
                    .build();
            var res = getStore().query(query);
            assertThat(res).isSucceeded();
            assertThat(res.getContent()).isEmpty();
        }

        @Test
        void byInvalidField_shouldReturnEmptyList() {
            var resources = range(0, 5)
                    .mapToObj(i -> createCredentialDefinition("id" + i, "Membership" + i))
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
        void byAttestation() {
            var def1 = createCredentialDefinitionBuilder("id1", "Membership")
                    .attestations(Set.of("att1", "att2"))
                    .build();
            var def2 = createCredentialDefinitionBuilder("id2", "Iso9001Cert")
                    .attestations(Set.of("att2", "att3"))
                    .build();

            var r = getStore().create(def1).compose(v -> getStore().create(def2));
            assertThat(r).isSucceeded();

            var query = QuerySpec.Builder.newInstance()
                    .filter(new Criterion("attestations", "contains", "att2"))
                    .build();

            var result = getStore().query(query);
            assertThat(result).isSucceeded();

            assertThat(result.getContent())
                    .hasSize(2)
                    .usingRecursiveFieldByFieldElementComparator()
                    .containsExactlyInAnyOrder(def1, def2);
        }

        @Test
        void byFormats() {
            var def1 = createCredentialDefinitionBuilder("id1", "Membership")
                    .build();
            var def2 = createCredentialDefinitionBuilder("id2", "Iso9001Cert")
                    .build();

            var r = getStore().create(def1).compose(v -> getStore().create(def2));
            assertThat(r).isSucceeded();

            var query = QuerySpec.Builder.newInstance()
                    .filter(new Criterion("format", "=", VC1_0_JWT.name()))
                    .build();

            var result = getStore().query(query);
            assertThat(result).isSucceeded();

            assertThat(result.getContent())
                    .hasSize(2)
                    .usingRecursiveFieldByFieldElementComparator()
                    .containsExactlyInAnyOrder(def1, def2);
        }

        @Test
        void byParticipantContext() {
            var def1 = createCredentialDefinitionBuilder("id1", "Membership")
                    .attestations(Set.of("att1", "att2"))
                    .build();
            var def2 = createCredentialDefinitionBuilder("id2", "Iso9001Cert")
                    .attestations(Set.of("att2", "att3"))
                    .build();

            var r = getStore().create(def1).compose(v -> getStore().create(def2));
            assertThat(r).isSucceeded();

            var query = QuerySpec.Builder.newInstance()
                    .filter(new Criterion("participantContextId", "=", def1.getParticipantContextId()))
                    .build();

            var result = getStore().query(query);
            assertThat(result).isSucceeded();

            assertThat(result.getContent())
                    .hasSize(1)
                    .usingRecursiveFieldByFieldElementComparator()
                    .containsExactlyInAnyOrder(def1);
        }


        @Test
        void byMappings() {
            var def1 = createCredentialDefinitionBuilder("id1", "Membership")
                    .mapping(new MappingDefinition("test-input", "test-output", true))
                    .attestations(Set.of("att1", "att2"))
                    .build();
            var def2 = createCredentialDefinitionBuilder("id2", "Iso9001Cert")
                    .mapping(new MappingDefinition("test-input", "another-test-output", true))
                    .attestations(Set.of("att2", "att3"))
                    .build();

            var r = getStore().create(def1).compose(v -> getStore().create(def2));
            assertThat(r).isSucceeded();

            var query = QuerySpec.Builder.newInstance()
                    .filter(new Criterion("mappings.input", "=", "test-input"))
                    .build();

            var result = getStore().query(query);
            assertThat(result).isSucceeded();

            assertThat(result.getContent())
                    .hasSize(2)
                    .usingRecursiveFieldByFieldElementComparator()
                    .containsExactlyInAnyOrder(def1, def2);
        }
    }

}
