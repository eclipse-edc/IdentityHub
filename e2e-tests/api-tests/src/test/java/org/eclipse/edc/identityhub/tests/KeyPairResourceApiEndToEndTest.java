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

import com.nimbusds.jose.jwk.Curve;
import io.restassured.http.Header;
import org.eclipse.edc.identityhub.spi.KeyPairService;
import org.eclipse.edc.identityhub.spi.events.keypair.KeyPairAdded;
import org.eclipse.edc.identityhub.spi.events.keypair.KeyPairRotated;
import org.eclipse.edc.identityhub.spi.model.KeyPairResource;
import org.eclipse.edc.identityhub.spi.model.participant.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.model.participant.ParticipantContext;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@EndToEndTest
public class KeyPairResourceApiEndToEndTest extends ManagementApiEndToEndTest {

    @Test
    void findById_notAuthorized() {
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

        var key = createKeyPair(user1);

        // attempt to publish user1's DID document, which should fail
        RUNTIME_CONFIGURATION.getManagementEndpoint().baseRequest()
                .contentType(JSON)
                .header(new Header("x-api-key", user2Token))
                .get("/v1/keypairs/" + key)
                .then()
                .log().ifValidationFails()
                .statusCode(403)
                .body(notNullValue());
    }

    @Test
    void findById() {
        var user1 = "user1";
        var token = createParticipant(user1);


        var key = createKeyPair(user1);

        // attempt to publish user1's DID document, which should fail
        RUNTIME_CONFIGURATION.getManagementEndpoint().baseRequest()
                .contentType(JSON)
                .header(new Header("x-api-key", token))
                .get("/v1/keypairs/" + key)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body(notNullValue());
    }

    @Test
    void findForParticipant_notAuthorized() {
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

        var key = createKeyPair(user1);

        // attempt to publish user1's DID document, which should fail
        var res = RUNTIME_CONFIGURATION.getManagementEndpoint().baseRequest()
                .contentType(JSON)
                .header(new Header("x-api-key", user2Token))
                .get("/v1/keypairs?participantId=" + user1)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract().body().as(KeyPairResource[].class);

        assertThat(res).isEmpty();

    }

    @Test
    void findForParticipant() {
        var user1 = "user1";
        var token = createParticipant(user1);


        var key = createKeyPair(user1);

        // attempt to publish user1's DID document, which should fail
        RUNTIME_CONFIGURATION.getManagementEndpoint().baseRequest()
                .contentType(JSON)
                .header(new Header("x-api-key", token))
                .get("/v1/keypairs?participantId=" + user1)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body(notNullValue());
    }

    @Test
    void addKeyPair() {
        var subscriber = mock(EventSubscriber.class);
        getService(EventRouter.class).registerSync(KeyPairAdded.class, subscriber);

        var user1 = "user1";
        var token = createParticipant(user1);


        // attempt to publish user1's DID document, which should fail
        var keyDesc = createKeyDescriptor(user1).build();
        RUNTIME_CONFIGURATION.getManagementEndpoint().baseRequest()
                .contentType(JSON)
                .header(new Header("x-api-key", token))
                .body(keyDesc)
                .put("/v1/keypairs?participantId=" + user1)
                .then()
                .log().ifValidationFails()
                .statusCode(204)
                .body(notNullValue());
        verify(subscriber).on(argThat(env -> {
            var evt = (KeyPairAdded) env.getPayload();
            return evt.getParticipantId().equals(user1) && evt.getKeyId().equals(keyDesc.getKeyId());
        }));
    }

    @Test
    void addKeyPair_notAuthorized() {
        var subscriber = mock(EventSubscriber.class);
        getService(EventRouter.class).registerSync(KeyPairAdded.class, subscriber);

        var user1 = "user1";
        var token = createParticipant(user1);

        var user2 = "user2";
        var token2 = createParticipant(user2);


        // attempt to publish user1's DID document, which should fail
        var keyDesc = createKeyDescriptor(user1).build();
        RUNTIME_CONFIGURATION.getManagementEndpoint().baseRequest()
                .contentType(JSON)
                .header(new Header("x-api-key", token2))
                .body(keyDesc)
                .put("/v1/keypairs?participantId=" + user1)
                .then()
                .log().ifValidationFails()
                .statusCode(403)
                .body(notNullValue());

        verify(subscriber, never()).on(argThat(env -> {
            if (env.getPayload() instanceof KeyPairAdded evt) {
                return evt.getKeyId().equals(keyDesc.getKeyId());
            }
            return false;
        }));
    }

    @Test
    void rotate() {
        var subscriber = mock(EventSubscriber.class);
        getService(EventRouter.class).registerSync(KeyPairRotated.class, subscriber);
        getService(EventRouter.class).registerSync(KeyPairAdded.class, subscriber);

        var user1 = "user1";
        var token = createParticipant(user1);

        var keyId = createKeyPair(user1);

        // attempt to publish user1's DID document, which should fail
        var keyDesc = createKeyDescriptor(user1).build();
        RUNTIME_CONFIGURATION.getManagementEndpoint().baseRequest()
                .contentType(JSON)
                .header(new Header("x-api-key", token))
                .body(keyDesc)
                .post("/v1/keypairs/%s/rotate".formatted(keyId))
                .then()
                .log().ifValidationFails()
                .statusCode(204)
                .body(notNullValue());

        // verify that the "rotated" event fired once
        verify(subscriber).on(argThat(env -> {
            if (env.getPayload() instanceof KeyPairRotated evt) {
                return evt.getParticipantId().equals(user1);
            }
            return false;
        }));
        // verify that the correct "added" event fired
        verify(subscriber).on(argThat(env -> {
            if (env.getPayload() instanceof KeyPairAdded evt) {
                return evt.getKeyId().equals(keyDesc.getKeyId());
            }
            return false;
        }));
    }

    @Test
    void rotate_notAuthorized() {
        var subscriber = mock(EventSubscriber.class);
        getService(EventRouter.class).registerSync(KeyPairRotated.class, subscriber);

        var user1 = "user1";
        var token = createParticipant(user1);

        var user2 = "user2";
        var token2 = createParticipant(user2);

        var keyId = createKeyPair(user1);

        // attempt to publish user1's DID document, which should fail
        var keyDesc = createKeyDescriptor(user1).build();
        RUNTIME_CONFIGURATION.getManagementEndpoint().baseRequest()
                .contentType(JSON)
                .header(new Header("x-api-key", token2))
                .body(keyDesc)
                .post("/v1/keypairs/%s/rotate".formatted(keyId))
                .then()
                .log().ifValidationFails()
                .statusCode(403)
                .body(notNullValue());

        // make sure that the event to add the _new_ keypair was never fired
        verify(subscriber, never()).on(argThat(env -> {
            if (env.getPayload() instanceof KeyPairRotated evt) {
                return evt.getParticipantId().equals(user1) && evt.getKeyId().equals(keyDesc.getKeyId());
            }
            return false;
        }));
    }

    @Test
    void revoke() {
        var user1 = "user1";
        var token = createParticipant(user1);

        var keyId = createKeyPair(user1);

        // attempt to publish user1's DID document, which should fail
        RUNTIME_CONFIGURATION.getManagementEndpoint().baseRequest()
                .contentType(JSON)
                .header(new Header("x-api-key", token))
                .post("/v1/keypairs/%s/revoke".formatted(keyId))
                .then()
                .log().ifValidationFails()
                .statusCode(204)
                .body(notNullValue());

        assertThat(getDidForParticipant(user1)).hasSize(1)
                .allSatisfy(dd -> assertThat(dd.getVerificationMethod()).noneMatch(vm -> vm.getId().equals(keyId)));
    }

    @Test
    void revoke_notAuthorized() {
        var user1 = "user1";
        var token = createParticipant(user1);

        var user2 = "user2";
        var token2 = createParticipant(user2);

        var keyId = createKeyPair(user1);

        // attempt to publish user1's DID document, which should fail
        RUNTIME_CONFIGURATION.getManagementEndpoint().baseRequest()
                .contentType(JSON)
                .header(new Header("x-api-key", token2))
                .post("/v1/keypairs/%s/revoke".formatted(keyId))
                .then()
                .log().ifValidationFails()
                .statusCode(403)
                .body(notNullValue());
    }

    @Test
    void getAll() {
        IntStream.range(0, 20)
                .forEach(i -> {
                    var u = "user" + i;
                    var token = createParticipant(u);
                    var key1 = createKeyPair(u);
                });

        // attempt to publish user1's DID document, which should fail
        var keypairs = RUNTIME_CONFIGURATION.getManagementEndpoint().baseRequest()
                .contentType(JSON)
                .header(new Header("x-api-key", getSuperUserApiKey()))
                .get("/v1/keypairs/")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract().body().as(KeyPairResource[].class);

        assertThat(keypairs).hasSize(20 * 2 + 1); // 2 keys per user, 20 users, plus the super-user
    }

    @Test
    void getAll_notAdmin() {

        var unauthorizedToken = createParticipant("forbidden-user");

        IntStream.range(0, 20)
                .forEach(i -> {
                    var u = "user" + i;
                    var token = createParticipant(u);
                    var key1 = createKeyPair(u);
                });

        // attempt to publish user1's DID document, which should fail
        RUNTIME_CONFIGURATION.getManagementEndpoint().baseRequest()
                .contentType(JSON)
                .header(new Header("x-api-key", unauthorizedToken))
                .get("/v1/keypairs/")
                .then()
                .log().ifValidationFails()
                .statusCode(403);

    }


    private String createKeyPair(String participantId) {

        var descriptor = createKeyDescriptor(participantId).build();

        var service = RUNTIME.getContext().getService(KeyPairService.class);
        service.addKeyPair(participantId, descriptor, true)
                .orElseThrow(f -> new EdcException(f.getFailureDetail()));
        return descriptor.getKeyId();
    }

    private static KeyDescriptor.Builder createKeyDescriptor(String participantId) {
        var id = UUID.randomUUID().toString();
        return KeyDescriptor.Builder.newInstance()
                .keyId(id)
                .keyGeneratorParams(Map.of("algorithm", "EC", "curve", Curve.P_384.getStdName()))
                .privateKeyAlias("%s-%s-alias".formatted(participantId, id));
    }

}
