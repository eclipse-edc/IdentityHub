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
import org.eclipse.edc.identityhub.spi.events.diddocument.DidDocumentPublished;
import org.eclipse.edc.identityhub.spi.events.diddocument.DidDocumentUnpublished;
import org.eclipse.edc.identityhub.spi.events.keypair.KeyPairAdded;
import org.eclipse.edc.identityhub.spi.model.participant.ParticipantContext;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import static io.restassured.http.ContentType.JSON;
import static org.mockito.Mockito.*;

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
                .post("/v1/dids/publish")
                .then()
                .log().ifValidationFails()
                .statusCode(403)
                .body(Matchers.notNullValue());

        verifyNoInteractions(subscriber);
    }

    @Test
    void publishDid() {

        var subscriber = mock(EventSubscriber.class);
        getService(EventRouter.class).registerSync(DidDocumentPublished.class, subscriber);

        var user = "test-user";
        var token = createParticipant(user);
        RUNTIME_CONFIGURATION.getManagementEndpoint().baseRequest()
                .contentType(JSON)
                .header(new Header("x-api-key", token))
                .body("""
                        {
                           "did": "did:web:test-user"
                        }
                        """)
                .post("/v1/dids/publish")
                .then()
                .log().ifValidationFails()
                .statusCode(204)
                .body(Matchers.notNullValue());

        // verify that the publish event was fired twice
        verify(subscriber, times(2)).on(argThat(env -> {
            if(env.getPayload() instanceof DidDocumentPublished event){
                return event.getDid().equals("did:web:test-user");
            }
            return false;
        }));
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
                .post("/v1/dids/unpublish")
                .then()
                .log().ifValidationFails()
                .statusCode(403)
                .body(Matchers.notNullValue());

        verifyNoInteractions(subscriber);
    }
    @Test
    void unpublishDid() {

        var subscriber = mock(EventSubscriber.class);
        getService(EventRouter.class).registerSync(DidDocumentUnpublished.class, subscriber);

        var user = "test-user";
        var token = createParticipant(user);
        RUNTIME_CONFIGURATION.getManagementEndpoint().baseRequest()
                .contentType(JSON)
                .header(new Header("x-api-key", token))
                .body("""
                        {
                           "did": "did:web:test-user"
                        }
                        """)
                .post("/v1/dids/unpublish")
                .then()
                .log().ifValidationFails()
                .statusCode(204)
                .body(Matchers.notNullValue());

        // verify that the publish event was fired twice
        verify(subscriber).on(argThat(env -> {
            if(env.getPayload() instanceof DidDocumentUnpublished event){
                return event.getDid().equals("did:web:test-user");
            }
            return false;
        }));
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
                .post("/v1/dids/state")
                .then()
                .log().ifValidationFails()
                .statusCode(403);
    }

}
