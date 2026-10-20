/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static org.eclipse.edc.identityhub.spi.participantcontext.IdentityApiScopes.WRITE;
import static org.eclipse.edc.identityhub.spi.participantcontext.IssuerAdminApiScopes.ADMIN;
import static org.eclipse.edc.identityhub.tests.TestData.ISSUER_RUNTIME_NAME;
import static org.eclipse.edc.identityhub.tests.fixtures.TestFunctions.authorizeOauth2;
import static org.eclipse.edc.identityhub.tests.fixtures.TestFunctions.authorizeTokenBased;
import static org.eclipse.edc.identityhub.tests.fixtures.common.AbstractIdentityHub.SUPER_USER;

/**
 * Verifies that the admin scope of one API does not elevate a principal on the other API of the IssuerService.
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class AdminScopeSeparationEndToEndTest {

    abstract static class Tests {

        static final String ISSUER_ADMIN = "issuer-admin";
        static final String OTHER_USER = "other-user";
        static final List<String> ISSUER_ADMIN_SCOPES = List.of(ADMIN, WRITE);

        @AfterEach
        void teardown(IdentityHubParticipantContextService pcService) {
            pcService.query(QuerySpec.max()).getContent()
                    .forEach(pc -> pcService.deleteParticipantContext(pc.getParticipantContextId()).getContent());
        }

        @Test
        void identityApi_issuerAdminScope_cannotReadOtherParticipant(IssuerService issuer) {
            issuer.createParticipant(OTHER_USER);

            issuer.getIdentityEndpoint().baseRequest()
                    .header(authorizeIssuerAdmin(issuer))
                    .get("/v1/participants/%s".formatted(OTHER_USER))
                    .then()
                    .statusCode(403);
        }

        @Test
        void identityApi_issuerAdminScope_cannotRegenerateTokenOfOtherParticipant(IssuerService issuer) {
            issuer.createParticipant(OTHER_USER);

            issuer.getIdentityEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header(authorizeIssuerAdmin(issuer))
                    .post("/v1/participants/%s/token".formatted(OTHER_USER))
                    .then()
                    .statusCode(403);
        }

        @Test
        void identityApi_issuerAdminScope_canReadOwnParticipant(IssuerService issuer) {
            issuer.getIdentityEndpoint().baseRequest()
                    .header(authorizeIssuerAdmin(issuer))
                    .get("/v1/participants/%s".formatted(ISSUER_ADMIN))
                    .then()
                    .statusCode(200);
        }

        @Test
        void identityApi_superUser_canReadOtherParticipant(IssuerService issuer) {
            issuer.createParticipant(OTHER_USER);

            issuer.getIdentityEndpoint().baseRequest()
                    .header(authorize(SUPER_USER, issuer))
                    .get("/v1/participants/%s".formatted(OTHER_USER))
                    .then()
                    .statusCode(200);
        }

        @Test
        void issuerAdminApi_issuerAdminScope_canAccessOtherParticipant(IssuerService issuer) {
            issuer.createParticipant(OTHER_USER);

            issuer.getAdminEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header(authorizeIssuerAdmin(issuer))
                    .body("""
                            {
                              "holderId": "test-holder-id",
                              "did": "did:web:test-holder",
                              "name": null
                            }
                            """)
                    .post("/v1/participants/%s/holders".formatted(OTHER_USER))
                    .then()
                    .statusCode(201);
        }

        protected abstract Header authorize(String participantContextId, IssuerService issuer);

        /**
         * Creates the {@link #ISSUER_ADMIN} participant context, which holds the Issuer Admin API admin scope and the
         * Identity API write scope, and returns its authorization header.
         */
        protected abstract Header authorizeIssuerAdmin(IssuerService issuer);
    }

    @Nested
    @EndToEndTest
    class ApiKey extends Tests {

        @RegisterExtension
        static final RuntimeExtension ISSUER_EXTENSION = ComponentRuntimeExtension.Builder.newInstance()
                .name(ISSUER_RUNTIME_NAME)
                .modules(DefaultRuntimes.Issuer.MODULES)
                .endpoints(DefaultRuntimes.Issuer.ENDPOINTS.build())
                .configurationProvider(DefaultRuntimes.Issuer::config)
                .paramProvider(IssuerService.class, IssuerService::forContext)
                .build();

        @Override
        protected Header authorize(String participantContextId, IssuerService issuer) {
            return authorizeTokenBased(participantContextId, issuer);
        }

        @Override
        protected Header authorizeIssuerAdmin(IssuerService issuer) {
            return new Header("x-api-key", issuer.createParticipant(ISSUER_ADMIN, ISSUER_ADMIN_SCOPES).apiKey());
        }
    }

    @Nested
    @EndToEndTest
    class Oauth2 extends Tests {

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

        @Override
        protected Header authorize(String participantContextId, IssuerService issuer) {
            return authorizeOauth2(participantContextId, issuer, OAUTH_2_EXTENSION.getAuthServer());
        }

        @Override
        protected Header authorizeIssuerAdmin(IssuerService issuer) {
            issuer.createParticipant(ISSUER_ADMIN, ISSUER_ADMIN_SCOPES);
            return new Header("Authorization", "Bearer " + OAUTH_2_EXTENSION.getAuthServer().createToken(ISSUER_ADMIN, String.join(" ", ISSUER_ADMIN_SCOPES)));
        }
    }
}
