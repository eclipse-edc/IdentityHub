/*
 *  Copyright (c) 2026 Metaform Systems Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.tests;

import io.restassured.http.ContentType;
import io.restassured.http.Header;
import org.eclipse.edc.api.authentication.OauthServerEndToEndExtension;
import org.eclipse.edc.identityhub.spi.participantcontext.IdentityHubParticipantContextService;
import org.eclipse.edc.identityhub.tests.fixtures.DefaultRuntimes;
import org.eclipse.edc.identityhub.tests.fixtures.issuerservice.IssuerService;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.spi.query.QuerySpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.eclipse.edc.identityhub.tests.TestData.ISSUER_RUNTIME_NAME;

/**
 * Verifies that the scopes declared on the Issuer Admin API and the Identity API are enforced when the IssuerService
 * uses OAuth2 authentication.
 */
@EndToEndTest
public class IssuerServiceOauth2ScopeEndToEndTest {

    private static final String USER = "user";

    @Order(0)
    @RegisterExtension
    static final OauthServerEndToEndExtension OAUTH_2_EXTENSION = OauthServerEndToEndExtension.Builder.newInstance()
            .issuer("issuer")
            .signingKeyId("signing-key-id")
            .build();

    @RegisterExtension
    static final RuntimeExtension ISSUER_EXTENSION = ComponentRuntimeExtension.Builder.newInstance()
            .name(ISSUER_RUNTIME_NAME)
            .modules(DefaultRuntimes.Issuer.MODULES_OAUTH2)
            .endpoints(DefaultRuntimes.Issuer.ENDPOINTS.build())
            .configurationProvider(DefaultRuntimes.Issuer::config)
            .configurationProvider(OAUTH_2_EXTENSION::getConfig)
            .paramProvider(IssuerService.class, IssuerService::forContext)
            .build();

    @AfterEach
    void teardown(IdentityHubParticipantContextService pcService) {
        pcService.query(QuerySpec.max()).getContent()
                .forEach(pc -> pcService.deleteParticipantContext(pc.getParticipantContextId()).getContent());
    }

    @Test
    void issuerAdminApi_readScope_canRead(IssuerService issuer) {
        issuer.createParticipant(USER);

        issuer.getAdminEndpoint().baseRequest()
                .contentType(ContentType.JSON)
                .header(authorize("issuer-admin-api:read"))
                .body(QuerySpec.max())
                .post("/v1/participants/%s/holders/query".formatted(USER))
                .then()
                .statusCode(200);
    }

    @Test
    void issuerAdminApi_readScope_cannotWrite(IssuerService issuer) {
        issuer.createParticipant(USER);

        issuer.getAdminEndpoint().baseRequest()
                .contentType(ContentType.JSON)
                .header(authorize("issuer-admin-api:read"))
                .body("""
                        {
                          "holderId": "test-participant-id",
                          "did": "did:web:test-participant",
                          "name": null
                        }
                        """)
                .post("/v1/participants/%s/holders".formatted(USER))
                .then()
                .statusCode(403);
    }

    @Test
    void identityApi_readScope_canRead(IssuerService issuer) {
        issuer.createParticipant(USER);

        issuer.getIdentityEndpoint().baseRequest()
                .header(authorize("identity-api:read"))
                .get("/v1/participants/%s".formatted(USER))
                .then()
                .statusCode(200);
    }

    @Test
    void identityApi_readScope_cannotWrite(IssuerService issuer) {
        issuer.createParticipant(USER);

        issuer.getIdentityEndpoint().baseRequest()
                .contentType(ContentType.JSON)
                .header(authorize("identity-api:read"))
                .post("/v1/participants/%s/token".formatted(USER))
                .then()
                .statusCode(403);
    }

    private Header authorize(String scopes) {
        return new Header("Authorization", "Bearer " + OAUTH_2_EXTENSION.getAuthServer().createToken(USER, scopes));
    }
}
