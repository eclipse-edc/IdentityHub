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

package org.eclipse.edc.identityhub.tests;

import io.restassured.http.Header;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.tests.fixtures.issuerservice.IssuerExtension;
import org.eclipse.edc.identityhub.tests.fixtures.issuerservice.IssuerRuntime;
import org.eclipse.edc.issuerservice.spi.holder.model.Holder;
import org.eclipse.edc.issuerservice.spi.holder.store.HolderStore;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionStore;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionValidatorRegistry;
import org.eclipse.edc.issuerservice.spi.issuance.model.AttestationDefinition;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Violation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Base64;
import java.util.Map;

import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.identityhub.tests.TestData.ISSUER_RUNTIME_ID;
import static org.eclipse.edc.identityhub.tests.TestData.ISSUER_RUNTIME_MEM_MODULES;
import static org.eclipse.edc.identityhub.tests.TestData.ISSUER_RUNTIME_NAME;
import static org.eclipse.edc.identityhub.tests.TestData.ISSUER_RUNTIME_SQL_MODULES;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@SuppressWarnings("JUnitMalformedDeclaration")
public class AttestationApiEndToEndTest {

    abstract static class Tests {

        public static final String USER = "user";

        @BeforeAll
        static void setup(IssuerRuntime runtime) {
            var registry = runtime.getService(AttestationDefinitionValidatorRegistry.class);
            registry.registerValidator("test-type", def -> ValidationResult.success());
            registry.registerValidator("test-failure-type", def -> ValidationResult.failure(Violation.violation("test", null)));
        }

        @AfterEach
        void teardown(AttestationDefinitionStore store, HolderStore holderStore, ParticipantContextService pcService) {
            store.query(QuerySpec.max()).getContent()
                    .forEach(att -> store.deleteById(att.getId()));

            holderStore.query(QuerySpec.max()).getContent()
                    .forEach(participant -> holderStore.deleteById(participant.getHolderId()));

            pcService.query(QuerySpec.max()).getContent()
                    .forEach(pc -> pcService.deleteParticipantContext(pc.getParticipantContextId()).getContent());
        }

        @Test
        void createAttestationDefinition(IssuerRuntime runtime, AttestationDefinitionStore store) {
            var token = runtime.createParticipant(USER).apiKey();
            runtime.getAdminEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", token))
                    .body(createAttestationDefinition("test-id", "test-type", Map.of("foo", "bar")))
                    .post("/v1alpha/participants/%s/attestations".formatted(toBase64(USER)))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(201);

            assertThat(store.resolveDefinition("test-id")).isNotNull();
        }

        @Test
        void createAttestationDefinition_notAuthorized(IssuerRuntime runtime) {
            runtime.createParticipant(USER);
            var token = runtime.createParticipant("anotherUser").apiKey();
            runtime.getAdminEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", token))
                    .body(createAttestationDefinition("test-id", "test-type", Map.of("foo", "bar")))
                    .post("/v1alpha/participants/%s/attestations".formatted(toBase64(USER)))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403);

        }

        @Test
        void createAttestationDefinition_shouldReturn400_whenValidationFails(IssuerRuntime runtime) {
            var token = runtime.createParticipant(USER).apiKey();

            runtime.getAdminEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", token))
                    .body(createAttestationDefinition("test-id", "test-failure-type", Map.of("foo", "bar")))
                    .post("/v1alpha/participants/%s/attestations".formatted(toBase64(USER)))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400);

        }

        @Test
        void queryAttestations(IssuerRuntime runtime, AttestationDefinitionStore store, HolderStore holderStore) {
            var token = runtime.createParticipant(USER).apiKey();

            var p1 = createHolder("p1", "did:web:foobar", "Foo Bar");
            var p2 = createHolder("p2", "did:web:barbaz", "Bar Baz");

            var r = holderStore.create(p1).compose(participant -> holderStore.create(p2));
            assertThat(r).isSucceeded();

            var attestation1 = createAttestationDefinition("att1", "test-type", Map.of("key1", "val1"));
            var attestation2 = createAttestationDefinition("att2", "test-type=1", Map.of("key2", "val2"));
            var attestation3 = createAttestationDefinition("att3", "test-type", Map.of("key2", "val2"), "anotherUser");

            store.create(attestation1);
            store.create(attestation2);
            store.create(attestation3);

            //query by attestation type
            runtime.getAdminEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", token))
                    .body(QuerySpec.Builder.newInstance()
                            .sortField("id")
                            .sortOrder(SortOrder.ASC)
                            .filter(new Criterion("attestationType", "=", "test-type"))
                            .build())
                    .post("/v1alpha/participants/%s/attestations/query".formatted(toBase64(USER)))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .body("size()", equalTo(1))
                    .body("[0].id", equalTo("att1"))
                    .body("[0].participantContextId", equalTo("user"));

        }

        @Test
        void queryAttestations_notAuthorized(IssuerRuntime runtime, AttestationDefinitionStore store) {
            runtime.createParticipant(USER);
            var token = runtime.createParticipant("anotherUser").apiKey();

            var attestation1 = createAttestationDefinition("att1", "test-type", Map.of("key1", "val1"));

            store.create(attestation1);

            //query by attestation type
            runtime.getAdminEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", token))
                    .body(QuerySpec.Builder.newInstance()
                            .sortField("id")
                            .sortOrder(SortOrder.ASC)
                            .filter(new Criterion("attestationType", "=", "test-type"))
                            .build())
                    .post("/v1alpha/participants/%s/attestations/query".formatted(toBase64(USER)))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .body("size()", equalTo(0));

        }

        private String toBase64(String s) {
            return Base64.getUrlEncoder().encodeToString(s.getBytes());
        }

        private AttestationDefinition createAttestationDefinition(String id, String type, Map<String, Object> configuration) {
            return createAttestationDefinition(id, type, configuration, USER);
        }

        private Holder createHolder(String id, String did, String name) {
            return Holder.Builder.newInstance()
                    .participantContextId(USER)
                    .holderId(id)
                    .did(did)
                    .holderName(name)
                    .build();
        }

        private AttestationDefinition createAttestationDefinition(String id, String type, Map<String, Object> configuration, String participantContext) {
            return AttestationDefinition.Builder.newInstance()
                    .id(id)
                    .attestationType(type)
                    .participantContextId(participantContext)
                    .configuration(configuration).build();
        }

    }

    @Nested
    @EndToEndTest
    class InMemory extends Tests {

        @RegisterExtension
        static final IssuerExtension ISSUER_EXTENSION = IssuerExtension.Builder.newInstance()
                .id(ISSUER_RUNTIME_ID)
                .name(ISSUER_RUNTIME_NAME)
                .modules(ISSUER_RUNTIME_MEM_MODULES)
                .build();

    }

    @Nested
    @PostgresqlIntegrationTest
    class Postgres extends Tests {

        @Order(0)
        @RegisterExtension
        static final PostgresqlEndToEndExtension POSTGRESQL_EXTENSION = new PostgresqlEndToEndExtension();

        private static final String ISSUER = "issuer";

        @Order(1)
        @RegisterExtension
        static final BeforeAllCallback POSTGRES_CONTAINER_STARTER = context -> {
            POSTGRESQL_EXTENSION.createDatabase(ISSUER);
        };

        @Order(2)
        @RegisterExtension
        static final IssuerExtension ISSUER_EXTENSION = IssuerExtension.Builder.newInstance()
                .id(ISSUER_RUNTIME_ID)
                .name(ISSUER_RUNTIME_NAME)
                .modules(ISSUER_RUNTIME_SQL_MODULES)
                .configurationProvider(() -> POSTGRESQL_EXTENSION.configFor(ISSUER))
                .build();
    }
}
