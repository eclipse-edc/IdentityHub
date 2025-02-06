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
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.identityhub.api.Versions;
import org.eclipse.edc.issuerservice.spi.statuslist.StatusListService;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;

class IssuerCredentialsAdminApiControllerTest extends RestControllerTestBase {

    private final StatusListService statuslistServiceMock = mock();

    @Test
    void getAllCredentials() {
        var credentials = baseRequest()
                .get("/test-participant")
                .then()
                .statusCode(200)
                .extract().body().as(VerifiableCredential[].class);

        assertThat(credentials).hasSize(2);
    }

    @Test
    void getAllCredentials_whenNoResult() {
        var credentials = baseRequest()
                .get("/test-participant")
                .then()
                .statusCode(200)
                .extract().body().as(VerifiableCredential[].class);

        assertThat(credentials).hasSize(2);
    }

    @Test
    void queryCredentials() {
    }

    @Test
    void revokeCredential_whenAlreadyRevoked() {
    }

    @Test
    void revokeCredential_whenNotRevoked() {
    }

    @Test
    void revokeCredential_whenNotFoundRevoked() {
    }

    @Test
    void suspendCredential() {
        baseRequest()
                .post("/test-credential-id/suspend")
                .then()
                .statusCode(501);
    }

    @Test
    void resumeCredential() {
        baseRequest()
                .post("/test-credential-id/resume")
                .then()
                .statusCode(501);
    }

    @Test
    void checkRevocationStatus_whenNotRevoked() {
        baseRequest()
                .post("/test-credential-id/status")
                .then()
                .statusCode(200)
                .body(isNull());
    }

    @Test
    void checkRevocationStatus_whenRevoked() {
        baseRequest()
                .post("/test-credential-id/status")
                .then()
                .statusCode(200)
                .body(equalTo("revocation"));
    }

    @Test
    void checkRevocationStatus_whenNotFound() {
        baseRequest()
                .post("/test-credential-id/status")
                .then()
                .statusCode(404)
                .body(notNullValue());
    }

    @Override
    protected Object controller() {
        return new IssuerCredentialsAdminApiController(statuslistServiceMock);
    }

    private RequestSpecification baseRequest() {
        return given()
                .contentType("application/json")
                .baseUri("http://localhost:" + port + Versions.UNSTABLE + "/credentials")
                .when();
    }
}