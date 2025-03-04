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
import org.eclipse.edc.identityhub.tests.fixtures.issuerservice.IssuerServiceEndToEndExtension;
import org.eclipse.edc.identityhub.tests.fixtures.issuerservice.IssuerServiceEndToEndTestContext;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import static io.restassured.http.ContentType.JSON;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@SuppressWarnings("JUnitMalformedDeclaration")
public class AttestationApiEndToEndTest {

    abstract static class Tests {

        public static final String USER = "user";

        @BeforeAll
        static void setup(IssuerServiceEndToEndTestContext context) {
            var registry = context.getRuntime().getService(AttestationDefinitionValidatorRegistry.class);
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
        void createAttestationDefinition(IssuerServiceEndToEndTestContext context, AttestationDefinitionStore store) {
            var token = context.createParticipant(USER);
            context.getAdminEndpoint().baseRequest()
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
        void createAttestationDefinition_shouldReturn400_whenValidationFails(IssuerServiceEndToEndTestContext context, AttestationDefinitionStore store) {
            var token = context.createParticipant(USER);

            context.getAdminEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", token))
                    .body(createAttestationDefinition("test-id", "test-failure-type", Map.of("foo", "bar")))
                    .post("/v1alpha/participants/%s/attestations".formatted(toBase64(USER)))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400);

        }

        @Test
        void getForParticipant(IssuerServiceEndToEndTestContext context, AttestationDefinitionStore store, HolderStore holderStore) {
            var token = context.createParticipant(USER);

            var att1 = createAttestationDefinition("att1", "test-type", Map.of("bar", "baz"));
            var att2 = createAttestationDefinition("att2", "test-type-1", Map.of("bar", "baz"));
            var p = createHolder("foobar", "did:web:foobar", "Foo Bar", List.of("att1", "att2"));
            var r = store.create(att1).compose(v -> store.create(att2)).compose(participant -> holderStore.create(p));
            assertThat(r).isSucceeded();

            context.getAdminEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", token))
                    .get("/v1alpha/participants/%s/attestations?holderId=foobar".formatted(USER))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .body("size()", equalTo(2))
                    .body("[0].id", equalTo("att1"))
                    .body("[1].id", equalTo("att2"));
        }

        @Test
        void linkAttestation_expect201(IssuerServiceEndToEndTestContext context, AttestationDefinitionStore store, HolderStore holderStore) {
            var token = context.createParticipant(USER);

            var attestation = createAttestationDefinition("att1", "test-type", Map.of("bar", "baz"));
            var r = store.create(attestation)
                    .compose(participant -> holderStore.create(createHolder("foobar", "did:web:foobar", "Foo Bar")));
            assertThat(r).isSucceeded();

            context.getAdminEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", token))
                    .post("/v1alpha/participants/%s/attestations/att1/link?holderId=foobar".formatted(USER))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(201);

            assertThat(holderStore.findById("foobar")).isSucceeded()
                    .satisfies(participant -> assertThat(participant.getAttestations()).containsExactly("att1"));
        }

        @Test
        void linkAttestation_alreadyLinked_expect204(IssuerServiceEndToEndTestContext context, AttestationDefinitionStore store, HolderStore holderStore) {
            var token = context.createParticipant(USER);

            var attestation = createAttestationDefinition("att1", "test-type", Map.of("bar", "baz"));
            var r = store.create(attestation)
                    .compose(participant -> holderStore.create(createHolder("foobar", "did:web:foobar", "Foo Bar", singletonList("att1"))));
            assertThat(r).isSucceeded();

            context.getAdminEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", token))
                    .post("/v1alpha/participants/%s/attestations/att1/link?holderId=foobar".formatted(USER))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(204);

            assertThat(holderStore.findById("foobar")).isSucceeded()
                    .satisfies(participant -> assertThat(participant.getAttestations()).containsExactly("att1"));
        }

        @Test
        void queryAttestations(IssuerServiceEndToEndTestContext context, AttestationDefinitionStore store, HolderStore holderStore) {
            var token = context.createParticipant(USER);

            var p1 = createHolder("p1", "did:web:foobar", "Foo Bar", singletonList("att1"));
            var p2 = createHolder("p2", "did:web:barbaz", "Bar Baz", List.of("att1", "att2"));

            var r = holderStore.create(p1).compose(participant -> holderStore.create(p2));
            assertThat(r).isSucceeded();

            var attestation1 = createAttestationDefinition("att1", "test-type", Map.of("key1", "val1"));
            var attestation2 = createAttestationDefinition("att2", "test-type=1", Map.of("key2", "val2"));
            var attestation3 = createAttestationDefinition("att3", "test-type", Map.of("key2", "val2"), "anotherUser");

            store.create(attestation1);
            store.create(attestation2);
            store.create(attestation3);

            //query by attestation type
            context.getAdminEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", token))
                    .body(QuerySpec.Builder.newInstance()
                            .sortField("id")
                            .sortOrder(SortOrder.ASC)
                            .filter(new Criterion("attestationType", "=", "test-type"))
                            .build())
                    .post("/v1alpha/participants/%s/attestations/query".formatted(USER))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .body("size()", equalTo(1))
                    .body("[0].id", equalTo("att1"))
                    .body("[0].participantContextId", equalTo("user"));

        }

        private String toBase64(String s) {
            return Base64.getUrlEncoder().encodeToString(s.getBytes());
        }

        private AttestationDefinition createAttestationDefinition(String id, String type, Map<String, Object> configuration) {
            return createAttestationDefinition(id, type, configuration, USER);
        }

        private Holder createHolder(String id, String did, String name) {
            return createHolder(id, did, name, List.of());
        }

        private Holder createHolder(String id, String did, String name, List<String> attestations) {
            return Holder.Builder.newInstance()
                    .holderId(id)
                    .did(did)
                    .holderName(name)
                    .attestations(attestations)
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
    @ExtendWith(IssuerServiceEndToEndExtension.InMemory.class)
    class InMemory extends Tests {
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
        static final IssuerServiceEndToEndExtension ISSUER_SERVICE = IssuerServiceEndToEndExtension.Postgres
                .withConfig(cfg -> POSTGRESQL_EXTENSION.configFor(ISSUER));
    }
}
