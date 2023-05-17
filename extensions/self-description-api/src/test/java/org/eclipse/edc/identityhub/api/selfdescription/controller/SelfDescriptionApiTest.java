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

package org.eclipse.edc.identityhub.api.selfdescription.controller;

import org.eclipse.edc.junit.extensions.EdcExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@ExtendWith(EdcExtension.class)
abstract class SelfDescriptionApiTest {

    private static final String IDENTITY_HUB_PATH = "/identity-hub";

    private String apiBasePath;

    @BeforeEach
    void setUp(EdcExtension extension) {
        apiBasePath = configureApi(extension);
    }

    @Test
    void getSelfDescription() {
        given()
                .baseUri(apiBasePath)
                .basePath(IDENTITY_HUB_PATH)
                .get("/self-description")
                .then()
                .assertThat()
                .statusCode(200)
                .body("selfDescriptionCredential.credentialSubject.gx-participant:headquarterAddress.gx-participant:country.@value", equalTo("FR"));
    }

    protected abstract String configureApi(EdcExtension extension);
}
