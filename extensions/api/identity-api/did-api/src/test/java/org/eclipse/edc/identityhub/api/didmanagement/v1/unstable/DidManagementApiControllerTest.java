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

package org.eclipse.edc.identityhub.api.didmanagement.v1.unstable;

import io.restassured.specification.RequestSpecification;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.identityhub.api.Versions;
import org.eclipse.edc.identityhub.spi.authorization.AuthorizationService;
import org.eclipse.edc.identityhub.spi.did.DidDocumentService;
import org.eclipse.edc.identityhub.spi.did.model.DidResource;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.identityhub.api.didmanagement.v1.unstable.TestFunctions.createDidDocument;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ApiTest
class DidManagementApiControllerTest extends RestControllerTestBase {

    public static final String TEST_DID = "did:web:host%3A1234:test-did";
    private final DidDocumentService didDocumentServiceMock = mock();
    private final AuthorizationService authService = mock();

    @BeforeEach
    void setUp() {
        when(authService.isAuthorized(any(), anyString(), any())).thenReturn(ServiceResult.success());
    }

    @Override
    protected DidManagementApiController controller() {
        return new DidManagementApiController(didDocumentServiceMock, authService);
    }

    private RequestSpecification baseRequest() {
        return given()
                .contentType("application/json")
                .baseUri("http://localhost:" + port + Versions.UNSTABLE + "/participants/test-participant/dids")
                .when();
    }

    @Nested
    class RemoveEndpoint {
        @Test
        void removeEndpoint() {
            when(didDocumentServiceMock.removeService(eq(TEST_DID), anyString())).thenReturn(ServiceResult.success());
            baseRequest()
                    .body(new DidRequestPayload(TEST_DID))
                    .delete("/%s/endpoints?serviceId=test-service-id".formatted(Base64.getUrlEncoder().encodeToString(TEST_DID.getBytes())))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(anyOf(equalTo(200), equalTo(204)));
            verify(didDocumentServiceMock).removeService(eq(TEST_DID), eq("test-service-id"));
            verifyNoMoreInteractions(didDocumentServiceMock);
        }

        @Test
        void removeEndpoint_unauthorized403() {
            when(authService.isAuthorized(any(), anyString(), any())).thenReturn(ServiceResult.unauthorized("test message"));
            baseRequest()
                    .body(new DidRequestPayload(TEST_DID))
                    .delete("/%s/endpoints?serviceId=test-service-id".formatted(Base64.getUrlEncoder().encodeToString(TEST_DID.getBytes())))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403);
            verify(authService).isAuthorized(any(), anyString(), eq(DidResource.class));
            verifyNoMoreInteractions(didDocumentServiceMock, authService);
        }

        @Test
        void removeEndpoint_withAutoPublish() {
            when(didDocumentServiceMock.removeService(eq(TEST_DID), anyString())).thenReturn(ServiceResult.success());
            when(didDocumentServiceMock.publish(eq(TEST_DID))).thenReturn(ServiceResult.success());
            baseRequest()
                    .body(new DidRequestPayload(TEST_DID))
                    .delete("/%s/endpoints?serviceId=test-service-id&autoPublish=true".formatted(Base64.getUrlEncoder().encodeToString(TEST_DID.getBytes())))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(anyOf(equalTo(200), equalTo(204)));
            verify(didDocumentServiceMock).removeService(eq(TEST_DID), eq("test-service-id"));
            verify(didDocumentServiceMock).publish(eq(TEST_DID));

            verifyNoMoreInteractions(didDocumentServiceMock);
        }

        @Test
        void removeEndpoint_whenAutoPublishFails_expect400() {
            when(didDocumentServiceMock.removeService(eq(TEST_DID), anyString())).thenReturn(ServiceResult.success());
            when(didDocumentServiceMock.publish(eq(TEST_DID))).thenReturn(ServiceResult.badRequest("publisher not reachable"));
            baseRequest()
                    .body(new DidRequestPayload(TEST_DID))
                    .delete("/%s/endpoints?serviceId=test-service-id&autoPublish=true".formatted(Base64.getUrlEncoder().encodeToString(TEST_DID.getBytes())))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400);
            verify(didDocumentServiceMock).removeService(eq(TEST_DID), eq("test-service-id"));
            verify(didDocumentServiceMock).publish(eq(TEST_DID));

            verifyNoMoreInteractions(didDocumentServiceMock);
        }

        @Test
        void removeEndpoint_doesNotExist() {
            when(didDocumentServiceMock.removeService(eq(TEST_DID), anyString())).thenReturn(ServiceResult.badRequest("service not found"));
            baseRequest()
                    .body(new DidRequestPayload(TEST_DID))
                    .delete("/%s/endpoints?serviceId=test-service-id".formatted(Base64.getUrlEncoder().encodeToString(TEST_DID.getBytes())))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400);
            verify(didDocumentServiceMock).removeService(eq(TEST_DID), eq("test-service-id"));
        }

        @Test
        void removeEndpoint_didNotFound() {
            when(didDocumentServiceMock.removeService(eq(TEST_DID), anyString())).thenReturn(ServiceResult.notFound("did not found"));
            baseRequest()
                    .body(new DidRequestPayload(TEST_DID))
                    .delete("/%s/endpoints?serviceId=test-service-id".formatted(Base64.getUrlEncoder().encodeToString(TEST_DID.getBytes())))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(404);
            verify(didDocumentServiceMock).removeService(eq(TEST_DID), eq("test-service-id"));
        }
    }

    @Nested
    class ReplaceEndpoint {
        @Test
        void replaceEndpoint() {
            when(didDocumentServiceMock.replaceService(eq(TEST_DID), any(Service.class))).thenReturn(ServiceResult.success());
            baseRequest()
                    .body(new DidRequestPayload(TEST_DID))
                    .patch("/%s/endpoints".formatted(Base64.getUrlEncoder().encodeToString(TEST_DID.getBytes())))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(anyOf(equalTo(200), equalTo(204)));
            verify(didDocumentServiceMock).replaceService(eq(TEST_DID), any(Service.class));
            verifyNoMoreInteractions(didDocumentServiceMock);
        }

        @Test
        void replaceEndpoint_unauthorized403() {
            when(authService.isAuthorized(any(), anyString(), any())).thenReturn(ServiceResult.unauthorized("test message"));
            baseRequest()
                    .body(new DidRequestPayload(TEST_DID))
                    .patch("/%s/endpoints".formatted(Base64.getUrlEncoder().encodeToString(TEST_DID.getBytes())))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403);
            verify(authService).isAuthorized(any(), anyString(), eq(DidResource.class));
            verifyNoMoreInteractions(didDocumentServiceMock, authService);
        }

        @Test
        void replaceEndpoint_withAutoPublish() {
            when(didDocumentServiceMock.replaceService(eq(TEST_DID), any(Service.class))).thenReturn(ServiceResult.success());
            when(didDocumentServiceMock.publish(eq(TEST_DID))).thenReturn(ServiceResult.success());

            baseRequest()
                    .body(new DidRequestPayload(TEST_DID))
                    .patch("/%s/endpoints?autoPublish=true".formatted(Base64.getUrlEncoder().encodeToString(TEST_DID.getBytes())))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(anyOf(equalTo(200), equalTo(204)));
            verify(didDocumentServiceMock).replaceService(eq(TEST_DID), any(Service.class));
            verify(didDocumentServiceMock).publish(eq(TEST_DID));
            verifyNoMoreInteractions(didDocumentServiceMock);
        }

        @Test
        void replaceEndpoint_whenAutoPublishFails_expect400() {
            when(didDocumentServiceMock.replaceService(eq(TEST_DID), any(Service.class))).thenReturn(ServiceResult.success());
            when(didDocumentServiceMock.publish(eq(TEST_DID))).thenReturn(ServiceResult.badRequest("publisher not working"));

            baseRequest()
                    .body(new DidRequestPayload(TEST_DID))
                    .patch("/%s/endpoints?autoPublish=true".formatted(Base64.getUrlEncoder().encodeToString(TEST_DID.getBytes())))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400);
            verify(didDocumentServiceMock).replaceService(eq(TEST_DID), any(Service.class));
            verify(didDocumentServiceMock).publish(eq(TEST_DID));
            verifyNoMoreInteractions(didDocumentServiceMock);
        }

        @Test
        void replaceEndpoint_doesNotExist() {
            when(didDocumentServiceMock.replaceService(eq(TEST_DID), any(Service.class))).thenReturn(ServiceResult.badRequest("service not found"));
            baseRequest()
                    .body(new DidRequestPayload(TEST_DID))
                    .patch("/%s/endpoints".formatted(Base64.getUrlEncoder().encodeToString(TEST_DID.getBytes())))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400);
            verify(didDocumentServiceMock).replaceService(eq(TEST_DID), any(Service.class));
        }

        @Test
        void replaceEndpoint_didNotFound() {
            when(didDocumentServiceMock.replaceService(eq(TEST_DID), any(Service.class))).thenReturn(ServiceResult.notFound("did not found"));
            baseRequest()
                    .body(new DidRequestPayload(TEST_DID))
                    .patch("/%s/endpoints".formatted(Base64.getUrlEncoder().encodeToString(TEST_DID.getBytes())))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(404);
            verify(didDocumentServiceMock).replaceService(eq(TEST_DID), any(Service.class));
        }
    }

    @Nested
    class AddEndpoint {
        @Test
        void addEndpoint() {
            when(didDocumentServiceMock.addService(eq(TEST_DID), any(Service.class))).thenReturn(ServiceResult.success());
            baseRequest()
                    .body(new DidRequestPayload(TEST_DID))
                    .post("/%s/endpoints".formatted(Base64.getUrlEncoder().encodeToString(TEST_DID.getBytes())))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(anyOf(equalTo(200), equalTo(204)));
            verify(didDocumentServiceMock).addService(eq(TEST_DID), any(Service.class));
            verifyNoMoreInteractions(didDocumentServiceMock);
        }

        @Test
        void addEndpoint_unauthorized403() {
            when(authService.isAuthorized(any(), anyString(), any())).thenReturn(ServiceResult.unauthorized("test message"));
            baseRequest()
                    .body(new DidRequestPayload(TEST_DID))
                    .post("/%s/endpoints".formatted(Base64.getUrlEncoder().encodeToString(TEST_DID.getBytes())))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403);
            verify(authService).isAuthorized(any(), anyString(), eq(DidResource.class));
            verifyNoMoreInteractions(didDocumentServiceMock, authService);
        }

        @Test
        void addEndpoint_withAutoPublish() {
            when(didDocumentServiceMock.addService(eq(TEST_DID), any(Service.class))).thenReturn(ServiceResult.success());
            when(didDocumentServiceMock.publish(eq(TEST_DID))).thenReturn(ServiceResult.success());
            baseRequest()
                    .body(new DidRequestPayload(TEST_DID))
                    .post("/%s/endpoints?autoPublish=true".formatted(Base64.getUrlEncoder().encodeToString(TEST_DID.getBytes())))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(anyOf(equalTo(200), equalTo(204)));
            verify(didDocumentServiceMock).addService(eq(TEST_DID), any(Service.class));
            verify(didDocumentServiceMock).publish(eq(TEST_DID));
            verifyNoMoreInteractions(didDocumentServiceMock);
        }

        @Test
        void addEndpoint_whenAutoPublishFails_expect400() {
            when(didDocumentServiceMock.addService(eq(TEST_DID), any(Service.class))).thenReturn(ServiceResult.success());
            when(didDocumentServiceMock.publish(eq(TEST_DID))).thenReturn(ServiceResult.badRequest("publisher not working"));
            baseRequest()
                    .body(new DidRequestPayload(TEST_DID))
                    .post("/%s/endpoints?autoPublish=true".formatted(Base64.getUrlEncoder().encodeToString(TEST_DID.getBytes())))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400);
            verify(didDocumentServiceMock).addService(eq(TEST_DID), any(Service.class));
            verify(didDocumentServiceMock).publish(eq(TEST_DID));
            verifyNoMoreInteractions(didDocumentServiceMock);
        }

        @Test
        void addEndpoint_alreadyExists() {
            when(didDocumentServiceMock.addService(eq(TEST_DID), any(Service.class))).thenReturn(ServiceResult.conflict("exists"));
            baseRequest()
                    .body(new DidRequestPayload(TEST_DID))
                    .post("/%s/endpoints".formatted(Base64.getUrlEncoder().encodeToString(TEST_DID.getBytes())))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(409);
            verify(didDocumentServiceMock).addService(eq(TEST_DID), any(Service.class));
        }

        @Test
        void addEndpoint_didNotFound() {
            when(didDocumentServiceMock.addService(eq(TEST_DID), any(Service.class))).thenReturn(ServiceResult.notFound("did not found"));
            baseRequest()
                    .body(new DidRequestPayload(TEST_DID))
                    .post("/%s/endpoints".formatted(Base64.getUrlEncoder().encodeToString(TEST_DID.getBytes())))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(404);
            verify(didDocumentServiceMock).addService(eq(TEST_DID), any(Service.class));
        }
    }

    @Nested
    class Query {
        @Test
        void query_withSimpleField() {
            var resultList = List.of(createDidDocument().build());
            when(didDocumentServiceMock.queryDocuments(any())).thenReturn(ServiceResult.success(resultList));
            var q = QuerySpec.Builder.newInstance().filter(new Criterion("id", "=", "foobar")).build();

            var docList = baseRequest()
                    .body(q)
                    .post("/query")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .extract().body().as(DidDocument[].class);

            assertThat(docList).isNotEmpty().hasSize(1)
                    .usingRecursiveFieldByFieldElementComparator()
                    .containsExactlyInAnyOrderElementsOf(resultList);
            verify(didDocumentServiceMock).queryDocuments(eq(q));
        }

        @Test
        void query_invalidQuery_expect400() {
            when(didDocumentServiceMock.queryDocuments(any())).thenReturn(ServiceResult.badRequest("test-message"));
            var q = QuerySpec.Builder.newInstance().build();
            baseRequest()
                    .body(q)
                    .post("/query")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400);

            verify(didDocumentServiceMock).queryDocuments(eq(q));
        }

        @Test
        void query_unauthorized403() {
            var resultList = List.of(createDidDocument().build());
            when(didDocumentServiceMock.queryDocuments(any())).thenReturn(ServiceResult.success(resultList));
            when(authService.isAuthorized(any(), anyString(), any())).thenReturn(ServiceResult.unauthorized("test-message"));
            var q = QuerySpec.Builder.newInstance().build();

            var result = baseRequest()
                    .body(q)
                    .post("/query")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .extract().body().as(DidDocument[].class);

            assertThat(result).isEmpty();

            verify(authService).isAuthorized(any(), anyString(), eq(DidResource.class));
            verify(didDocumentServiceMock).queryDocuments(eq(q));
            verifyNoMoreInteractions(didDocumentServiceMock, authService);
        }
    }

    @Nested
    class Unpublish {
        @Test
        void unpublish_success() {

            when(didDocumentServiceMock.unpublish(eq(TEST_DID))).thenReturn(ServiceResult.success());

            baseRequest()
                    .body(new DidRequestPayload(TEST_DID))
                    .post("/unpublish")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(anyOf(equalTo(200), equalTo(204)));
            verify(didDocumentServiceMock).unpublish(eq(TEST_DID));
            verifyNoMoreInteractions(didDocumentServiceMock);
        }

        @Test
        void unpublish_unauthorized403() {
            when(authService.isAuthorized(any(), anyString(), any())).thenReturn(ServiceResult.unauthorized("test message"));

            baseRequest()
                    .body(new DidRequestPayload(TEST_DID))
                    .post("/unpublish")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403);

            verify(authService).isAuthorized(any(), anyString(), eq(DidResource.class));
            verifyNoMoreInteractions(didDocumentServiceMock, authService);
        }

        @Test
        void unpublish_whenNotExist_expect404() {
            when(didDocumentServiceMock.unpublish(eq(TEST_DID))).thenReturn(ServiceResult.notFound("test-message"));

            baseRequest()
                    .body(new DidRequestPayload(TEST_DID))
                    .post("/unpublish")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(404);
            verify(didDocumentServiceMock).unpublish(eq(TEST_DID));
            verifyNoMoreInteractions(didDocumentServiceMock);
        }

        @Test
        void unpublish_whenNotPublished_expect200() {
            // not needed - test setup is identical to publish_success
        }

        @Test
        void unpublish_whenAlreadyUnpublished_expect200() {
            // not needed - test setup is identical to publish_success
        }

        @Test
        void unpublish_whenNotSupported_expect400() {
            when(didDocumentServiceMock.unpublish(eq(TEST_DID))).thenReturn(ServiceResult.badRequest("test-message"));

            baseRequest()
                    .body(new DidRequestPayload(TEST_DID))
                    .post("/unpublish")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400);
            verify(didDocumentServiceMock).unpublish(eq(TEST_DID));
            verifyNoMoreInteractions(didDocumentServiceMock);
        }
    }

    @Nested
    class Publish {
        @Test
        void publish_success() {

            when(didDocumentServiceMock.publish(eq(TEST_DID))).thenReturn(ServiceResult.success());

            baseRequest()
                    .body(new DidRequestPayload(TEST_DID))
                    .post("/publish")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(anyOf(equalTo(200), equalTo(204)));
            verify(didDocumentServiceMock).publish(eq(TEST_DID));
        }

        @Test
        void publish_unauthorized403() {
            when(authService.isAuthorized(any(), anyString(), any())).thenReturn(ServiceResult.unauthorized("test-msg"));

            baseRequest()
                    .body(new DidRequestPayload(TEST_DID))
                    .post("/publish")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403);
            verify(authService).isAuthorized(any(), anyString(), eq(DidResource.class));
            verifyNoMoreInteractions(didDocumentServiceMock, authService);
        }

        @Test
        void publish_whenNotExist_expect404() {

            when(didDocumentServiceMock.publish(eq(TEST_DID))).thenReturn(ServiceResult.notFound("test-message"));

            baseRequest()
                    .body(new DidRequestPayload(TEST_DID))
                    .post("/publish")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(equalTo(404));
            verify(didDocumentServiceMock).publish(eq(TEST_DID));
            verifyNoMoreInteractions(didDocumentServiceMock);
        }

        @Test
        void publish_whenAlreadyPublished_expect200() {

            when(didDocumentServiceMock.publish(eq(TEST_DID))).thenReturn(ServiceResult.success());

            baseRequest()
                    .body(new DidRequestPayload(TEST_DID))
                    .post("/publish")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(anyOf(equalTo(200), equalTo(204)));
            verify(didDocumentServiceMock).publish(eq(TEST_DID));
            verifyNoMoreInteractions(didDocumentServiceMock);
        }
    }
}