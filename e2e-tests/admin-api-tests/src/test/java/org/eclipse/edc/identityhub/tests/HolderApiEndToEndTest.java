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

package org.eclipse.edc.identityhub.tests;

import io.restassured.http.ContentType;
import io.restassured.http.Header;
import org.eclipse.edc.api.authentication.OauthServerEndToEndExtension;
import org.eclipse.edc.identityhub.spi.participantcontext.IdentityHubParticipantContextService;
import org.eclipse.edc.identityhub.tests.fixtures.DefaultRuntimes;
import org.eclipse.edc.identityhub.tests.fixtures.issuerservice.IssuerService;
import org.eclipse.edc.issuerservice.api.admin.holder.v1.unstable.model.HolderDto;
import org.eclipse.edc.issuerservice.spi.holder.HolderService;
import org.eclipse.edc.issuerservice.spi.holder.model.Holder;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.identityhub.tests.TestData.ISSUER_RUNTIME_NAME;
import static org.eclipse.edc.identityhub.tests.fixtures.TestFunctions.authorizeOauth2;
import static org.eclipse.edc.identityhub.tests.fixtures.TestFunctions.authorizeTokenBased;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

@SuppressWarnings("JUnitMalformedDeclaration")
public class HolderApiEndToEndTest {
    abstract static class Tests {

        public static final String USER = "user";

        @AfterEach
        void teardown(HolderService holderService, IdentityHubParticipantContextService pcService) {
            holderService.queryHolders(QuerySpec.max()).getContent()
                    .forEach(p -> holderService.deleteHolder(p.getHolderId()).getContent());

            pcService.query(QuerySpec.max()).getContent()
                    .forEach(pc -> pcService.deleteParticipantContext(pc.getParticipantContextId()).getContent());

        }

        @Test
        void createHolder(IssuerService issuer) {
            issuer.getAdminEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header(authorizeUser(USER, issuer))
                    .body("""
                            {
                              "holderId": "test-participant-id",
                              "did": "did:web:test-participant",
                              "name": null
                            }
                            """)
                    .post("/v1alpha/participants/%s/holders".formatted(toBase64(USER)))
                    .then()
                    .statusCode(201)
                    .header("Location", Matchers.endsWith("/holders/test-participant-id"));
        }

        @Test
        void createParticipant_whenExists(IssuerService issuer, HolderService service) {
            service.createHolder(createHolder("test-participant-id", "did:web:foo", "foobar"));

            issuer.getAdminEndpoint().baseRequest()
                    .header(authorizeUser(USER, issuer))
                    .contentType(ContentType.JSON)
                    .body("""
                            {
                              "holderId": "test-participant-id",
                              "did": "did:web:test-participant",
                              "name": null
                            }
                            """)
                    .post("/v1alpha/participants/%s/holders".formatted(toBase64(USER)))
                    .then()
                    .statusCode(409);
        }

        @Test
        void createHolder_notAuthorized(IssuerService issuer) {
            issuer.createParticipant(USER);

            issuer.getAdminEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header(authorizeUser("anotherParticipant", issuer))
                    .body("""
                            {
                              "holderId": "test-participant-id",
                              "did": "did:web:test-participant",
                              "name": null
                            }
                            """)
                    .post("/v1alpha/participants/%s/holders".formatted(toBase64(USER)))
                    .then()
                    .statusCode(403);
        }

        @Test
        void createParticipant_whenMissingFields(IssuerService issuer) {

            issuer.getAdminEndpoint().baseRequest()
                    .header(authorizeUser(USER, issuer))
                    .contentType(ContentType.JSON)
                    .body("""
                            {
                            "holderId": "test-participant-id"
                            }
                            """)
                    .post("/v1alpha/participants/%s/holders".formatted(toBase64(USER)))
                    .then()
                    .statusCode(400);
        }

        @Test
        void updateHolder(IssuerService issuer, HolderService service) {
            var initialHolder = createHolder("test-participant-id", "did:web:foo", null);
            service.createHolder(initialHolder);

            issuer.getAdminEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header(authorizeUser(USER, issuer))
                    .body("""
                            {
                              "holderId": "test-participant-id",
                              "did": "did:web:foo",
                              "name": "Foo"
                            }
                            """)
                    .put("/v1alpha/participants/%s/holders".formatted(toBase64(USER)))
                    .then()
                    .statusCode(200);

            assertThat(service.findById("test-participant-id")).isSucceeded()
                    .satisfies(holder -> {
                        assertThat(holder.getHolderName()).isEqualTo("Foo");
                    });
        }

        @Test
        void updateHolder_notAuthorized(IssuerService issuer, HolderService service) {
            var initialHolder = createHolder("test-participant-id", "did:web:foo", null);
            service.createHolder(initialHolder);

            issuer.getAdminEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header(authorizeUser("anotherUser", issuer))
                    .body("""
                            {
                              "holderId": "test-participant-id",
                              "did": "did:web:foo",
                              "name": "Foo"
                            }
                            """)
                    .put("/v1alpha/participants/%s/holders".formatted(toBase64(USER)))
                    .then()
                    .statusCode(403);

        }

        @Test
        void queryParticipant(IssuerService issuer, HolderService service) {

            var holder1 = createHolder("test-participant-id", "did:web:foo", "foobar");
            var holder2 = createHolder("test-participant-id", "did:web:foo", "foobar", "anotherParticipantContext");
            service.createHolder(holder1);
            service.createHolder(holder2);

            var res = issuer.getAdminEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header(authorizeUser(USER, issuer))
                    .body(QuerySpec.Builder.newInstance().filter(new Criterion("holderName", "=", "foobar")).build())
                    .post("/v1alpha/participants/%s/holders/query".formatted(toBase64(USER)))
                    .then()
                    .statusCode(200)
                    .body(Matchers.notNullValue())
                    .extract().body().as(Holder[].class);

            assertThat(res).hasSize(1).allSatisfy(p -> assertThat(holder1).usingRecursiveComparison().isEqualTo(p));
        }

        @Test
        void queryParticipant_noResult(IssuerService issuer) {
            var res = issuer.getAdminEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header(authorizeUser(USER, issuer))
                    .body(QuerySpec.Builder.newInstance().filter(new Criterion("holderId", "=", "test-participant-id")).build())
                    .post("/v1alpha/participants/%s/holders/query".formatted(toBase64(USER)))
                    .then()
                    .statusCode(200)
                    .body(Matchers.notNullValue())
                    .extract().body().as(HolderDto[].class);

            assertThat(res).isEmpty();
        }

        @Test
        void getById(IssuerService issuer, HolderService service) {
            var expectedParticipant = createHolder("test-participant-id", "did:web:foo", "foobar");
            service.createHolder(expectedParticipant);

            var res = issuer.getAdminEndpoint().baseRequest()
                    .header(authorizeUser(USER, issuer))
                    .get("/v1alpha/participants/%s/holders/test-participant-id".formatted(toBase64(USER)))
                    .then()
                    .statusCode(200)
                    .body(Matchers.notNullValue())
                    .extract().body().as(Holder.class);

            assertThat(res).usingRecursiveComparison().isEqualTo(expectedParticipant);
        }

        @Test
        void getById_notAuthorized(IssuerService issuer, HolderService service) {
            issuer.createParticipant(USER);

            var expectedParticipant = createHolder("test-participant-id", "did:web:foo", "foobar");
            service.createHolder(expectedParticipant);

            issuer.getAdminEndpoint().baseRequest()
                    .header(authorizeUser("anotherUser", issuer))
                    .get("/v1alpha/participants/%s/holders/test-participant-id".formatted(toBase64(USER)))
                    .then()
                    .statusCode(403);

        }

        protected abstract Header authorizeUser(String participantContextId, IssuerService issuerService);

        private Holder createHolder(String id, String did, String name) {
            return createHolder(id, did, name, USER);
        }

        private Holder createHolder(String id, String did, String name, String participantContextId) {
            return Holder.Builder.newInstance()
                    .participantContextId(participantContextId)
                    .holderId(id)
                    .did(did)
                    .holderName(name)
                    .build();
        }

        private String toBase64(String s) {
            return Base64.getUrlEncoder().encodeToString(s.getBytes());
        }
    }

    @Nested
    @EndToEndTest
    class InMemory extends Tests {

        @RegisterExtension
        static final RuntimeExtension ISSUER_EXTENSION = ComponentRuntimeExtension.Builder.newInstance()
                .name(ISSUER_RUNTIME_NAME)
                .modules(DefaultRuntimes.Issuer.MODULES)
                .endpoints(DefaultRuntimes.Issuer.ENDPOINTS.build())
                .configurationProvider(DefaultRuntimes.Issuer::config)
                .paramProvider(IssuerService.class, IssuerService::forContext)
                .build();

        @Override
        protected Header authorizeUser(String participantContextId, IssuerService issuerService) {
            return authorizeTokenBased(participantContextId, issuerService);
        }
    }

    @Nested
    @PostgresqlIntegrationTest
    class Postgres extends Tests {

        @Order(0)
        @RegisterExtension
        static final PostgresqlEndToEndExtension POSTGRESQL_EXTENSION = new PostgresqlEndToEndExtension();

        private static final String ISSUER = "issuer";

        @Order(1)
        @RegisterExtension
        static final BeforeAllCallback POSTGRES_CONTAINER_STARTER = context -> {
            POSTGRESQL_EXTENSION.createDatabase(ISSUER);
        };

        @Order(2)
        @RegisterExtension
        static final RuntimeExtension ISSUER_EXTENSION = ComponentRuntimeExtension.Builder.newInstance()
                .name(ISSUER_RUNTIME_NAME)
                .modules(DefaultRuntimes.Issuer.SQL_MODULES)
                .endpoints(DefaultRuntimes.Issuer.ENDPOINTS.build())
                .configurationProvider(DefaultRuntimes.Issuer::config)
                .configurationProvider(() -> POSTGRESQL_EXTENSION.configFor(ISSUER))
                .paramProvider(IssuerService.class, IssuerService::forContext)
                .build();

        @Override
        protected Header authorizeUser(String participantContextId, IssuerService issuerService) {
            return authorizeTokenBased(participantContextId, issuerService);
        }
    }

    @Nested
    @EndToEndTest
    class InMemoryOauth2 extends Tests {

        private static final String ISSUER = "issuer";

        @Order(0)
        @RegisterExtension
        static final OauthServerEndToEndExtension OAUTH_2_EXTENSION = OauthServerEndToEndExtension.Builder.newInstance()
                .issuer(ISSUER)
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
        protected Header authorizeUser(String participantContextId, IssuerService issuerService) {
            return authorizeOauth2(participantContextId, issuerService, OAUTH_2_EXTENSION.getAuthServer());
        }
    }

    @Nested
    @PostgresqlIntegrationTest
    class PostgresOauth2 extends Tests {
        @Order(0)
        @RegisterExtension
        static final PostgresqlEndToEndExtension POSTGRESQL_EXTENSION = new PostgresqlEndToEndExtension();
        private static final String ISSUER = "issuer";

        @Order(0)
        @RegisterExtension
        static final OauthServerEndToEndExtension OAUTH_2_EXTENSION = OauthServerEndToEndExtension.Builder.newInstance()
                .issuer(ISSUER)
                .signingKeyId("signing-key-id")
                .build();

        @Order(2)
        @RegisterExtension
        static final RuntimeExtension ISSUER_EXTENSION = ComponentRuntimeExtension.Builder.newInstance()
                .name(ISSUER_RUNTIME_NAME)
                .modules(DefaultRuntimes.Issuer.SQL_OAUTH2_MODULES)
                .endpoints(DefaultRuntimes.Issuer.ENDPOINTS.build())
                .configurationProvider(DefaultRuntimes.Issuer::config)
                .configurationProvider(OAUTH_2_EXTENSION::getConfig)
                .configurationProvider(() -> POSTGRESQL_EXTENSION.configFor(ISSUER))
                .paramProvider(IssuerService.class, IssuerService::forContext)
                .build();

        @Order(1)
        @RegisterExtension
        static final BeforeAllCallback POSTGRES_CONTAINER_STARTER = context -> {
            POSTGRESQL_EXTENSION.createDatabase(ISSUER);
        };

        @Override
        protected Header authorizeUser(String participantContextId, IssuerService issuerService) {
            return authorizeOauth2(participantContextId, issuerService, OAUTH_2_EXTENSION.getAuthServer());
        }
    }
}
