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
import org.eclipse.edc.identityhub.spi.keypair.KeyPairService;
import org.eclipse.edc.identityhub.spi.keypair.events.KeyPairAdded;
import org.eclipse.edc.identityhub.spi.keypair.events.KeyPairRotated;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairResource;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairState;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.query.QuerySpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import static io.restassured.http.ContentType.JSON;
import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@EndToEndTest
public class KeyPairResourceApiEndToEndTest extends IdentityApiEndToEndTest {

    @AfterEach
    void tearDown() {
        // purge all users
        var pcService = RUNTIME.getService(ParticipantContextService.class);
        pcService.query(QuerySpec.max()).getContent()
                .forEach(pc -> pcService.deleteParticipantContext(pc.getParticipantId()).getContent());
    }

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
        RUNTIME_CONFIGURATION.getIdentityApiEndpoint().baseRequest()
                .contentType(JSON)
                .header(new Header("x-api-key", user2Token))
                .get("/v1alpha/participants/%s/keypairs/%s".formatted(toBase64(user1), key))
                .then()
                .log().ifValidationFails()
                .statusCode(403)
                .body(notNullValue());
    }

    @Test
    void findById() {
        var superUserKey = createSuperUser();
        var user1 = "user1";
        var token = createParticipant(user1);

        var key = createKeyPair(user1);

        assertThat(Arrays.asList(token, superUserKey))
                .allSatisfy(t -> RUNTIME_CONFIGURATION.getIdentityApiEndpoint().baseRequest()
                        .contentType(JSON)
                        .header(new Header("x-api-key", t))
                        .get("/v1alpha/participants/%s/keypairs/%s".formatted(toBase64(user1), key))
                        .then()
                        .log().ifValidationFails()
                        .statusCode(200)
                        .body(notNullValue()));
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

        createKeyPair(user1);

        // attempt to publish user1's DID document, which should fail
        var res = RUNTIME_CONFIGURATION.getIdentityApiEndpoint().baseRequest()
                .contentType(JSON)
                .header(new Header("x-api-key", user2Token))
                .get("/v1alpha/participants/%s/keypairs".formatted(toBase64(user1)))
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract().body().as(KeyPairResource[].class);

        assertThat(res).isEmpty();

    }

    @Test
    void findForParticipant() {
        var superUserKey = createSuperUser();
        var user1 = "user1";
        var token = createParticipant(user1);
        createKeyPair(user1);

        assertThat(Arrays.asList(token, superUserKey))
                .allSatisfy(t -> RUNTIME_CONFIGURATION.getIdentityApiEndpoint().baseRequest()
                        .contentType(JSON)
                        .header(new Header("x-api-key", t))
                        .get("/v1alpha/participants/%s/keypairs".formatted(toBase64(user1)))
                        .then()
                        .log().ifValidationFails()
                        .statusCode(200)
                        .body(notNullValue()));

    }

    @Test
    void addKeyPair() {
        var superUserKey = createSuperUser();
        var subscriber = mock(EventSubscriber.class);
        getService(EventRouter.class).registerSync(KeyPairAdded.class, subscriber);

        var user1 = "user1";
        var token = createParticipant(user1);

        assertThat(Arrays.asList(token, superUserKey))
                .allSatisfy(t -> {
                    var keyDesc = createKeyDescriptor(user1).build();
                    RUNTIME_CONFIGURATION.getIdentityApiEndpoint().baseRequest()
                            .contentType(JSON)
                            .header(new Header("x-api-key", t))
                            .body(keyDesc)
                            .put("/v1alpha/participants/%s/keypairs".formatted(toBase64(user1)))
                            .then()
                            .log().ifValidationFails()
                            .statusCode(204)
                            .body(notNullValue());

                    verify(subscriber).on(argThat(env -> {
                        var evt = (KeyPairAdded) env.getPayload();
                        return evt.getParticipantId().equals(user1) &&
                                evt.getKeyPairResourceId().equals(keyDesc.getResourceId()) &&
                                evt.getKeyId().equals(keyDesc.getKeyId());
                    }));
                });
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
        RUNTIME_CONFIGURATION.getIdentityApiEndpoint().baseRequest()
                .contentType(JSON)
                .header(new Header("x-api-key", token2))
                .body(keyDesc)
                .put("/v1alpha/participants/%s/keypairs".formatted(toBase64(user1)))
                .then()
                .log().ifValidationFails()
                .statusCode(403)
                .body(notNullValue());

        verify(subscriber, never()).on(argThat(env -> {
            if (env.getPayload() instanceof KeyPairAdded evt) {
                return evt.getKeyPairResourceId().equals(keyDesc.getKeyId());
            }
            return false;
        }));
    }

    @Test
    void rotate() {
        var superUserKey = createSuperUser();
        var subscriber = mock(EventSubscriber.class);
        getService(EventRouter.class).registerSync(KeyPairRotated.class, subscriber);
        getService(EventRouter.class).registerSync(KeyPairAdded.class, subscriber);

        var user1 = "user1";
        var token = createParticipant(user1);

        var keyPairId = createKeyPair(user1);

        assertThat(Arrays.asList(token, superUserKey))
                .allSatisfy(t -> {
                    reset(subscriber);
                    // attempt to publish user1's DID document, which should fail
                    var keyDesc = createKeyDescriptor(user1).build();
                    RUNTIME_CONFIGURATION.getIdentityApiEndpoint().baseRequest()
                            .contentType(JSON)
                            .header(new Header("x-api-key", t))
                            .body(keyDesc)
                            .post("/v1alpha/participants/%s/keypairs/%s/rotate".formatted(toBase64(user1), keyPairId))
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
                            return evt.getKeyPairResourceId().equals(keyDesc.getResourceId()) &&
                                    evt.getKeyId().equals(keyDesc.getKeyId());
                        }
                        return false;
                    }));
                });
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
        RUNTIME_CONFIGURATION.getIdentityApiEndpoint().baseRequest()
                .contentType(JSON)
                .header(new Header("x-api-key", token2))
                .body(keyDesc)
                .post("/v1alpha/participants/%s/keypairs/%s/rotate".formatted(user1, keyId))
                .then()
                .log().ifValidationFails()
                .statusCode(403)
                .body(notNullValue());

        // make sure that the event to add the _new_ keypair was never fired
        verify(subscriber, never()).on(argThat(env -> {
            if (env.getPayload() instanceof KeyPairRotated evt) {
                return evt.getParticipantId().equals(user1) && evt.getKeyPairResourceId().equals(keyDesc.getKeyId());
            }
            return false;
        }));
    }

    @Test
    void revoke() {
        var superUserKey = createSuperUser();
        var user1 = "user1";
        var token = createParticipant(user1);

        var keyId = createKeyPair(user1);

        assertThat(Arrays.asList(token, superUserKey))
                .allSatisfy(t -> {
                    var keyDesc = createKeyDescriptor(user1).build();
                    RUNTIME_CONFIGURATION.getIdentityApiEndpoint().baseRequest()
                            .contentType(JSON)
                            .header(new Header("x-api-key", t))
                            .body(keyDesc)
                            .post("/v1alpha/participants/%s/keypairs/%s/revoke".formatted(toBase64(user1), keyId))
                            .then()
                            .log().ifValidationFails()
                            .statusCode(204)
                            .body(notNullValue());

                    assertThat(getDidForParticipant(user1)).hasSize(1)
                            .allSatisfy(dd -> assertThat(dd.getVerificationMethod()).noneMatch(vm -> vm.getId().equals(keyId)));
                });
    }

    @Test
    void revoke_notAuthorized() {
        var user1 = "user1";
        var token1 = createParticipant(user1);

        var user2 = "user2";
        var token2 = createParticipant(user2);

        var keyId = createKeyPair(user1);

        // attempt to publish user1's DID document, which should fail
        var keyDesc = createKeyDescriptor(user1).build();
        RUNTIME_CONFIGURATION.getIdentityApiEndpoint().baseRequest()
                .contentType(JSON)
                .header(new Header("x-api-key", token2))
                .body(keyDesc)
                .post("/v1alpha/participants/%s/keypairs/%s/revoke".formatted(toBase64(user1), keyId))
                .then()
                .log().ifValidationFails()
                .statusCode(403)
                .body(notNullValue());
    }

    @Test
    void getAll() {
        var superUserKey = createSuperUser();
        range(0, 10)
                .forEach(i -> {
                    var participantId = "user" + i;
                    createParticipant(participantId); // implicitly creates a keypair
                });
        var found = RUNTIME_CONFIGURATION.getIdentityApiEndpoint().baseRequest()
                .contentType(JSON)
                .header(new Header("x-api-key", superUserKey))
                .get("/v1alpha/keypairs")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract().body().as(KeyPairResource[].class);
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
        var found = RUNTIME_CONFIGURATION.getIdentityApiEndpoint().baseRequest()
                .contentType(JSON)
                .header(new Header("x-api-key", superUserKey))
                .get("/v1alpha/keypairs?offset=2&limit=4")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract().body().as(KeyPairResource[].class);
        assertThat(found).hasSize(4);
    }

    @Test
    void getAll_withDefaultPaging() {
        var superUserKey = createSuperUser();
        range(0, 70)
                .forEach(i -> {
                    var participantId = "user" + i;
                    createParticipant(participantId); // implicitly creates a keypair
                });
        var found = RUNTIME_CONFIGURATION.getIdentityApiEndpoint().baseRequest()
                .contentType(JSON)
                .header(new Header("x-api-key", superUserKey))
                .get("/v1alpha/keypairs")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract().body().as(KeyPairResource[].class);
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
        RUNTIME_CONFIGURATION.getIdentityApiEndpoint().baseRequest()
                .contentType(JSON)
                .header(new Header("x-api-key", attackerToken))
                .get("/v1alpha/keypairs")
                .then()
                .log().ifValidationFails()
                .statusCode(403);
    }

    @Test
    void activate() {
        var superUserKey = createSuperUser();
        var user1 = "user1";
        var token = createParticipant(user1);
        var keyPairId = createKeyPair(user1);

        assertThat(Arrays.asList(token, superUserKey))
                .allSatisfy(t -> {
                    RUNTIME_CONFIGURATION.getIdentityApiEndpoint().baseRequest()
                            .contentType(JSON)
                            .header(new Header("x-api-key", t))
                            .post("/v1alpha/participants/%s/keypairs/%s/activate".formatted(toBase64(user1), keyPairId))
                            .then()
                            .log().ifValidationFails()
                            .statusCode(204)
                            .body(notNullValue());

                    assertThat(getDidForParticipant(user1))
                            .hasSize(1)
                            .allSatisfy(dd -> assertThat(dd.getVerificationMethod()).noneMatch(vm -> vm.getId().equals(keyPairId)));
                });
    }

    @Test
    void activate_notAuthorized() {
        var user1 = "user1";
        createParticipant(user1);
        var keyId = createKeyPair(user1);
        var attackerToken = createParticipant("attacker");

        RUNTIME_CONFIGURATION.getIdentityApiEndpoint().baseRequest()
                .contentType(JSON)
                .header(new Header("x-api-key", attackerToken))
                .post("/v1alpha/participants/%s/keypairs/%s/activate".formatted(toBase64(user1), keyId))
                .then()
                .log().ifValidationFails()
                .statusCode(403)
                .body(notNullValue());

        assertThat(getKeyPairsForParticipant(user1))
                .hasSize(2)
                .allMatch(keyPairResource -> keyPairResource.getState() == KeyPairState.ACTIVE.code());
    }

    @Test
    void activate_illegalState() {
        var user1 = "user1";
        var token = createParticipant(user1);
        var keyPairId = createKeyPair(user1);

        // first revoke the key, which puts it in the REVOKED state
        RUNTIME_CONFIGURATION.getIdentityApiEndpoint().baseRequest()
                .contentType(JSON)
                .header(new Header("x-api-key", token))
                .post("/v1alpha/participants/%s/keypairs/%s/revoke".formatted(toBase64(user1), keyPairId))
                .then()
                .log().ifValidationFails()
                .statusCode(204)
                .body(notNullValue());

        // now attempt to activate
        RUNTIME_CONFIGURATION.getIdentityApiEndpoint().baseRequest()
                .contentType(JSON)
                .header(new Header("x-api-key", token))
                .post("/v1alpha/participants/%s/keypairs/%s/activate".formatted(toBase64(user1), keyPairId))
                .then()
                .log().ifValidationFails()
                .statusCode(400)
                .body(notNullValue());
    }

    private KeyDescriptor.Builder createKeyDescriptor(String participantId) {
        var keyId = UUID.randomUUID().toString();
        return KeyDescriptor.Builder.newInstance()
                .keyId(keyId)
                .resourceId(UUID.randomUUID().toString())
                .keyGeneratorParams(Map.of("algorithm", "EC", "curve", Curve.P_384.getStdName()))
                .privateKeyAlias("%s-%s-alias".formatted(participantId, keyId));
    }

    private String createKeyPair(String participantId) {

        var descriptor = createKeyDescriptor(participantId).build();

        var service = RUNTIME.getService(KeyPairService.class);
        service.addKeyPair(participantId, descriptor, true)
                .orElseThrow(f -> new EdcException(f.getFailureDetail()));
        return descriptor.getResourceId();
    }

    private String toBase64(String s) {
        return Base64.getUrlEncoder().encodeToString(s.getBytes());
    }

}
