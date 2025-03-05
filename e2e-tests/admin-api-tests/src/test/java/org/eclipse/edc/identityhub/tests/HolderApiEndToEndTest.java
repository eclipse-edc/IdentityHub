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

import io.restassured.http.ContentType;
import io.restassured.http.Header;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.tests.fixtures.issuerservice.IssuerServiceEndToEndExtension;
import org.eclipse.edc.identityhub.tests.fixtures.issuerservice.IssuerServiceEndToEndTestContext;
import org.eclipse.edc.issuerservice.api.admin.holder.v1.unstable.model.HolderDto;
import org.eclipse.edc.issuerservice.spi.holder.HolderService;
import org.eclipse.edc.issuerservice.spi.holder.model.Holder;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@SuppressWarnings("JUnitMalformedDeclaration")
public class HolderApiEndToEndTest {
    abstract static class Tests {

        public static final String USER = "user";

        @AfterEach
        void teardown(HolderService holderService, ParticipantContextService pcService) {
            holderService.queryHolders(QuerySpec.max()).getContent()
                    .forEach(p -> holderService.deleteHolder(p.getHolderId()).getContent());

            pcService.query(QuerySpec.max()).getContent()
                    .forEach(pc -> pcService.deleteParticipantContext(pc.getParticipantContextId()).getContent());

        }

        @Test
        void createHolder(IssuerServiceEndToEndTestContext context) {
            var token = context.createParticipant(USER);

            context.getAdminEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header(new Header("x-api-key", token))
                    .body("""
                            {
                              "holderId": "test-participant-id",
                              "did": "did:web:test-participant",
                              "name": null
                            }
                            """)
                    .post("/v1alpha/participants/%s/holders".formatted(toBase64(USER)))
                    .then()
                    .statusCode(201)
                    .header("Location", Matchers.endsWith("/holders/test-participant-id"));
        }

        @Test
        void createParticipant_whenExists(IssuerServiceEndToEndTestContext context, HolderService service) {
            var token = context.createParticipant(USER);

            service.createHolder(createHolder("test-participant-id", "did:web:foo", "foobar"));

            context.getAdminEndpoint().baseRequest()
                    .header(new Header("x-api-key", token))
                    .contentType(ContentType.JSON)
                    .body("""
                            {
                              "holderId": "test-participant-id",
                              "did": "did:web:test-participant",
                              "name": null
                            }
                            """)
                    .post("/v1alpha/participants/%s/holders".formatted(toBase64(USER)))
                    .then()
                    .statusCode(409);
        }

        @Test
        void createHolder_notAuthorized(IssuerServiceEndToEndTestContext context) {
            context.createParticipant(USER);
            var token = context.createParticipant("anotherParticipant");

            context.getAdminEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header(new Header("x-api-key", token))
                    .body("""
                            {
                              "holderId": "test-participant-id",
                              "did": "did:web:test-participant",
                              "name": null
                            }
                            """)
                    .post("/v1alpha/participants/%s/holders".formatted(toBase64(USER)))
                    .then()
                    .statusCode(403);
        }

        @Test
        void createParticipant_whenMissingFields(IssuerServiceEndToEndTestContext context) {
            var token = context.createParticipant(USER);

            context.getAdminEndpoint().baseRequest()
                    .header(new Header("x-api-key", token))
                    .contentType(ContentType.JSON)
                    .body("""
                            {
                            "holderId": "test-participant-id"
                            }
                            """)
                    .post("/v1alpha/participants/%s/holders".formatted(toBase64(USER)))
                    .then()
                    .statusCode(400);
        }

        @Test
        void updateHolder(IssuerServiceEndToEndTestContext context, HolderService service) {
            var token = context.createParticipant(USER);
            var initialHolder = createHolder("test-participant-id", "did:web:foo", null);
            service.createHolder(initialHolder);

            context.getAdminEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header(new Header("x-api-key", token))
                    .body("""
                            {
                              "holderId": "test-participant-id",
                              "did": "did:web:foo",
                              "name": "Foo"
                            }
                            """)
                    .put("/v1alpha/participants/%s/holders".formatted(toBase64(USER)))
                    .then()
                    .statusCode(200);

            assertThat(service.findById("test-participant-id")).isSucceeded()
                    .satisfies(holder -> {
                        assertThat(holder.getHolderName()).isEqualTo("Foo");
                    });
        }

        @Test
        void updateHolder_notAuthorized(IssuerServiceEndToEndTestContext context, HolderService service) {
            context.createParticipant(USER);
            var anotherToken = context.createParticipant("anotherUser");
            var initialHolder = createHolder("test-participant-id", "did:web:foo", null);
            service.createHolder(initialHolder);

            context.getAdminEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header(new Header("x-api-key", anotherToken))
                    .body("""
                            {
                              "holderId": "test-participant-id",
                              "did": "did:web:foo",
                              "name": "Foo"
                            }
                            """)
                    .put("/v1alpha/participants/%s/holders".formatted(toBase64(USER)))
                    .then()
                    .statusCode(403);

        }

        @Test
        void queryParticipant(IssuerServiceEndToEndTestContext context, HolderService service) {
            var token = context.createParticipant(USER);

            var holder1 = createHolder("test-participant-id", "did:web:foo", "foobar");
            var holder2 = createHolder("test-participant-id", "did:web:foo", "foobar", "anotherParticipantContext");
            service.createHolder(holder1);
            service.createHolder(holder2);

            var res = context.getAdminEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header(new Header("x-api-key", token))
                    .body(QuerySpec.Builder.newInstance().filter(new Criterion("holderName", "=", "foobar")).build())
                    .post("/v1alpha/participants/%s/holders/query".formatted(toBase64(USER)))
                    .then()
                    .statusCode(200)
                    .body(Matchers.notNullValue())
                    .extract().body().as(Holder[].class);

            assertThat(res).hasSize(1).allSatisfy(p -> assertThat(holder1).usingRecursiveComparison().isEqualTo(p));
        }

        @Test
        void queryParticipant_noResult(IssuerServiceEndToEndTestContext context) {
            var token = context.createParticipant(USER);

            var res = context.getAdminEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header(new Header("x-api-key", token))
                    .body(QuerySpec.Builder.newInstance().filter(new Criterion("holderId", "=", "test-participant-id")).build())
                    .post("/v1alpha/participants/%s/holders/query".formatted(toBase64(USER)))
                    .then()
                    .statusCode(200)
                    .body(Matchers.notNullValue())
                    .extract().body().as(HolderDto[].class);

            assertThat(res).isEmpty();
        }

        @Test
        void queryParticipant_byAttestationId(IssuerServiceEndToEndTestContext context, HolderService service) {
            var token = context.createParticipant(USER);

            service.createHolder(createHolder("test-participant-id1", "did:web:barbaz", "barbaz", List.of("att1", "att2")));
            service.createHolder(createHolder("test-participant-id2", "did:web:quizzquazz", "quizzquazz", List.of("att2", "att3")));

            var query = QuerySpec.Builder.newInstance()
                    .filter(new Criterion("attestations", "contains", "att2"))
                    .sortField("holderId")
                    .sortOrder(SortOrder.ASC)
                    .build();

            context.getAdminEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header(new Header("x-api-key", token))
                    .body(query)
                    .post("/v1alpha/participants/%s/holders/query".formatted(toBase64(USER)))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .body("size()", is(2))
                    .body("[0].holderId", equalTo("test-participant-id1"))
                    .body("[1].holderId", equalTo("test-participant-id2"));
        }

        @Test
        void getById(IssuerServiceEndToEndTestContext context, HolderService service) {
            var token = context.createParticipant(USER);

            var expectedParticipant = createHolder("test-participant-id", "did:web:foo", "foobar");
            service.createHolder(expectedParticipant);

            var res = context.getAdminEndpoint().baseRequest()
                    .header(new Header("x-api-key", token))
                    .get("/v1alpha/participants/%s/holders/test-participant-id".formatted(toBase64(USER)))
                    .then()
                    .statusCode(200)
                    .body(Matchers.notNullValue())
                    .extract().body().as(Holder.class);

            assertThat(res).usingRecursiveComparison().isEqualTo(expectedParticipant);
        }

        @Test
        void getById_notAuthorized(IssuerServiceEndToEndTestContext context, HolderService service) {
            context.createParticipant(USER);
            var token = context.createParticipant("anotherUser");

            var expectedParticipant = createHolder("test-participant-id", "did:web:foo", "foobar");
            service.createHolder(expectedParticipant);

            context.getAdminEndpoint().baseRequest()
                    .header(new Header("x-api-key", token))
                    .get("/v1alpha/participants/%s/holders/test-participant-id".formatted(toBase64(USER)))
                    .then()
                    .statusCode(403);

        }

        private Holder createHolder(String id, String did, String name) {
            return createHolder(id, did, name, List.of());
        }

        private Holder createHolder(String id, String did, String name, String participantContextId) {
            return createHolder(id, did, name, List.of(), participantContextId);
        }

        private Holder createHolder(String id, String did, String name, List<String> attestations) {
            return createHolder(id, did, name, attestations, USER);
        }

        private Holder createHolder(String id, String did, String name, List<String> attestations, String participantContextId) {
            return Holder.Builder.newInstance()
                    .participantContextId(USER)
                    .holderId(id)
                    .did(did)
                    .holderName(name)
                    .attestations(attestations)
                    .build();
        }

        private String toBase64(String s) {
            return Base64.getUrlEncoder().encodeToString(s.getBytes());
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
