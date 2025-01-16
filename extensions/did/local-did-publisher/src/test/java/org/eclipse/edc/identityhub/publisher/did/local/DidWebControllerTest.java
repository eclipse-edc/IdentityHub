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

package org.eclipse.edc.identityhub.publisher.did.local;

import io.restassured.specification.RequestSpecification;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.identityhub.spi.did.DidWebParser;
import org.eclipse.edc.identityhub.spi.did.model.DidResource;
import org.eclipse.edc.identityhub.spi.did.model.DidState;
import org.eclipse.edc.identityhub.spi.did.store.DidResourceStore;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.identityhub.publisher.did.local.TestFunctions.createDidResource;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ApiTest
class DidWebControllerTest extends RestControllerTestBase {

    private final DidResourceStore storeMock = mock();

    private static DidResource publishedDid(String did) {
        return createDidResource(did).state(DidState.PUBLISHED).build();
    }

    @Test
    void getDidDocument() {
        when(storeMock.query(any())).thenReturn(List.of(publishedDid("did:web:testdid1")));

        var doc = baseRequest()
                .get("/foo/bar")
                .then()
                .log().ifError()
                .statusCode(200)
                .extract().body().as(DidDocument.class);
        assertThat(doc).isNotNull()
                .extracting(DidDocument::getId).isEqualTo("did:web:testdid1");
    }

    @Test
    void getDidDocument_multipleDocumentsForDid() {
        when(storeMock.query(any())).thenReturn(List.of(publishedDid("did:web:testdid1"), publishedDid("did:web:testdid1")));

        baseRequest()
                .get("/foo/bar")
                .then()
                .log().ifValidationFails()
                .statusCode(400)
                .body(containsString("DID '%s' resolved more than one document".formatted("did:web:localhost%%3A%s:foo:bar".formatted(port))));
    }

    @Test
    void getDidDocument_withWellKnown() {
        when(storeMock.query(any())).thenReturn(List.of(publishedDid("did:web:testdid1")));


        baseRequest()
                .get("/foo/bar/.well-known/did.json")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body(containsString("did:web:testdid1"));

        // verify that the query to the store was issued using the parsed DID
        verify(storeMock).query(argThat(qs -> qs.getFilterExpression().stream().anyMatch(c -> c.getOperandRight().equals("did:web:localhost%%3A%s:foo:bar".formatted(port)))));
    }

    @Test
    void getDidDocument_noResult() {
        baseRequest()
                .get("/foo/bar")
                .then()
                .log().ifError()
                .statusCode(204)
                .body(emptyString());
    }

    @Override
    protected Object controller() {
        return new DidWebController(monitor, storeMock, new DidWebParser());
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port)
                .when();
    }
}