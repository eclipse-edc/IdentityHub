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

import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.identityhub.api.Versions;
import org.eclipse.edc.identityhub.spi.did.DidDocumentService;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.identityhub.api.didmanagement.v1.unstable.TestFunctions.createDidDocument;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetAllDidsApiControllerTest extends RestControllerTestBase {
    private final DidDocumentService didDocumentServiceMock = mock();

    @Test
    void getAll() {

        var didDocs = IntStream.range(0, 10).mapToObj(i -> createDidDocument().id("did:web:test" + i).build()).toList();

        when(didDocumentServiceMock.queryDocuments(any())).thenReturn(ServiceResult.success(didDocs));
        var docs = given()
                .when()
                .contentType("application/json")
                .baseUri("http://localhost:" + port + Versions.UNSTABLE + "/dids")
                .get()
                .then()
                .statusCode(200)
                .extract().body().as(DidDocument[].class);

        assertThat(docs).usingRecursiveFieldByFieldElementComparator().containsExactlyInAnyOrderElementsOf(didDocs);
    }

    @Override
    protected Object controller() {
        return new GetAllDidsApiController(didDocumentServiceMock);
    }
}