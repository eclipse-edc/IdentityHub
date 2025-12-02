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
import org.eclipse.edc.api.auth.spi.ParticipantPrincipal;
import org.eclipse.edc.iam.decentralizedclaims.sts.spi.store.StsAccountStore;
import org.eclipse.edc.identityhub.spi.did.events.DidDocumentPublished;
import org.eclipse.edc.identityhub.spi.did.model.DidState;
import org.eclipse.edc.identityhub.spi.did.store.DidResourceStore;
import org.eclipse.edc.identityhub.spi.keypair.events.KeyPairActivated;
import org.eclipse.edc.identityhub.spi.keypair.events.KeyPairAdded;
import org.eclipse.edc.identityhub.spi.keypair.events.KeyPairRevoked;
import org.eclipse.edc.identityhub.spi.keypair.events.KeyPairRotated;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairResource;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairState;
import org.eclipse.edc.identityhub.spi.keypair.store.KeyPairResourceStore;
import org.eclipse.edc.identityhub.spi.participantcontext.IdentityHubParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.IdentityHubParticipantContext;
import org.eclipse.edc.identityhub.tests.fixtures.DefaultRuntimes;
import org.eclipse.edc.identityhub.tests.fixtures.credentialservice.IdentityHub;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static io.restassured.http.ContentType.JSON;
import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.identityhub.tests.fixtures.TestData.IH_RUNTIME_NAME;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@SuppressWarnings("JUnitMalformedDeclaration")
public class KeyPairResourceApiEndToEndTest {

    abstract static class Tests {

        @AfterEach
        void tearDown(IdentityHubParticipantContextService pcService, DidResourceStore didResourceStore, KeyPairResourceStore keyPairResourceStore, StsAccountStore accountStore) {
            // purge all users, dids, keypairs

            pcService.query(QuerySpec.max()).getContent()
                    .forEach(pc -> pcService.deleteParticipantContext(pc.getParticipantContextId()).getContent());

            didResourceStore.query(QuerySpec.max()).forEach(dr -> didResourceStore.deleteById(dr.getDid()).getContent());

            keyPairResourceStore.query(QuerySpec.max()).getContent()
                    .forEach(kpr -> keyPairResourceStore.deleteById(kpr.getId()).getContent());

            accountStore.findAll(QuerySpec.max())
                    .forEach(sts -> accountStore.deleteById(sts.getId()).getContent());
        }

        @Test
        void findById_notAuthorized(IdentityHub identityHub) {
            var user1 = "user1";
            identityHub.createParticipant(user1);

            // create second user
            var user2 = "user2";
            var user2Context = IdentityHubParticipantContext.Builder.newInstance()
                    .participantContextId(user2)
                    .did("did:web:" + user2)
                    .apiTokenAlias(user2 + "-alias")
                    .build();
            var user2Token = identityHub.storeParticipant(user2Context);

            var key = identityHub.createKeyPair(user1).getResourceId();

            // attempt to publish user1's DID document, which should fail
            identityHub.getIdentityEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", user2Token))
                    .get("/v1alpha/participants/%s/keypairs/%s".formatted(toBase64(user1), key))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403)
                    .body(notNullValue());
        }

        @Test
        void findById(IdentityHub identityHub) {
            var superUserKey = identityHub.createSuperUser().apiKey();
            var user1 = "user1";
            var token = identityHub.createParticipant(user1).apiKey();

            var key = identityHub.createKeyPair(user1).getResourceId();

            assertThat(Arrays.asList(token, superUserKey))
                    .allSatisfy(t -> identityHub.getIdentityEndpoint().baseRequest()
                            .contentType(JSON)
                            .header(new Header("x-api-key", t))
                            .get("/v1alpha/participants/%s/keypairs/%s".formatted(toBase64(user1), key))
                            .then()
                            .log().ifValidationFails()
                            .statusCode(200)
                            .body(notNullValue()));
        }

        @Test
        void findForParticipant_notAuthorized(IdentityHub identityHub) {
            var user1 = "user1";
            identityHub.createParticipant(user1);

            // create second user
            var user2 = "user2";
            var user2Context = IdentityHubParticipantContext.Builder.newInstance()
                    .participantContextId(user2)
                    .roles(List.of(ParticipantPrincipal.ROLE_PARTICIPANT))
                    .did("did:web:" + user2)
                    .apiTokenAlias(user2 + "-alias")
                    .build();
            var user2Token = identityHub.storeParticipant(user2Context);

            identityHub.createKeyPair(user1);

            // attempt to publish user1's DID document, which should fail
            var res = identityHub.getIdentityEndpoint().baseRequest()
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
        void findForParticipant(IdentityHub identityHub) {
            var superUserKey = identityHub.createSuperUser().apiKey();
            var user1 = "user1";
            var token = identityHub.createParticipant(user1).apiKey();
            identityHub.createKeyPair(user1);

            assertThat(Arrays.asList(token, superUserKey))
                    .allSatisfy(t -> identityHub.getIdentityEndpoint().baseRequest()
                            .contentType(JSON)
                            .header(new Header("x-api-key", t))
                            .get("/v1alpha/participants/%s/keypairs".formatted(toBase64(user1)))
                            .then()
                            .log().ifValidationFails()
                            .statusCode(200)
                            .body(notNullValue()));

        }

        @Test
        void addKeyPair(IdentityHub identityHub, EventRouter router) {
            var superUserKey = identityHub.createSuperUser().apiKey();
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(KeyPairAdded.class, subscriber);

            var participantContextId = "user1";
            var token = identityHub.createParticipant(participantContextId).apiKey();

            assertThat(Arrays.asList(token, superUserKey))
                    .allSatisfy(t -> {
                        var keyDesc = identityHub.createKeyDescriptor(participantContextId)
                                .keyId(UUID.randomUUID().toString())
                                .build();
                        identityHub.getIdentityEndpoint().baseRequest()
                                .contentType(JSON)
                                .header(new Header("x-api-key", t))
                                .body(keyDesc)
                                .put("/v1alpha/participants/%s/keypairs".formatted(toBase64(participantContextId)))
                                .then()
                                .log().ifValidationFails()
                                .statusCode(204)
                                .body(notNullValue());

                        assertThat(identityHub.getKeyPairsForParticipant(participantContextId))
                                .hasSizeGreaterThanOrEqualTo(2)
                                .anyMatch(kpr -> kpr.getKeyId().equals(keyDesc.getKeyId()));
                        verify(subscriber).on(argThat(env -> {
                            var evt = (KeyPairAdded) env.getPayload();
                            return evt.getParticipantContextId().equals(participantContextId) &&
                                    evt.getKeyPairResource().getId().equals(keyDesc.getResourceId()) &&
                                    evt.getKeyId().equals(keyDesc.getKeyId());
                        }));
                    });
        }

        @Test
        void addKeyPair_notAuthorized(IdentityHub identityHub, EventRouter router) {
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(KeyPairAdded.class, subscriber);

            var user1 = "user1";
            var token = identityHub.createParticipant(user1);

            var user2 = "user2";
            var token2 = identityHub.createParticipant(user2).apiKey();


            // attempt to publish user1's DID document, which should fail
            var keyDesc = identityHub.createKeyDescriptor(user1).build();
            identityHub.getIdentityEndpoint().baseRequest()
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
        void addKeyPair_participantNotFound(IdentityHub identityHub, EventRouter router) {
            var superUserKey = identityHub.createSuperUser().apiKey();
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(KeyPairAdded.class, subscriber);

            var participantId = "user1";

            var keyDesc = identityHub.createKeyDescriptor(participantId).keyId("new-key-id").build();
            identityHub.getIdentityEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", superUserKey))
                    .body(keyDesc)
                    .put("/v1alpha/participants/%s/keypairs".formatted(toBase64(participantId)))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(404)
                    .body(notNullValue());

            assertThat(identityHub.getKeyPairsForParticipant(participantId)).isEmpty();
            verifyNoInteractions(subscriber);
        }

        @Test
        void addKeyPair_participantDeactivated(IdentityHub identityHub, EventRouter router) {
            var superUserKey = identityHub.createSuperUser().apiKey();
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(KeyPairAdded.class, subscriber);

            var participantId = "user1";
            identityHub.createParticipant(participantId);

            // deactivate participant
            identityHub.getIdentityEndpoint().baseRequest()
                    .header(new Header("x-api-key", superUserKey))
                    .contentType(ContentType.JSON)
                    .post("/v1alpha/participants/%s/state?isActive=false".formatted(toBase64(participantId)))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(204);


            var keyDesc = identityHub.createKeyDescriptor(participantId).keyId("new-key-id").build();
            identityHub.getIdentityEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", superUserKey))
                    .body(keyDesc)
                    .put("/v1alpha/participants/%s/keypairs".formatted(toBase64(participantId)))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400)
                    .body(containsString("To add a key pair, the ParticipantContext with ID 'user1' must be in state"));

            assertThat(identityHub.getKeyPairsForParticipant(participantId)).hasSize(1)
                    .noneMatch(kpr -> kpr.getKeyId().equals(keyDesc.getKeyId()));
            verify(subscriber, never()).on(argThat(e -> e.getPayload() instanceof KeyPairAdded evt && evt.getKeyId().equals(keyDesc.getKeyId())));
        }

        @Test
        void addKeyPair_withoutActivate(IdentityHub identityHub, EventRouter router) {
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(KeyPairActivated.class, subscriber);

            var participantId = "user1";
            var token = identityHub.createParticipant(participantId).apiKey();

            var keyDesc = identityHub.createKeyDescriptor(participantId)
                    .keyId(UUID.randomUUID().toString())
                    .active(false)
                    .build();
            identityHub.getIdentityEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", token))
                    .body(keyDesc)
                    .put("/v1alpha/participants/%s/keypairs".formatted(toBase64(participantId)))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(204)
                    .body(notNullValue());

            assertThat(identityHub.getKeyPairsForParticipant(participantId))
                    .hasSizeGreaterThanOrEqualTo(2)
                    .anyMatch(kpr -> kpr.getState() == KeyPairState.CREATED.code());
            verify(subscriber, never()).on(argThat(evt -> evt.getPayload() instanceof KeyPairActivated kpa && kpa.getKeyId().equals(keyDesc.getKeyId())));
        }

        @Test
        void rotate_withSuperUserToken(IdentityHub identityHub, EventRouter router) {
            var superUserKey = identityHub.createSuperUser().apiKey();
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(KeyPairRotated.class, subscriber);
            router.registerSync(KeyPairAdded.class, subscriber);

            var user1 = "user1";
            identityHub.createParticipant(user1);

            var keyPairId = identityHub.createKeyPair(user1).getResourceId();

            // attempt to publish user1's DID document, which should fail
            var keyDesc = identityHub.createKeyDescriptor(user1).build();
            identityHub.getIdentityEndpoint().baseRequest()
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
                    return evt.getParticipantContextId().equals(user1);
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
        @ValueSource(strings = {"did:web:user1#new-key-id", "new-key-id"})
        void rotate_withUserToken(String keyId, IdentityHub identityHub, EventRouter router, StsAccountStore accountStore) {
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(KeyPairRotated.class, subscriber);
            router.registerSync(KeyPairAdded.class, subscriber);

            var participantId = "user1";
            var userToken = identityHub.createParticipant(participantId).apiKey();

            var keyPairId = identityHub.createKeyPair(participantId).getResourceId();

            // attempt to publish user1's DID document, which should fail
            var keyDesc = identityHub.createKeyDescriptor(participantId)
                    .privateKeyAlias("new-key-alias")
                    .keyId(keyId)
                    .build();
            identityHub.getIdentityEndpoint().baseRequest()
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
                    return evt.getParticipantContextId().equals(participantId);
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

        @Test
        void rotate_withoutNewKey(IdentityHub identityHub, EventRouter router, StsAccountStore accountStore) {

            var participantId = "user1";
            var userToken = identityHub.createParticipant(participantId).apiKey();

            var keyPairId = identityHub.createKeyPair(participantId).getResourceId();

            var subscriber = mock(EventSubscriber.class);
            router.registerSync(KeyPairRotated.class, subscriber);
            router.registerSync(KeyPairAdded.class, subscriber);


            // attempt to publish user1's DID document, which should fail
            identityHub.getIdentityEndpoint().baseRequest()
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
                    return evt.getParticipantContextId().equals(participantId);
                }
                return false;
            }));
            // verify that the correct "added" event fired
            verify(subscriber, never()).on(argThat(env -> env.getPayload() instanceof KeyPairAdded));

        }

        @Test
        void rotate_notAuthorized(IdentityHub identityHub, EventRouter router) {
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(KeyPairRotated.class, subscriber);

            var user1 = "user1";
            var token = identityHub.createParticipant(user1);

            var user2 = "user2";
            var token2 = identityHub.createParticipant(user2).apiKey();

            var keyId = identityHub.createKeyPair(user1).getResourceId();

            // attempt to publish user1's DID document, which should fail
            var keyDesc = identityHub.createKeyDescriptor(user1).build();
            identityHub.getIdentityEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", token2))
                    .body(keyDesc)
                    .post("/v1alpha/participants/%s/keypairs/%s/rotate".formatted(toBase64(user1), keyId))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403)
                    .body(notNullValue());

            // make sure that the event to add the _new_ keypair was never fired
            verify(subscriber, never()).on(argThat(env -> {
                if (env.getPayload() instanceof KeyPairRotated evt) {
                    return evt.getParticipantContextId().equals(user1) && evt.getKeyPairResource().equals(keyDesc.getKeyId());
                }
                return false;
            }));
        }

        @Test
        void rotate_withNewKey_shouldUpdateDidDocument(IdentityHub identityHub, EventRouter router, Vault vault) {
            var participantId = "user1";
            var userToken = identityHub.createParticipant(participantId).apiKey();
            var keyPair = identityHub.getKeyPairsForParticipant(participantId).stream().findFirst().orElseThrow();

            var subscriber = mock(EventSubscriber.class);
            router.registerSync(KeyPairRotated.class, subscriber);
            router.registerSync(KeyPairAdded.class, subscriber);

            var originalAlias = participantId + "-alias";
            var originalKeyId = participantId + "-key";
            var newPrivateKeyAlias = "new-alias";
            var newKeyId = "new-keyId";
            var keyDesc = identityHub.createKeyDescriptor(participantId)
                    .active(true)
                    .privateKeyAlias(newPrivateKeyAlias)
                    .keyId(newKeyId)
                    .build();
            identityHub.getIdentityEndpoint().baseRequest()
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
            var didDoc = identityHub.getDidForParticipant(participantId);
            assertThat(didDoc).isNotEmpty()
                    .allSatisfy(doc -> assertThat(doc.getVerificationMethod()).hasSize(2)
                            .anyMatch(vm -> vm.getId().equals(originalKeyId)) // the original (now-rotated) key
                            .anyMatch(vm -> vm.getId().equals(newKeyId))); // the new key
            assertThat(identityHub.getKeyPairsForParticipant(participantId).stream().filter(kpr -> kpr.getKeyId().equals(originalKeyId)))
                    .allMatch(kpr -> kpr.getState() == KeyPairState.ROTATED.code());
            assertThat(vault.resolveSecret(participantId, originalAlias)).isNull();
            assertThat(vault.resolveSecret(participantId, newPrivateKeyAlias)).isNotNull();

        }

        @Test
        void rotate_withNewKey_whenDidNotPublished_shouldNotUpdate(IdentityHub identityHub, EventRouter router) {
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(KeyPairRotated.class, subscriber);
            router.registerSync(KeyPairAdded.class, subscriber);
            router.registerSync(DidDocumentPublished.class, subscriber);

            var participantId = "user1";
            var userToken = identityHub.createParticipant(participantId, List.of(), false).apiKey();
            var keyPair = identityHub.getKeyPairsForParticipant(participantId).stream().findFirst().orElseThrow();

            var originalKeyId = participantId + "-key";
            var newPrivateKeyAlias = "new-alias";
            var newKeyId = "new-keyId";
            var keyDesc = identityHub.createKeyDescriptor(participantId)
                    .active(true)
                    .privateKeyAlias(newPrivateKeyAlias)
                    .keyId(newKeyId)
                    .build();
            identityHub.getIdentityEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", userToken))
                    .body(keyDesc)
                    .post("/v1alpha/participants/%s/keypairs/%s/rotate".formatted(toBase64(participantId), keyPair.getId()))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(204)
                    .body(notNullValue());

            var didDoc = identityHub.getDidForParticipant(participantId);
            assertThat(didDoc).isNotEmpty()
                    .allSatisfy(doc -> assertThat(doc.getVerificationMethod()).hasSize(2)
                            .anyMatch(vm -> vm.getId().equals(originalKeyId)) // the original (now-rotated) key
                            .anyMatch(vm -> vm.getId().equals(newKeyId))); // the new key
            assertThat(identityHub.getKeyPairsForParticipant(participantId).stream().filter(kpr -> kpr.getKeyId().equals(originalKeyId)))
                    .allMatch(kpr -> kpr.getState() == KeyPairState.ROTATED.code());
            verify(subscriber, never()).on(argThat(evt -> evt.getPayload() instanceof DidDocumentPublished));
        }

        @ParameterizedTest(name = "New Key-ID: {0}")
        @ValueSource(strings = {"new-keyId", "did:web:user1#new-keyId"})
        void revoke(String newKeyId, IdentityHub identityHub, StsAccountStore accountStore) {
            var superUserKey = identityHub.createSuperUser().apiKey();
            var participantId = "user1";
            var token = identityHub.createParticipant(participantId).apiKey();

            var keyId = identityHub.createKeyPair(participantId).getResourceId();

            assertThat(Arrays.asList(token, superUserKey))
                    .allSatisfy(t -> {
                        var keyDesc = identityHub.createKeyDescriptor(participantId)
                                .privateKeyAlias("new-alias")
                                .keyId(newKeyId)
                                .build();

                        identityHub.getIdentityEndpoint().baseRequest()
                                .contentType(JSON)
                                .header(new Header("x-api-key", t))
                                .body(keyDesc)
                                .post("/v1alpha/participants/%s/keypairs/%s/revoke".formatted(toBase64(participantId), keyId))
                                .then()
                                .log().ifValidationFails()
                                .statusCode(204)
                                .body(notNullValue());

                        assertThat(identityHub.getDidForParticipant(participantId)).hasSize(1)
                                .allSatisfy(dd -> assertThat(dd.getVerificationMethod()).noneMatch(vm -> vm.getId().equals(keyId)));

                    });
        }

        @Test
        void revoke_withoutNewKey(IdentityHub identityHub, EventRouter router, StsAccountStore accountStore) {
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(KeyPairRotated.class, subscriber);
            router.registerSync(KeyPairRevoked.class, subscriber);

            var participantId = "user1";
            var userToken = identityHub.createParticipant(participantId).apiKey();

            var keyPairId = identityHub.createKeyPair(participantId).getResourceId();

            // attempt to publish user1's DID document, which should fail
            identityHub.getIdentityEndpoint().baseRequest()
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
                    return evt.getParticipantContextId().equals(participantId);
                }
                return false;
            }));
            // verify that the correct "added" event fired
            verify(subscriber, never()).on(argThat(env -> env.getPayload() instanceof KeyPairAdded));

        }

        @Test
        void revoke_notAuthorized(IdentityHub identityHub) {
            var user1 = "user1";
            var token1 = identityHub.createParticipant(user1);

            var user2 = "user2";
            var token2 = identityHub.createParticipant(user2).apiKey();

            var keyId = identityHub.createKeyPair(user1).getResourceId();

            // attempt to publish user1's DID document, which should fail
            var keyDesc = identityHub.createKeyDescriptor(user1).build();
            identityHub.getIdentityEndpoint().baseRequest()
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
        void getAll(IdentityHub identityHub) {
            var superUserKey = identityHub.createSuperUser().apiKey();
            range(0, 10)
                    .forEach(i -> {
                        var participantId = "user" + i;
                        identityHub.createParticipant(participantId); // implicitly creates a keypair
                    });
            var found = identityHub.getIdentityEndpoint().baseRequest()
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
        void getAll_withPaging(IdentityHub identityHub) {
            var superUserKey = identityHub.createSuperUser().apiKey();
            range(0, 10)
                    .forEach(i -> {
                        var participantId = "user" + i;
                        identityHub.createParticipant(participantId); // implicitly creates a keypair
                    });
            var found = identityHub.getIdentityEndpoint().baseRequest()
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
        void getAll_withDefaultPaging(IdentityHub identityHub) {
            var superUserKey = identityHub.createSuperUser().apiKey();
            range(0, 70)
                    .forEach(i -> {
                        var participantId = "user" + i;
                        identityHub.createParticipant(participantId); // implicitly creates a keypair
                    });
            var found = identityHub.getIdentityEndpoint().baseRequest()
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
        void getAll_notAuthorized(IdentityHub identityHub) {
            var attackerToken = identityHub.createParticipant("attacker").apiKey();

            range(0, 10)
                    .forEach(i -> {
                        var participantId = "user" + i;
                        identityHub.createParticipant(participantId); // implicitly creates a keypair
                    });
            identityHub.getIdentityEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", attackerToken))
                    .get("/v1alpha/keypairs")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403);
        }

        @Test
        void activate_superUserToken(IdentityHub identityHub, EventRouter router) {
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(KeyPairActivated.class, subscriber);

            var superUserKey = identityHub.createSuperUser().apiKey();
            var user1 = "user1";
            identityHub.createParticipant(user1);
            var keyDescriptor = identityHub.createKeyPair(user1);
            var keyPairId = keyDescriptor.getResourceId();

            identityHub.getIdentityEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", superUserKey))
                    .post("/v1alpha/participants/%s/keypairs/%s/activate".formatted(toBase64(user1), keyPairId))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(204)
                    .body(notNullValue());

            assertThat(identityHub.getDidForParticipant(user1))
                    .hasSize(1)
                    .anySatisfy(dd -> assertThat(dd.getVerificationMethod()).hasSize(2).anyMatch(vm -> vm.getId().equals(keyDescriptor.getKeyId())));

            assertThat(identityHub.getDidResourceForParticipant("did:web:" + user1).getState()).isEqualTo(DidState.PUBLISHED.code());
            verify(subscriber).on(argThat(e -> e.getPayload() instanceof KeyPairActivated kpa && kpa.getKeyPairResource().getId().equals(keyPairId)));
        }

        @Test
        void activate_userToken(IdentityHub identityHub, EventRouter router) {
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(KeyPairActivated.class, subscriber);
            router.registerSync(DidDocumentPublished.class, subscriber);

            var participantId = "user1";
            var token = identityHub.createParticipant(participantId).apiKey();
            assertThat(identityHub.getDidForParticipant(participantId))
                    .allSatisfy(dd -> assertThat(dd.getVerificationMethod()).hasSize(1));

            var keyDescriptor = identityHub.createKeyDescriptor(participantId).active(false).build();
            identityHub.createKeyPair(participantId, keyDescriptor);
            var keyPairId = keyDescriptor.getResourceId();

            identityHub.getIdentityEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", token))
                    .post("/v1alpha/participants/%s/keypairs/%s/activate".formatted(toBase64(participantId), keyPairId))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(204)
                    .body(notNullValue());

            assertThat(identityHub.getDidForParticipant(participantId))
                    .hasSize(1)
                    .allSatisfy(dd -> assertThat(dd.getVerificationMethod())
                            .hasSize(2)
                            .anyMatch(vm -> vm.getId().equals(keyDescriptor.getKeyId())));

            assertThat(identityHub.getDidResourceForParticipant("did:web:" + participantId).getState()).isEqualTo(DidState.PUBLISHED.code());
            verify(subscriber).on(argThat(e -> e.getPayload() instanceof KeyPairActivated kpa && kpa.getKeyPairResource().getId().equals(keyPairId)));
            // publishes did when creating the user, and when activating
            verify(subscriber, atLeast(2)).on(argThat(e -> e.getPayload() instanceof DidDocumentPublished));
        }

        @Test
        void activate_whenParticipantNotActive_shouldNotPublishDid(IdentityHub identityHub, EventRouter router) {
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(KeyPairActivated.class, subscriber);

            var user1 = "user1";
            var token = identityHub.createParticipant(user1, List.of(), false).apiKey();
            var keyDescriptor = identityHub.createKeyPair(user1);
            var keyPairId = keyDescriptor.getResourceId();

            identityHub.getIdentityEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", token))
                    .post("/v1alpha/participants/%s/keypairs/%s/activate".formatted(toBase64(user1), keyPairId))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(204)
                    .body(notNullValue());

            // DID should contain 2 VerificationMethods, one of with should be the newly activated one
            assertThat(identityHub.getDidForParticipant(user1))
                    .hasSize(1)
                    .allSatisfy(dd -> assertThat(dd.getVerificationMethod())
                            .hasSize(2)
                            .anyMatch(vm -> vm.getId().equals(keyDescriptor.getKeyId())));

            assertThat(identityHub.getDidResourceForParticipant("did:web:" + user1).getState()).isNotEqualTo(DidState.PUBLISHED.code());
            // all key pairs should be ACTIVATED
            assertThat(identityHub.getKeyPairsForParticipant(user1))
                    .allMatch(kpr -> kpr.getState() == KeyPairState.ACTIVATED.code());

            verify(subscriber).on(argThat(e -> e.getPayload() instanceof KeyPairActivated kpa && kpa.getKeyPairResource().getId().equals(keyPairId)));
        }

        @Test
        void activate_notExists(IdentityHub identityHub, EventRouter router) {
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(KeyPairActivated.class, subscriber);

            var superUserKey = identityHub.createSuperUser().apiKey();
            var user1 = "user1";
            var token = identityHub.createParticipant(user1).apiKey();
            var keyPairId = "non-exist-keypair-id";

            assertThat(Arrays.asList(token, superUserKey))
                    .allSatisfy(t -> {
                        reset(subscriber);
                        identityHub.getIdentityEndpoint().baseRequest()
                                .contentType(JSON)
                                .header(new Header("x-api-key", t))
                                .post("/v1alpha/participants/%s/keypairs/%s/activate".formatted(toBase64(user1), keyPairId))
                                .then()
                                .log().ifError()
                                .statusCode(404)
                                .body(notNullValue());

                        assertThat(identityHub.getDidForParticipant(user1))
                                .hasSize(1)
                                .allSatisfy(dd -> assertThat(dd.getVerificationMethod()).noneMatch(vm -> vm.getId().equals(keyPairId)));

                        verifyNoInteractions(subscriber);
                    });
        }

        @Test
        void activate_notAuthorized(IdentityHub identityHub) {
            var user1 = "user1";
            identityHub.createParticipant(user1);
            var keyId = identityHub.createKeyPair(user1).getResourceId();
            var attackerToken = identityHub.createParticipant("attacker").apiKey();

            identityHub.getIdentityEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", attackerToken))
                    .post("/v1alpha/participants/%s/keypairs/%s/activate".formatted(toBase64(user1), keyId))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403)
                    .body(notNullValue());

            assertThat(identityHub.getKeyPairsForParticipant(user1))
                    .hasSize(2)
                    .anyMatch(keyPairResource -> keyPairResource.getId().equals(keyId) && keyPairResource.getState() != KeyPairState.ACTIVATED.code());
        }

        @Test
        void activate_illegalState(IdentityHub identityHub) {
            var user1 = "user1";
            var token = identityHub.createParticipant(user1).apiKey();
            var keyPairId = identityHub.createKeyPair(user1).getResourceId();

            // first revoke the key, which puts it in the REVOKED state
            identityHub.getIdentityEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", token))
                    .post("/v1alpha/participants/%s/keypairs/%s/revoke".formatted(toBase64(user1), keyPairId))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(204)
                    .body(notNullValue());

            // now attempt to activate
            identityHub.getIdentityEndpoint().baseRequest()
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
    class InMemory extends Tests {

        @RegisterExtension
        static final RuntimeExtension IDENTITY_HUB_EXTENSION = ComponentRuntimeExtension.Builder.newInstance()
                .name(IH_RUNTIME_NAME)
                .modules(DefaultRuntimes.IdentityHub.MODULES)
                .endpoints(DefaultRuntimes.IdentityHub.ENDPOINTS.build())
                .configurationProvider(DefaultRuntimes.IdentityHub::config)
                .paramProvider(IdentityHub.class, IdentityHub::forContext)
                .build();
    }

    @Nested
    @PostgresqlIntegrationTest
    class Postgres extends Tests {

        @Order(0)
        @RegisterExtension
        static final PostgresqlEndToEndExtension POSTGRESQL_EXTENSION = new PostgresqlEndToEndExtension();
        private static final String DB_NAME = "runtime";

        @Order(1)
        @RegisterExtension
        static final BeforeAllCallback POSTGRES_CONTAINER_STARTER = context -> {
            POSTGRESQL_EXTENSION.createDatabase(DB_NAME);
        };

        @Order(2)
        @RegisterExtension
        static final RuntimeExtension IDENTITY_HUB_EXTENSION = ComponentRuntimeExtension.Builder.newInstance()
                .name(IH_RUNTIME_NAME)
                .modules(DefaultRuntimes.IdentityHub.SQL_MODULES)
                .endpoints(DefaultRuntimes.IdentityHub.ENDPOINTS.build())
                .configurationProvider(DefaultRuntimes.IdentityHub::config)
                .configurationProvider(() -> POSTGRESQL_EXTENSION.configFor(DB_NAME))
                .paramProvider(IdentityHub.class, IdentityHub::forContext)
                .build();

    }
}
