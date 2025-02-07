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
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.identityhub.api.Versions;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.issuerservice.api.admin.credentials.v1.unstable.model.VerifiableCredentialDto;
import org.eclipse.edc.issuerservice.spi.statuslist.StatusListService;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesRegex;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class IssuerCredentialsAdminApiControllerTest extends RestControllerTestBase {

    private final StatusListService statuslistService = mock();

    @Test
    void getAllCredentials() {

        when(statuslistService.getCredentialForParticipant(eq("test-participant")))
                .thenReturn(ServiceResult.success(List.of(createCredential(), createCredential())));
        var credentials = baseRequest()
                .get("/test-participant")
                .then()
                .statusCode(200)
                .extract().body().as(VerifiableCredentialDto[].class);

        assertThat(credentials).hasSize(2);
    }

    @Test
    void getAllCredentials_whenNoResult() {
        when(statuslistService.getCredentialForParticipant(eq("test-participant")))
                .thenReturn(ServiceResult.success(List.of()));
        var credentials = baseRequest()
                .get("/test-participant")
                .then()
                .statusCode(200)
                .extract().body().as(VerifiableCredentialDto[].class);

        assertThat(credentials).isEmpty();
    }

    @Test
    void queryCredentials() {
        when(statuslistService.queryCredentials(any(QuerySpec.class)))
                .thenReturn(ServiceResult.success(List.of(createCredential(), createCredential())));

        var credentials = baseRequest()
                .body("{}")
                .post("/query")
                .then()
                .statusCode(200)
                .extract().body().as(VerifiableCredentialDto[].class);

        assertThat(credentials).hasSize(2);
    }


    @Test
    void queryCredentials_whenNoResult() {
        when(statuslistService.queryCredentials(any(QuerySpec.class)))
                .thenReturn(ServiceResult.success(Collections.emptyList()));

        var credentials = baseRequest()
                .body("{}")
                .post("/query")
                .then()
                .statusCode(200)
                .extract().body().as(VerifiableCredentialDto[].class);

        assertThat(credentials).isEmpty();
    }

    @Test
    void revokeCredential_whenAlreadyRevoked() {
        when(statuslistService.revokeCredential(anyString()))
                .thenReturn(ServiceResult.success());

        baseRequest()
                .post("/test-credential/revoke")
                .then()
                .statusCode(204)
                .body(notNullValue());
    }


    @Test
    void revokeCredential_whenNotFound() {
        when(statuslistService.revokeCredential(anyString()))
                .thenReturn(ServiceResult.notFound("foo"));

        baseRequest()
                .post("/test-credential/revoke")
                .then()
                .statusCode(404)
                .body(containsString("not found"));
    }

    @Test
    void suspendCredential() {
        baseRequest()
                .post("/test-credential-id/suspend")
                .then()
                .statusCode(501);
        verifyNoInteractions(statuslistService);
    }

    @Test
    void resumeCredential() {
        baseRequest()
                .post("/test-credential-id/resume")
                .then()
                .statusCode(501);
        verifyNoInteractions(statuslistService);
    }

    @Test
    void checkRevocationStatus_whenNotRevoked() {
        when(statuslistService.getCredentialStatus(eq("test-credential-id")))
                .thenReturn(ServiceResult.success());
        baseRequest()
                .get("/test-credential-id/status")
                .then()
                .statusCode(200)
                .body(matchesRegex(".*\"status\":.*null.*"));
    }

    @Test
    void checkRevocationStatus_whenRevoked() {
        when(statuslistService.getCredentialStatus(eq("test-credential-id")))
                .thenReturn(ServiceResult.success("revocation"));

        baseRequest()
                .get("/test-credential-id/status")
                .then()
                .statusCode(200)
                .body(matchesRegex(".*\"status\":.*\"revocation\".*"));
    }

    @Test
    void checkRevocationStatus_whenNotFound() {
        when(statuslistService.getCredentialStatus(eq("test-credential-id")))
                .thenReturn(ServiceResult.notFound("foo"));

        baseRequest()
                .get("/test-credential-id/status")
                .then()
                .statusCode(404)
                .body(notNullValue());

    }

    @Override
    protected Object controller() {
        return new IssuerCredentialsAdminApiController(statuslistService);
    }

    private @NotNull VerifiableCredentialResource createCredential() {
        var cred = VerifiableCredential.Builder.newInstance()
                .issuanceDate(Instant.now())
                .id(UUID.randomUUID().toString())
                .type("VerifiableCredential")
                .credentialSubject(CredentialSubject.Builder.newInstance().id(UUID.randomUUID().toString()).claim("foo", "bar").build())
                .issuer(new Issuer(UUID.randomUUID().toString()))
                .build();
        return VerifiableCredentialResource.Builder.newInstance()
                .state(VcStatus.ISSUED)
                .issuerId("issuer-id")
                .holderId("holder-id")
                .credential(new VerifiableCredentialContainer("JWT_STRING", CredentialFormat.VC1_0_JWT, cred))
                .build();
    }

    private RequestSpecification baseRequest() {
        return given()
                .contentType("application/json")
                .baseUri("http://localhost:" + port + Versions.UNSTABLE + "/credentials")
                .when();
    }
}