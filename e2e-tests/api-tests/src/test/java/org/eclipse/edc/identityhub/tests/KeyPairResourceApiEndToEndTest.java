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
import org.eclipse.edc.iam.identitytrust.sts.spi.store.StsAccountStore;
import org.eclipse.edc.identithub.spi.did.events.DidDocumentPublished;
import org.eclipse.edc.identithub.spi.did.model.DidState;
import org.eclipse.edc.identithub.spi.did.store.DidResourceStore;
import org.eclipse.edc.identityhub.spi.keypair.events.KeyPairActivated;
import org.eclipse.edc.identityhub.spi.keypair.events.KeyPairAdded;
import org.eclipse.edc.identityhub.spi.keypair.events.KeyPairRevoked;
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
import org.eclipse.edc.spi.security.Vault;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static io.restassured.http.ContentType.JSON;
import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class KeyPairResourceApiEndToEndTest {

    abstract static class Tests {

        @AfterEach
        void tearDown(ParticipantContextService pcService, DidResourceStore didResourceStore, KeyPairResourceStore keyPairResourceStore, StsAccountStore accountStore) {
            // purge all users, dids, keypairs

            pcService.query(QuerySpec.max()).getContent()
                    .forEach(pc -> pcService.deleteParticipantContext(pc.getParticipantId()).getContent());

            didResourceStore.query(QuerySpec.max()).forEach(dr -> didResourceStore.deleteById(dr.getDid()).getContent());

            keyPairResourceStore.query(QuerySpec.max()).getContent()
                    .forEach(kpr -> keyPairResourceStore.deleteById(kpr.getId()).getContent());

            accountStore.findAll(QuerySpec.max())
                    .forEach(sts -> accountStore.deleteById(sts.getId()).getContent());
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

            var participantId = "user1";
            var token = context.createParticipant(participantId);

            assertThat(Arrays.asList(token, superUserKey))
                    .allSatisfy(t -> {
                        var keyDesc = context.createKeyDescriptor(participantId)
                                .keyId(UUID.randomUUID().toString())
                                .build();
                        context.getIdentityApiEndpoint().baseRequest()
                                .contentType(JSON)
                                .header(new Header("x-api-key", t))
                                .body(keyDesc)
                                .put("/v1alpha/participants/%s/keypairs".formatted(toBase64(participantId)))
                                .then()
                                .log().ifValidationFails()
                                .statusCode(204)
                                .body(notNullValue());

                        assertThat(context.getKeyPairsForParticipant(participantId))
                                .hasSizeGreaterThanOrEqualTo(2)
                                .anyMatch(kpr -> kpr.getKeyId().equals(keyDesc.getKeyId()));
                        verify(subscriber).on(argThat(env -> {
                            var evt = (KeyPairAdded) env.getPayload();
                            return evt.getParticipantId().equals(participantId) &&
                                    evt.getKeyPairResource().getId().equals(keyDesc.getResourceId()) &&
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
                    return evt.getKeyPairResource().equals(keyDesc.getKeyId());
                }
                return false;
            }));
        }

        @Test
        void addKeyPair_participantNotFound(IdentityHubEndToEndTestContext context, EventRouter router) {
            var superUserKey = context.createSuperUser();
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(KeyPairAdded.class, subscriber);

            var participantId = "user1";

            var keyDesc = context.createKeyDescriptor(participantId).keyId("new-key-id").build();
            context.getIdentityApiEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", superUserKey))
                    .body(keyDesc)
                    .put("/v1alpha/participants/%s/keypairs".formatted(toBase64(participantId)))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(404)
                    .body(notNullValue());

            assertThat(context.getKeyPairsForParticipant(participantId)).isEmpty();
            verifyNoInteractions(subscriber);
        }

        @Test
        void addKeyPair_participantDeactivated(IdentityHubEndToEndTestContext context, EventRouter router) {
            var superUserKey = context.createSuperUser();
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(KeyPairAdded.class, subscriber);

            var participantId = "user1";
            context.createParticipant(participantId);

            // deactivate participant
            context.getIdentityApiEndpoint().baseRequest()
                    .header(new Header("x-api-key", superUserKey))
                    .contentType(ContentType.JSON)
                    .post("/v1alpha/participants/%s/state?isActive=false".formatted(toBase64(participantId)))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(204);


            var keyDesc = context.createKeyDescriptor(participantId).keyId("new-key-id").build();
            context.getIdentityApiEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", superUserKey))
                    .body(keyDesc)
                    .put("/v1alpha/participants/%s/keypairs".formatted(toBase64(participantId)))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400)
                    .body(containsString("To add a key pair, the ParticipantContext with ID 'user1' must be in state"));

            assertThat(context.getKeyPairsForParticipant(participantId)).hasSize(1)
                    .noneMatch(kpr -> kpr.getKeyId().equals(keyDesc.getKeyId()));
            verify(subscriber, never()).on(argThat(e -> e.getPayload() instanceof KeyPairAdded evt && evt.getKeyId().equals(keyDesc.getKeyId())));
        }

        @Test
        void addKeyPair_withoutActivate(IdentityHubEndToEndTestContext context, EventRouter router) {
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(KeyPairActivated.class, subscriber);

            var participantId = "user1";
            var token = context.createParticipant(participantId);

            var keyDesc = context.createKeyDescriptor(participantId)
                    .keyId(UUID.randomUUID().toString())
                    .active(false)
                    .build();
            context.getIdentityApiEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", token))
                    .body(keyDesc)
                    .put("/v1alpha/participants/%s/keypairs".formatted(toBase64(participantId)))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(204)
                    .body(notNullValue());

            assertThat(context.getKeyPairsForParticipant(participantId))
                    .hasSizeGreaterThanOrEqualTo(2)
                    .anyMatch(kpr -> kpr.getState() == KeyPairState.CREATED.code());
            verify(subscriber, never()).on(argThat(evt -> evt.getPayload() instanceof KeyPairActivated kpa && kpa.getKeyId().equals(keyDesc.getKeyId())));
        }

        @Test
        void rotate_withSuperUserToken(IdentityHubEndToEndTestContext context, EventRouter router) {
            var superUserKey = context.createSuperUser();
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(KeyPairRotated.class, subscriber);
            router.registerSync(KeyPairAdded.class, subscriber);

            var user1 = "user1";
            context.createParticipant(user1);

            var keyPairId = context.createKeyPair(user1).getResourceId();

            // attempt to publish user1's DID document, which should fail
            var keyDesc = context.createKeyDescriptor(user1).build();
            context.getIdentityApiEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", superUserKey))
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
                    return evt.getKeyPairResource().getId().equals(keyDesc.getResourceId()) &&
                            evt.getKeyId().equals(keyDesc.getKeyId());
                }
                return false;
            }));
        }

        @ParameterizedTest(name = "New KeyID {0}")
        @ValueSource(strings = { "did:web:user1#new-key-id", "new-key-id" })
        void rotate_withUserToken(String keyId, IdentityHubEndToEndTestContext context, EventRouter router, StsAccountStore accountStore) {
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(KeyPairRotated.class, subscriber);
            router.registerSync(KeyPairAdded.class, subscriber);

            var participantId = "user1";
            var userToken = context.createParticipant(participantId);

            var keyPairId = context.createKeyPair(participantId).getResourceId();

            // attempt to publish user1's DID document, which should fail
            var keyDesc = context.createKeyDescriptor(participantId)
                    .privateKeyAlias("new-key-alias")
                    .keyId(keyId)
                    .build();
            context.getIdentityApiEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", userToken))
                    .body(keyDesc)
                    .post("/v1alpha/participants/%s/keypairs/%s/rotate".formatted(toBase64(participantId), keyPairId))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(204)
                    .body(notNullValue());

            // verify that the "rotated" event fired once
            verify(subscriber).on(argThat(env -> {
                if (env.getPayload() instanceof KeyPairRotated evt) {
                    return evt.getParticipantId().equals(participantId);
                }
                return false;
            }));
            // verify that the correct "added" event fired
            verify(subscriber).on(argThat(env -> {
                if (env.getPayload() instanceof KeyPairAdded evt) {
                    return evt.getKeyPairResource().getId().equals(keyDesc.getResourceId()) &&
                            evt.getKeyId().equals(keyDesc.getKeyId());
                }
                return false;
            }));

            // verify that the STS client got updated correctly
            assertThat(accountStore.findById(participantId)).isSucceeded()
                    .satisfies(stsClient -> {
                        assertThat(stsClient.getPrivateKeyAlias()).isEqualTo("new-key-alias");
                        assertThat(stsClient.getPublicKeyReference()).isEqualTo("did:web:" + participantId + "#new-key-id");
                    });
        }

        @Test
        void rotate_withoutNewKey(IdentityHubEndToEndTestContext context, EventRouter router, StsAccountStore accountStore) {

            var participantId = "user1";
            var userToken = context.createParticipant(participantId);

            var keyPairId = context.createKeyPair(participantId).getResourceId();

            var subscriber = mock(EventSubscriber.class);
            router.registerSync(KeyPairRotated.class, subscriber);
            router.registerSync(KeyPairAdded.class, subscriber);


            // attempt to publish user1's DID document, which should fail
            context.getIdentityApiEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", userToken))
                    .post("/v1alpha/participants/%s/keypairs/%s/rotate".formatted(toBase64(participantId), keyPairId))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(204)
                    .body(notNullValue());

            // verify that the "rotated" event fired once
            verify(subscriber).on(argThat(env -> {
                if (env.getPayload() instanceof KeyPairRotated evt) {
                    return evt.getParticipantId().equals(participantId);
                }
                return false;
            }));
            // verify that the correct "added" event fired
            verify(subscriber, never()).on(argThat(env -> env.getPayload() instanceof KeyPairAdded));

            // verify that the STS client got updated correctly
            assertThat(accountStore.findById(participantId)).isSucceeded()
                    .satisfies(stsClient -> {
                        assertThat(stsClient.getPrivateKeyAlias()).isEqualTo("");
                        assertThat(stsClient.getPublicKeyReference()).isEqualTo("");
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
                    return evt.getParticipantId().equals(user1) && evt.getKeyPairResource().equals(keyDesc.getKeyId());
                }
                return false;
            }));
        }

        @Test
        void rotate_withNewKey_shouldUpdateDidDocument(IdentityHubEndToEndTestContext context, EventRouter router, Vault vault) {
            var participantId = "user1";
            var userToken = context.createParticipant(participantId);
            var keyPair = context.getKeyPairsForParticipant(participantId).stream().findFirst().orElseThrow();

            var subscriber = mock(EventSubscriber.class);
            router.registerSync(KeyPairRotated.class, subscriber);
            router.registerSync(KeyPairAdded.class, subscriber);

            var originalAlias = participantId + "-alias";
            var originalKeyId = participantId + "-key";
            var newPrivateKeyAlias = "new-alias";
            var newKeyId = "new-keyId";
            var keyDesc = context.createKeyDescriptor(participantId)
                    .active(true)
                    .privateKeyAlias(newPrivateKeyAlias)
                    .keyId(newKeyId)
                    .build();
            context.getIdentityApiEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", userToken))
                    .body(keyDesc)
                    .post("/v1alpha/participants/%s/keypairs/%s/rotate".formatted(toBase64(participantId), keyPair.getId()))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(204)
                    .body(notNullValue());

            verify(subscriber).on(argThat(evt -> evt.getPayload() instanceof KeyPairRotated));
            verify(subscriber).on(argThat(evt -> evt.getPayload() instanceof KeyPairAdded));
            var didDoc = context.getDidForParticipant(participantId);
            assertThat(didDoc).isNotEmpty()
                    .allSatisfy(doc -> assertThat(doc.getVerificationMethod()).hasSize(2)
                            .anyMatch(vm -> vm.getId().equals(originalKeyId)) // the original (now-rotated) key
                            .anyMatch(vm -> vm.getId().equals(newKeyId))); // the new key
            assertThat(context.getKeyPairsForParticipant(participantId).stream().filter(kpr -> kpr.getKeyId().equals(originalKeyId)))
                    .allMatch(kpr -> kpr.getState() == KeyPairState.ROTATED.code());
            assertThat(vault.resolveSecret(originalAlias)).isNull();
            assertThat(vault.resolveSecret(newPrivateKeyAlias)).isNotNull();

        }

        @Test
        void rotate_withNewKey_whenDidNotPublished_shouldNotUpdate(IdentityHubEndToEndTestContext context, EventRouter router) {
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(KeyPairRotated.class, subscriber);
            router.registerSync(KeyPairAdded.class, subscriber);
            router.registerSync(DidDocumentPublished.class, subscriber);

            var participantId = "user1";
            var userToken = context.createParticipant(participantId, List.of(), false);
            var keyPair = context.getKeyPairsForParticipant(participantId).stream().findFirst().orElseThrow();

            var originalKeyId = participantId + "-key";
            var newPrivateKeyAlias = "new-alias";
            var newKeyId = "new-keyId";
            var keyDesc = context.createKeyDescriptor(participantId)
                    .active(true)
                    .privateKeyAlias(newPrivateKeyAlias)
                    .keyId(newKeyId)
                    .build();
            context.getIdentityApiEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", userToken))
                    .body(keyDesc)
                    .post("/v1alpha/participants/%s/keypairs/%s/rotate".formatted(toBase64(participantId), keyPair.getId()))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(204)
                    .body(notNullValue());

            var didDoc = context.getDidForParticipant(participantId);
            assertThat(didDoc).isNotEmpty()
                    .allSatisfy(doc -> assertThat(doc.getVerificationMethod()).hasSize(2)
                            .anyMatch(vm -> vm.getId().equals(originalKeyId)) // the original (now-rotated) key
                            .anyMatch(vm -> vm.getId().equals(newKeyId))); // the new key
            assertThat(context.getKeyPairsForParticipant(participantId).stream().filter(kpr -> kpr.getKeyId().equals(originalKeyId)))
                    .allMatch(kpr -> kpr.getState() == KeyPairState.ROTATED.code());
            verify(subscriber, never()).on(argThat(evt -> evt.getPayload() instanceof DidDocumentPublished));
        }

        @ParameterizedTest(name = "New Key-ID: {0}")
        @ValueSource(strings = { "new-keyId", "did:web:user1#new-keyId" })
        void revoke(String newKeyId, IdentityHubEndToEndTestContext context, StsAccountStore accountStore) {
            var superUserKey = context.createSuperUser();
            var participantId = "user1";
            var token = context.createParticipant(participantId);

            var keyId = context.createKeyPair(participantId).getResourceId();

            assertThat(Arrays.asList(token, superUserKey))
                    .allSatisfy(t -> {
                        var keyDesc = context.createKeyDescriptor(participantId)
                                .privateKeyAlias("new-alias")
                                .keyId(newKeyId)
                                .build();

                        context.getIdentityApiEndpoint().baseRequest()
                                .contentType(JSON)
                                .header(new Header("x-api-key", t))
                                .body(keyDesc)
                                .post("/v1alpha/participants/%s/keypairs/%s/revoke".formatted(toBase64(participantId), keyId))
                                .then()
                                .log().ifValidationFails()
                                .statusCode(204)
                                .body(notNullValue());

                        assertThat(context.getDidForParticipant(participantId)).hasSize(1)
                                .allSatisfy(dd -> assertThat(dd.getVerificationMethod()).noneMatch(vm -> vm.getId().equals(keyId)));

                        // verify that the STS client got updated correctly
                        assertThat(accountStore.findById(participantId)).isSucceeded()
                                .satisfies(stsClient -> {
                                    assertThat(stsClient.getPrivateKeyAlias()).isEqualTo("new-alias");
                                    assertThat(stsClient.getPublicKeyReference()).isEqualTo("did:web:" + participantId + "#new-keyId");
                                });
                    });
        }

        @Test
        void revoke_withoutNewKey(IdentityHubEndToEndTestContext context, EventRouter router, StsAccountStore accountStore) {
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(KeyPairRotated.class, subscriber);
            router.registerSync(KeyPairRevoked.class, subscriber);

            var participantId = "user1";
            var userToken = context.createParticipant(participantId);

            var keyPairId = context.createKeyPair(participantId).getResourceId();

            // attempt to publish user1's DID document, which should fail
            context.getIdentityApiEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", userToken))
                    .post("/v1alpha/participants/%s/keypairs/%s/revoke".formatted(toBase64(participantId), keyPairId))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(204)
                    .body(notNullValue());

            // verify that the "rotated" event fired once
            verify(subscriber).on(argThat(env -> {
                if (env.getPayload() instanceof KeyPairRevoked evt) {
                    return evt.getParticipantId().equals(participantId);
                }
                return false;
            }));
            // verify that the correct "added" event fired
            verify(subscriber, never()).on(argThat(env -> env.getPayload() instanceof KeyPairAdded));

            // verify that the STS client got updated correctly
            assertThat(accountStore.findById(participantId)).isSucceeded()
                    .satisfies(stsClient -> {
                        assertThat(stsClient.getPrivateKeyAlias()).isEqualTo("");
                        assertThat(stsClient.getPublicKeyReference()).isEqualTo("");
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
            verify(subscriber).on(argThat(e -> e.getPayload() instanceof KeyPairActivated kpa && kpa.getKeyPairResource().getId().equals(keyPairId)));
        }

        @Test
        void activate_userToken(IdentityHubEndToEndTestContext context, EventRouter router) {
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(KeyPairActivated.class, subscriber);
            router.registerSync(DidDocumentPublished.class, subscriber);

            var participantId = "user1";
            var token = context.createParticipant(participantId);
            assertThat(context.getDidForParticipant(participantId))
                    .allSatisfy(dd -> assertThat(dd.getVerificationMethod()).hasSize(1));

            var keyDescriptor = context.createKeyDescriptor(participantId).active(false).build();
            context.createKeyPair(participantId, keyDescriptor);
            var keyPairId = keyDescriptor.getResourceId();

            context.getIdentityApiEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", token))
                    .post("/v1alpha/participants/%s/keypairs/%s/activate".formatted(toBase64(participantId), keyPairId))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(204)
                    .body(notNullValue());

            assertThat(context.getDidForParticipant(participantId))
                    .hasSize(1)
                    .allSatisfy(dd -> assertThat(dd.getVerificationMethod())
                            .hasSize(2)
                            .anyMatch(vm -> vm.getId().equals(keyDescriptor.getKeyId())));

            assertThat(context.getDidResourceForParticipant("did:web:" + participantId).getState()).isEqualTo(DidState.PUBLISHED.code());
            verify(subscriber).on(argThat(e -> e.getPayload() instanceof KeyPairActivated kpa && kpa.getKeyPairResource().getId().equals(keyPairId)));
            // publishes did when creating the user, and when activating
            verify(subscriber, atLeast(2)).on(argThat(e -> e.getPayload() instanceof DidDocumentPublished));
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

            verify(subscriber).on(argThat(e -> e.getPayload() instanceof KeyPairActivated kpa && kpa.getKeyPairResource().getId().equals(keyPairId)));
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
