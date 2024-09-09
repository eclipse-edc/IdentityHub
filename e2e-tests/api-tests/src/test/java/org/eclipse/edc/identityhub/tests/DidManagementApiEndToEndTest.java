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
import org.eclipse.edc.identithub.spi.did.events.DidDocumentPublished;
import org.eclipse.edc.identithub.spi.did.events.DidDocumentUnpublished;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.identityhub.tests.fixtures.IdentityHubEndToEndExtension;
import org.eclipse.edc.identityhub.tests.fixtures.IdentityHubEndToEndTestContext;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.query.QuerySpec;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;

import static io.restassured.http.ContentType.JSON;
import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class DidManagementApiEndToEndTest {

    abstract static class Tests {

        @AfterEach
        void tearDown(ParticipantContextService store) {
            // purge all users
            store.query(QuerySpec.max()).getContent()
                    .forEach(pc -> store.deleteParticipantContext(pc.getParticipantId()));
        }

        @Test
        void publishDid_notOwner_expect403(IdentityHubEndToEndTestContext context, EventRouter router) {
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(DidDocumentPublished.class, subscriber);

            var user1 = "user1";
            context.createParticipant(user1);


            // create second user
            var user2 = "user2";
            var user2Context = ParticipantContext.Builder.newInstance()
                    .participantId(user2)
                    .did("did:web:" + user2)
                    .apiTokenAlias(user2 + "-alias")
                    .build();
            var user2Token = context.storeParticipant(user2Context);

            reset(subscriber); // need to reset here, to ignore a previous interaction

            // attempt to publish user1's DID document, which should fail
            context.getIdentityApiEndpoint().baseRequest()
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
        void publishDid(IdentityHubEndToEndTestContext context, EventRouter router) {
            var superUserKey = context.createSuperUser();
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(DidDocumentPublished.class, subscriber);

            var user = "test-user";
            var token = context.createParticipant(user);

            assertThat(Arrays.asList(token, superUserKey))
                    .allSatisfy(t -> {
                        reset(subscriber);
                        context.getIdentityApiEndpoint().baseRequest()
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
        void unpublishDid_notOwner_expect403(IdentityHubEndToEndTestContext context, EventRouter router) {
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(DidDocumentPublished.class, subscriber);

            var user1 = "user1";
            context.createParticipant(user1);


            // create second user
            var user2 = "user2";
            var user2Context = ParticipantContext.Builder.newInstance()
                    .participantId(user2)
                    .did("did:web:" + user2)
                    .apiTokenAlias(user2 + "-alias")
                    .build();
            var user2Token = context.storeParticipant(user2Context);

            reset(subscriber); // need to reset here, to ignore a previous interaction

            // attempt to publish user1's DID document, which should fail
            context.getIdentityApiEndpoint().baseRequest()
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
        void unpublishDid_withSuperUserToken(IdentityHubEndToEndTestContext context, EventRouter router) {
            var superUserKey = context.createSuperUser();
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(DidDocumentUnpublished.class, subscriber);

            var user = "test-user";
            context.createParticipant(user);

            reset(subscriber);
            context.getIdentityApiEndpoint().baseRequest()
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
            verify(subscriber).on(argThat(env -> {
                if (env.getPayload() instanceof DidDocumentUnpublished event) {
                    return event.getDid().equals("did:web:test-user");
                }
                return false;
            }));
        }

        @Test
        void unpublishDid_withUserToken(IdentityHubEndToEndTestContext context, EventRouter router) {
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(DidDocumentUnpublished.class, subscriber);

            var user = "test-user";
            var token = context.createParticipant(user);

            reset(subscriber);
            context.getIdentityApiEndpoint().baseRequest()
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
            verify(subscriber).on(argThat(env -> {
                if (env.getPayload() instanceof DidDocumentUnpublished event) {
                    return event.getDid().equals("did:web:test-user");
                }
                return false;
            }));
        }

        @Test
        void getState_nowOwner_expect403(IdentityHubEndToEndTestContext context) {
            var user1 = "user1";
            context.createParticipant(user1);

            var user2 = "user2";
            var token2 = context.createParticipant(user2);

            context.getIdentityApiEndpoint().baseRequest()
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
        void getAll(IdentityHubEndToEndTestContext context) {
            var superUserKey = context.createSuperUser();
            range(0, 20).forEach(i -> context.createParticipant("user-" + i));

            var docs = context.getIdentityApiEndpoint().baseRequest()
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
        void getAll_withDefaultPaging(IdentityHubEndToEndTestContext context) {
            var superUserKey = context.createSuperUser();
            range(0, 70).forEach(i -> context.createParticipant("user-" + i));

            var docs = context.getIdentityApiEndpoint().baseRequest()
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
        void getAll_withPaging(IdentityHubEndToEndTestContext context) {
            var superUserKey = context.createSuperUser();
            range(0, 20).forEach(i -> context.createParticipant("user-" + i));

            var docs = context.getIdentityApiEndpoint().baseRequest()
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
        void getAll_notAuthorized(IdentityHubEndToEndTestContext context) {

            var attackerToken = context.createParticipant("attacker");

            context.getIdentityApiEndpoint().baseRequest()
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
    @ExtendWith(IdentityHubEndToEndExtension.InMemory.class)
    class InMemory extends Tests {
    }

    @Nested
    @PostgresqlIntegrationTest
    @ExtendWith(IdentityHubEndToEndExtension.Postgres.class)
    class Postgres extends Tests {
    }
}
