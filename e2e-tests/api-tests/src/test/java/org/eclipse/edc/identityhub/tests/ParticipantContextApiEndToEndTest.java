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

import io.restassured.http.ContentType;
import io.restassured.http.Header;
import org.eclipse.edc.identityhub.spi.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.events.ParticipantContextCreated;
import org.eclipse.edc.identityhub.spi.events.ParticipantContextUpdated;
import org.eclipse.edc.identityhub.spi.model.participant.ParticipantContext;
import org.eclipse.edc.identityhub.spi.model.participant.ParticipantContextState;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@EndToEndTest
public class ParticipantContextApiEndToEndTest extends ManagementApiEndToEndTest {

    @Test
    void getUserById() {
        var apikey = getSuperUserApiKey();

        var su = RUNTIME_CONFIGURATION.getManagementEndpoint().baseRequest()
                .header(new Header("x-api-key", apikey))
                .get("/v1/participants/" + SUPER_USER)
                .then()
                .statusCode(200)
                .extract().body().as(ParticipantContext.class);
        assertThat(su.getParticipantId()).isEqualTo(SUPER_USER);
    }

    @Test
    void getUserById_notOwner_expect403() {
        var user1 = "user1";
        var user1Context = ParticipantContext.Builder.newInstance()
                .participantId(user1)
                .did("did:web:" + user1)
                .apiTokenAlias(user1 + "-alias")
                .build();
        var apiToken1 = storeParticipant(user1Context);

        var user2 = "user2";
        var user2Context = ParticipantContext.Builder.newInstance()
                .participantId(user2)
                .did("did:web:" + user2)
                .apiTokenAlias(user2 + "-alias")
                .build();
        var apiToken2 = storeParticipant(user2Context);

        //user1 attempts to read user2 -> fail
        RUNTIME_CONFIGURATION.getManagementEndpoint().baseRequest()
                .header(new Header("x-api-key", apiToken1))
                .contentType(ContentType.JSON)
                .get("/v1/participants/" + user2)
                .then()
                .log().ifValidationFails()
                .statusCode(403);
    }

    @Test
    void createNewUser_principalIsAdmin() {
        var subscriber = mock(EventSubscriber.class);
        getService(EventRouter.class).registerSync(ParticipantContextCreated.class, subscriber);
        var apikey = getSuperUserApiKey();

        var manifest = createNewParticipant();

        RUNTIME_CONFIGURATION.getManagementEndpoint().baseRequest()
                .header(new Header("x-api-key", apikey))
                .contentType(ContentType.JSON)
                .body(manifest)
                .post("/v1/participants/")
                .then()
                .log().ifError()
                .statusCode(anyOf(equalTo(200), equalTo(204)))
                .body(notNullValue());

        verify(subscriber).on(argThat(env -> ((ParticipantContextCreated) env.getPayload()).getParticipantId().equals(manifest.getParticipantId())));

        assertThat(getKeyPairsForParticipant(manifest)).hasSize(1);
        assertThat(getDidForParticipant(manifest.getParticipantId())).hasSize(1);
    }


    @Test
    void createNewUser_principalIsNotAdmin_expect403() {
        var subscriber = mock(EventSubscriber.class);
        getService(EventRouter.class).registerSync(ParticipantContextCreated.class, subscriber);

        var principal = "another-user";
        var anotherUser = ParticipantContext.Builder.newInstance()
                .participantId(principal)
                .did("did:web:" + principal)
                .apiTokenAlias(principal + "-alias")
                .build();
        var apiToken = storeParticipant(anotherUser);
        var manifest = createNewParticipant();

        RUNTIME_CONFIGURATION.getManagementEndpoint().baseRequest()
                .header(new Header("x-api-key", apiToken))
                .contentType(ContentType.JSON)
                .body(manifest)
                .post("/v1/participants/")
                .then()
                .log().ifError()
                .statusCode(403)
                .body(notNullValue());
        verifyNoInteractions(subscriber);

        assertThat(getKeyPairsForParticipant(manifest)).isEmpty();
    }

    @Test
    void createNewUser_principalIsKnown_expect401() {
        var subscriber = mock(EventSubscriber.class);
        getService(EventRouter.class).registerSync(ParticipantContextCreated.class, subscriber);
        var principal = "another-user";

        var manifest = createNewParticipant();

        RUNTIME_CONFIGURATION.getManagementEndpoint().baseRequest()
                .header(new Header("x-api-key", createTokenFor(principal)))
                .contentType(ContentType.JSON)
                .body(manifest)
                .post("/v1/participants/")
                .then()
                .log().ifError()
                .statusCode(401)
                .body(notNullValue());
        verifyNoInteractions(subscriber);
        assertThat(getKeyPairsForParticipant(manifest)).isEmpty();
    }

    @Test
    void activateParticipant_principalIsAdmin() {
        var subscriber = mock(EventSubscriber.class);
        getService(EventRouter.class).registerSync(ParticipantContextUpdated.class, subscriber);

        var participantId = "another-user";
        var anotherUser = ParticipantContext.Builder.newInstance()
                .participantId(participantId)
                .did("did:web:" + participantId)
                .apiTokenAlias(participantId + "-alias")
                .state(ParticipantContextState.CREATED)
                .build();
        storeParticipant(anotherUser);

        RUNTIME_CONFIGURATION.getManagementEndpoint().baseRequest()
                .header(new Header("x-api-key", getSuperUserApiKey()))
                .contentType(ContentType.JSON)
                .post("/v1/participants/%s/state?isActive=true".formatted(participantId))
                .then()
                .log().ifError()
                .statusCode(204);

        var updatedParticipant = RUNTIME.getContext().getService(ParticipantContextService.class).getParticipantContext(participantId).orElseThrow(f -> new EdcException(f.getFailureDetail()));
        assertThat(updatedParticipant.getState()).isEqualTo(ParticipantContextState.ACTIVATED.ordinal());
        // verify the correct event was emitted
        verify(subscriber).on(argThat(env -> {
            var evt = (ParticipantContextUpdated) env.getPayload();
            return evt.getParticipantId().equals(participantId) && evt.getNewState() == ParticipantContextState.ACTIVATED;
        }));

    }

    @Test
    void deleteParticipant() {
        var participantId = "another-user";
        createParticipant(participantId);

        assertThat(getDidForParticipant(participantId)).hasSize(1);

        RUNTIME_CONFIGURATION.getManagementEndpoint().baseRequest()
                .header(new Header("x-api-key", getSuperUserApiKey()))
                .contentType(ContentType.JSON)
                .delete("/v1/participants/%s".formatted(participantId))
                .then()
                .log().ifError()
                .statusCode(204);

        assertThat(getDidForParticipant(participantId)).isEmpty();
    }

}
