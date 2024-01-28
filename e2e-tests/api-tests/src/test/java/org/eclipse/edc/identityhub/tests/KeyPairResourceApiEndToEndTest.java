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
import org.eclipse.edc.identityhub.spi.model.KeyPairResource;
import org.eclipse.edc.identityhub.spi.model.participant.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.model.participant.ParticipantContext;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.spi.EdcException;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;

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
    }

    @Test
    void addKeyPair_notAuthorized() {
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
    }

    @Test
    void rotate() {
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
    }

    @Test
    void rotate_notAuthorized() {
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

    private String createKeyPair(String participantId) {

        var descriptor = createKeyDescriptor(participantId).build();

        var service = RUNTIME.getContext().getService(KeyPairService.class);
        service.addKeyPair(participantId, descriptor, true)
                .orElseThrow(f -> new EdcException(f.getFailureDetail()));
        return descriptor.getKeyId();
    }

    private static KeyDescriptor.Builder createKeyDescriptor(String participantId) {
        return KeyDescriptor.Builder.newInstance()
                .keyId(UUID.randomUUID().toString())
                .keyGeneratorParams(Map.of("algorithm", "EC", "curve", Curve.P_384.getStdName()))
                .privateKeyAlias(participantId + "-alias");
    }

}
