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

package org.eclipse.edc.issuerservice.api.admin.issuance.v1.unstable;

import io.restassured.specification.RequestSpecification;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.identityhub.api.Versions;
import org.eclipse.edc.identityhub.spi.authorization.AuthorizationService;
import org.eclipse.edc.issuerservice.api.admin.issuance.v1.unstable.model.IssuanceProcessDto;
import org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcess;
import org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcessStates;
import org.eclipse.edc.issuerservice.spi.issuance.process.IssuanceProcessService;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ComponentTest
class IssuanceProcessAdminApiControllerTest extends RestControllerTestBase {

    private final IssuanceProcessService issuanceProcessService = mock();
    private final AuthorizationService authService = mock();

    @BeforeEach
    void setUp() {
        when(authService.isAuthorized(any(), anyString(), any())).thenReturn(ServiceResult.success());
    }

    @Test
    void getIssuanceProcessById() {
        var test = createIssuanceProcess();
        when(issuanceProcessService.findById(any())).thenReturn(test);

        var response = baseRequest()
                .get("/test-id")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract().body().as(IssuanceProcessDto.class);

        assertThat(response).usingRecursiveComparison()
                .ignoringFields("state")
                .isEqualTo(test);
    }

    @Test
    void getIssuanceProcessById_notFound() {
        when(issuanceProcessService.findById(any())).thenReturn(null);

        baseRequest()
                .get("/test-id")
                .then()
                .log().ifValidationFails()
                .statusCode(404);
    }

    @Test
    void queryIssuanceProcesses() {
        var test = createIssuanceProcess();
        when(issuanceProcessService.search(any())).thenReturn(ServiceResult.success(List.of(test)));

        var dto = baseRequest()
                .body(QuerySpec.Builder.newInstance().build())
                .post("/query")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract().body().as(IssuanceProcessDto[].class);

        assertThat(dto).hasSize(1)
                .usingRecursiveComparison()
                .ignoringFields("state")
                .isEqualTo(test);
    }


    @Test
    void queryIssuanceProcesses_noneFound() {
        when(issuanceProcessService.search(any())).thenReturn(ServiceResult.success(List.of()));

        var dto = baseRequest()
                .body(QuerySpec.Builder.newInstance().build())
                .post("/query")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract().body().as(IssuanceProcessDto[].class);

        assertThat(dto).isEmpty();
    }

    @Override
    protected Object controller() {
        return new IssuanceProcessAdminApiController(issuanceProcessService, authService);
    }

    private RequestSpecification baseRequest(String participantContextId) {
        return given()
                .contentType("application/json")
                .baseUri("http://localhost:" + port + Versions.UNSTABLE + "/participants/%s/issuanceprocesses".formatted(participantContextId))
                .when();
    }

    private RequestSpecification baseRequest() {
        return baseRequest("test-issuer");
    }

    private IssuanceProcess createIssuanceProcess() {
        return IssuanceProcess.Builder.newInstance()
                .id("test-id")
                .state(IssuanceProcessStates.APPROVED.code())
                .holderId("test-participant")
                .participantContextId("test-issuer")
                .holderPid("test-holder")
                .claims(Map.of("test-claim", "test-value"))
                .credentialDefinitions(List.of("test-cred-def"))
                .credentialFormats(Map.of("test-format", CredentialFormat.VC1_0_JWT))
                .build();
    }
}