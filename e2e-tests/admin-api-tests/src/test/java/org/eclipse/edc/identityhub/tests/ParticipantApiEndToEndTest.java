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
import org.eclipse.edc.identityhub.tests.fixtures.issuerservice.IssuerServiceEndToEndExtension;
import org.eclipse.edc.identityhub.tests.fixtures.issuerservice.IssuerServiceEndToEndTestContext;
import org.eclipse.edc.issuerservice.api.admin.participant.v1.unstable.model.ParticipantDto;
import org.eclipse.edc.issuerservice.spi.participant.ParticipantService;
import org.eclipse.edc.issuerservice.spi.participant.model.Participant;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class ParticipantApiEndToEndTest {
    abstract static class Tests {

        @AfterEach
        void teardown(ParticipantService participantService) {
            participantService.queryParticipants(QuerySpec.max()).getContent()
                    .forEach(p -> participantService.deleteParticipant(p.participantId()).getContent());

        }

        @Test
        void createParticipant(IssuerServiceEndToEndTestContext context) {

            context.getAdminEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .body("""
                            {
                              "participantId": "test-participant-id",
                              "did": "did:web:test-participant",
                              "name": null
                            }
                            """)
                    .post("/v1alpha/participants")
                    .then()
                    .statusCode(201)
                    .header("Location", Matchers.endsWith("/participants/test-participant-id"));
        }

        @Test
        void createParticipant_whenExists(IssuerServiceEndToEndTestContext context, ParticipantService service) {

            service.createParticipant(new Participant("test-participant-id", "did:web:foo", "foobar"));

            context.getAdminEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .body("""
                            {
                              "participantId": "test-participant-id",
                              "did": "did:web:test-participant",
                              "name": null
                            }
                            """)
                    .post("/v1alpha/participants")
                    .then()
                    .statusCode(409);
        }

        @Test
        void createParticipant_whenMissingFields(IssuerServiceEndToEndTestContext context) {
            context.getAdminEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .body("""
                            {
                            "participantId": "test-participant-id"
                            }
                            """)
                    .post("/v1alpha/participants")
                    .then()
                    .statusCode(400);
        }

        @Test
        void queryParticipant(IssuerServiceEndToEndTestContext context, ParticipantService service) {

            var expectedParticipant = new Participant("test-participant-id", "did:web:foo", "foobar");
            service.createParticipant(expectedParticipant);

            var res = context.getAdminEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .body(QuerySpec.Builder.newInstance().filter(new Criterion("participantId", "=", "test-participant-id")).build())
                    .post("/v1alpha/participants/query")
                    .then()
                    .statusCode(200)
                    .body(Matchers.notNullValue())
                    .extract().body().as(ParticipantDto[].class);

            assertThat(res).hasSize(1).allSatisfy(p -> assertThat(expectedParticipant).isEqualTo(p.toParticipant()));
        }

        @Test
        void queryParticipant_noResult(IssuerServiceEndToEndTestContext context) {

            var res = context.getAdminEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .body(QuerySpec.Builder.newInstance().filter(new Criterion("participantId", "=", "test-participant-id")).build())
                    .post("/v1alpha/participants/query")
                    .then()
                    .statusCode(200)
                    .body(Matchers.notNullValue())
                    .extract().body().as(ParticipantDto[].class);

            assertThat(res).isEmpty();
        }

        @Test
        void queryParticipant_byAttestationId(IssuerServiceEndToEndTestContext context, ParticipantService service) {
            service.createParticipant(new Participant("test-participant-id1", "did:web:barbaz", "barbaz", List.of("att1", "att2")));
            service.createParticipant(new Participant("test-participant-id2", "did:web:quizzquazz", "quizzquazz", List.of("att2", "att3")));

            var query = QuerySpec.Builder.newInstance()
                    .filter(new Criterion("attestations", "contains", "att2"))
                    .sortField("participantId")
                    .sortOrder(SortOrder.ASC)
                    .build();

            context.getAdminEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .body(query)
                    .post("/v1alpha/participants/query")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .body("size()", is(2))
                    .body("[0].participantId", equalTo("test-participant-id1"))
                    .body("[1].participantId", equalTo("test-participant-id2"));
        }

        @Test
        void getById(IssuerServiceEndToEndTestContext context, ParticipantService service) {

            var expectedParticipant = new Participant("test-participant-id", "did:web:foo", "foobar");
            service.createParticipant(expectedParticipant);

            var res = context.getAdminEndpoint().baseRequest()
                    .get("/v1alpha/participants/test-participant-id")
                    .then()
                    .statusCode(200)
                    .body(Matchers.notNullValue())
                    .extract().body().as(ParticipantDto.class);

            assertThat(res.toParticipant()).isEqualTo(expectedParticipant);
        }

    }

    @Nested
    @EndToEndTest
    @ExtendWith(IssuerServiceEndToEndExtension.InMemory.class)
    class InMemory extends Tests {
    }

    @Nested
    @PostgresqlIntegrationTest
    @ExtendWith(IssuerServiceEndToEndExtension.Postgres.class)
    class Postgres extends Tests {

    }
}
