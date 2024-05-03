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
import org.eclipse.edc.identityhub.spi.events.diddocument.DidDocumentPublished;
import org.eclipse.edc.identityhub.spi.events.diddocument.DidDocumentUnpublished;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static io.restassured.http.ContentType.JSON;
import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@EndToEndTest
public class DidManagementApiEndToEndTest extends ManagementApiEndToEndTest {

    @Test
    void publishDid_notOwner_expect403() {
        var subscriber = mock(EventSubscriber.class);
        getService(EventRouter.class).registerSync(DidDocumentPublished.class, subscriber);

        var user1 = "user1";
        createParticipant(user1);


        // create second user
        var user2 = "user2";
        var user2Context = ParticipantContext.Builder.newInstance()
                .participantId(user2)
                .did("did:web:" + user2)
                .apiTokenAlias(user2 + "-alias")
                .build();
        var user2Token = storeParticipant(user2Context);

        reset(subscriber); // need to reset here, to ignore a previous interaction

        // attempt to publish user1's DID document, which should fail
        RUNTIME_CONFIGURATION.getManagementEndpoint().baseRequest()
                .contentType(JSON)
                .header(new Header("x-api-key", user2Token))
                .body("""
                        {
                           "did": "did:web:user1"
                        }
                        """)
                .post("/v1/participants/%s/dids/publish".formatted(user1))
                .then()
                .log().ifValidationFails()
                .statusCode(403)
                .body(Matchers.notNullValue());

        verifyNoInteractions(subscriber);
    }

    @Test
    void publishDid() {
        var superUserKey = createSuperUser();
        var subscriber = mock(EventSubscriber.class);
        getService(EventRouter.class).registerSync(DidDocumentPublished.class, subscriber);

        var user = "test-user";
        var token = createParticipant(user);

        assertThat(Arrays.asList(token, superUserKey))
                .allSatisfy(t -> {
                    reset(subscriber);
                    RUNTIME_CONFIGURATION.getManagementEndpoint().baseRequest()
                            .contentType(JSON)
                            .header(new Header("x-api-key", t))
                            .body("""
                                    {
                                       "did": "did:web:test-user"
                                    }
                                    """)
                            .post("/v1/participants/%s/dids/publish".formatted(user))
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
    void unpublishDid_notOwner_expect403() {
        var subscriber = mock(EventSubscriber.class);
        getService(EventRouter.class).registerSync(DidDocumentPublished.class, subscriber);

        var user1 = "user1";
        createParticipant(user1);


        // create second user
        var user2 = "user2";
        var user2Context = ParticipantContext.Builder.newInstance()
                .participantId(user2)
                .did("did:web:" + user2)
                .apiTokenAlias(user2 + "-alias")
                .build();
        var user2Token = storeParticipant(user2Context);

        reset(subscriber); // need to reset here, to ignore a previous interaction

        // attempt to publish user1's DID document, which should fail
        RUNTIME_CONFIGURATION.getManagementEndpoint().baseRequest()
                .contentType(JSON)
                .header(new Header("x-api-key", user2Token))
                .body("""
                        {
                           "did": "did:web:user1"
                        }
                        """)
                .post("/v1/participants/%s/dids/unpublish".formatted(user1))
                .then()
                .log().ifValidationFails()
                .statusCode(403)
                .body(Matchers.notNullValue());

        verifyNoInteractions(subscriber);
    }

    @Test
    void unpublishDid() {
        var superUserKey = createSuperUser();
        var subscriber = mock(EventSubscriber.class);
        getService(EventRouter.class).registerSync(DidDocumentUnpublished.class, subscriber);

        var user = "test-user";
        var token = createParticipant(user);

        assertThat(Arrays.asList(token, superUserKey))
                .allSatisfy(t -> {
                    reset(subscriber);
                    RUNTIME_CONFIGURATION.getManagementEndpoint().baseRequest()
                            .contentType(JSON)
                            .header(new Header("x-api-key", t))
                            .body("""
                                    {
                                       "did": "did:web:test-user"
                                    }
                                    """)
                            .post("/v1/participants/%s/dids/unpublish".formatted(user))
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
                });

    }

    @Test
    void getState_nowOwner_expect403() {
        var user1 = "user1";
        createParticipant(user1);

        var user2 = "user2";
        var token2 = createParticipant(user2);

        RUNTIME_CONFIGURATION.getManagementEndpoint().baseRequest()
                .header(new Header("x-api-key", token2))
                .contentType(JSON)
                .body(""" 
                        {
                           "did": "did:web:user1"
                        }
                        """)
                .post("/v1/participants/%s/dids/state".formatted(user1))
                .then()
                .log().ifValidationFails()
                .statusCode(403);
    }

    @Test
    void getAll() {
        var superUserKey = createSuperUser();
        range(0, 20).forEach(i -> createParticipant("user-" + i));

        var docs = RUNTIME_CONFIGURATION.getManagementEndpoint().baseRequest()
                .contentType(JSON)
                .header(new Header("x-api-key", superUserKey))
                .get("/v1/dids")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract().body().as(DidDocument[].class);

        assertThat(docs).hasSize(21); //includes the super-user's DID doc
    }

    @Test
    void getAll_withDefaultPaging() {
        var superUserKey = createSuperUser();
        range(0, 70).forEach(i -> createParticipant("user-" + i));

        var docs = RUNTIME_CONFIGURATION.getManagementEndpoint().baseRequest()
                .contentType(JSON)
                .header(new Header("x-api-key", superUserKey))
                .get("/v1/dids")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract().body().as(DidDocument[].class);

        assertThat(docs).hasSize(50); //includes the super-user's DID doc
    }

    @Test
    void getAll_withPaging() {
        var superUserKey = createSuperUser();
        range(0, 20).forEach(i -> createParticipant("user-" + i));

        var docs = RUNTIME_CONFIGURATION.getManagementEndpoint().baseRequest()
                .contentType(JSON)
                .header(new Header("x-api-key", superUserKey))
                .get("/v1/dids?offset=5&limit=10")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract().body().as(DidDocument[].class);

        assertThat(docs).hasSize(10);
    }

    @Test
    void getAll_notAuthorized() {

        var attackerToken = createParticipant("attacker");

        RUNTIME_CONFIGURATION.getManagementEndpoint().baseRequest()
                .contentType(JSON)
                .header(new Header("x-api-key", attackerToken))
                .get("/v1/dids")
                .then()
                .log().ifValidationFails()
                .statusCode(403);
    }
}
