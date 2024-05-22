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
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairState;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.events.ParticipantContextCreated;
import org.eclipse.edc.identityhub.spi.participantcontext.events.ParticipantContextUpdated;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContextState;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static io.restassured.http.ContentType.JSON;
import static java.util.stream.IntStream.range;
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
        var apikey = createSuperUser();

        var su = RUNTIME_CONFIGURATION.getManagementEndpoint().baseRequest()
                .header(new Header("x-api-key", apikey))
                .get("/v1alpha/participants/" + toBase64(SUPER_USER))
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
                .get("/v1alpha/participants/" + toBase64(user2))
                .then()
                .log().ifValidationFails()
                .statusCode(403);
    }

    @Test
    void createNewUser_principalIsSuperser() {
        var subscriber = mock(EventSubscriber.class);
        getService(EventRouter.class).registerSync(ParticipantContextCreated.class, subscriber);
        var apikey = createSuperUser();

        var manifest = createNewParticipant().build();

        RUNTIME_CONFIGURATION.getManagementEndpoint().baseRequest()
                .header(new Header("x-api-key", apikey))
                .contentType(ContentType.JSON)
                .body(manifest)
                .post("/v1alpha/participants/")
                .then()
                .log().ifError()
                .statusCode(anyOf(equalTo(200), equalTo(204)))
                .body(notNullValue());

        verify(subscriber).on(argThat(env -> ((ParticipantContextCreated) env.getPayload()).getParticipantId().equals(manifest.getParticipantId())));

        assertThat(getKeyPairsForParticipant(manifest.getParticipantId())).hasSize(1);
        assertThat(getDidForParticipant(manifest.getParticipantId())).hasSize(1)
                .allSatisfy(dd -> assertThat(dd.getVerificationMethod()).hasSize(1));
    }

    @ParameterizedTest(name = "Create participant with key pair active = {0}")
    @ValueSource(booleans = { true, false })
    void createNewUser_verifyKeyPairActive(boolean isActive) {
        var subscriber = mock(EventSubscriber.class);
        getService(EventRouter.class).registerSync(ParticipantContextCreated.class, subscriber);
        var apikey = createSuperUser();

        var participantId = UUID.randomUUID().toString();
        var manifest = createNewParticipant()
                .participantId(participantId)
                .did("did:web:" + participantId)
                .key(createKeyDescriptor().active(isActive).build())
                .build();

        RUNTIME_CONFIGURATION.getManagementEndpoint().baseRequest()
                .header(new Header("x-api-key", apikey))
                .contentType(ContentType.JSON)
                .body(manifest)
                .post("/v1alpha/participants/")
                .then()
                .log().ifError()
                .statusCode(anyOf(equalTo(200), equalTo(204)))
                .body(notNullValue());

        verify(subscriber).on(argThat(env -> ((ParticipantContextCreated) env.getPayload()).getParticipantId().equals(manifest.getParticipantId())));

        assertThat(getKeyPairsForParticipant(manifest.getParticipantId())).hasSize(1)
                .allSatisfy(kpr -> assertThat(kpr.getState()).isEqualTo(isActive ? KeyPairState.ACTIVE.code() : KeyPairState.CREATED.code()));
        assertThat(getDidForParticipant(manifest.getParticipantId())).hasSize(1)
                .allSatisfy(dd -> assertThat(dd.getVerificationMethod()).hasSize(1));

    }


    @Test
    void createNewUser_principalIsNotSuperuser_expect403() {
        var subscriber = mock(EventSubscriber.class);
        getService(EventRouter.class).registerSync(ParticipantContextCreated.class, subscriber);

        var principal = "another-user";
        var anotherUser = ParticipantContext.Builder.newInstance()
                .participantId(principal)
                .did("did:web:" + principal)
                .apiTokenAlias(principal + "-alias")
                .build();
        var apiToken = storeParticipant(anotherUser);
        var manifest = createNewParticipant().build();

        RUNTIME_CONFIGURATION.getManagementEndpoint().baseRequest()
                .header(new Header("x-api-key", apiToken))
                .contentType(ContentType.JSON)
                .body(manifest)
                .post("/v1alpha/participants/")
                .then()
                .log().ifError()
                .statusCode(403)
                .body(notNullValue());
        verifyNoInteractions(subscriber);

        assertThat(getKeyPairsForParticipant(manifest.getParticipantId())).isEmpty();
    }

    @Test
    void createNewUser_principalIsKnown_expect401() {
        var subscriber = mock(EventSubscriber.class);
        getService(EventRouter.class).registerSync(ParticipantContextCreated.class, subscriber);
        var principal = "another-user";

        var manifest = createNewParticipant().build();

        RUNTIME_CONFIGURATION.getManagementEndpoint().baseRequest()
                .header(new Header("x-api-key", createTokenFor(principal)))
                .contentType(ContentType.JSON)
                .body(manifest)
                .post("/v1alpha/participants/")
                .then()
                .log().ifError()
                .statusCode(401)
                .body(notNullValue());
        verifyNoInteractions(subscriber);
        assertThat(getKeyPairsForParticipant(manifest.getParticipantId())).isEmpty();
    }

    @Test
    void activateParticipant_principalIsSuperser() {
        var superUserKey = createSuperUser();
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
                .header(new Header("x-api-key", superUserKey))
                .contentType(ContentType.JSON)
                .post("/v1alpha/participants/%s/state?isActive=true".formatted(toBase64(participantId)))
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
        var superUserKey = createSuperUser();
        var participantId = "another-user";
        createParticipant(participantId);

        assertThat(getDidForParticipant(participantId)).hasSize(1);

        RUNTIME_CONFIGURATION.getManagementEndpoint().baseRequest()
                .header(new Header("x-api-key", superUserKey))
                .contentType(ContentType.JSON)
                .delete("/v1alpha/participants/%s".formatted(toBase64(participantId)))
                .then()
                .log().ifError()
                .statusCode(204);

        assertThat(getDidForParticipant(participantId)).isEmpty();
    }

    @Test
    void regenerateToken() {
        var superUserKey = createSuperUser();
        var participantId = "another-user";
        var userToken = createParticipant(participantId);

        assertThat(Arrays.asList(userToken, superUserKey))
                .allSatisfy(t -> RUNTIME_CONFIGURATION.getManagementEndpoint().baseRequest()
                        .header(new Header("x-api-key", t))
                        .contentType(ContentType.JSON)
                        .post("/v1alpha/participants/%s/token".formatted(toBase64(participantId)))
                        .then()
                        .log().ifError()
                        .statusCode(200)
                        .body(notNullValue()));
    }

    @Test
    void updateRoles() {
        var superUserKey = createSuperUser();
        var participantId = "some-user";
        createParticipant(participantId);

        RUNTIME_CONFIGURATION.getManagementEndpoint().baseRequest()
                .header(new Header("x-api-key", superUserKey))
                .contentType(ContentType.JSON)
                .body(List.of("role1", "role2", "admin"))
                .put("/v1alpha/participants/%s/roles".formatted(toBase64(participantId)))
                .then()
                .log().ifError()
                .statusCode(204);

        assertThat(getParticipant(participantId).getRoles()).containsExactlyInAnyOrder("role1", "role2", "admin");
    }

    @ParameterizedTest(name = "Expect 403, role = {0}")
    @ValueSource(strings = { "some-role", "admin" })
    void updateRoles_whenNotSuperuser(String role) {
        var participantId = "some-user";
        var userToken = createParticipant(participantId);

        RUNTIME_CONFIGURATION.getManagementEndpoint().baseRequest()
                .header(new Header("x-api-key", userToken))
                .contentType(ContentType.JSON)
                .body(List.of(role))
                .put("/v1alpha/participants/%s/roles".formatted(toBase64(participantId)))
                .then()
                .log().ifError()
                .statusCode(403);
    }

    @Test
    void getAll() {
        var superUserKey = createSuperUser();
        range(0, 10)
                .forEach(i -> {
                    var participantId = "user" + i;
                    createParticipant(participantId);
                });
        var found = RUNTIME_CONFIGURATION.getManagementEndpoint().baseRequest()
                .contentType(JSON)
                .header(new Header("x-api-key", superUserKey))
                .get("/v1alpha/participants")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract().body().as(ParticipantContext[].class);
        assertThat(found).hasSize(11); //10 + 1 for the super user
    }

    @Test
    void getAll_withPaging() {
        var superUserKey = createSuperUser();
        range(0, 10)
                .forEach(i -> {
                    var participantId = "user" + i;
                    createParticipant(participantId); // implicitly creates a keypair
                });
        var found = RUNTIME_CONFIGURATION.getManagementEndpoint().baseRequest()
                .contentType(JSON)
                .header(new Header("x-api-key", superUserKey))
                .get("/v1alpha/participants?offset=2&limit=4")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract().body().as(ParticipantContext[].class);
        assertThat(found).hasSize(4);
    }

    @Test
    void getAll_withDefaultPaging() {
        var superUserKey = createSuperUser();
        IntStream.range(0, 70)
                .forEach(i -> {
                    var participantId = "user" + i;
                    createParticipant(participantId); // implicitly creates a keypair
                });
        var found = RUNTIME_CONFIGURATION.getManagementEndpoint().baseRequest()
                .contentType(JSON)
                .header(new Header("x-api-key", superUserKey))
                .get("/v1alpha/participants")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract().body().as(ParticipantContext[].class);
        assertThat(found).hasSize(50);
    }

    @Test
    void getAll_notAuthorized() {
        var attackerToken = createParticipant("attacker");

        range(0, 10)
                .forEach(i -> {
                    var participantId = "user" + i;
                    createParticipant(participantId); // implicitly creates a keypair
                });
        RUNTIME_CONFIGURATION.getManagementEndpoint().baseRequest()
                .contentType(JSON)
                .header(new Header("x-api-key", attackerToken))
                .get("/v1alpha/participants")
                .then()
                .log().ifValidationFails()
                .statusCode(403);
    }

    private String toBase64(String s) {
        return Base64.getUrlEncoder().encodeToString(s.getBytes());
    }
}
