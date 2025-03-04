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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@SuppressWarnings("JUnitMalformedDeclaration")
public class HolderApiEndToEndTest {
    abstract static class Tests {


        private static String token = "";

        @BeforeAll
        static void setup(IssuerServiceEndToEndTestContext context) {
            token = context.createSuperUser();
        }

        @AfterEach
        void teardown(HolderService holderService) {
            holderService.queryHolders(QuerySpec.max()).getContent()
                    .forEach(p -> holderService.deleteHolder(p.holderId()).getContent());

        }

        @Test
        void createHolder(IssuerServiceEndToEndTestContext context) {

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
                    .post("/v1alpha/holders")
                    .then()
                    .statusCode(201)
                    .header("Location", Matchers.endsWith("/holders/test-participant-id"));
        }

        @Test
        void createParticipant_whenExists(IssuerServiceEndToEndTestContext context, HolderService service) {

            service.createHolder(new Holder("test-participant-id", "did:web:foo", "foobar"));

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
                    .post("/v1alpha/holders")
                    .then()
                    .statusCode(409);
        }

        @Test
        void createParticipant_whenMissingFields(IssuerServiceEndToEndTestContext context) {
            context.getAdminEndpoint().baseRequest()
                    .header(new Header("x-api-key", token))
                    .contentType(ContentType.JSON)
                    .body("""
                            {
                            "holderId": "test-participant-id"
                            }
                            """)
                    .post("/v1alpha/holders")
                    .then()
                    .statusCode(400);
        }

        @Test
        void queryParticipant(IssuerServiceEndToEndTestContext context, HolderService service) {

            var expectedParticipant = new Holder("test-participant-id", "did:web:foo", "foobar");
            service.createHolder(expectedParticipant);

            var res = context.getAdminEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header(new Header("x-api-key", token))
                    .body(QuerySpec.Builder.newInstance().filter(new Criterion("holderId", "=", "test-participant-id")).build())
                    .post("/v1alpha/holders/query")
                    .then()
                    .statusCode(200)
                    .body(Matchers.notNullValue())
                    .extract().body().as(HolderDto[].class);

            assertThat(res).hasSize(1).allSatisfy(p -> assertThat(expectedParticipant).isEqualTo(p.toHolder()));
        }

        @Test
        void queryParticipant_noResult(IssuerServiceEndToEndTestContext context) {

            var res = context.getAdminEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header(new Header("x-api-key", token))
                    .body(QuerySpec.Builder.newInstance().filter(new Criterion("holderId", "=", "test-participant-id")).build())
                    .post("/v1alpha/holders/query")
                    .then()
                    .statusCode(200)
                    .body(Matchers.notNullValue())
                    .extract().body().as(HolderDto[].class);

            assertThat(res).isEmpty();
        }

        @Test
        void queryParticipant_byAttestationId(IssuerServiceEndToEndTestContext context, HolderService service) {
            service.createHolder(new Holder("test-participant-id1", "did:web:barbaz", "barbaz", List.of("att1", "att2")));
            service.createHolder(new Holder("test-participant-id2", "did:web:quizzquazz", "quizzquazz", List.of("att2", "att3")));

            var query = QuerySpec.Builder.newInstance()
                    .filter(new Criterion("attestations", "contains", "att2"))
                    .sortField("holderId")
                    .sortOrder(SortOrder.ASC)
                    .build();

            context.getAdminEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header(new Header("x-api-key", token))
                    .body(query)
                    .post("/v1alpha/holders/query")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .body("size()", is(2))
                    .body("[0].holderId", equalTo("test-participant-id1"))
                    .body("[1].holderId", equalTo("test-participant-id2"));
        }

        @Test
        void getById(IssuerServiceEndToEndTestContext context, HolderService service) {

            var expectedParticipant = new Holder("test-participant-id", "did:web:foo", "foobar");
            service.createHolder(expectedParticipant);

            var res = context.getAdminEndpoint().baseRequest()
                    .header(new Header("x-api-key", token))
                    .get("/v1alpha/holders/test-participant-id")
                    .then()
                    .statusCode(200)
                    .body(Matchers.notNullValue())
                    .extract().body().as(HolderDto.class);

            assertThat(res.toHolder()).isEqualTo(expectedParticipant);
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
