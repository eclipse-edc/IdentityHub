/*
 *  Copyright (c) 2023 Metaform Systems, Inc.
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

package org.eclipse.edc.identityhub.api.didmanagement.v1;

import io.restassured.common.mapper.TypeRef;
import io.restassured.specification.RequestSpecification;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.iam.did.spi.document.VerificationMethod;
import org.eclipse.edc.identithub.did.spi.DidDocumentService;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ApiTest
class DidManagementApiControllerTest extends RestControllerTestBase {

    public static final String TEST_DID = "did:web:host%3A1234:test-did";
    private final DidDocumentService didDocumentServiceMock = mock();

    @Test
    void create_success() {
        when(didDocumentServiceMock.store(any())).thenReturn(ServiceResult.success());
        var document = createDidDocument().build();

        baseRequest()
                .with()
                .body(document)
                .post()
                .then()
                .log().ifError()
                .statusCode(anyOf(equalTo(200), equalTo(204)));
    }

    @Test
    void create_alreadyExists_expect409() {
        when(didDocumentServiceMock.store(any())).thenReturn(ServiceResult.conflict("already exists"));
        var document = createDidDocument().build();

        baseRequest()
                .body(document)
                .post()
                .then()
                .log().ifValidationFails()
                .statusCode(409);
    }

    @Test
    void create_malformedBody_expect400() {
        when(didDocumentServiceMock.store(any())).thenReturn(ServiceResult.success());
        var document = createDidDocument().id("not a uri").build();

        baseRequest()
                .body(document)
                .post()
                .then()
                .log().ifValidationFails()
                .statusCode(400);
    }

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

    @Test
    void updateDid_success() {
        var doc = createDidDocument().id(TEST_DID).build();
        when(didDocumentServiceMock.update(any())).thenReturn(ServiceResult.success());

        baseRequest()
                .body(doc)
                .put()
                .then()
                .log().ifError()
                .statusCode(204);
        verify(didDocumentServiceMock).update(argThat(dd -> dd.getId().equals(TEST_DID)));
        verifyNoMoreInteractions(didDocumentServiceMock);
    }

    @Test
    void updateDid_success_withRepublish() {
        var doc = createDidDocument().id(TEST_DID).build();
        when(didDocumentServiceMock.update(any())).thenReturn(ServiceResult.success());
        when(didDocumentServiceMock.publish(eq(TEST_DID))).thenReturn(ServiceResult.success());

        baseRequest()
                .body(doc)
                .put("?republish=true")
                .then()
                .log().ifError()
                .statusCode(204);
        verify(didDocumentServiceMock).update(argThat(dd -> dd.getId().equals(TEST_DID)));
        verify(didDocumentServiceMock).publish(eq(TEST_DID));
        verifyNoMoreInteractions(didDocumentServiceMock);
    }

    @Test
    void updateDid_success_withRepublishFails() {

        var doc = createDidDocument().id(TEST_DID).build();
        when(didDocumentServiceMock.update(any())).thenReturn(ServiceResult.success());
        when(didDocumentServiceMock.publish(eq(TEST_DID))).thenReturn(ServiceResult.badRequest("test-failure"));

        baseRequest()
                .body(doc)
                .put("?republish=true")
                .then()
                .log().ifError()
                .statusCode(400);
        verify(didDocumentServiceMock).update(argThat(dd -> dd.getId().equals(TEST_DID)));
        verify(didDocumentServiceMock).publish(eq(TEST_DID));
        verifyNoMoreInteractions(didDocumentServiceMock);
    }

    @Test
    void updateDid_whenNotExist_expect404() {

        var doc = createDidDocument().id(TEST_DID).build();
        when(didDocumentServiceMock.update(any())).thenReturn(ServiceResult.notFound("test-failure"));

        baseRequest()
                .body(doc)
                .put()
                .then()
                .log().ifError()
                .statusCode(404);
        verify(didDocumentServiceMock).update(argThat(dd -> dd.getId().equals(TEST_DID)));
        verifyNoMoreInteractions(didDocumentServiceMock);
    }

    @Test
    void deleteDid_success() {

        when(didDocumentServiceMock.deleteById(eq(TEST_DID))).thenReturn(ServiceResult.success());
        baseRequest()
                .body(new DidRequestPayload(TEST_DID))
                .delete("/")
                .then()
                .log().ifError()
                .statusCode(204);
        verify(didDocumentServiceMock).deleteById(eq(TEST_DID));
        verifyNoMoreInteractions(didDocumentServiceMock);
    }

    @Test
    void deleteDid_whenNotExist_expect404() {

        when(didDocumentServiceMock.deleteById(eq(TEST_DID))).thenReturn(ServiceResult.notFound("test-message"));
        baseRequest()
                .body(new DidRequestPayload(TEST_DID))
                .delete("/")
                .then()
                .log().ifError()
                .statusCode(404);
        verify(didDocumentServiceMock).deleteById(eq(TEST_DID));
        verifyNoMoreInteractions(didDocumentServiceMock);
    }

    @Test
    void deleteDid_whenAlreadyPublished_expect409() {

        when(didDocumentServiceMock.deleteById(eq(TEST_DID))).thenReturn(ServiceResult.conflict("test-message"));
        baseRequest()
                .body(new DidRequestPayload(TEST_DID))
                .delete("/")
                .then()
                .log().ifError()
                .statusCode(409);
        verify(didDocumentServiceMock).deleteById(eq(TEST_DID));
        verifyNoMoreInteractions(didDocumentServiceMock);
    }

    @Test
    void query_withSimpleField() {
        var resultList = List.of(createDidDocument().build());
        when(didDocumentServiceMock.queryDocuments(any())).thenReturn(ServiceResult.success(resultList));
        var q = QuerySpec.Builder.newInstance().filter(new Criterion("id", "=", "foobar")).build();

        var docListType = new TypeRef<List<DidDocument>>() {
        };
        var docList = baseRequest()
                .body(q)
                .post("/query")
                .then()
                .log().ifError()
                .statusCode(200)
                .extract().body().as(docListType);

        assertThat(docList).isNotEmpty().hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .isEqualTo(resultList);
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

    @Override
    protected DidManagementApiController controller() {
        return new DidManagementApiController(didDocumentServiceMock);
    }

    private DidDocument.Builder createDidDocument() {
        return DidDocument.Builder.newInstance()
                .id("did:web:testdid")
                .service(List.of(new Service("test-service", "test-service", "https://test.service.com/")))
                .verificationMethod(List.of(VerificationMethod.Builder.newInstance()
                        .id("did:web:testdid#key-1")
                        .publicKeyMultibase("saflasjdflaskjdflasdkfj")
                        .build()));
    }

    private RequestSpecification baseRequest() {
        return given()
                .contentType("application/json")
                .baseUri("http://localhost:" + port + "/v1/dids")
                .when();
    }
}