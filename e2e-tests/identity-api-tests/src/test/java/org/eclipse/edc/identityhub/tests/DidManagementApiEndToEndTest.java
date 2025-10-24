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

import io.restassured.http.Header;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.identitytrust.sts.spi.store.StsAccountStore;
import org.eclipse.edc.identityhub.spi.did.events.DidDocumentPublished;
import org.eclipse.edc.identityhub.spi.did.events.DidDocumentUnpublished;
import org.eclipse.edc.identityhub.spi.did.store.DidResourceStore;
import org.eclipse.edc.identityhub.spi.keypair.store.KeyPairResourceStore;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.identityhub.tests.fixtures.DefaultRuntimes;
import org.eclipse.edc.identityhub.tests.fixtures.credentialservice.IdentityHub;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Arrays;
import java.util.List;

import static io.restassured.http.ContentType.JSON;
import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.identityhub.tests.fixtures.TestData.IH_RUNTIME_NAME;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@SuppressWarnings("JUnitMalformedDeclaration")
public class DidManagementApiEndToEndTest {

    abstract static class Tests {

        @AfterEach
        void tearDown(ParticipantContextService pcService, DidResourceStore didResourceStore, KeyPairResourceStore keyPairResourceStore, StsAccountStore stsAccountStore) {
            // purge all users, dids, keypairs

            pcService.query(QuerySpec.max()).getContent()
                    .forEach(pc -> pcService.deleteParticipantContext(pc.getParticipantContextId()).getContent());

            didResourceStore.query(QuerySpec.max()).forEach(dr -> didResourceStore.deleteById(dr.getDid()).getContent());

            keyPairResourceStore.query(QuerySpec.max()).getContent()
                    .forEach(kpr -> keyPairResourceStore.deleteById(kpr.getId()).getContent());

            stsAccountStore.findAll(QuerySpec.max())
                    .forEach(sts -> stsAccountStore.deleteById(sts.getId()).getContent());
        }

        @Test
        void publishDid_notOwner_expect403(IdentityHub identityHub, EventRouter router) {
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(DidDocumentPublished.class, subscriber);

            var user1 = "user1";
            identityHub.createParticipant(user1);


            // create second user
            var user2 = "user2";
            var user2Context = ParticipantContext.Builder.newInstance()
                    .participantContextId(user2)
                    .did("did:web:" + user2)
                    .apiTokenAlias(user2 + "-alias")
                    .build();
            var user2Token = identityHub.storeParticipant(user2Context);

            reset(subscriber); // need to reset here, to ignore a previous interaction

            // attempt to publish user1's DID document, which should fail
            identityHub.getIdentityEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", user2Token))
                    .body("""
                            {
                               "did": "did:web:user1"
                            }
                            """)
                    .post("/v1alpha/participants/%s/dids/publish".formatted(user1))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403)
                    .body(Matchers.notNullValue());

            verifyNoInteractions(subscriber);
        }

        @Test
        void publishDid(IdentityHub identityHub, EventRouter router) {
            var superUserKey = identityHub.createSuperUser().apiKey();
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(DidDocumentPublished.class, subscriber);

            var user = "test-user";
            var token = identityHub.createParticipant(user).apiKey();

            assertThat(Arrays.asList(token, superUserKey))
                    .allSatisfy(t -> {
                        reset(subscriber);
                        identityHub.getIdentityEndpoint().baseRequest()
                                .contentType(JSON)
                                .header(new Header("x-api-key", t))
                                .body("""
                                        {
                                           "did": "did:web:test-user"
                                        }
                                        """)
                                .post("/v1alpha/participants/%s/dids/publish".formatted(user))
                                .then()
                                .log().ifValidationFails()
                                .statusCode(204)
                                .body(Matchers.notNullValue());

                        // verify that the publish event was fired twice
                        verify(subscriber).on(argThat(env -> {
                            if (env.getPayload() instanceof DidDocumentPublished event) {
                                return event.getDid().equals("did:web:test-user");
                            }
                            return false;
                        }));

                    });

        }


        @Test
        void publishDid_participantNotActivated_expect400(IdentityHub identityHub, EventRouter router) {
            var superUserKey = identityHub.createSuperUser().apiKey();
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(DidDocumentPublished.class, subscriber);

            var user = "test-user";
            var token = identityHub.createParticipant(user, List.of(), false).apiKey();

            assertThat(Arrays.asList(token, superUserKey))
                    .allSatisfy(t -> {
                        reset(subscriber);
                        identityHub.getIdentityEndpoint().baseRequest()
                                .contentType(JSON)
                                .header(new Header("x-api-key", t))
                                .body("""
                                        {
                                           "did": "did:web:test-user"
                                        }
                                        """)
                                .post("/v1alpha/participants/%s/dids/publish".formatted(user))
                                .then()
                                .log().ifValidationFails()
                                .statusCode(400)
                                .body(Matchers.containsString("Cannot publish DID 'did:web:test-user' for participant 'test-user' because the ParticipantContext state is not 'ACTIVATED', but 'CREATED'."));

                        // verify that the publish event was fired twice
                        verifyNoInteractions(subscriber);
                    });
        }

        @Test
        void unpublishDid_notOwner_expect403(IdentityHub identityHub, EventRouter router) {
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(DidDocumentPublished.class, subscriber);

            var user1 = "user1";
            identityHub.createParticipant(user1);


            // create second user
            var user2 = "user2";
            var user2Context = ParticipantContext.Builder.newInstance()
                    .participantContextId(user2)
                    .did("did:web:" + user2)
                    .apiTokenAlias(user2 + "-alias")
                    .build();
            var user2Token = identityHub.storeParticipant(user2Context);

            reset(subscriber); // need to reset here, to ignore a previous interaction

            // attempt to publish user1's DID document, which should fail
            identityHub.getIdentityEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", user2Token))
                    .body("""
                            {
                               "did": "did:web:user1"
                            }
                            """)
                    .post("/v1alpha/participants/%s/dids/unpublish".formatted(user1))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403)
                    .body(Matchers.notNullValue());

            verifyNoInteractions(subscriber);
        }

        @Test
        void unpublishDid_withSuperUserToken(IdentityHub identityHub, EventRouter router, ParticipantContextService participantContextService) {
            var superUserKey = identityHub.createSuperUser().apiKey();
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(DidDocumentUnpublished.class, subscriber);

            var user = "test-user";
            identityHub.createParticipant(user);

            participantContextService.updateParticipant(user, ParticipantContext::deactivate);

            reset(subscriber);
            identityHub.getIdentityEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", superUserKey))
                    .body("""
                            {
                               "did": "did:web:test-user"
                            }
                            """)
                    .post("/v1alpha/participants/%s/dids/unpublish".formatted(user))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(204)
                    .body(Matchers.notNullValue());

            // verify that the publish event was fired twice
            verifyNoInteractions(subscriber);
        }

        @Test
        void unpublishDid_withUserToken(IdentityHub identityHub, EventRouter router, ParticipantContextService participantContextService) {
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(DidDocumentUnpublished.class, subscriber);

            var user = "test-user";
            var token = identityHub.createParticipant(user).apiKey();

            participantContextService.updateParticipant(user, ParticipantContext::deactivate);


            reset(subscriber);
            identityHub.getIdentityEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", token))
                    .body("""
                            {
                               "did": "did:web:test-user"
                            }
                            """)
                    .post("/v1alpha/participants/%s/dids/unpublish".formatted(user))
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

            var user = "test-user";
            var token = identityHub.createParticipant(user, List.of(), true).apiKey();

            reset(subscriber);
            identityHub.getIdentityEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", token))
                    .body("""
                            {
                               "did": "did:web:test-user"
                            }
                            """)
                    .post("/v1alpha/participants/%s/dids/unpublish".formatted(user))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400)
                    .body(Matchers.containsString("Cannot un-publish DID 'did:web:test-user' for participant 'test-user' because the ParticipantContext is not 'DEACTIVATED' state, but was 'ACTIVATED'."));

            // verify that the unpublish event was fired
            verifyNoInteractions(subscriber);
        }

        @Test
        void getState_nowOwner_expect403(IdentityHub identityHub) {
            var user1 = "user1";
            identityHub.createParticipant(user1);

            var user2 = "user2";
            var token2 = identityHub.createParticipant(user2).apiKey();

            identityHub.getIdentityEndpoint().baseRequest()
                    .header(new Header("x-api-key", token2))
                    .contentType(JSON)
                    .body(""" 
                            {
                               "did": "did:web:user1"
                            }
                            """)
                    .post("/v1alpha/participants/%s/dids/state".formatted(user1))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403);
        }

        @Test
        void getAll(IdentityHub identityHub) {
            var superUserKey = identityHub.createSuperUser().apiKey();
            range(0, 20).forEach(i -> identityHub.createParticipant("user-" + i));

            var docs = identityHub.getIdentityEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", superUserKey))
                    .get("/v1alpha/dids")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .extract().body().as(DidDocument[].class);

            assertThat(docs).hasSize(21); //includes the super-user's DID doc
        }

        @Test
        void getAll_withDefaultPaging(IdentityHub identityHub) {
            var superUserKey = identityHub.createSuperUser().apiKey();
            range(0, 70).forEach(i -> identityHub.createParticipant("user-" + i));

            var docs = identityHub.getIdentityEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", superUserKey))
                    .get("/v1alpha/dids")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .extract().body().as(DidDocument[].class);

            assertThat(docs).hasSize(50); //includes the super-user's DID doc
        }

        @Test
        void getAll_withPaging(IdentityHub identityHub) {
            var superUserKey = identityHub.createSuperUser().apiKey();
            range(0, 20).forEach(i -> identityHub.createParticipant("user-" + i));

            var docs = identityHub.getIdentityEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", superUserKey))
                    .get("/v1alpha/dids?offset=5&limit=10")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .extract().body().as(DidDocument[].class);

            assertThat(docs).hasSize(10);
        }

        @Test
        void getAll_notAuthorized(IdentityHub identityHub) {

            var attackerToken = identityHub.createParticipant("attacker").apiKey();

            identityHub.getIdentityEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", attackerToken))
                    .get("/v1alpha/dids")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403);
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
    }
}
