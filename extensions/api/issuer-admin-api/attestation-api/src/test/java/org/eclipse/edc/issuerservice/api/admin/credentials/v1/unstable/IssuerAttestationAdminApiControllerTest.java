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

package org.eclipse.edc.issuerservice.api.admin.credentials.v1.unstable;

import io.restassured.specification.RequestSpecification;
import org.eclipse.edc.identityhub.api.Versions;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionService;
import org.eclipse.edc.issuerservice.spi.issuance.model.AttestationDefinition;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class IssuerAttestationAdminApiControllerTest extends RestControllerTestBase {

    private final AttestationDefinitionService attestationDefinitionService = mock();

    @Test
    void linkAttestation() {
        when(attestationDefinitionService.linkAttestation(anyString(), anyString()))
                .thenReturn(ServiceResult.success(true));

        baseRequest()
                .post("/test-attestation/link?holderId=test-participant")
                .then()
                .log().ifError()
                .statusCode(201);
    }

    @Test
    void linkAttestation_noHolderId_expect400() {
        when(attestationDefinitionService.linkAttestation(anyString(), anyString()))
                .thenReturn(ServiceResult.success(true));

        baseRequest()
                .post("/test-attestation/link")
                .then()
                .log().ifError()
                .statusCode(400);
    }

    @Test
    void linkAttestation_whenNotFound_expect400() {
        when(attestationDefinitionService.linkAttestation(anyString(), anyString()))
                .thenReturn(ServiceResult.notFound("foo"));
        baseRequest()
                .post("/test-attestation/link?holderId=test-participant")
                .then()
                .statusCode(400);
    }


    @Test
    void linkAttestation_alreadyLinked_expect204() {
        when(attestationDefinitionService.linkAttestation(anyString(), anyString()))
                .thenReturn(ServiceResult.success(false));
        baseRequest()
                .post("/test-attestation/link?holderId=test-participant")
                .then()
                .statusCode(204);
    }

    @Test
    void unlinkAttestation_expect200() {
        when(attestationDefinitionService.unlinkAttestation(anyString(), anyString()))
                .thenReturn(ServiceResult.success(true));

        baseRequest()
                .post("/test-attestation/unlink?holderId=test-participant")
                .then()
                .statusCode(200);
    }

    @Test
    void unlinkAttestation_noHolderId_expect400() {
        when(attestationDefinitionService.unlinkAttestation(anyString(), anyString()))
                .thenReturn(ServiceResult.success(true));

        baseRequest()
                .post("/test-attestation/unlink")
                .then()
                .log().ifError()
                .statusCode(400);
    }

    @Test
    void unlinkAttestation_holderNotFound_expect400() {
        when(attestationDefinitionService.unlinkAttestation(anyString(), anyString()))
                .thenReturn(ServiceResult.notFound("foo"));
        baseRequest()
                .post("/test-attestation/unlink?participantId=test-participant")
                .then()
                .statusCode(400);
    }


    @Test
    void unlinkAttestation_notLinked_expect204() {
        when(attestationDefinitionService.unlinkAttestation(anyString(), anyString()))
                .thenReturn(ServiceResult.success(false));
        baseRequest()
                .post("/test-attestation/unlink?holderId=test-participant")
                .then()
                .statusCode(204);
    }

    @Test
    void createAttestationDefinition_expect201() {
        var def = new AttestationDefinition("test-id", "test-type", Map.of());
        when(attestationDefinitionService.createAttestation(eq(def)))
                .thenReturn(ServiceResult.success());

        baseRequest()
                .body(def)
                .post()
                .then()
                .statusCode(201)
                .header("Location", Matchers.endsWith("/attestations/test-id"));
    }

    @Test
    void createAttestationDefinition_whenExists_expectConflict() {
        var def = new AttestationDefinition("test-id", "test-type", Map.of());
        when(attestationDefinitionService.createAttestation(eq(def)))
                .thenReturn(ServiceResult.conflict("foo"));

        baseRequest()
                .body(def)
                .post()
                .then()
                .statusCode(409);
    }

    @Test
    void deleteAttestationDefinition() {
        when(attestationDefinitionService.deleteAttestation(anyString()))
                .thenReturn(ServiceResult.success());

        baseRequest()
                .delete("/test-attestation-definition")
                .then()
                .statusCode(204);
    }


    @Test
    void deleteAttestationDefinition_whenNotFound_expect404() {
        when(attestationDefinitionService.deleteAttestation(anyString()))
                .thenReturn(ServiceResult.notFound("foo"));

        baseRequest()
                .delete("/notexist-attestation-definition")
                .then()
                .statusCode(400);
    }

    @Test
    void getAttestationDefinitionsForHolder() {
        when(attestationDefinitionService.getAttestationsForHolder(anyString()))
                .thenReturn(ServiceResult.success(List.of(
                        new AttestationDefinition("test-id", "test-type", Map.of()),
                        new AttestationDefinition("test-id2", "test-type", Map.of())))
                );

        var attestations = baseRequest()
                .get("?holderId=test-participant")
                .then()
                .statusCode(200)
                .extract().body().as(AttestationDefinition[].class);

        assertThat(attestations).hasSize(2);
    }

    @Test
    void getAttestationDefinitionsForParticipant_whenHolderIdMissing_expect400() {

        baseRequest()
                .get()
                .then()
                .statusCode(400);

        verifyNoInteractions(attestationDefinitionService);
    }


    @Test
    void getAttestationDefinitionsForHolder_noResult_expect200() {
        when(attestationDefinitionService.getAttestationsForHolder(anyString()))
                .thenReturn(ServiceResult.success(List.of()));

        var attestations = baseRequest()
                .get("?holderId=test-participant")
                .then()
                .statusCode(200)
                .extract().body().as(AttestationDefinition[].class);

        assertThat(attestations).isEmpty();
    }

    @Test
    void getAttestationDefinitionsForParticipant_holderNotFound_expect400() {
        baseRequest()
                .get("?holder=test-participant")
                .then()
                .statusCode(400);
    }

    @Test
    void queryAttestationDefinitions() {
        var definitions = List.of(
                new AttestationDefinition("test-id1", "test-type", Map.of()),
                new AttestationDefinition("test-id2", "test-type", Map.of())
        );
        when(attestationDefinitionService.queryAttestations(any()))
                .thenReturn(ServiceResult.success(definitions));
        var query = QuerySpec.max();
        var attestationDefs = baseRequest()
                .body(query)
                .post("/query")
                .then()
                .statusCode(200)
                .extract().body().as(AttestationDefinition[].class);

        assertThat(attestationDefs).hasSize(2);
    }

    @Test
    void queryAttestationDefinitions_whenNoResult_expect200() {

        when(attestationDefinitionService.queryAttestations(any()))
                .thenReturn(ServiceResult.success(List.of()));
        var query = QuerySpec.max();
        var attestationDefs = baseRequest()
                .body(query)
                .post("/query")
                .then()
                .statusCode(200)
                .extract().body().as(AttestationDefinition[].class);

        assertThat(attestationDefs).isEmpty();
    }

    @Test
    void queryAttestationDefinitions_whenInvalidField_expect400() {
        when(attestationDefinitionService.queryAttestations(any()))
                .thenReturn(ServiceResult.badRequest("foo"));

        var query = QuerySpec.max();
        baseRequest()
                .body(query)
                .post("/query")
                .then()
                .statusCode(400);
    }

    @Override
    protected Object controller() {
        return new IssuerAttestationAdminApiController(attestationDefinitionService);
    }

    private RequestSpecification baseRequest() {
        return given()
                .contentType("application/json")
                .baseUri("http://localhost:" + port + Versions.UNSTABLE + "/attestations")
                .when();
    }
}