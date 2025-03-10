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
import org.eclipse.edc.iam.identitytrust.sts.spi.store.StsAccountStore;
import org.eclipse.edc.identityhub.spi.did.DidConstants;
import org.eclipse.edc.identityhub.spi.did.DidDocumentPublisher;
import org.eclipse.edc.identityhub.spi.did.DidDocumentPublisherRegistry;
import org.eclipse.edc.identityhub.spi.did.events.DidDocumentPublished;
import org.eclipse.edc.identityhub.spi.did.model.DidResource;
import org.eclipse.edc.identityhub.spi.did.model.DidState;
import org.eclipse.edc.identityhub.spi.did.store.DidResourceStore;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairResource;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairState;
import org.eclipse.edc.identityhub.spi.keypair.store.KeyPairResourceStore;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.events.ParticipantContextCreated;
import org.eclipse.edc.identityhub.spi.participantcontext.events.ParticipantContextUpdated;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContextState;
import org.eclipse.edc.identityhub.tests.fixtures.credentialservice.IdentityHubExtension;
import org.eclipse.edc.identityhub.tests.fixtures.credentialservice.IdentityHubRuntime;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
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
import java.util.stream.IntStream;

import static io.restassured.http.ContentType.JSON;
import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.identityhub.spi.participantcontext.StsAccountProvisioner.CLIENT_SECRET_PROPERTY;
import static org.eclipse.edc.identityhub.tests.fixtures.TestData.IH_RUNTIME_ID;
import static org.eclipse.edc.identityhub.tests.fixtures.TestData.IH_RUNTIME_MEM_MODULES;
import static org.eclipse.edc.identityhub.tests.fixtures.TestData.IH_RUNTIME_NAME;
import static org.eclipse.edc.identityhub.tests.fixtures.TestData.IH_RUNTIME_SQL_MODULES;
import static org.eclipse.edc.identityhub.tests.fixtures.TestFunctions.createKeyDescriptor;
import static org.eclipse.edc.identityhub.tests.fixtures.TestFunctions.createNewParticipant;
import static org.eclipse.edc.identityhub.tests.fixtures.common.AbstractIdentityHubRuntime.SUPER_USER;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("JUnitMalformedDeclaration")
public class ParticipantContextApiEndToEndTest {

    abstract static class Tests {

        @AfterEach
        void tearDown(ParticipantContextService pcService, DidResourceStore didResourceStore, KeyPairResourceStore keyPairResourceStore, StsAccountStore accountStore) {
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
        void getUserById(IdentityHubRuntime identityHubRuntime) {
            var apikey = identityHubRuntime.createSuperUser().apiKey();

            var su = identityHubRuntime.getIdentityEndpoint().baseRequest()
                    .header(new Header("x-api-key", apikey))
                    .get("/v1alpha/participants/" + toBase64(SUPER_USER))
                    .then()
                    .statusCode(200)
                    .extract().body().as(ParticipantContext.class);
            assertThat(su.getParticipantContextId()).isEqualTo(SUPER_USER);
        }

        @Test
        void getUserById_notOwner_expect403(IdentityHubRuntime identityHubRuntime) {
            var user1 = "user1";
            var user1Context = ParticipantContext.Builder.newInstance()
                    .participantContextId(user1)
                    .did("did:web:" + user1)
                    .apiTokenAlias(user1 + "-alias")
                    .build();
            var apiToken1 = identityHubRuntime.storeParticipant(user1Context);

            var user2 = "user2";
            var user2Context = ParticipantContext.Builder.newInstance()
                    .participantContextId(user2)
                    .did("did:web:" + user2)
                    .apiTokenAlias(user2 + "-alias")
                    .build();
            identityHubRuntime.storeParticipant(user2Context);

            //user1 attempts to read user2 -> fail
            identityHubRuntime.getIdentityEndpoint().baseRequest()
                    .header(new Header("x-api-key", apiToken1))
                    .contentType(ContentType.JSON)
                    .get("/v1alpha/participants/" + toBase64(user2))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403);
        }

        @Test
        void createNewUser_principalIsSuperuser(IdentityHubRuntime identityHubRuntime, EventRouter router) {
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(ParticipantContextCreated.class, subscriber);
            var apikey = identityHubRuntime.createSuperUser().apiKey();

            var manifest = createNewParticipant().build();

            identityHubRuntime.getIdentityEndpoint().baseRequest()
                    .header(new Header("x-api-key", apikey))
                    .contentType(ContentType.JSON)
                    .body(manifest)
                    .post("/v1alpha/participants/")
                    .then()
                    .log().ifError()
                    .statusCode(anyOf(equalTo(200), equalTo(204)))
                    .body("clientId", notNullValue())
                    .body("apiKey", notNullValue())
                    .body("clientSecret", notNullValue());

            verify(subscriber).on(argThat(env -> ((ParticipantContextCreated) env.getPayload()).getParticipantContextId().equals(manifest.getParticipantId())));

            assertThat(identityHubRuntime.getKeyPairsForParticipant(manifest.getParticipantId())).hasSize(1);
            assertThat(identityHubRuntime.getDidForParticipant(manifest.getParticipantId())).hasSize(1)
                    .allSatisfy(dd -> assertThat(dd.getVerificationMethod()).hasSize(1));
        }

        @Test
        void createNewUser_whenKeyPairActive(IdentityHubRuntime identityHubRuntime, EventRouter router) {
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(ParticipantContextCreated.class, subscriber);
            var apikey = identityHubRuntime.createSuperUser().apiKey();

            var participantId = UUID.randomUUID().toString();
            var manifest = createNewParticipant()
                    .participantId(participantId)
                    .active(true)
                    .did("did:web:" + participantId)
                    .key(createKeyDescriptor().active(true).build())
                    .build();

            router.registerSync(DidDocumentPublished.class, subscriber);

            identityHubRuntime.getIdentityEndpoint().baseRequest()
                    .header(new Header("x-api-key", apikey))
                    .contentType(ContentType.JSON)
                    .body(manifest)
                    .post("/v1alpha/participants/")
                    .then()
                    .log().ifError()
                    .statusCode(anyOf(equalTo(200), equalTo(204)))
                    .body(notNullValue());

            verify(subscriber).on(argThat(env -> env.getPayload() instanceof ParticipantContextCreated created && created.getParticipantContextId().equals(manifest.getParticipantId())));
            verify(subscriber, times(1)).on(argThat(evt -> evt.getPayload() instanceof DidDocumentPublished));

            assertThat(identityHubRuntime.getKeyPairsForParticipant(manifest.getParticipantId())).hasSize(1)
                    .allSatisfy(kpr -> assertThat(kpr.getState()).isEqualTo(KeyPairState.ACTIVATED.code()));
            assertThat(identityHubRuntime.getDidForParticipant(manifest.getParticipantId()))
                    .hasSize(1)
                    .allSatisfy(dd -> assertThat(dd.getVerificationMethod()).hasSize(1));

        }

        @Test
        void createNewUser_withCustomSecretAlias(IdentityHubRuntime identityHubRuntime, Vault vault) {
            var apikey = identityHubRuntime.createSuperUser().apiKey();

            var participantId = UUID.randomUUID().toString();
            var manifest = createNewParticipant()
                    .participantId(participantId)
                    .active(true)
                    .did("did:web:" + participantId)
                    .key(createKeyDescriptor().active(true).build())
                    .property(CLIENT_SECRET_PROPERTY, "test-alias")
                    .build();

            identityHubRuntime.getIdentityEndpoint().baseRequest()
                    .header(new Header("x-api-key", apikey))
                    .contentType(ContentType.JSON)
                    .body(manifest)
                    .post("/v1alpha/participants/")
                    .then()
                    .log().ifError()
                    .statusCode(anyOf(equalTo(200), equalTo(204)))
                    .body(notNullValue());

            assertThat(vault.resolveSecret("test-alias")).isNotNull();
        }

        @Test
        void createNewUser_whenKeyPairNotActive(IdentityHubRuntime identityHubRuntime, EventRouter router) {
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(ParticipantContextCreated.class, subscriber);
            var apikey = identityHubRuntime.createSuperUser().apiKey();

            var participantId = UUID.randomUUID().toString();
            var manifest = createNewParticipant()
                    .active(true)
                    .participantId(participantId)
                    .did("did:web:" + participantId)
                    .key(createKeyDescriptor().active(false).build())
                    .build();

            identityHubRuntime.getIdentityEndpoint().baseRequest()
                    .header(new Header("x-api-key", apikey))
                    .contentType(ContentType.JSON)
                    .body(manifest)
                    .post("/v1alpha/participants/")
                    .then()
                    .log().ifError()
                    .statusCode(anyOf(equalTo(200), equalTo(204)))
                    .body(notNullValue());

            verify(subscriber).on(argThat(env -> ((ParticipantContextCreated) env.getPayload()).getParticipantContextId().equals(manifest.getParticipantId())));

            assertThat(identityHubRuntime.getKeyPairsForParticipant(manifest.getParticipantId())).hasSize(1)
                    .allSatisfy(kpr -> assertThat(kpr.getState()).isEqualTo(KeyPairState.CREATED.code()));

            // inactive key-pairs don't get added to the DID Document
            assertThat(identityHubRuntime.getDidForParticipant(manifest.getParticipantId()))
                    .hasSize(1)
                    .allSatisfy(dd -> assertThat(dd.getVerificationMethod()).isEmpty());

        }

        @Test
        void createNewUser_principalIsNotSuperuser_expect403(IdentityHubRuntime identityHubRuntime, EventRouter router) {
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(ParticipantContextCreated.class, subscriber);

            var principal = "another-user";
            var anotherUser = ParticipantContext.Builder.newInstance()
                    .participantContextId(principal)
                    .did("did:web:" + principal)
                    .apiTokenAlias(principal + "-alias")
                    .build();
            var apiToken = identityHubRuntime.storeParticipant(anotherUser);
            var manifest = createNewParticipant().build();

            identityHubRuntime.getIdentityEndpoint().baseRequest()
                    .header(new Header("x-api-key", apiToken))
                    .contentType(ContentType.JSON)
                    .body(manifest)
                    .post("/v1alpha/participants/")
                    .then()
                    .log().ifError()
                    .statusCode(403)
                    .body(notNullValue());
            verifyNoInteractions(subscriber);

            assertThat(identityHubRuntime.getKeyPairsForParticipant(manifest.getParticipantId())).isEmpty();
        }

        @Test
        void createNewUser_principalIsKnown_expect401(IdentityHubRuntime identityHubRuntime, EventRouter router) {
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(ParticipantContextCreated.class, subscriber);
            var principal = "another-user";

            var manifest = createNewParticipant().build();

            identityHubRuntime.getIdentityEndpoint().baseRequest()
                    .header(new Header("x-api-key", identityHubRuntime.createTokenFor(principal)))
                    .contentType(ContentType.JSON)
                    .body(manifest)
                    .post("/v1alpha/participants/")
                    .then()
                    .log().ifError()
                    .statusCode(401)
                    .body(notNullValue());
            verifyNoInteractions(subscriber);
            assertThat(identityHubRuntime.getKeyPairsForParticipant(manifest.getParticipantId())).isEmpty();
        }

        @Test
        void createNewUser_whenDidAlreadyExists_expect409(IdentityHubRuntime identityHubRuntime, DidResourceStore didResourceStore, EventRouter router) {
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(ParticipantContextCreated.class, subscriber);
            var apikey = identityHubRuntime.createSuperUser().apiKey();

            var manifest = createNewParticipant().build();

            didResourceStore.save(DidResource.Builder.newInstance().did(manifest.getDid()).document(DidDocument.Builder.newInstance().build()).build());

            identityHubRuntime.getIdentityEndpoint().baseRequest()
                    .header(new Header("x-api-key", apikey))
                    .contentType(ContentType.JSON)
                    .body(manifest)
                    .post("/v1alpha/participants/")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(409);

            verify(subscriber, never()).on(argThat(env -> ((ParticipantContextCreated) env.getPayload()).getParticipantContextId().equals(manifest.getParticipantId())));
        }

        @Test
        void createNewUser_andNotActive_shouldNotPublishDid(IdentityHubRuntime context, DidResourceStore didResourceStore, DidDocumentPublisherRegistry publisherRegistry) {
            var apikey = context.createSuperUser().apiKey();

            var mockedPublisher = mock(DidDocumentPublisher.class);
            when(mockedPublisher.publish(anyString())).thenReturn(Result.success());
            when(mockedPublisher.unpublish(anyString())).thenReturn(Result.success());
            publisherRegistry.addPublisher(DidConstants.DID_WEB_METHOD, mockedPublisher);
            var manifest = createNewParticipant()
                    .active(false)
                    .build();

            context.getIdentityEndpoint().baseRequest()
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
        void createNewUser_andActive_shouldAutoPublish(IdentityHubRuntime identityHubRuntime, DidResourceStore didResourceStore) {
            var apikey = identityHubRuntime.createSuperUser().apiKey();

            var manifest = createNewParticipant()
                    .active(true)
                    .build();

            identityHubRuntime.getIdentityEndpoint().baseRequest()
                    .header(new Header("x-api-key", apikey))
                    .contentType(ContentType.JSON)
                    .body(manifest)
                    .post("/v1alpha/participants/")
                    .then()
                    .log().ifError()
                    .statusCode(anyOf(equalTo(200), equalTo(204)))
                    .body(notNullValue());

            assertThat(identityHubRuntime.getKeyPairsForParticipant(manifest.getParticipantId())).hasSize(1).allMatch(KeyPairResource::isDefaultPair);
            assertThat(identityHubRuntime.getDidForParticipant(manifest.getParticipantId())).hasSize(1)
                    .allSatisfy(dd -> assertThat(dd.getVerificationMethod()).hasSize(1));
            var storedDidResource = didResourceStore.findById(manifest.getDid());
            assertThat(storedDidResource.getState()).withFailMessage("Expected DID resource state %s, got %s", DidState.PUBLISHED, storedDidResource.getStateAsEnum()).isEqualTo(DidState.PUBLISHED.code());
        }

        @Test
        void activateParticipant_principalIsSuperser(IdentityHubRuntime identityHubRuntime, ParticipantContextService participantContextService, EventRouter router) {
            var superUserKey = identityHubRuntime.createSuperUser().apiKey();
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(ParticipantContextUpdated.class, subscriber);

            var participantId = "another-user";
            var did = "did:web:" + participantId;

            identityHubRuntime.createParticipant(participantId, List.of(), false);
            assertThat(identityHubRuntime.getDidResourceForParticipant(did).getState()).isEqualTo(DidState.GENERATED.code());

            identityHubRuntime.getIdentityEndpoint().baseRequest()
                    .header(new Header("x-api-key", superUserKey))
                    .contentType(ContentType.JSON)
                    .post("/v1alpha/participants/%s/state?isActive=true".formatted(toBase64(participantId)))
                    .then()
                    .log().ifError()
                    .statusCode(204);

            var updatedParticipant = participantContextService.getParticipantContext(participantId).orElseThrow(f -> new EdcException(f.getFailureDetail()));
            assertThat(updatedParticipant.getState()).isEqualTo(ParticipantContextState.ACTIVATED.ordinal());
            assertThat(identityHubRuntime.getDidResourceForParticipant(did).getState()).isEqualTo(DidState.PUBLISHED.code());

            // verify the correct event was emitted
            verify(subscriber).on(argThat(env -> {
                var evt = (ParticipantContextUpdated) env.getPayload();
                return evt.getParticipantContextId().equals(participantId) && evt.getNewState() == ParticipantContextState.ACTIVATED;
            }));
        }

        @Test
        void deactivateParticipant_shouldUnpublishDid(IdentityHubRuntime identityHubRuntime, ParticipantContextService participantContextService, EventRouter router) {
            var superUserKey = identityHubRuntime.createSuperUser().apiKey();
            var subscriber = mock(EventSubscriber.class);
            router.registerSync(ParticipantContextUpdated.class, subscriber);

            var participantContextId = "test-user";
            var did = "did:web:" + participantContextId;

            identityHubRuntime.createParticipant(participantContextId);
            assertThat(identityHubRuntime.getDidResourceForParticipant(did).getState()).isEqualTo(DidState.PUBLISHED.code());

            identityHubRuntime.getIdentityEndpoint().baseRequest()
                    .header(new Header("x-api-key", superUserKey))
                    .contentType(ContentType.JSON)
                    .post("/v1alpha/participants/%s/state?isActive=false".formatted(toBase64(participantContextId)))
                    .then()
                    .log().ifError()
                    .statusCode(204);

            var updatedParticipant = participantContextService.getParticipantContext(participantContextId).orElseThrow(f -> new EdcException(f.getFailureDetail()));
            assertThat(updatedParticipant.getState()).isEqualTo(ParticipantContextState.DEACTIVATED.ordinal());
            assertThat(identityHubRuntime.getDidResourceForParticipant(did).getState()).isEqualTo(DidState.UNPUBLISHED.code());

            // verify the correct event was emitted
            verify(subscriber).on(argThat(env -> {
                var evt = (ParticipantContextUpdated) env.getPayload();
                return evt.getParticipantContextId().equals(participantContextId) && evt.getNewState() == ParticipantContextState.DEACTIVATED;
            }));
        }

        @Test
        void deleteParticipant(IdentityHubRuntime identityHubRuntime, Vault vault) {
            var superUserKey = identityHubRuntime.createSuperUser().apiKey();
            var participantContextId = "another-user";
            identityHubRuntime.createParticipant(participantContextId);
            assertThat(identityHubRuntime.getDidForParticipant(participantContextId)).hasSize(1);

            var pc = identityHubRuntime.getParticipant(participantContextId);
            var alias = identityHubRuntime.getKeyPairsForParticipant(participantContextId).stream().findFirst().map(KeyPairResource::getPrivateKeyAlias).orElseThrow();
            var apiTokenAlias = pc.getApiTokenAlias();


            identityHubRuntime.getIdentityEndpoint().baseRequest()
                    .header(new Header("x-api-key", superUserKey))
                    .contentType(ContentType.JSON)
                    .delete("/v1alpha/participants/%s".formatted(toBase64(participantContextId)))
                    .then()
                    .log().ifError()
                    .statusCode(204);

            assertThat(identityHubRuntime.getDidForParticipant(participantContextId)).isEmpty();
            assertThat(identityHubRuntime.getKeyPairsForParticipant(participantContextId)).isEmpty();
            assertThat(vault.resolveSecret(alias)).isNull();
            assertThat(vault.resolveSecret(apiTokenAlias)).isNull();
        }

        @Test
        void regenerateToken(IdentityHubRuntime identityHubRuntime) {
            var superUserKey = identityHubRuntime.createSuperUser().apiKey();
            var participantContextId = "another-user";
            var userToken = identityHubRuntime.createParticipant(participantContextId).apiKey();

            assertThat(Arrays.asList(userToken, superUserKey))
                    .allSatisfy(t -> identityHubRuntime.getIdentityEndpoint().baseRequest()
                            .header(new Header("x-api-key", t))
                            .contentType(ContentType.JSON)
                            .post("/v1alpha/participants/%s/token".formatted(toBase64(participantContextId)))
                            .then()
                            .log().ifError()
                            .statusCode(200)
                            .body(notNullValue()));
        }

        @Test
        void updateRoles(IdentityHubRuntime identityHubRuntime) {
            var superUserKey = identityHubRuntime.createSuperUser().apiKey();
            var participantContextId = "some-user";
            identityHubRuntime.createParticipant(participantContextId);

            identityHubRuntime.getIdentityEndpoint().baseRequest()
                    .header(new Header("x-api-key", superUserKey))
                    .contentType(ContentType.JSON)
                    .body(List.of("role1", "role2", "admin"))
                    .put("/v1alpha/participants/%s/roles".formatted(toBase64(participantContextId)))
                    .then()
                    .log().ifError()
                    .statusCode(204);

            assertThat(identityHubRuntime.getParticipant(participantContextId).getRoles()).containsExactlyInAnyOrder("role1", "role2", "admin");
        }

        @ParameterizedTest(name = "Expect 403, role = {0}")
        @ValueSource(strings = {"some-role", "admin"})
        void updateRoles_whenNotSuperuser(String role, IdentityHubRuntime identityHubRuntime) {
            var participantContextId = "some-user";
            var userToken = identityHubRuntime.createParticipant(participantContextId).apiKey();

            identityHubRuntime.getIdentityEndpoint().baseRequest()
                    .header(new Header("x-api-key", userToken))
                    .contentType(ContentType.JSON)
                    .body(List.of(role))
                    .put("/v1alpha/participants/%s/roles".formatted(toBase64(participantContextId)))
                    .then()
                    .log().ifError()
                    .statusCode(403);
        }

        @Test
        void getAll(IdentityHubRuntime identityHubRuntime) {
            var superUserKey = identityHubRuntime.createSuperUser().apiKey();
            range(0, 10)
                    .forEach(i -> {
                        var participantContextId = "user" + i;
                        identityHubRuntime.createParticipant(participantContextId);
                    });
            var found = identityHubRuntime.getIdentityEndpoint().baseRequest()
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
        void getAll_withPaging(IdentityHubRuntime identityHubRuntime) {
            var superUserKey = identityHubRuntime.createSuperUser().apiKey();
            range(0, 10)
                    .forEach(i -> {
                        var participantContextId = "user" + i;
                        identityHubRuntime.createParticipant(participantContextId); // implicitly creates a keypair
                    });
            var found = identityHubRuntime.getIdentityEndpoint().baseRequest()
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
        void getAll_withDefaultPaging(IdentityHubRuntime identityHubRuntime) {
            var superUserKey = identityHubRuntime.createSuperUser().apiKey();
            IntStream.range(0, 70)
                    .forEach(i -> {
                        var participantContextId = "user" + i;
                        identityHubRuntime.createParticipant(participantContextId); // implicitly creates a keypair
                    });
            var found = identityHubRuntime.getIdentityEndpoint().baseRequest()
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
        void getAll_notAuthorized(IdentityHubRuntime identityHubRuntime) {
            var attackerToken = identityHubRuntime.createParticipant("attacker").apiKey();

            range(0, 10)
                    .forEach(i -> {
                        var participantContextId = "user" + i;
                        identityHubRuntime.createParticipant(participantContextId); // implicitly creates a keypair
                    });
            identityHubRuntime.getIdentityEndpoint().baseRequest()
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
    class InMemory extends Tests {

        @RegisterExtension
        static final IdentityHubExtension IDENTITY_HUB_EXTENSION = IdentityHubExtension.Builder.newInstance()
                .name(IH_RUNTIME_NAME)
                .id(IH_RUNTIME_ID)
                .modules(IH_RUNTIME_MEM_MODULES)
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
        static final IdentityHubExtension IDENTITY_HUB_EXTENSION = IdentityHubExtension.Builder.newInstance()
                .name(IH_RUNTIME_NAME)
                .id(IH_RUNTIME_ID)
                .modules(IH_RUNTIME_SQL_MODULES)
                .configurationProvider(() -> POSTGRESQL_EXTENSION.configFor(DB_NAME))
                .build();
    }
}
