/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.api.verifiablecredentials.v1;

import io.restassured.specification.RequestSpecification;
import org.eclipse.edc.identityhub.spi.AuthorizationService;
import org.eclipse.edc.identityhub.spi.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.store.CredentialStore;
import org.eclipse.edc.identitytrust.model.CredentialFormat;
import org.eclipse.edc.identitytrust.model.CredentialSubject;
import org.eclipse.edc.identitytrust.model.Issuer;
import org.eclipse.edc.identitytrust.model.VerifiableCredential;
import org.eclipse.edc.identitytrust.model.VerifiableCredentialContainer;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class VerifiableCredentialsApiControllerTest extends RestControllerTestBase {

    private static final String CREDENTIAL_ID = "test-credential-id";
    private final CredentialStore credentialStore = mock();
    private final AuthorizationService authorizationService = mock();

    @BeforeEach
    void setUp() {
        when(authorizationService.isAuthorized(any(), anyString(), any())).thenReturn(ServiceResult.success());
    }

    @Test
    void findById() {
        var credential = createCredential("VerifiableCredential").build();
        when(credentialStore.query(any())).thenReturn(StoreResult.success(List.of(credential)));

        var result = baseRequest()
                .get("/%s".formatted(CREDENTIAL_ID))
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract().body().as(VerifiableCredentialResource.class);

        assertThat(result).usingRecursiveComparison().ignoringFields("clock").isEqualTo(credential);
        verify(credentialStore).query(any());
        verifyNoMoreInteractions(credentialStore);
    }


    @Test
    void findById_unauthorized403() {
        when(authorizationService.isAuthorized(any(), anyString(), any())).thenReturn(ServiceResult.unauthorized("test-message"));
        baseRequest()
                .get("/%s".formatted(CREDENTIAL_ID))
                .then()
                .log().ifValidationFails()
                .statusCode(403);
        verify(authorizationService).isAuthorized(any(), anyString(), eq(VerifiableCredentialResource.class));
        verifyNoMoreInteractions(credentialStore, authorizationService);
    }

    @Test
    void findById_whenNotExists_expect404() {
        when(credentialStore.query(any())).thenReturn(StoreResult.success(List.of()));

        baseRequest()
                .get("/%s".formatted(CREDENTIAL_ID))
                .then()
                .log().ifValidationFails()
                .statusCode(404);

        verify(credentialStore).query(any());
        verifyNoMoreInteractions(credentialStore);
    }

    @Test
    void findByType() {
        var credential1 = createCredential("test-type").build();
        var credential2 = createCredential("test-type").build();
        when(credentialStore.query(any())).thenReturn(StoreResult.success(List.of(credential1, credential2)));

        var result = baseRequest()
                .get("?type=test-type")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract().body().as(VerifiableCredentialResource[].class);

        assertThat(result).usingRecursiveFieldByFieldElementComparatorIgnoringFields("clock").containsExactlyInAnyOrder(credential1, credential2);
        verify(credentialStore).query(any());
        verifyNoMoreInteractions(credentialStore);
    }

    @Test
    void findByType_unauthorized403() {
        var credential1 = createCredential("test-type").build();
        var credential2 = createCredential("test-type").build();
        when(credentialStore.query(any())).thenReturn(StoreResult.success(List.of(credential1, credential2)));
        when(authorizationService.isAuthorized(any(), eq(credential1.getId()), any())).thenReturn(ServiceResult.unauthorized("test-message"));

        var result = baseRequest()
                .get("?type=test-type")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract().body().as(VerifiableCredentialResource[].class);

        assertThat(result).usingRecursiveFieldByFieldElementComparatorIgnoringFields("clock").containsExactlyInAnyOrder(credential2);
        verify(credentialStore).query(any());
        verifyNoMoreInteractions(credentialStore);
    }

    @Test
    void findByType_noResult() {
        when(credentialStore.query(any())).thenReturn(StoreResult.success(List.of()));

        var result = baseRequest()
                .get("?type=test-type")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract().body().as(VerifiableCredentialResource[].class);

        assertThat(result).isEmpty();
        verify(credentialStore).query(any());
        verifyNoMoreInteractions(credentialStore);
    }

    @Test
    void deleteCredential() {
        when(credentialStore.deleteById(CREDENTIAL_ID)).thenReturn(StoreResult.success());

        baseRequest()
                .delete("/%s".formatted(CREDENTIAL_ID))
                .then()
                .log().ifValidationFails()
                .statusCode(204);

        verify(credentialStore).deleteById(eq(CREDENTIAL_ID));
        verifyNoMoreInteractions(credentialStore);
    }

    @Test
    void deleteCredential_unauthorized403() {
        when(authorizationService.isAuthorized(any(), anyString(), any())).thenReturn(ServiceResult.unauthorized("test-message"));

        baseRequest()
                .delete("/%s".formatted(CREDENTIAL_ID))
                .then()
                .log().ifValidationFails()
                .statusCode(403);
        verify(authorizationService).isAuthorized(any(), anyString(), eq(VerifiableCredentialResource.class));
        verifyNoMoreInteractions(credentialStore, authorizationService);
    }

    @Test
    void deleteCredential_whenNotExists() {
        when(credentialStore.deleteById(CREDENTIAL_ID)).thenReturn(StoreResult.notFound("test-message"));

        baseRequest()
                .delete("/%s".formatted(CREDENTIAL_ID))
                .then()
                .log().ifValidationFails()
                .statusCode(404);

        verify(credentialStore).deleteById(eq(CREDENTIAL_ID));
        verifyNoMoreInteractions(credentialStore);
    }

    @Override
    protected Object controller() {
        return new VerifiableCredentialsApiController(credentialStore, authorizationService);
    }

    private VerifiableCredentialResource.Builder createCredential(String... types) {
        var cred = VerifiableCredential.Builder.newInstance()
                .types(Arrays.asList(types))
                .issuer(new Issuer("test-issuer", Map.of()))
                .issuanceDate(Instant.now())
                .credentialSubject(CredentialSubject.Builder.newInstance().id("test-cred-id").claim("test-claim", "test-value").build())
                .build();
        return VerifiableCredentialResource.Builder.newInstance()
                .credential(new VerifiableCredentialContainer("foobar", CredentialFormat.JSON_LD, cred))
                .holderId("test-holder")
                .issuerId("test-issuer");
    }

    private RequestSpecification baseRequest() {
        return given()
                .contentType("application/json")
                .baseUri("http://localhost:" + port + "/v1/participants/test-participant/credentials")
                .when();
    }
}