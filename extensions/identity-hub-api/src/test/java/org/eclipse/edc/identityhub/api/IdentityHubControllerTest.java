/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.api;

import com.nimbusds.jwt.SignedJWT;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.eclipse.edc.identityhub.spi.model.Descriptor;
import org.eclipse.edc.identityhub.spi.model.MessageRequestObject;
import org.eclipse.edc.identityhub.spi.model.RequestObject;
import org.eclipse.edc.junit.extensions.EdcExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.buildSignedJwt;
import static org.eclipse.edc.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.generateEcKey;
import static org.eclipse.edc.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.generateVerifiableCredential;
import static org.eclipse.edc.identityhub.spi.model.WebNodeInterfaceMethod.COLLECTIONS_QUERY;
import static org.eclipse.edc.identityhub.spi.model.WebNodeInterfaceMethod.COLLECTIONS_WRITE;
import static org.eclipse.edc.identityhub.spi.model.WebNodeInterfaceMethod.FEATURE_DETECTION_READ;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

@ExtendWith(EdcExtension.class)
class IdentityHubControllerTest {

    private static final int PORT = getFreePort();
    private static final String API_URL = String.format("http://localhost:%s/api", PORT);
    private static final String NONCE = UUID.randomUUID().toString();
    private static final String TARGET = "http://some.test.url";
    private static final String REQUEST_ID = UUID.randomUUID().toString();

    private static final String BASE_PATH = "/identity-hub";

    @BeforeEach
    void setUp(EdcExtension extension) {
        extension.setConfiguration(Map.of("web.http.port", String.valueOf(PORT)));
    }

    @Test
    void writeAndQueryObject() {
        // Arrange
        var issuer = "http://some.test.url";
        var subject = "http://some.test.url";
        var credential = generateVerifiableCredential();
        var jwt = buildSignedJwt(credential, issuer, subject, generateEcKey());

        // Act
        collectionsWrite(jwt);
        var credentials = collectionsQuery();

        // Assert
        assertThat(credentials).usingRecursiveFieldByFieldElementComparator().containsExactly(jwt.serialize().getBytes(UTF_8));
    }

    @Test
    void detectFeatures() {
        baseRequest()
                .body(createRequestObject(FEATURE_DETECTION_READ.getName()))
                .post()
                .then()
                .statusCode(200)
                .body("requestId", equalTo(REQUEST_ID))
                .body("replies", hasSize(1))
                .body("replies[0].entries", hasSize(1))
                .body("replies[0].entries[0].interfaces.collections['CollectionsQuery']", is(true))
                .body("replies[0].entries[0].interfaces.collections['CollectionsWrite']", is(true));
    }

    @Test
    void useUnsupportedMethod() {
        baseRequest()
                .body(createRequestObject("Not supported method"))
                .post()
                .then()
                .statusCode(200)
                .body("requestId", equalTo(REQUEST_ID))
                .body("replies", hasSize(1))
                .body("replies[0].status.code", equalTo(501))
                .body("replies[0].status.detail", equalTo("The interface method is not implemented"));
    }

    @Test
    void writeMalformedMessage() {
        byte[] data = "invalid base64".getBytes(UTF_8);
        baseRequest()
                .body(createRequestObject(COLLECTIONS_WRITE.getName(), data))
                .post()
                .then()
                .statusCode(200)
                .body("requestId", equalTo(REQUEST_ID))
                .body("replies", hasSize(1))
                .body("replies[0].status.code", equalTo(400))
                .body("replies[0].status.detail", equalTo("The message was malformed or improperly constructed"));
    }

    @Test
    void getSelfDescription() {
        given()
                .baseUri(API_URL)
                .basePath(BASE_PATH)
                .get("/self-description")
                .then()
                .assertThat()
                .statusCode(200)
                .body("selfDescriptionCredential.credentialSubject.gx-participant:headquarterAddress.gx-participant:country.@value", equalTo("FR"));
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri(API_URL)
                .basePath(BASE_PATH)
                .contentType(ContentType.JSON)
                .when();
    }

    private RequestObject createRequestObject(String method) {
        return createRequestObject(method, null);
    }

    private RequestObject createRequestObject(String method, byte[] data) {
        return RequestObject.Builder.newInstance()
                .requestId(REQUEST_ID)
                .target(TARGET)
                .messages(List.of(
                        MessageRequestObject.Builder.newInstance()
                                .descriptor(Descriptor.Builder.newInstance()
                                        .method(method)
                                        .nonce(NONCE)
                                        .recordId(UUID.randomUUID().toString())
                                        .dateCreated(Instant.now().getEpochSecond())
                                        .build())
                                .data(data)
                                .build()))
                .build();
    }

    private void collectionsWrite(SignedJWT verifiableCredential) {
        byte[] data = verifiableCredential.serialize().getBytes(UTF_8);
        baseRequest()
                .body(createRequestObject(COLLECTIONS_WRITE.getName(), data))
                .post()
                .then()
                .statusCode(200)
                .body("requestId", equalTo(REQUEST_ID))
                .body("replies", hasSize(1))
                .body("replies[0].status.code", equalTo(200))
                .body("replies[0].status.detail", equalTo("The message was successfully processed"));
    }

    private List<byte[]> collectionsQuery() {
        return baseRequest()
                .body(createRequestObject(COLLECTIONS_QUERY.getName()))
                .post()
                .then()
                .statusCode(200)
                .body("requestId", equalTo(REQUEST_ID))
                .body("replies", hasSize(1))
                .body("replies[0].status.code", equalTo(200))
                .body("replies[0].status.detail", equalTo("The message was successfully processed"))
                .extract().body().jsonPath().getList("replies[0].entries", String.class)
                .stream().map(s -> Base64.getDecoder().decode(s))
                .collect(Collectors.toList());
    }
}
