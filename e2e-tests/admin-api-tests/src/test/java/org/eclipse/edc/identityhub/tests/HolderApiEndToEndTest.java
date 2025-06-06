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
import org.eclipse.edc.identityhub.tests.fixtures.issuerservice.IssuerExtension;
import org.eclipse.edc.identityhub.tests.fixtures.issuerservice.IssuerRuntime;
import org.eclipse.edc.issuerservice.api.admin.holder.v1.unstable.model.HolderDto;
import org.eclipse.edc.issuerservice.spi.holder.HolderService;
import org.eclipse.edc.issuerservice.spi.holder.model.Holder;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.identityhub.tests.TestData.ISSUER_RUNTIME_ID;
import static org.eclipse.edc.identityhub.tests.TestData.ISSUER_RUNTIME_MEM_MODULES;
import static org.eclipse.edc.identityhub.tests.TestData.ISSUER_RUNTIME_NAME;
import static org.eclipse.edc.identityhub.tests.TestData.ISSUER_RUNTIME_SQL_MODULES;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

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
        void createHolder(IssuerRuntime runtime) {
            var token = runtime.createParticipant(USER).apiKey();

            runtime.getAdminEndpoint().baseRequest()
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
        void createParticipant_whenExists(IssuerRuntime runtime, HolderService service) {
            var token = runtime.createParticipant(USER).apiKey();

            service.createHolder(createHolder("test-participant-id", "did:web:foo", "foobar"));

            runtime.getAdminEndpoint().baseRequest()
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
        void createHolder_notAuthorized(IssuerRuntime runtime) {
            runtime.createParticipant(USER);
            var token = runtime.createParticipant("anotherParticipant").apiKey();

            runtime.getAdminEndpoint().baseRequest()
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
        void createParticipant_whenMissingFields(IssuerRuntime runtime) {
            var token = runtime.createParticipant(USER).apiKey();

            runtime.getAdminEndpoint().baseRequest()
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
        void updateHolder(IssuerRuntime runtime, HolderService service) {
            var token = runtime.createParticipant(USER).apiKey();
            var initialHolder = createHolder("test-participant-id", "did:web:foo", null);
            service.createHolder(initialHolder);

            runtime.getAdminEndpoint().baseRequest()
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
        void updateHolder_notAuthorized(IssuerRuntime runtime, HolderService service) {
            runtime.createParticipant(USER);
            var anotherToken = runtime.createParticipant("anotherUser").apiKey();
            var initialHolder = createHolder("test-participant-id", "did:web:foo", null);
            service.createHolder(initialHolder);

            runtime.getAdminEndpoint().baseRequest()
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
        void queryParticipant(IssuerRuntime runtime, HolderService service) {
            var token = runtime.createParticipant(USER).apiKey();

            var holder1 = createHolder("test-participant-id", "did:web:foo", "foobar");
            var holder2 = createHolder("test-participant-id", "did:web:foo", "foobar", "anotherParticipantContext");
            service.createHolder(holder1);
            service.createHolder(holder2);

            var res = runtime.getAdminEndpoint().baseRequest()
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
        void queryParticipant_noResult(IssuerRuntime runtime) {
            var token = runtime.createParticipant(USER).apiKey();

            var res = runtime.getAdminEndpoint().baseRequest()
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
        void getById(IssuerRuntime runtime, HolderService service) {
            var token = runtime.createParticipant(USER).apiKey();

            var expectedParticipant = createHolder("test-participant-id", "did:web:foo", "foobar");
            service.createHolder(expectedParticipant);

            var res = runtime.getAdminEndpoint().baseRequest()
                    .header(new Header("x-api-key", token))
                    .get("/v1alpha/participants/%s/holders/test-participant-id".formatted(toBase64(USER)))
                    .then()
                    .statusCode(200)
                    .body(Matchers.notNullValue())
                    .extract().body().as(Holder.class);

            assertThat(res).usingRecursiveComparison().isEqualTo(expectedParticipant);
        }

        @Test
        void getById_notAuthorized(IssuerRuntime runtime, HolderService service) {
            runtime.createParticipant(USER);
            var token = runtime.createParticipant("anotherUser").apiKey();

            var expectedParticipant = createHolder("test-participant-id", "did:web:foo", "foobar");
            service.createHolder(expectedParticipant);

            runtime.getAdminEndpoint().baseRequest()
                    .header(new Header("x-api-key", token))
                    .get("/v1alpha/participants/%s/holders/test-participant-id".formatted(toBase64(USER)))
                    .then()
                    .statusCode(403);

        }


        private Holder createHolder(String id, String did, String name) {
            return createHolder(id, did, name, USER);
        }

        private Holder createHolder(String id, String did, String name, String participantContextId) {
            return Holder.Builder.newInstance()
                    .participantContextId(participantContextId)
                    .holderId(id)
                    .did(did)
                    .holderName(name)
                    .build();
        }

        private String toBase64(String s) {
            return Base64.getUrlEncoder().encodeToString(s.getBytes());
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
