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
import org.eclipse.edc.identityhub.spi.authorization.AuthorizationService;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionService;
import org.eclipse.edc.issuerservice.spi.issuance.model.AttestationDefinition;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ApiTest
class IssuerAttestationAdminApiControllerTest extends RestControllerTestBase {

    private static final String PARTICIPANT_ID = "test-participant";
    private static final String PARTICIPANT_ID_ENCODED = Base64.getUrlEncoder().encodeToString(PARTICIPANT_ID.getBytes());
    private final AttestationDefinitionService attestationDefinitionService = mock();
    private final AuthorizationService authorizationService = mock();

    @BeforeEach
    void setUp() {
        when(authorizationService.isAuthorized(any(), anyString(), any())).thenReturn(ServiceResult.success());
    }

    @Test
    void createAttestationDefinition_expect201() {
        var def = createAttestationDefinition("test-id", "test-type", Map.of());
        when(attestationDefinitionService.createAttestation(refEq(def)))
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
        var def = createAttestationDefinition("test-id", "test-type", Map.of());
        when(attestationDefinitionService.createAttestation(refEq(def)))
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
                .statusCode(404);
    }

    @Test
    void queryAttestationDefinitions() {
        var definitions = List.of(
                createAttestationDefinition("test-id1", "test-type", Map.of()),
                createAttestationDefinition("test-id2", "test-type", Map.of())
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
        return new IssuerAttestationAdminApiController(authorizationService, attestationDefinitionService);
    }

    private RequestSpecification baseRequest() {
        return given()
                .contentType("application/json")
                .baseUri("http://localhost:" + port + Versions.UNSTABLE + "/participants/%s/attestations".formatted(PARTICIPANT_ID_ENCODED))
                .when();
    }

    private AttestationDefinition createAttestationDefinition(String id, String type, Map<String, Object> configuration) {
        return AttestationDefinition.Builder.newInstance()
                .id(id)
                .attestationType(type)
                .configuration(configuration)
                .participantContextId(PARTICIPANT_ID)
                .build();
    }

}