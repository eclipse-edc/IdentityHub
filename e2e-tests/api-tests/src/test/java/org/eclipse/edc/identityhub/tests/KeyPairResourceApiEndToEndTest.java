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
import org.eclipse.edc.identithub.spi.did.model.DidState;
import org.eclipse.edc.identithub.spi.did.store.DidResourceStore;
import org.eclipse.edc.identityhub.spi.keypair.events.KeyPairActivated;
import org.eclipse.edc.identityhub.spi.keypair.events.KeyPairAdded;
import org.eclipse.edc.identityhub.spi.keypair.events.KeyPairRotated;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairResource;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairState;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.identityhub.spi.store.KeyPairResourceStore;
import org.eclipse.edc.identityhub.tests.fixtures.IdentityHubEndToEndExtension;
import org.eclipse.edc.identityhub.tests.fixtures.IdentityHubEndToEndTestContext;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.query.QuerySpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import static io.restassured.http.ContentType.JSON;
import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class KeyPairResourceApiEndToEndTest {

    abstract static class Tests {

        @AfterEach
        void tearDown(ParticipantContextService pcService, DidResourceStore didResourceStore, KeyPairResourceStore keyPairResourceStore) {
            // purge all users, dids, keypairs

            pcService.query(QuerySpec.max()).getContent()
                    .forEach(pc -> pcService.deleteParticipantContext(pc.getParticipantId()).getContent());

            didResourceStore.query(QuerySpec.max()).forEach(dr -> didResourceStore.deleteById(dr.getDid()).getContent());

            keyPairResourceStore.query(QuerySpec.max()).getContent()
                    .forEach(kpr -> keyPairResourceStore.deleteById(kpr.getId()).getContent());
        }

        @Test
        void findById_notAuthorized(IdentityHubEndToEndTestContext context) {
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

            var key = context.createKeyPair(user1).getResourceId();

            // attempt to publish user1's DID document, which should fail
            context.getIdentityApiEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", user2Token))
                    .get("/v1alpha/participants/%s/keypairs/%s".formatted(toBase64(user1), key))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403)
                    .body(notNullValue());
        }

        @Test
        void findById(IdentityHubEndToEndTestContext context) {
            var superUserKey = context.createSuperUser();
            var user1 = "user1";
            var token = context.createParticipant(user1);

            var key = context.createKeyPair(user1).getResourceId();

            assertThat(Arrays.asList(token, superUserKey))
                    .allSatisfy(t -> context.getIdentityApiEndpoint().baseRequest()
                            .contentType(JSON)
                            .header(new Header("x-api-key", t))
                            .get("/v1alpha/participants/%s/keypairs/%s".formatted(toBase64(user1), key))
                            .then()
                            .log().ifValidationFails()
                            .statusCode(200)
                            .body(notNullValue()));
        }

        @Test
        void findForParticipant_notAuthorized(IdentityHubEndToEndTestContext context) {
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

            context.createKeyPair(user1);

            // attempt to publish user1's DID document, which should fail
            var res = context.getIdentityApiEndpoint().baseRequest()
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
        void findForParticipant(IdentityHubEndToEndTestContext context) {
            var superUserKey = context.createSuperUser();
            var user1 = "user1";
            var token = context.createParticipant(user1);
            context.createKeyPair(user1);

            assertThat(Arrays.asList(token, superUserKey))
                    .allSatisfy(t -> context.getIdentityApiEndpoint().baseRequest()
                            .contentType(JSON)
                            .header(new Header("x-api-key", t))
                            .get("/v1alpha/participants/%s/keypairs".formatted(toBase64(user1)))
                            .then()
                            .log().ifValidationFails()
                            .statusCode(200)
                            .body(notNullValue()));

        }

        @Test
        void addKeyPair(IdentityHubEndToEndTestContext context, EventRouter router) {
            var superUserKey = context.createSuperUser();
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(KeyPairAdded.class, subscriber);

            var user1 = "user1";
            var token = context.createParticipant(user1);

            assertThat(Arrays.asList(token, superUserKey))
                    .allSatisfy(t -> {
                        var keyDesc = context.createKeyDescriptor(user1).build();
                        context.getIdentityApiEndpoint().baseRequest()
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
        void addKeyPair_notAuthorized(IdentityHubEndToEndTestContext context, EventRouter router) {
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(KeyPairAdded.class, subscriber);

            var user1 = "user1";
            var token = context.createParticipant(user1);

            var user2 = "user2";
            var token2 = context.createParticipant(user2);


            // attempt to publish user1's DID document, which should fail
            var keyDesc = context.createKeyDescriptor(user1).build();
            context.getIdentityApiEndpoint().baseRequest()
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
        void rotate(IdentityHubEndToEndTestContext context, EventRouter router) {
            var superUserKey = context.createSuperUser();
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(KeyPairRotated.class, subscriber);
            router.registerSync(KeyPairAdded.class, subscriber);

            var user1 = "user1";
            var token = context.createParticipant(user1);

            var keyPairId = context.createKeyPair(user1).getResourceId();

            assertThat(Arrays.asList(token, superUserKey))
                    .allSatisfy(t -> {
                        reset(subscriber);
                        // attempt to publish user1's DID document, which should fail
                        var keyDesc = context.createKeyDescriptor(user1).build();
                        context.getIdentityApiEndpoint().baseRequest()
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
        void rotate_notAuthorized(IdentityHubEndToEndTestContext context, EventRouter router) {
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(KeyPairRotated.class, subscriber);

            var user1 = "user1";
            var token = context.createParticipant(user1);

            var user2 = "user2";
            var token2 = context.createParticipant(user2);

            var keyId = context.createKeyPair(user1).getResourceId();

            // attempt to publish user1's DID document, which should fail
            var keyDesc = context.createKeyDescriptor(user1).build();
            context.getIdentityApiEndpoint().baseRequest()
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
        void revoke(IdentityHubEndToEndTestContext context) {
            var superUserKey = context.createSuperUser();
            var user1 = "user1";
            var token = context.createParticipant(user1);

            var keyId = context.createKeyPair(user1).getResourceId();

            assertThat(Arrays.asList(token, superUserKey))
                    .allSatisfy(t -> {
                        var keyDesc = context.createKeyDescriptor(user1).build();
                        context.getIdentityApiEndpoint().baseRequest()
                                .contentType(JSON)
                                .header(new Header("x-api-key", t))
                                .body(keyDesc)
                                .post("/v1alpha/participants/%s/keypairs/%s/revoke".formatted(toBase64(user1), keyId))
                                .then()
                                .log().ifValidationFails()
                                .statusCode(204)
                                .body(notNullValue());

                        assertThat(context.getDidForParticipant(user1)).hasSize(1)
                                .allSatisfy(dd -> assertThat(dd.getVerificationMethod()).noneMatch(vm -> vm.getId().equals(keyId)));
                    });
        }

        @Test
        void revoke_notAuthorized(IdentityHubEndToEndTestContext context) {
            var user1 = "user1";
            var token1 = context.createParticipant(user1);

            var user2 = "user2";
            var token2 = context.createParticipant(user2);

            var keyId = context.createKeyPair(user1).getResourceId();

            // attempt to publish user1's DID document, which should fail
            var keyDesc = context.createKeyDescriptor(user1).build();
            context.getIdentityApiEndpoint().baseRequest()
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
        void getAll(IdentityHubEndToEndTestContext context) {
            var superUserKey = context.createSuperUser();
            range(0, 10)
                    .forEach(i -> {
                        var participantId = "user" + i;
                        context.createParticipant(participantId); // implicitly creates a keypair
                    });
            var found = context.getIdentityApiEndpoint().baseRequest()
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
        void getAll_withPaging(IdentityHubEndToEndTestContext context) {
            var superUserKey = context.createSuperUser();
            range(0, 10)
                    .forEach(i -> {
                        var participantId = "user" + i;
                        context.createParticipant(participantId); // implicitly creates a keypair
                    });
            var found = context.getIdentityApiEndpoint().baseRequest()
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
        void getAll_withDefaultPaging(IdentityHubEndToEndTestContext context) {
            var superUserKey = context.createSuperUser();
            range(0, 70)
                    .forEach(i -> {
                        var participantId = "user" + i;
                        context.createParticipant(participantId); // implicitly creates a keypair
                    });
            var found = context.getIdentityApiEndpoint().baseRequest()
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
        void getAll_notAuthorized(IdentityHubEndToEndTestContext context) {
            var attackerToken = context.createParticipant("attacker");

            range(0, 10)
                    .forEach(i -> {
                        var participantId = "user" + i;
                        context.createParticipant(participantId); // implicitly creates a keypair
                    });
            context.getIdentityApiEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", attackerToken))
                    .get("/v1alpha/keypairs")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403);
        }

        @Test
        void activate_superUserToken(IdentityHubEndToEndTestContext context, EventRouter router) {
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(KeyPairActivated.class, subscriber);

            var superUserKey = context.createSuperUser();
            var user1 = "user1";
            context.createParticipant(user1);
            var keyDescriptor = context.createKeyPair(user1);
            var keyPairId = keyDescriptor.getResourceId();

            context.getIdentityApiEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", superUserKey))
                    .post("/v1alpha/participants/%s/keypairs/%s/activate".formatted(toBase64(user1), keyPairId))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(204)
                    .body(notNullValue());

            assertThat(context.getDidForParticipant(user1))
                    .hasSize(1)
                    .anySatisfy(dd -> assertThat(dd.getVerificationMethod()).hasSize(2).anyMatch(vm -> vm.getId().equals(keyDescriptor.getKeyId())));

            assertThat(context.getDidResourceForParticipant("did:web:" + user1).getState()).isEqualTo(DidState.PUBLISHED.code());
            verify(subscriber).on(argThat(e -> e.getPayload() instanceof KeyPairActivated kpa && kpa.getKeyPairResourceId().equals(keyPairId)));
        }

        @Test
        void activate_userToken(IdentityHubEndToEndTestContext context, EventRouter router) {
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(KeyPairActivated.class, subscriber);

            var user1 = "user1";
            var token = context.createParticipant(user1);
            assertThat(context.getDidForParticipant(user1))
                    .allSatisfy(dd -> assertThat(dd.getVerificationMethod()).hasSize(1));

            var keyDescriptor = context.createKeyPair(user1);
            var keyPairId = keyDescriptor.getResourceId();

            context.getIdentityApiEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", token))
                    .post("/v1alpha/participants/%s/keypairs/%s/activate".formatted(toBase64(user1), keyPairId))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(204)
                    .body(notNullValue());

            assertThat(context.getDidForParticipant(user1))
                    .hasSize(1)
                    .allSatisfy(dd -> assertThat(dd.getVerificationMethod())
                            .hasSize(2)
                            .anyMatch(vm -> vm.getId().equals(keyDescriptor.getKeyId())));

            assertThat(context.getDidResourceForParticipant("did:web:" + user1).getState()).isEqualTo(DidState.PUBLISHED.code());
            verify(subscriber).on(argThat(e -> e.getPayload() instanceof KeyPairActivated kpa && kpa.getKeyPairResourceId().equals(keyPairId)));
        }

        @Test
        void activate_whenParticipantNotActive_shouldNotPublishDid(IdentityHubEndToEndTestContext context, EventRouter router) {
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(KeyPairActivated.class, subscriber);

            var user1 = "user1";
            var token = context.createParticipant(user1, List.of(), false);
            var keyDescriptor = context.createKeyPair(user1);
            var keyPairId = keyDescriptor.getResourceId();

            context.getIdentityApiEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", token))
                    .post("/v1alpha/participants/%s/keypairs/%s/activate".formatted(toBase64(user1), keyPairId))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(204)
                    .body(notNullValue());

            // DID should contain 2 VerificationMethods, one of with should be the newly activated one
            assertThat(context.getDidForParticipant(user1))
                    .hasSize(1)
                    .allSatisfy(dd -> assertThat(dd.getVerificationMethod())
                            .hasSize(2)
                            .anyMatch(vm -> vm.getId().equals(keyDescriptor.getKeyId())));

            assertThat(context.getDidResourceForParticipant("did:web:" + user1).getState()).isNotEqualTo(DidState.PUBLISHED.code());
            // all key pairs should be ACTIVATED
            assertThat(context.getKeyPairsForParticipant(user1))
                    .allMatch(kpr -> kpr.getState() == KeyPairState.ACTIVATED.code());

            verify(subscriber).on(argThat(e -> e.getPayload() instanceof KeyPairActivated kpa && kpa.getKeyPairResourceId().equals(keyPairId)));
        }

        @Test
        void activate_notExists(IdentityHubEndToEndTestContext context, EventRouter router) {
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(KeyPairActivated.class, subscriber);

            var superUserKey = context.createSuperUser();
            var user1 = "user1";
            var token = context.createParticipant(user1);
            var keyPairId = "non-exist-keypair-id";

            assertThat(Arrays.asList(token, superUserKey))
                    .allSatisfy(t -> {
                        reset(subscriber);
                        context.getIdentityApiEndpoint().baseRequest()
                                .contentType(JSON)
                                .header(new Header("x-api-key", t))
                                .post("/v1alpha/participants/%s/keypairs/%s/activate".formatted(toBase64(user1), keyPairId))
                                .then()
                                .log().ifError()
                                .statusCode(404)
                                .body(notNullValue());

                        assertThat(context.getDidForParticipant(user1))
                                .hasSize(1)
                                .allSatisfy(dd -> assertThat(dd.getVerificationMethod()).noneMatch(vm -> vm.getId().equals(keyPairId)));

                        verifyNoInteractions(subscriber);
                    });
        }

        @Test
        void activate_notAuthorized(IdentityHubEndToEndTestContext context) {
            var user1 = "user1";
            context.createParticipant(user1);
            var keyId = context.createKeyPair(user1).getResourceId();
            var attackerToken = context.createParticipant("attacker");

            context.getIdentityApiEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", attackerToken))
                    .post("/v1alpha/participants/%s/keypairs/%s/activate".formatted(toBase64(user1), keyId))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403)
                    .body(notNullValue());

            assertThat(context.getKeyPairsForParticipant(user1))
                    .hasSize(2)
                    .anyMatch(keyPairResource -> keyPairResource.getId().equals(keyId) && keyPairResource.getState() != KeyPairState.ACTIVATED.code());
        }

        @Test
        void activate_illegalState(IdentityHubEndToEndTestContext context) {
            var user1 = "user1";
            var token = context.createParticipant(user1);
            var keyPairId = context.createKeyPair(user1).getResourceId();

            // first revoke the key, which puts it in the REVOKED state
            context.getIdentityApiEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", token))
                    .post("/v1alpha/participants/%s/keypairs/%s/revoke".formatted(toBase64(user1), keyPairId))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(204)
                    .body(notNullValue());

            // now attempt to activate
            context.getIdentityApiEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", token))
                    .post("/v1alpha/participants/%s/keypairs/%s/activate".formatted(toBase64(user1), keyPairId))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400)
                    .body(notNullValue());
        }


        private String toBase64(String s) {
            return Base64.getUrlEncoder().encodeToString(s.getBytes());
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
