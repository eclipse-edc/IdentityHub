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

package org.eclipse.edc.identityhub.tests.fixtures.credentialservice;

import io.restassured.http.Header;
import io.restassured.specification.RequestSpecification;
import org.eclipse.edc.junit.extensions.ComponentRuntimeContext;
import org.eclipse.edc.junit.utils.LazySupplier;

import java.net.URI;
import java.util.Objects;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.eclipse.edc.identityhub.tests.fixtures.common.TestFunctions.base64Encode;

public class IdentityHubApiClient {

    private final LazySupplier<URI> identityEndpoint;

    public IdentityHubApiClient(LazySupplier<URI> identityEndpoint) {
        this.identityEndpoint = identityEndpoint;
    }

    public static IdentityHubApiClient forContext(ComponentRuntimeContext ctx) {
        var identityEndpoint = Objects.requireNonNull(ctx.getEndpoint("identity"));
        return new IdentityHubApiClient(identityEndpoint);
    }

    public String requestCredential(String token, String participantId, String issuerDid, String id, String type) {
        var holderPid = UUID.randomUUID().toString();
        var request = """
                {
                  "issuerDid": "%s",
                  "holderPid": "%s",
                  "credentials": [{ "format": "VC1_0_JWT", "type": "%s", "id": "%s" }]
                }
                """.formatted(issuerDid, holderPid, type, id);

        baseIdentityRequest()
                .contentType(JSON)
                .header(new Header("x-api-key", token))
                .body(request)
                .post("/v1alpha/participants/%s/credentials/request".formatted(base64Encode(participantId)))
                .then()
                .log().ifValidationFails()
                .statusCode(201)
                .log().ifValidationFails();
        return holderPid;
    }


    private RequestSpecification baseIdentityRequest() {
        return given().baseUri(identityEndpoint.get().toString());
    }

}
