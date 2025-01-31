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

package org.eclipse.edc.issuerservice.api.admin.participant.v1.unstable;

import io.restassured.specification.RequestSpecification;
import org.eclipse.edc.identityhub.api.Versions;
import org.eclipse.edc.issuerservice.api.admin.participant.v1.unstable.model.ParticipantDto;
import org.eclipse.edc.issuerservice.spi.participant.ParticipantService;
import org.eclipse.edc.issuerservice.spi.participant.models.Participant;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ComponentTest
class IssuerParticipantAdminApiControllerTest extends RestControllerTestBase {

    private final ParticipantService participantService = mock();


    @Test
    void addParticipant() {
        when(participantService.createParticipant(any())).thenReturn(ServiceResult.success());

        baseRequest()
                .body(new ParticipantDto("test-id", "did:web:test", "test name"))
                .post()
                .then()
                .log().ifValidationFails()
                .statusCode(201)
                .header("Location", endsWith("/participants/test-id"))
                .body(emptyString());
    }

    @Test
    void addParticipant_whenAlreadyExists() {
        when(participantService.createParticipant(any())).thenReturn(ServiceResult.conflict("already exists"));

        baseRequest()
                .body(new ParticipantDto("test-id", "did:web:test", "test name"))
                .post()
                .then()
                .log().ifValidationFails()
                .statusCode(409)
                .body(notNullValue());
    }

    @Test
    void updateParticipant() {
        when(participantService.updateParticipant(any())).thenReturn(ServiceResult.success());

        baseRequest()
                .body(new ParticipantDto("test-id", "did:web:test", "test name"))
                .put()
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body(emptyString());
    }

    @Test
    void updateParticipant_notFound() {
        when(participantService.updateParticipant(any())).thenReturn(ServiceResult.notFound("not found"));

        baseRequest()
                .body(new ParticipantDto("test-id", "did:web:test", "test name"))
                .put()
                .then()
                .log().ifValidationFails()
                .statusCode(404)
                .body(notNullValue());
    }

    @Test
    void getParticipantById() {
        var test = new Participant("test-id", "did:web:test", "test name");
        when(participantService.findById(any())).thenReturn(ServiceResult.success(test));

        var response = baseRequest()
                .get("/test-id")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract().body().as(ParticipantDto.class);

        assertThat(response.toParticipant()).usingRecursiveComparison().isEqualTo(test);
    }

    @Test
    void getParticipantById_notFound() {
        when(participantService.findById(any())).thenReturn(ServiceResult.notFound("not found"));

        baseRequest()
                .get("/test-id")
                .then()
                .log().ifValidationFails()
                .statusCode(404);
    }

    @Test
    void queryParticipants() {
        var test = new Participant("test-id", "did:web:test", "test name");
        when(participantService.queryParticipants(any())).thenReturn(ServiceResult.success(Set.of(test)));

        var dto = baseRequest()
                .body(QuerySpec.Builder.newInstance().build())
                .post("/query")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract().body().as(ParticipantDto[].class);

        assertThat(dto).hasSize(1)
                .allSatisfy(d -> assertThat(d.toParticipant()).usingRecursiveComparison().isEqualTo(test));
    }


    @Test
    void queryParticipants_noneFound() {
        when(participantService.queryParticipants(any())).thenReturn(ServiceResult.success(Set.of()));

        var dto = baseRequest()
                .body(QuerySpec.Builder.newInstance().build())
                .post("/query")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract().body().as(ParticipantDto[].class);

        assertThat(dto).isEmpty();
    }

    @Override
    protected Object controller() {
        return new IssuerParticipantAdminApiController(participantService);
    }

    private RequestSpecification baseRequest() {
        return given()
                .contentType("application/json")
                .baseUri("http://localhost:" + port + Versions.UNSTABLE + "/participants")
                .when();
    }
}