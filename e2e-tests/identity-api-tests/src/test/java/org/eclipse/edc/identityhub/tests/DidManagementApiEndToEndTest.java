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

package org.eclipse.edc.identityhub.tests;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.restassured.http.Header;
import org.eclipse.edc.iam.decentralizedclaims.sts.spi.store.StsAccountStore;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.identityhub.spi.did.events.DidDocumentPublished;
import org.eclipse.edc.identityhub.spi.did.events.DidDocumentUnpublished;
import org.eclipse.edc.identityhub.spi.did.store.DidResourceStore;
import org.eclipse.edc.identityhub.spi.keypair.store.KeyPairResourceStore;
import org.eclipse.edc.identityhub.spi.participantcontext.IdentityHubParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.IdentityHubParticipantContext;
import org.eclipse.edc.identityhub.tests.fixtures.DefaultRuntimes;
import org.eclipse.edc.identityhub.tests.fixtures.common.Oauth2Extension;
import org.eclipse.edc.identityhub.tests.fixtures.credentialservice.IdentityHub;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Arrays;
import java.util.Base64;
import java.util.Map;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.restassured.http.ContentType.JSON;
import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.identityhub.tests.fixtures.TestData.IH_RUNTIME_NAME;
import static org.eclipse.edc.identityhub.tests.fixtures.TestFunctions.authorizeOauth2;
import static org.eclipse.edc.identityhub.tests.fixtures.TestFunctions.authorizeTokenBased;
import static org.eclipse.edc.identityhub.tests.fixtures.common.AbstractIdentityHub.SUPER_USER;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@SuppressWarnings("JUnitMalformedDeclaration")
public class DidManagementApiEndToEndTest {

    public static final String PARTICIPANT_CONTEXT_ID = "user1";

    abstract static class Tests {

        @AfterEach
        void tearDown(IdentityHubParticipantContextService pcService, DidResourceStore didResourceStore, KeyPairResourceStore keyPairResourceStore, StsAccountStore stsAccountStore) {
            // purge all users, dids, keypairs

            pcService.query(QuerySpec.max()).getContent()
                    .forEach(pc -> pcService.deleteParticipantContext(pc.getParticipantContextId()).orElseThrow(f -> new EdcException(f.getFailureDetail())));

            didResourceStore.query(QuerySpec.max()).forEach(dr -> didResourceStore.deleteById(dr.getDid()).orElseThrow(f -> new EdcException(f.getFailureDetail())));

            keyPairResourceStore.query(QuerySpec.max()).getContent()
                    .forEach(kpr -> keyPairResourceStore.deleteById(kpr.getId()).orElseThrow(f -> new EdcException(f.getFailureDetail())));

            stsAccountStore.findAll(QuerySpec.max())
                    .forEach(sts -> stsAccountStore.deleteById(sts.getId()).orElseThrow(f -> new EdcException(f.getFailureDetail())));
        }

        @Test
        void publishDid_notOwner_expect403(IdentityHub identityHub, EventRouter router) {
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(DidDocumentPublished.class, subscriber);

            var userAuth = authorizeUser(PARTICIPANT_CONTEXT_ID, identityHub);

            // create second user
            var user2 = "user2";
            var user2Context = IdentityHubParticipantContext.Builder.newInstance()
                    .participantContextId(user2)
                    .did("did:web:" + user2)
                    .apiTokenAlias(user2 + "-alias")
                    .build();
            identityHub.storeParticipant(user2Context);
            var user2Auth = authorizeUser(user2, identityHub);

            reset(subscriber); // need to reset here, to ignore a previous interaction

            // attempt to publish user1's DID document, which should fail
            identityHub.getIdentityEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(user2Auth)
                    .body("""
                            {
                               "did": "did:web:user1"
                            }
                            """)
                    .post("/v1alpha/participants/%s/dids/publish".formatted(toBase64(PARTICIPANT_CONTEXT_ID)))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403)
                    .body(Matchers.notNullValue());

            verifyNoInteractions(subscriber);
        }

        @Test
        void publishDid(IdentityHub identityHub, EventRouter router) {
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(DidDocumentPublished.class, subscriber);

            var superUserAuth = authorizeUser(SUPER_USER, identityHub);
            var userAuth = authorizeUser(PARTICIPANT_CONTEXT_ID, identityHub);
            var did = "did:web:" + PARTICIPANT_CONTEXT_ID;

            assertThat(Arrays.asList(userAuth, superUserAuth))
                    .allSatisfy(t -> {
                        reset(subscriber);
                        identityHub.getIdentityEndpoint().baseRequest()
                                .contentType(JSON)
                                .header(t)
                                .body("""
                                        {
                                           "did": "%s"
                                        }
                                        """.formatted(did))
                                .post("/v1alpha/participants/%s/dids/publish".formatted(toBase64(PARTICIPANT_CONTEXT_ID)))
                                .then()
                                .log().ifValidationFails()
                                .statusCode(204)
                                .body(Matchers.notNullValue());

                        // verify that the publish event was fired twice
                        verify(subscriber).on(argThat(env -> {
                            if (env.getPayload() instanceof DidDocumentPublished event) {
                                return event.getDid().equals(did);
                            }
                            return false;
                        }));
                    });
        }

        @Test
        void publishDid_participantNotActivated_expect400(IdentityHub identityHub, EventRouter router, IdentityHubParticipantContextService service) {
            var superUserAuth = authorizeUser(SUPER_USER, identityHub);
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(DidDocumentPublished.class, subscriber);

            var did = "did:web:" + PARTICIPANT_CONTEXT_ID;
            var p = identityHub.buildParticipantManifest(PARTICIPANT_CONTEXT_ID, PARTICIPANT_CONTEXT_ID + "-key")
                    .active(false)
                    .did(did)
                    .build();
            service.createParticipantContext(p).orElseThrow(f -> new EdcException(f.getFailureDetail()));
            var token = authorizeUser(p.getParticipantContextId(), identityHub);

            assertThat(Arrays.asList(token, superUserAuth))
                    .allSatisfy(t -> {
                        reset(subscriber);
                        identityHub.getIdentityEndpoint().baseRequest()
                                .contentType(JSON)
                                .header(t)
                                .body("""
                                        {
                                           "did": "%s"
                                        }
                                        """.formatted(did))
                                .post("/v1alpha/participants/%s/dids/publish".formatted(toBase64(PARTICIPANT_CONTEXT_ID)))
                                .then()
                                .log().ifValidationFails()
                                .statusCode(400)
                                .body(Matchers.matchesRegex(".*Cannot publish DID 'did:web:.*' for participant '.*' because the ParticipantContext state is not 'ACTIVATED', but 'CREATED'.*"));

                        // verify that the publish event was fired twice
                        verifyNoInteractions(subscriber);
                    });
        }

        @Test
        void unpublishDid_notOwner_expect403(IdentityHub identityHub, EventRouter router) {
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(DidDocumentPublished.class, subscriber);

            var did = "did:web:" + PARTICIPANT_CONTEXT_ID;
            identityHub.createParticipant(PARTICIPANT_CONTEXT_ID);

            // create second user
            var user2 = "user2";
            var user2Auth = authorizeUser(user2, identityHub);

            reset(subscriber); // need to reset here, to ignore a previous interaction

            // attempt to publish user1's DID document, which should fail
            identityHub.getIdentityEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(user2Auth)
                    .body("""
                            {
                               "did": "%s"
                            }
                            """.formatted(did))
                    .post("/v1alpha/participants/%s/dids/unpublish".formatted(toBase64(PARTICIPANT_CONTEXT_ID)))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403)
                    .body(Matchers.notNullValue());

            verifyNoInteractions(subscriber);
        }

        @Test
        void unpublishDid_withSuperUserToken(IdentityHub identityHub, EventRouter router, IdentityHubParticipantContextService participantContextService) {
            var superUserKey = authorizeUser(SUPER_USER, identityHub);
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(DidDocumentUnpublished.class, subscriber);

            identityHub.createParticipant(PARTICIPANT_CONTEXT_ID);

            participantContextService.updateParticipant(PARTICIPANT_CONTEXT_ID, IdentityHubParticipantContext::deactivate);

            reset(subscriber);
            identityHub.getIdentityEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(superUserKey)
                    .body("""
                            {
                               "did": "did:web:%s"
                            }
                            """.formatted(PARTICIPANT_CONTEXT_ID))
                    .post("/v1alpha/participants/%s/dids/unpublish".formatted(toBase64(PARTICIPANT_CONTEXT_ID)))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(204)
                    .body(Matchers.notNullValue());

            // verify that the publish event was fired twice
            verifyNoInteractions(subscriber);
        }

        @Test
        void unpublishDid_withUserToken(IdentityHub identityHub, EventRouter router, IdentityHubParticipantContextService participantContextService) {
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(DidDocumentUnpublished.class, subscriber);

            var user = PARTICIPANT_CONTEXT_ID;
            var userAuth = authorizeUser(user, identityHub);

            participantContextService.updateParticipant(user, IdentityHubParticipantContext::deactivate);

            reset(subscriber);
            identityHub.getIdentityEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(userAuth)
                    .body("""
                            {
                               "did": "did:web:%s"
                            }
                            """.formatted(PARTICIPANT_CONTEXT_ID))
                    .post("/v1alpha/participants/%s/dids/unpublish".formatted(toBase64(user)))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(204)
                    .body(Matchers.notNullValue());

            // verify that the unpublish event was fired
            verifyNoInteractions(subscriber);
        }

        @Test
        void unpublishDid_participantActive_expect400(IdentityHub identityHub, EventRouter router) {
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(DidDocumentUnpublished.class, subscriber);

            var user = PARTICIPANT_CONTEXT_ID;
            var did = "did:web:" + user;
            var userAuth = authorizeUser(user, identityHub);

            reset(subscriber);
            identityHub.getIdentityEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(userAuth)
                    .body("""
                            {
                               "did": "%s"
                            }
                            """.formatted(did))
                    .post("/v1alpha/participants/%s/dids/unpublish".formatted(toBase64(user)))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400)
                    .body(containsString("Cannot un-publish DID '%s' for participant '%s' because the ParticipantContext is not 'DEACTIVATED' state, but was 'ACTIVATED'."
                            .formatted(did, user)));

            // verify that the unpublish event was fired
            verifyNoInteractions(subscriber);
        }

        @Test
        void getState_nowOwner_expect403(IdentityHub identityHub) {
            var user1 = PARTICIPANT_CONTEXT_ID;
            identityHub.createParticipant(user1);

            var user2 = "user2";
            var token2 = authorizeUser(user2, identityHub);

            identityHub.getIdentityEndpoint().baseRequest()
                    .header(token2)
                    .contentType(JSON)
                    .body(""" 
                            {
                               "did": "did:web:%s"
                            }
                            """.formatted(user1))
                    .post("/v1alpha/participants/%s/dids/state".formatted(toBase64(user1)))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403);
        }

        @Test
        void getAll(IdentityHub identityHub) {
            var superUserAuth = authorizeUser(SUPER_USER, identityHub);
            range(0, 20).forEach(i -> identityHub.createParticipant("user-" + i));

            var docs = identityHub.getIdentityEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(superUserAuth)
                    .get("/v1alpha/dids")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .extract().body().as(DidDocument[].class);

            assertThat(docs).hasSize(21); //includes the super-user's DID doc
        }

        @Test
        void getAll_withDefaultPaging(IdentityHub identityHub) {
            var superUserKey = authorizeUser(SUPER_USER, identityHub);
            range(0, 70).forEach(i -> identityHub.createParticipant("user-" + i));

            var docs = identityHub.getIdentityEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(superUserKey)
                    .get("/v1alpha/dids")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .extract().body().as(DidDocument[].class);

            assertThat(docs).hasSize(50); //includes the super-user's DID doc
        }

        @Test
        void getAll_withPaging(IdentityHub identityHub) {
            var superUserAuth = authorizeUser(SUPER_USER, identityHub);
            range(0, 20).forEach(i -> identityHub.createParticipant("user-" + i));

            var docs = identityHub.getIdentityEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(superUserAuth)
                    .get("/v1alpha/dids?offset=5&limit=10")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .extract().body().as(DidDocument[].class);

            assertThat(docs).hasSize(10);
        }

        @Test
        void getAll_notAuthorized(IdentityHub identityHub) {

            var attackerToken = authorizeUser("attacker", identityHub);

            identityHub.getIdentityEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(attackerToken)
                    .get("/v1alpha/dids")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403);
        }

        protected abstract Header authorizeUser(String participantContextId, IdentityHub identityHub);


        private String toBase64(String s) {
            return Base64.getUrlEncoder().encodeToString(s.getBytes());
        }

    }

    @Nested
    @EndToEndTest
    class InMemory extends Tests {

        @RegisterExtension
        static final RuntimeExtension IDENTITY_HUB_EXTENSION = ComponentRuntimeExtension.Builder.newInstance()
                .name(IH_RUNTIME_NAME)
                .modules(DefaultRuntimes.IdentityHub.MODULES)
                .endpoints(DefaultRuntimes.IdentityHub.ENDPOINTS.build())
                .configurationProvider(DefaultRuntimes.IdentityHub::config)
                .paramProvider(IdentityHub.class, IdentityHub::forContext)
                .build();

        @Override
        protected Header authorizeUser(String participantContextId, IdentityHub identityHub) {
            return authorizeTokenBased(participantContextId, identityHub);
        }
    }

    @Nested
    @PostgresqlIntegrationTest
    class Postgres extends Tests {

        @Order(0)
        @RegisterExtension
        static final PostgresqlEndToEndExtension POSTGRESQL_EXTENSION = new PostgresqlEndToEndExtension();
        private static final String DB_NAME = "runtime";

        @Order(1)
        @RegisterExtension
        static final BeforeAllCallback POSTGRES_CONTAINER_STARTER = context -> {
            POSTGRESQL_EXTENSION.createDatabase(DB_NAME);
        };

        @Order(2)
        @RegisterExtension
        static final RuntimeExtension IDENTITY_HUB_EXTENSION = ComponentRuntimeExtension.Builder.newInstance()
                .name(IH_RUNTIME_NAME)
                .modules(DefaultRuntimes.IdentityHub.SQL_MODULES)
                .endpoints(DefaultRuntimes.IdentityHub.ENDPOINTS.build())
                .configurationProvider(DefaultRuntimes.IdentityHub::config)
                .configurationProvider(() -> POSTGRESQL_EXTENSION.configFor(DB_NAME))
                .paramProvider(IdentityHub.class, IdentityHub::forContext)
                .build();

        @Override
        protected Header authorizeUser(String participantContextId, IdentityHub identityHub) {
            return authorizeTokenBased(participantContextId, identityHub);
        }
    }

    @Nested
    @EndToEndTest
    class InMemoryOauth2 extends Tests {

        private static final String SOMEISSUER = "someissuer";
        @Order(0)
        @RegisterExtension
        static WireMockExtension mockJwksServer = WireMockExtension.newInstance()
                .options(wireMockConfig().dynamicPort())
                .build();
        @Order(1)
        @RegisterExtension
        static final RuntimeExtension IDENTITY_HUB_EXTENSION = ComponentRuntimeExtension.Builder.newInstance()
                .name(IH_RUNTIME_NAME)
                .modules(DefaultRuntimes.IdentityHub.MODULES_OAUTH2)
                .endpoints(DefaultRuntimes.IdentityHub.ENDPOINTS.build())
                .configurationProvider(DefaultRuntimes.IdentityHub::config)
                .configurationProvider(() -> ConfigFactory.fromMap(Map.of(
                        "edc.iam.oauth2.issuer", SOMEISSUER,
                        "edc.iam.oauth2.jwks.url", mockJwksServer.baseUrl() + "/.well-known/jwks.json")))
                .paramProvider(IdentityHub.class, IdentityHub::forContext)
                .build();

        @Order(100)
        @RegisterExtension
        static final Oauth2Extension OAUTH_2_EXTENSION = new Oauth2Extension(mockJwksServer);

        @Override
        protected Header authorizeUser(String participantContextId, IdentityHub identityHub) {
            return authorizeOauth2(participantContextId, identityHub, OAUTH_2_EXTENSION);
        }
    }

    @Nested
    @EndToEndTest
    class PostgresOauth2 extends Tests {
        @Order(0)
        @RegisterExtension
        static final PostgresqlEndToEndExtension POSTGRESQL_EXTENSION = new PostgresqlEndToEndExtension();
        private static final String DB_NAME = "runtime";
        @Order(1)
        @RegisterExtension
        static final BeforeAllCallback POSTGRES_CONTAINER_STARTER = context -> {
            POSTGRESQL_EXTENSION.createDatabase(DB_NAME);
        };
        @Order(0)
        @RegisterExtension
        static WireMockExtension mockJwksServer = WireMockExtension.newInstance()
                .options(wireMockConfig().dynamicPort())
                .build();
        @Order(100)
        @RegisterExtension
        static final Oauth2Extension OAUTH_2_EXTENSION = new Oauth2Extension(mockJwksServer);
        @Order(2)
        @RegisterExtension
        static final RuntimeExtension IDENTITY_HUB_EXTENSION = ComponentRuntimeExtension.Builder.newInstance()
                .name(IH_RUNTIME_NAME)
                .modules(DefaultRuntimes.IdentityHub.SQL_OAUTH2_MODULES)
                .endpoints(DefaultRuntimes.IdentityHub.ENDPOINTS.build())
                .configurationProvider(DefaultRuntimes.IdentityHub::config)
                .configurationProvider(() -> POSTGRESQL_EXTENSION.configFor(DB_NAME))
                .configurationProvider(() -> ConfigFactory.fromMap(Map.of(
                        "edc.iam.oauth2.issuer", "someissuer",
                        "edc.iam.oauth2.jwks.url", mockJwksServer.baseUrl() + "/.well-known/jwks.json")))
                .paramProvider(IdentityHub.class, IdentityHub::forContext)
                .build();

        @Override
        protected Header authorizeUser(String participantContextId, IdentityHub identityHub) {
            return authorizeOauth2(participantContextId, identityHub, OAUTH_2_EXTENSION);
        }
    }
}
