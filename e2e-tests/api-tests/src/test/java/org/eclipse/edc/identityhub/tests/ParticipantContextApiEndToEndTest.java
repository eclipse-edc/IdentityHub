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
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.identithub.spi.did.DidConstants;
import org.eclipse.edc.identithub.spi.did.DidDocumentPublisher;
import org.eclipse.edc.identithub.spi.did.DidDocumentPublisherRegistry;
import org.eclipse.edc.identithub.spi.did.model.DidResource;
import org.eclipse.edc.identithub.spi.did.model.DidState;
import org.eclipse.edc.identithub.spi.did.store.DidResourceStore;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairResource;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairState;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.events.ParticipantContextCreated;
import org.eclipse.edc.identityhub.spi.participantcontext.events.ParticipantContextUpdated;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContextState;
import org.eclipse.edc.identityhub.spi.store.KeyPairResourceStore;
import org.eclipse.edc.identityhub.tests.fixtures.IdentityHubEndToEndExtension;
import org.eclipse.edc.identityhub.tests.fixtures.IdentityHubEndToEndTestContext;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
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
import java.util.stream.IntStream;

import static io.restassured.http.ContentType.JSON;
import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.identityhub.tests.fixtures.IdentityHubEndToEndTestContext.SUPER_USER;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class ParticipantContextApiEndToEndTest {

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
        void getUserById(IdentityHubEndToEndTestContext context) {
            var apikey = context.createSuperUser();

            var su = context.getIdentityApiEndpoint().baseRequest()
                    .header(new Header("x-api-key", apikey))
                    .get("/v1alpha/participants/" + toBase64(SUPER_USER))
                    .then()
                    .statusCode(200)
                    .extract().body().as(ParticipantContext.class);
            assertThat(su.getParticipantId()).isEqualTo(SUPER_USER);
        }

        @Test
        void getUserById_notOwner_expect403(IdentityHubEndToEndTestContext context) {
            var user1 = "user1";
            var user1Context = ParticipantContext.Builder.newInstance()
                    .participantId(user1)
                    .did("did:web:" + user1)
                    .apiTokenAlias(user1 + "-alias")
                    .build();
            var apiToken1 = context.storeParticipant(user1Context);

            var user2 = "user2";
            var user2Context = ParticipantContext.Builder.newInstance()
                    .participantId(user2)
                    .did("did:web:" + user2)
                    .apiTokenAlias(user2 + "-alias")
                    .build();
            context.storeParticipant(user2Context);

            //user1 attempts to read user2 -> fail
            context.getIdentityApiEndpoint().baseRequest()
                    .header(new Header("x-api-key", apiToken1))
                    .contentType(ContentType.JSON)
                    .get("/v1alpha/participants/" + toBase64(user2))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403);
        }

        @Test
        void createNewUser_principalIsSuperuser(IdentityHubEndToEndTestContext context, EventRouter router) {
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(ParticipantContextCreated.class, subscriber);
            var apikey = context.createSuperUser();

            var manifest = context.createNewParticipant().build();

            context.getIdentityApiEndpoint().baseRequest()
                    .header(new Header("x-api-key", apikey))
                    .contentType(ContentType.JSON)
                    .body(manifest)
                    .post("/v1alpha/participants/")
                    .then()
                    .log().ifError()
                    .statusCode(anyOf(equalTo(200), equalTo(204)))
                    .body(notNullValue());

            verify(subscriber).on(argThat(env -> ((ParticipantContextCreated) env.getPayload()).getParticipantId().equals(manifest.getParticipantId())));

            assertThat(context.getKeyPairsForParticipant(manifest.getParticipantId())).hasSize(1);
            assertThat(context.getDidForParticipant(manifest.getParticipantId())).hasSize(1)
                    .allSatisfy(dd -> assertThat(dd.getVerificationMethod()).hasSize(1));
        }

        @Test
        void createNewUser_whenKeyPairActive(IdentityHubEndToEndTestContext context, EventRouter router) {
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(ParticipantContextCreated.class, subscriber);
            var apikey = context.createSuperUser();

            var participantId = UUID.randomUUID().toString();
            var manifest = context.createNewParticipant()
                    .participantId(participantId)
                    .active(true)
                    .did("did:web:" + participantId)
                    .key(context.createKeyDescriptor().active(true).build())
                    .build();

            context.getIdentityApiEndpoint().baseRequest()
                    .header(new Header("x-api-key", apikey))
                    .contentType(ContentType.JSON)
                    .body(manifest)
                    .post("/v1alpha/participants/")
                    .then()
                    .log().ifError()
                    .statusCode(anyOf(equalTo(200), equalTo(204)))
                    .body(notNullValue());

            verify(subscriber).on(argThat(env -> ((ParticipantContextCreated) env.getPayload()).getParticipantId().equals(manifest.getParticipantId())));

            assertThat(context.getKeyPairsForParticipant(manifest.getParticipantId())).hasSize(1)
                    .allSatisfy(kpr -> assertThat(kpr.getState()).isEqualTo(KeyPairState.ACTIVATED.code()));
            assertThat(context.getDidForParticipant(manifest.getParticipantId()))
                    .hasSize(1)
                    .allSatisfy(dd -> assertThat(dd.getVerificationMethod()).hasSize(1));

        }

        @Test
        void createNewUser_whenKeyPairNotActive(IdentityHubEndToEndTestContext context, EventRouter router) {
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(ParticipantContextCreated.class, subscriber);
            var apikey = context.createSuperUser();

            var participantId = UUID.randomUUID().toString();
            var manifest = context.createNewParticipant()
                    .active(true)
                    .participantId(participantId)
                    .did("did:web:" + participantId)
                    .key(context.createKeyDescriptor().active(false).build())
                    .build();

            context.getIdentityApiEndpoint().baseRequest()
                    .header(new Header("x-api-key", apikey))
                    .contentType(ContentType.JSON)
                    .body(manifest)
                    .post("/v1alpha/participants/")
                    .then()
                    .log().ifError()
                    .statusCode(anyOf(equalTo(200), equalTo(204)))
                    .body(notNullValue());

            verify(subscriber).on(argThat(env -> ((ParticipantContextCreated) env.getPayload()).getParticipantId().equals(manifest.getParticipantId())));

            assertThat(context.getKeyPairsForParticipant(manifest.getParticipantId())).hasSize(1)
                    .allSatisfy(kpr -> assertThat(kpr.getState()).isEqualTo(KeyPairState.CREATED.code()));

            // inactive key-pairs don't get added to the DID Document
            assertThat(context.getDidForParticipant(manifest.getParticipantId()))
                    .hasSize(1)
                    .allSatisfy(dd -> assertThat(dd.getVerificationMethod()).isEmpty());

        }

        @Test
        void createNewUser_principalIsNotSuperuser_expect403(IdentityHubEndToEndTestContext context, EventRouter router) {
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(ParticipantContextCreated.class, subscriber);

            var principal = "another-user";
            var anotherUser = ParticipantContext.Builder.newInstance()
                    .participantId(principal)
                    .did("did:web:" + principal)
                    .apiTokenAlias(principal + "-alias")
                    .build();
            var apiToken = context.storeParticipant(anotherUser);
            var manifest = context.createNewParticipant().build();

            context.getIdentityApiEndpoint().baseRequest()
                    .header(new Header("x-api-key", apiToken))
                    .contentType(ContentType.JSON)
                    .body(manifest)
                    .post("/v1alpha/participants/")
                    .then()
                    .log().ifError()
                    .statusCode(403)
                    .body(notNullValue());
            verifyNoInteractions(subscriber);

            assertThat(context.getKeyPairsForParticipant(manifest.getParticipantId())).isEmpty();
        }

        @Test
        void createNewUser_principalIsKnown_expect401(IdentityHubEndToEndTestContext context, EventRouter router) {
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(ParticipantContextCreated.class, subscriber);
            var principal = "another-user";

            var manifest = context.createNewParticipant().build();

            context.getIdentityApiEndpoint().baseRequest()
                    .header(new Header("x-api-key", context.createTokenFor(principal)))
                    .contentType(ContentType.JSON)
                    .body(manifest)
                    .post("/v1alpha/participants/")
                    .then()
                    .log().ifError()
                    .statusCode(401)
                    .body(notNullValue());
            verifyNoInteractions(subscriber);
            assertThat(context.getKeyPairsForParticipant(manifest.getParticipantId())).isEmpty();
        }

        @Test
        void createNewUser_whenDidAlreadyExists_expect409(IdentityHubEndToEndTestContext context, DidResourceStore didResourceStore, EventRouter router) {
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(ParticipantContextCreated.class, subscriber);
            var apikey = context.createSuperUser();

            var manifest = context.createNewParticipant().build();

            didResourceStore.save(DidResource.Builder.newInstance().did(manifest.getDid()).document(DidDocument.Builder.newInstance().build()).build());

            context.getIdentityApiEndpoint().baseRequest()
                    .header(new Header("x-api-key", apikey))
                    .contentType(ContentType.JSON)
                    .body(manifest)
                    .post("/v1alpha/participants/")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(409);

            verify(subscriber, never()).on(argThat(env -> ((ParticipantContextCreated) env.getPayload()).getParticipantId().equals(manifest.getParticipantId())));
        }


        @Test
        void createNewUser_andNotActive_shouldNotPublishDid(IdentityHubEndToEndTestContext context, DidResourceStore didResourceStore, DidDocumentPublisherRegistry publisherRegistry) {
            var apikey = context.createSuperUser();

            var mockedPublisher = mock(DidDocumentPublisher.class);
            when(mockedPublisher.publish(anyString())).thenReturn(Result.success());
            when(mockedPublisher.unpublish(anyString())).thenReturn(Result.success());
            publisherRegistry.addPublisher(DidConstants.DID_WEB_METHOD, mockedPublisher);
            var manifest = context.createNewParticipant()
                    .active(false)
                    .build();

            context.getIdentityApiEndpoint().baseRequest()
                    .header(new Header("x-api-key", apikey))
                    .contentType(ContentType.JSON)
                    .body(manifest)
                    .post("/v1alpha/participants/")
                    .then()
                    .log().ifError()
                    .statusCode(anyOf(equalTo(200), equalTo(204)))
                    .body(notNullValue());

            assertThat(context.getKeyPairsForParticipant(manifest.getParticipantId())).hasSize(1).allMatch(KeyPairResource::isDefaultPair);
            assertThat(context.getDidForParticipant(manifest.getParticipantId())).hasSize(1)
                    .allSatisfy(dd -> assertThat(dd.getVerificationMethod()).hasSize(1));
            var storedDidResource = didResourceStore.findById(manifest.getDid());
            assertThat(storedDidResource.getState()).withFailMessage("Expected DID resource state %s, got %s", DidState.GENERATED, storedDidResource.getStateAsEnum()).isEqualTo(DidState.GENERATED.code());
            verify(mockedPublisher, never()).publish(manifest.getDid());
        }

        @Test
        void createNewUser_andActive_shouldAutoPublish(IdentityHubEndToEndTestContext context, DidResourceStore didResourceStore) {
            var apikey = context.createSuperUser();

            var manifest = context.createNewParticipant()
                    .active(true)
                    .build();

            context.getIdentityApiEndpoint().baseRequest()
                    .header(new Header("x-api-key", apikey))
                    .contentType(ContentType.JSON)
                    .body(manifest)
                    .post("/v1alpha/participants/")
                    .then()
                    .log().ifError()
                    .statusCode(anyOf(equalTo(200), equalTo(204)))
                    .body(notNullValue());

            assertThat(context.getKeyPairsForParticipant(manifest.getParticipantId())).hasSize(1).allMatch(KeyPairResource::isDefaultPair);
            assertThat(context.getDidForParticipant(manifest.getParticipantId())).hasSize(1)
                    .allSatisfy(dd -> assertThat(dd.getVerificationMethod()).hasSize(1));
            var storedDidResource = didResourceStore.findById(manifest.getDid());
            assertThat(storedDidResource.getState()).withFailMessage("Expected DID resource state %s, got %s", DidState.PUBLISHED, storedDidResource.getStateAsEnum()).isEqualTo(DidState.PUBLISHED.code());
        }

        @Test
        void activateParticipant_principalIsSuperser(IdentityHubEndToEndTestContext context, ParticipantContextService participantContextService, EventRouter router) {
            var superUserKey = context.createSuperUser();
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(ParticipantContextUpdated.class, subscriber);

            var participantId = "another-user";
            var did = "did:web:" + participantId;

            context.createParticipant(participantId, List.of(), false);
            assertThat(context.getDidResourceForParticipant(did).getState()).isEqualTo(DidState.GENERATED.code());

            context.getIdentityApiEndpoint().baseRequest()
                    .header(new Header("x-api-key", superUserKey))
                    .contentType(ContentType.JSON)
                    .post("/v1alpha/participants/%s/state?isActive=true".formatted(toBase64(participantId)))
                    .then()
                    .log().ifError()
                    .statusCode(204);

            var updatedParticipant = participantContextService.getParticipantContext(participantId).orElseThrow(f -> new EdcException(f.getFailureDetail()));
            assertThat(updatedParticipant.getState()).isEqualTo(ParticipantContextState.ACTIVATED.ordinal());
            assertThat(context.getDidResourceForParticipant(did).getState()).isEqualTo(DidState.PUBLISHED.code());

            // verify the correct event was emitted
            verify(subscriber).on(argThat(env -> {
                var evt = (ParticipantContextUpdated) env.getPayload();
                return evt.getParticipantId().equals(participantId) && evt.getNewState() == ParticipantContextState.ACTIVATED;
            }));
        }

        @Test
        void deactivateParticipant_shouldUnpublishDid(IdentityHubEndToEndTestContext context, ParticipantContextService participantContextService, EventRouter router) {
            var superUserKey = context.createSuperUser();
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(ParticipantContextUpdated.class, subscriber);

            var participantId = "test-user";
            var did = "did:web:" + participantId;

            context.createParticipant(participantId);
            assertThat(context.getDidResourceForParticipant(did).getState()).isEqualTo(DidState.PUBLISHED.code());

            context.getIdentityApiEndpoint().baseRequest()
                    .header(new Header("x-api-key", superUserKey))
                    .contentType(ContentType.JSON)
                    .post("/v1alpha/participants/%s/state?isActive=false".formatted(toBase64(participantId)))
                    .then()
                    .log().ifError()
                    .statusCode(204);

            var updatedParticipant = participantContextService.getParticipantContext(participantId).orElseThrow(f -> new EdcException(f.getFailureDetail()));
            assertThat(updatedParticipant.getState()).isEqualTo(ParticipantContextState.DEACTIVATED.ordinal());
            assertThat(context.getDidResourceForParticipant(did).getState()).isEqualTo(DidState.UNPUBLISHED.code());

            // verify the correct event was emitted
            verify(subscriber).on(argThat(env -> {
                var evt = (ParticipantContextUpdated) env.getPayload();
                return evt.getParticipantId().equals(participantId) && evt.getNewState() == ParticipantContextState.DEACTIVATED;
            }));
        }

        @Test
        void deleteParticipant(IdentityHubEndToEndTestContext context, Vault vault) {
            var superUserKey = context.createSuperUser();
            var participantId = "another-user";
            context.createParticipant(participantId);
            assertThat(context.getDidForParticipant(participantId)).hasSize(1);

            var pc = context.getParticipant(participantId);
            var alias = context.getKeyPairsForParticipant(participantId).stream().findFirst().map(KeyPairResource::getPrivateKeyAlias).orElseThrow();
            var apiTokenAlias = pc.getApiTokenAlias();


            context.getIdentityApiEndpoint().baseRequest()
                    .header(new Header("x-api-key", superUserKey))
                    .contentType(ContentType.JSON)
                    .delete("/v1alpha/participants/%s".formatted(toBase64(participantId)))
                    .then()
                    .log().ifError()
                    .statusCode(204);

            assertThat(context.getDidForParticipant(participantId)).isEmpty();
            assertThat(context.getKeyPairsForParticipant(participantId)).isEmpty();
            assertThat(vault.resolveSecret(alias)).isNull();
            assertThat(vault.resolveSecret(apiTokenAlias)).isNull();
        }

        @Test
        void regenerateToken(IdentityHubEndToEndTestContext context) {
            var superUserKey = context.createSuperUser();
            var participantId = "another-user";
            var userToken = context.createParticipant(participantId);

            assertThat(Arrays.asList(userToken, superUserKey))
                    .allSatisfy(t -> context.getIdentityApiEndpoint().baseRequest()
                            .header(new Header("x-api-key", t))
                            .contentType(ContentType.JSON)
                            .post("/v1alpha/participants/%s/token".formatted(toBase64(participantId)))
                            .then()
                            .log().ifError()
                            .statusCode(200)
                            .body(notNullValue()));
        }

        @Test
        void updateRoles(IdentityHubEndToEndTestContext context) {
            var superUserKey = context.createSuperUser();
            var participantId = "some-user";
            context.createParticipant(participantId);

            context.getIdentityApiEndpoint().baseRequest()
                    .header(new Header("x-api-key", superUserKey))
                    .contentType(ContentType.JSON)
                    .body(List.of("role1", "role2", "admin"))
                    .put("/v1alpha/participants/%s/roles".formatted(toBase64(participantId)))
                    .then()
                    .log().ifError()
                    .statusCode(204);

            assertThat(context.getParticipant(participantId).getRoles()).containsExactlyInAnyOrder("role1", "role2", "admin");
        }

        @ParameterizedTest(name = "Expect 403, role = {0}")
        @ValueSource(strings = { "some-role", "admin" })
        void updateRoles_whenNotSuperuser(String role, IdentityHubEndToEndTestContext context) {
            var participantId = "some-user";
            var userToken = context.createParticipant(participantId);

            context.getIdentityApiEndpoint().baseRequest()
                    .header(new Header("x-api-key", userToken))
                    .contentType(ContentType.JSON)
                    .body(List.of(role))
                    .put("/v1alpha/participants/%s/roles".formatted(toBase64(participantId)))
                    .then()
                    .log().ifError()
                    .statusCode(403);
        }

        @Test
        void getAll(IdentityHubEndToEndTestContext context) {
            var superUserKey = context.createSuperUser();
            range(0, 10)
                    .forEach(i -> {
                        var participantId = "user" + i;
                        context.createParticipant(participantId);
                    });
            var found = context.getIdentityApiEndpoint().baseRequest()
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
                    .get("/v1alpha/participants?offset=2&limit=4")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .extract().body().as(ParticipantContext[].class);
            assertThat(found).hasSize(4);
        }

        @Test
        void getAll_withDefaultPaging(IdentityHubEndToEndTestContext context) {
            var superUserKey = context.createSuperUser();
            IntStream.range(0, 70)
                    .forEach(i -> {
                        var participantId = "user" + i;
                        context.createParticipant(participantId); // implicitly creates a keypair
                    });
            var found = context.getIdentityApiEndpoint().baseRequest()
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
                    .get("/v1alpha/participants")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403);
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
