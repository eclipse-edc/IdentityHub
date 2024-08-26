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

package org.eclipse.edc.identityhub.api.verifiablecredentials.v1.unstable;

import io.restassured.specification.RequestSpecification;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.identityhub.api.Versions;
import org.eclipse.edc.identityhub.api.v1.validation.VerifiableCredentialManifestValidator;
import org.eclipse.edc.identityhub.spi.AuthorizationService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.identityhub.spi.store.CredentialStore;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialManifest;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Violation;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.ServiceResult.unauthorized;
import static org.eclipse.edc.spi.result.StoreResult.alreadyExists;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ApiTest
class VerifiableCredentialsApiControllerTest extends RestControllerTestBase {

    private static final String PARTICIPANT_ID = "test-participant";
    private static final String CREDENTIAL_ID = "test-credential-id";
    private final CredentialStore credentialStore = mock();
    private final AuthorizationService authorizationService = mock();
    private final VerifiableCredentialManifestValidator validator = mock();
    private final TypeTransformerRegistry typeTransformerRegistry = mock();

    @BeforeEach
    void setUp() {
        when(authorizationService.isAuthorized(any(), anyString(), any())).thenReturn(ServiceResult.success());
    }

    @Override
    protected Object controller() {
        return new VerifiableCredentialsApiController(credentialStore, authorizationService, validator, typeTransformerRegistry);
    }

    private VerifiableCredential createCredential(String... types) {
        return VerifiableCredential.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .types(Arrays.asList(types))
                .issuer(new Issuer("test-issuer", Map.of()))
                .issuanceDate(Instant.now())
                .credentialSubject(CredentialSubject.Builder.newInstance().id("test-cred-id").claim("test-claim", "test-value").build())
                .build();
    }

    private VerifiableCredentialResource.Builder createCredentialResource(String... types) {
        var cred = createCredential(types);
        return VerifiableCredentialResource.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .credential(new VerifiableCredentialContainer("foobar", CredentialFormat.JSON_LD, cred))
                .holderId("test-holder")
                .issuerId("test-issuer");
    }

    private VerifiableCredentialManifest createManifest(VerifiableCredential credential) {
        return VerifiableCredentialManifest.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .participantId(PARTICIPANT_ID)
                .verifiableCredentialContainer(new VerifiableCredentialContainer("rawVc", CredentialFormat.JSON_LD, credential))
                .build();
    }

    private RequestSpecification baseRequest() {
        return given()
                .contentType("application/json")
                .baseUri("http://localhost:" + port + Versions.UNSTABLE + "/participants/" + Base64.getUrlEncoder().encodeToString(PARTICIPANT_ID.getBytes()) + "/credentials")
                .when();
    }

    @Nested
    class Create {
        @Test
        void success() {
            var credential = createCredential("type");
            var manifest = createManifest(credential);
            var resource = mock(VerifiableCredentialResource.class);
            when(validator.validate(any())).thenReturn(ValidationResult.success());
            when(authorizationService.isAuthorized(any(), eq(PARTICIPANT_ID), eq(ParticipantContext.class))).thenReturn(ServiceResult.success());
            when(typeTransformerRegistry.transform(any(), eq(VerifiableCredentialResource.class))).thenReturn(Result.success(resource));
            when(credentialStore.create(resource)).thenReturn(StoreResult.success());

            baseRequest()
                    .contentType(JSON)
                    .body(manifest)
                    .post()
                    .then()
                    .log().ifValidationFails()
                    .statusCode(204);
        }

        @Test
        void validationFails_returns400() {
            when(validator.validate(any())).thenReturn(ValidationResult.failure(new Violation("test-message", "test-path", "test-value")));

            baseRequest()
                    .contentType(JSON)
                    .body(createManifest(createCredential("type")))
                    .post()
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400);
        }

        @Test
        void notAuthorized_returns403() {
            when(validator.validate(any())).thenReturn(ValidationResult.success());
            when(authorizationService.isAuthorized(any(), eq(PARTICIPANT_ID), eq(ParticipantContext.class))).thenReturn(unauthorized("test-message"));

            baseRequest()
                    .contentType(JSON)
                    .body(createManifest(createCredential("type")))
                    .post()
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403);
        }

        @Test
        void transformFails_returns400() {
            when(validator.validate(any())).thenReturn(ValidationResult.success());
            when(authorizationService.isAuthorized(any(), eq(PARTICIPANT_ID), eq(ParticipantContext.class))).thenReturn(ServiceResult.success());
            when(typeTransformerRegistry.transform(any(), eq(VerifiableCredentialResource.class))).thenReturn(failure("transform-failure"));

            baseRequest()
                    .contentType(JSON)
                    .body(createManifest(createCredential("type")))
                    .post()
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400);
        }

        @Test
        void vcAlreadyExists_returns() {
            when(validator.validate(any())).thenReturn(ValidationResult.success());
            when(authorizationService.isAuthorized(any(), eq(PARTICIPANT_ID), eq(ParticipantContext.class))).thenReturn(ServiceResult.success());
            when(typeTransformerRegistry.transform(any(), eq(VerifiableCredentialResource.class))).thenReturn(Result.success(mock(VerifiableCredentialResource.class)));
            when(credentialStore.create(any())).thenReturn(alreadyExists("already-exists"));

            baseRequest()
                    .contentType(JSON)
                    .body(createManifest(createCredential("type")))
                    .post()
                    .then()
                    .log().ifValidationFails()
                    .statusCode(409);
        }
    }

    @Nested
    class Update {
        @Test
        void success() {
            var credential = createCredential("type");
            var manifest = createManifest(credential);
            var resource = mock(VerifiableCredentialResource.class);
            when(validator.validate(any())).thenReturn(ValidationResult.success());
            when(authorizationService.isAuthorized(any(), eq(manifest.getId()), eq(VerifiableCredentialResource.class))).thenReturn(ServiceResult.success());
            when(typeTransformerRegistry.transform(any(), eq(VerifiableCredentialResource.class))).thenReturn(Result.success(resource));
            when(credentialStore.update(resource)).thenReturn(StoreResult.success());

            baseRequest()
                    .contentType(JSON)
                    .body(manifest)
                    .put()
                    .then()
                    .log().ifValidationFails()
                    .statusCode(204);
        }

        @Test
        void validationFails_returns400() {
            when(validator.validate(any())).thenReturn(ValidationResult.failure(new Violation("test-message", "test-path", "test-value")));

            baseRequest()
                    .contentType(JSON)
                    .body(createManifest(createCredential("type")))
                    .put()
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400);
        }

        @Test
        void notAuthorized_returns403() {
            when(validator.validate(any())).thenReturn(ValidationResult.success());
            when(authorizationService.isAuthorized(any(), any(), eq(VerifiableCredentialResource.class))).thenReturn(unauthorized("test-message"));

            baseRequest()
                    .contentType(JSON)
                    .body(createManifest(createCredential("type")))
                    .put()
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403);
        }

        @Test
        void transformFails_returns400() {
            var manifest = createManifest(createCredential("type"));
            when(validator.validate(any())).thenReturn(ValidationResult.success());
            when(authorizationService.isAuthorized(any(), eq(manifest.getId()), eq(VerifiableCredentialResource.class))).thenReturn(ServiceResult.success());
            when(typeTransformerRegistry.transform(any(), eq(VerifiableCredentialResource.class))).thenReturn(failure("transform-failure"));

            baseRequest()
                    .contentType(JSON)
                    .body(manifest)
                    .put()
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400);
        }

        @Test
        void vcAlreadyExists_returns() {
            var manifest = createManifest(createCredential("type"));
            when(validator.validate(any())).thenReturn(ValidationResult.success());
            when(authorizationService.isAuthorized(any(), eq(manifest.getId()), eq(VerifiableCredentialResource.class))).thenReturn(ServiceResult.success());
            when(typeTransformerRegistry.transform(any(), eq(VerifiableCredentialResource.class))).thenReturn(Result.success(mock(VerifiableCredentialResource.class)));
            when(credentialStore.create(any())).thenReturn(alreadyExists("already-exists"));

            baseRequest()
                    .contentType(JSON)
                    .body(manifest)
                    .put()
                    .then()
                    .log().ifValidationFails()
                    .statusCode(500);
        }
    }

    @Nested
    class FindByType {
        @Test
        void success() {
            var credential1 = createCredentialResource("test-type").build();
            var credential2 = createCredentialResource("test-type").build();
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
        void notAuthorized_returns403() {
            var credential1 = createCredentialResource("test-type").build();
            var credential2 = createCredentialResource("test-type").build();
            when(credentialStore.query(any())).thenReturn(StoreResult.success(List.of(credential1, credential2)));
            when(authorizationService.isAuthorized(any(), eq(credential1.getId()), eq(VerifiableCredentialResource.class))).thenReturn(unauthorized("test-message"));
            when(authorizationService.isAuthorized(any(), eq(credential2.getId()), eq(VerifiableCredentialResource.class))).thenReturn(ServiceResult.success());

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
        void emptyResult() {
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
    }

    @Nested
    class Delete {

        @Test
        void success() {
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
        void notAuthorized_returns403() {
            when(authorizationService.isAuthorized(any(), anyString(), any())).thenReturn(unauthorized("test-message"));

            baseRequest()
                    .delete("/%s".formatted(CREDENTIAL_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403);
            verify(authorizationService).isAuthorized(any(), anyString(), eq(VerifiableCredentialResource.class));
            verifyNoMoreInteractions(credentialStore, authorizationService);
        }

        @Test
        void idDoesNotExist_returns404() {
            when(credentialStore.deleteById(CREDENTIAL_ID)).thenReturn(StoreResult.notFound("test-message"));

            baseRequest()
                    .delete("/%s".formatted(CREDENTIAL_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(404);

            verify(credentialStore).deleteById(eq(CREDENTIAL_ID));
            verifyNoMoreInteractions(credentialStore);
        }
    }

    @Nested
    class FindById {

        @Test
        void success() {
            var credential = createCredentialResource("VerifiableCredential").build();
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
        void notAuthorized_returns403() {
            when(authorizationService.isAuthorized(any(), anyString(), any())).thenReturn(unauthorized("test-message"));
            baseRequest()
                    .get("/%s".formatted(CREDENTIAL_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403);
            verify(authorizationService).isAuthorized(any(), anyString(), eq(VerifiableCredentialResource.class));
            verifyNoMoreInteractions(credentialStore, authorizationService);
        }

        @Test
        void idDoesNotExist_returns404() {
            when(credentialStore.query(any())).thenReturn(StoreResult.success(List.of()));

            baseRequest()
                    .get("/%s".formatted(CREDENTIAL_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(404);

            verify(credentialStore).query(any());
            verifyNoMoreInteractions(credentialStore);
        }
    }
}