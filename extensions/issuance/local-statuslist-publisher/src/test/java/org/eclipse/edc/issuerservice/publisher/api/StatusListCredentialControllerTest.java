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

package org.eclipse.edc.issuerservice.publisher.api;

import io.restassured.specification.RequestSpecification;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ApiTest
class StatusListCredentialControllerTest extends RestControllerTestBase {


    private static final String CREDENTIAL_ID = "test-credential-id";
    private static final String RAW_CREDENTIAL = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    private final CredentialStore store = mock();
    private final Monitor monitor = mock();
    private final VerifiableCredential credential = VerifiableCredential.Builder.newInstance()
            .type("TestCredential")
            .credentialSubject(new CredentialSubject())
            .issuer(new Issuer("did:web:issuer"))
            .issuanceDate(Instant.now())
            .build();
    private final VerifiableCredentialResource credentialResource = VerifiableCredentialResource.Builder.newInstance()
            .issuerId("did:web:issuer")
            .holderId("did:web:issuer")
            .metadata("publicUrl", "http://localhost:%s/statuslist/foobar/%s".formatted(port, CREDENTIAL_ID))
            .credential(new VerifiableCredentialContainer(RAW_CREDENTIAL, CredentialFormat.VC1_0_JWT, credential))
            .build();

    @BeforeEach
    void setUp() {
        when(store.query(any())).thenReturn(StoreResult.success(List.of(credentialResource)));
    }

    @Test
    void resolveStatusListCredential_noAcceptHeader_expectDefaultFormat() {
        baseRequest()
                .get("/foobar/" + CREDENTIAL_ID)
                .then()
                .statusCode(200)
                .header("Content-Type", "application/vc+jwt")
                .body(equalTo(RAW_CREDENTIAL));
        verifyNoInteractions(monitor);
    }

    @Test
    void resolveStatusListCredential_whenUrlNotEqual() {
        baseRequest()
                .get("/foobar/barbaz/" + CREDENTIAL_ID)
                .then()
                .statusCode(200)
                .body(equalTo(RAW_CREDENTIAL));
        verify(monitor).warning(contains("not equal to the request URL"));
    }


    @Test
    void resolveStatusListCredential_acceptJson() {
        var body = baseRequest()
                .header("Accept", "application/json")
                .get("/foobar/" + CREDENTIAL_ID)
                .then()
                .statusCode(200)
                .header("Content-Type", APPLICATION_JSON)
                // this implicitly verifies that it is JSON
                .extract().body().as(VerifiableCredential.class);

        assertThat(body).usingRecursiveComparison().isEqualTo(credential);
        verifyNoInteractions(monitor);
    }

    @Test
    void resolveStatusListCredential_acceptJwt() {
        baseRequest()
                .header("Accept", "application/vc+jwt")
                .get("/foobar/" + CREDENTIAL_ID)
                .then()
                .statusCode(200)
                .header("Content-Type", "application/vc+jwt")
                .body(equalTo(RAW_CREDENTIAL));
        verifyNoInteractions(monitor);
    }

    @Test
    void resolveStatusListCredential_invalidAcceptHeader_expect415() {
        baseRequest()
                .header("Accept", "application/vc+cose")
                .get("/foobar/" + CREDENTIAL_ID)
                .then()
                .statusCode(415);
        verifyNoInteractions(monitor);
    }

    @Test
    void resolveStatusListCredential_credentialNotFound_expect404() {
        when(store.query(any())).thenReturn(StoreResult.success(List.of()));
        baseRequest()
                .get("/foobar/nonexist-credential")
                .then()
                .statusCode(404);
        verifyNoInteractions(monitor);
    }

    @Test
    void resolveStatusListCredential_moreThanOneCredential_expect409() {
        when(store.query(any())).thenReturn(StoreResult.success(List.of(credentialResource, credentialResource)));
        baseRequest()
                .get("/foobar/" + CREDENTIAL_ID)
                .then()
                .statusCode(409);
        verifyNoInteractions(monitor);
    }


    @Override
    protected Object controller() {
        return new StatusListCredentialController(store, monitor, () -> objectMapper);
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:%s/statuslist".formatted(port));
    }
}