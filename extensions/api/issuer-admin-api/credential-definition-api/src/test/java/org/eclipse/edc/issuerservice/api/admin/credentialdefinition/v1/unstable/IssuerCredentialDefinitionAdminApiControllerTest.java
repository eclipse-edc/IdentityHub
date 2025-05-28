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

package org.eclipse.edc.issuerservice.api.admin.credentialdefinition.v1.unstable;

import io.restassured.specification.RequestSpecification;
import org.eclipse.edc.identityhub.api.Versions;
import org.eclipse.edc.identityhub.spi.authorization.AuthorizationService;
import org.eclipse.edc.issuerservice.spi.issuance.credentialdefinition.CredentialDefinitionService;
import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialDefinition;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat.VC1_0_JWT;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ComponentTest
class IssuerCredentialDefinitionAdminApiControllerTest extends RestControllerTestBase {

    private static final String PARTICIPANT_ID = "test-participant";
    private static final String PARTICIPANT_ID_ENCODED = Base64.getUrlEncoder().encodeToString(PARTICIPANT_ID.getBytes());

    private final CredentialDefinitionService credentialDefinitionService = mock();
    private final AuthorizationService authorizationService = mock();

    @BeforeEach
    void setUp() {
        when(authorizationService.isAuthorized(any(), anyString(), any())).thenReturn(ServiceResult.success());
    }

    @Test
    void createCredentialDefinition() {
        when(credentialDefinitionService.createCredentialDefinition(any())).thenReturn(ServiceResult.success());

        baseRequest()
                .body(credentialDefinition())
                .post()
                .then()
                .log().ifValidationFails()
                .statusCode(201)
                .header("Location", endsWith("/credentialdefinitions/test-id"))
                .body(emptyString());
    }

    @Test
    void createCredentialDefinition_whenAlreadyExists() {
        when(credentialDefinitionService.createCredentialDefinition(any())).thenReturn(ServiceResult.conflict("already exists"));

        baseRequest()
                .body(credentialDefinition())
                .post()
                .then()
                .log().ifValidationFails()
                .statusCode(409)
                .body(notNullValue());
    }

    @Test
    void updateCredentialDefinition() {
        when(credentialDefinitionService.updateCredentialDefinition(any())).thenReturn(ServiceResult.success());

        baseRequest()
                .body(credentialDefinition())
                .put()
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body(emptyString());
    }

    @Test
    void updateCredentialDefinition_notFound() {
        when(credentialDefinitionService.updateCredentialDefinition(any())).thenReturn(ServiceResult.notFound("not found"));

        baseRequest()
                .body(credentialDefinition())
                .put()
                .then()
                .log().ifValidationFails()
                .statusCode(404)
                .body(notNullValue());
    }

    @Test
    void getCredentialDefinitionById() {
        var test = credentialDefinition();
        when(credentialDefinitionService.findCredentialDefinitionById(any())).thenReturn(ServiceResult.success(test));

        var response = baseRequest()
                .get("/test-id")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract().body().as(CredentialDefinition.class);

        assertThat(response).usingRecursiveComparison().isEqualTo(test);
    }

    @Test
    void getCredentialDefinitionById_notFound() {
        when(credentialDefinitionService.findCredentialDefinitionById(any())).thenReturn(ServiceResult.notFound("not found"));

        baseRequest()
                .get("/test-id")
                .then()
                .log().ifValidationFails()
                .statusCode(404);
    }

    @Test
    void queryCredentialDefinitions() {
        var test = credentialDefinition();
        when(credentialDefinitionService.queryCredentialDefinitions(any())).thenReturn(ServiceResult.success(Set.of(test)));

        var dto = baseRequest()
                .body(QuerySpec.Builder.newInstance().build())
                .post("/query")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract().body().as(CredentialDefinition[].class);

        assertThat(dto).hasSize(1)
                .usingRecursiveComparison().isEqualTo(test);
    }


    @Test
    void queryCredentialDefinitions_noneFound() {
        when(credentialDefinitionService.queryCredentialDefinitions(any())).thenReturn(ServiceResult.success(Set.of()));

        var dto = baseRequest()
                .body(QuerySpec.Builder.newInstance().build())
                .post("/query")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract().body().as(CredentialDefinition[].class);

        assertThat(dto).isEmpty();
    }

    @Override
    protected Object controller() {
        return new IssuerCredentialDefinitionAdminApiController(authorizationService, credentialDefinitionService);
    }

    private RequestSpecification baseRequest() {
        return given()
                .contentType("application/json")
                .baseUri("http://localhost:" + port + Versions.UNSTABLE + "/participants/%s/credentialdefinitions".formatted(PARTICIPANT_ID_ENCODED))
                .when();
    }


    private CredentialDefinition credentialDefinition() {
        return CredentialDefinition.Builder.newInstance()
                .id("test-id")
                .credentialType("Membership")
                .jsonSchema("json-schema")
                .jsonSchemaUrl("json-schema-url")
                .participantContextId(PARTICIPANT_ID)
                .formatFrom(VC1_0_JWT)
                .build();
    }
}