/*
 *  Copyright (c) 2024 Amadeus IT Group.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus IT Group - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.tests;

import io.restassured.http.Header;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.iam.identitytrust.sts.spi.store.StsAccountStore;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.identityhub.spi.credential.request.model.HolderCredentialRequest;
import org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState;
import org.eclipse.edc.identityhub.spi.credential.request.store.HolderCredentialRequestStore;
import org.eclipse.edc.identityhub.spi.did.store.DidResourceStore;
import org.eclipse.edc.identityhub.spi.keypair.store.KeyPairResourceStore;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialManifest;
import org.eclipse.edc.identityhub.tests.fixtures.credentialservice.IdentityHubExtension;
import org.eclipse.edc.identityhub.tests.fixtures.credentialservice.IdentityHubRuntime;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.integration.ClientAndServer;

import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.identityhub.tests.fixtures.TestData.IH_RUNTIME_ID;
import static org.eclipse.edc.identityhub.tests.fixtures.TestData.IH_RUNTIME_MEM_MODULES;
import static org.eclipse.edc.identityhub.tests.fixtures.TestData.IH_RUNTIME_NAME;
import static org.eclipse.edc.identityhub.tests.fixtures.TestData.IH_RUNTIME_SQL_MODULES;
import static org.eclipse.edc.identityhub.tests.fixtures.TestFunctions.createCredential;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class VerifiableCredentialApiEndToEndTest {

    @SuppressWarnings("JUnitMalformedDeclaration")
    abstract static class Tests {
        protected static final DidResolverRegistry DID_RESOLVER_REGISTRY = mock();


        @AfterEach
        void tearDown(ParticipantContextService pcService, DidResourceStore didResourceStore, KeyPairResourceStore keyPairResourceStore, StsAccountStore stsAccountStore) {
            // purge all users, dids, keypairs

            pcService.query(QuerySpec.max()).getContent()
                    .forEach(pc -> pcService.deleteParticipantContext(pc.getParticipantContextId()).getContent());

            didResourceStore.query(QuerySpec.max()).forEach(dr -> didResourceStore.deleteById(dr.getDid()).getContent());

            keyPairResourceStore.query(QuerySpec.max()).getContent()
                    .forEach(kpr -> keyPairResourceStore.deleteById(kpr.getId()).getContent());

            stsAccountStore.findAll(QuerySpec.max())
                    .forEach(sts -> stsAccountStore.deleteById(sts.getId()).getContent());

        }

        @Test
        void findById(IdentityHubRuntime runtime) {
            var superUserKey = runtime.createSuperUser().apiKey();
            var user = "user1";
            var token = runtime.createParticipant(user).apiKey();

            var credential = createCredential();
            var resourceId = runtime.storeCredential(credential, user);

            assertThat(Arrays.asList(token, superUserKey))
                    .allSatisfy(t -> runtime.getIdentityEndpoint().baseRequest()
                            .contentType(JSON)
                            .header(new Header("x-api-key", t))
                            .get("/v1alpha/participants/%s/credentials/%s".formatted(toBase64(user), resourceId))
                            .then()
                            .log().ifValidationFails()
                            .statusCode(200)
                            .body(notNullValue()));
        }

        @Test
        void create(IdentityHubRuntime runtime) {
            var superUserKey = runtime.createSuperUser().apiKey();
            var user = "user1";
            var token = runtime.createParticipant(user).apiKey();

            assertThat(Arrays.asList(token, superUserKey))
                    .allSatisfy(t -> {
                        var vc = createCredential();
                        var resourceId = UUID.randomUUID().toString();
                        var manifest = createManifest(user, vc).id(resourceId).build();
                        runtime.getIdentityEndpoint().baseRequest()
                                .contentType(JSON)
                                .header(new Header("x-api-key", t))
                                .body(manifest)
                                .post("/v1alpha/participants/%s/credentials".formatted(toBase64(user)))
                                .then()
                                .log().ifValidationFails()
                                .statusCode(204)
                                .body(notNullValue());

                        var resource = runtime.getCredential(resourceId).orElseThrow(() -> new EdcException("Failed to credential with id %s".formatted(resourceId)));
                        assertThat(resource.getVerifiableCredential().credential()).usingRecursiveComparison().isEqualTo(vc);
                    });
        }

        @Test
        void update(IdentityHubRuntime identityHubRuntime) {
            var superUserKey = identityHubRuntime.createSuperUser().apiKey();
            var user = "user1";
            var token = identityHubRuntime.createParticipant(user).apiKey();

            assertThat(Arrays.asList(token, superUserKey))
                    .allSatisfy(t -> {
                        var credential1 = createCredential();
                        var credential2 = createCredential();
                        var resourceId1 = identityHubRuntime.storeCredential(credential1, user);
                        var manifest = createManifest(user, credential2).id(resourceId1).build();
                        identityHubRuntime.getIdentityEndpoint().baseRequest()
                                .contentType(JSON)
                                .header(new Header("x-api-key", t))
                                .body(manifest)
                                .put("/v1alpha/participants/%s/credentials".formatted(toBase64(user)))
                                .then()
                                .log().ifValidationFails()
                                .statusCode(204)
                                .body(notNullValue());

                        var resource = identityHubRuntime.getCredential(resourceId1).orElseThrow(() -> new EdcException("Failed to retrieve credential with id %s".formatted(resourceId1)));
                        assertThat(resource.getVerifiableCredential().credential()).usingRecursiveComparison().isEqualTo(credential2);
                    });
        }

        @Test
        void delete(IdentityHubRuntime runtime) {
            var superUserKey = runtime.createSuperUser().apiKey();
            var user = "user1";
            var token = runtime.createParticipant(user).apiKey();

            assertThat(Arrays.asList(token, superUserKey))
                    .allSatisfy(t -> {
                        var credential = createCredential();
                        var resourceId = runtime.storeCredential(credential, user);
                        runtime.getIdentityEndpoint().baseRequest()
                                .contentType(JSON)
                                .header(new Header("x-api-key", t))
                                .delete("/v1alpha/participants/%s/credentials/%s".formatted(toBase64(user), resourceId))
                                .then()
                                .log().ifValidationFails()
                                .statusCode(204)
                                .body(notNullValue());

                        var resource = runtime.getCredential(resourceId);
                        assertThat(resource.isEmpty()).isTrue();
                    });
        }

        @Test
        void queryByType(IdentityHubRuntime runtime) {
            var superUserKey = runtime.createSuperUser().apiKey();
            var user = "user1";
            var token = runtime.createParticipant(user).apiKey();

            var credential = createCredential();
            runtime.storeCredential(credential, user);

            assertThat(Arrays.asList(token, superUserKey))
                    .allSatisfy(t -> runtime.getIdentityEndpoint().baseRequest()
                            .contentType(JSON)
                            .header(new Header("x-api-key", t))
                            .get("/v1alpha/participants/%s/credentials?type=%s".formatted(toBase64(user), credential.getType().get(0)))
                            .then()
                            .log().ifValidationFails()
                            .statusCode(200)
                            .body(notNullValue()));
        }

        @Test
        void queryByType_noTypeSpecified(IdentityHubRuntime runtime) {
            var superUserKey = runtime.createSuperUser().apiKey();
            var user = "user1";
            var token = runtime.createParticipant(user).apiKey();

            var credential = createCredential();
            runtime.storeCredential(credential, user);

            assertThat(Arrays.asList(token, superUserKey))
                    .allSatisfy(t -> runtime.getIdentityEndpoint().baseRequest()
                            .contentType(JSON)
                            .header(new Header("x-api-key", t))
                            .get("/v1alpha/participants/%s/credentials".formatted(toBase64(user)))
                            .then()
                            .log().ifValidationFails()
                            .statusCode(200)
                            .body(notNullValue()));
        }

        @Test
        void createCredentialRequest(IdentityHubRuntime runtime, HolderCredentialRequestStore store) {
            var port = getFreePort();
            try (var mockedIssuer = ClientAndServer.startClientAndServer(port)) {
                var issuerPid = "dummy-issuance-id";
                // prepare DCP credential requests
                mockedIssuer.when(request()
                                .withMethod("POST")
                                .withPath("/api/issuance/credentials"))
                        .respond(response()
                                .withBody(issuerPid)
                                .withStatusCode(201));

                // prepare DCP credential status requests. The state machine is so fast, that it may tick over
                mockedIssuer.when(request()
                                .withMethod("GET")
                                .withPath("/api/issuance/request/" + issuerPid))
                        .respond(response()
                                .withBody("""
                                        {
                                          "@context": [
                                            "https://w3id.org/dspace-dcp/v1.0/dcp.jsonld"
                                          ],
                                          "type": "CredentialStatus",
                                          "holderPid": "holderPid",
                                          "status": "RECEIVED"
                                        }
                                        """)
                                .withStatusCode(200));


                when(DID_RESOLVER_REGISTRY.resolve(eq("did:web:issuer")))
                        .thenReturn(Result.success(DidDocument.Builder.newInstance()
                                .service(List.of(new Service(UUID.randomUUID().toString(),
                                        "IssuerService",
                                        "http://localhost:%s/api/issuance".formatted(port)))).build()));
                runtime.createSuperUser();
                var user = "user1";
                var token = runtime.createParticipant(user).apiKey();
                var holderPid = UUID.randomUUID().toString();
                var request =
                        """
                                {
                                  "issuerDid": "did:web:issuer",
                                  "holderPid": "%s",
                                  "credentials": [{ "format": "VC1_0_JWT", "credentialType": "TestCredential"}]
                                }
                                """.formatted(holderPid);
                runtime.getIdentityEndpoint().baseRequest()
                        .contentType(JSON)
                        .header(new Header("x-api-key", token))
                        .body(request)
                        .post("/v1alpha/participants/%s/credentials/request".formatted(toBase64(user)))
                        .then()
                        .log().ifValidationFails()
                        .statusCode(201)
                        .body(equalTo(holderPid));

                // wait until the state machine has progress to the REQUESTED state
                await().pollInterval(Duration.ofSeconds(1))
                        .atMost(Duration.ofSeconds(10))
                        .untilAsserted(() -> {
                            var result = store.findById(holderPid);
                            assertThat(result).isNotNull();
                            assertThat(result.getState()).isEqualTo(HolderRequestState.REQUESTED.code());
                            assertThat(result.getIssuerPid()).isEqualTo(issuerPid);
                            assertThat(result.getHolderPid()).isEqualTo(holderPid);
                        });
            }
        }

        @Test
        void getRequest_success(IdentityHubRuntime runtime, TransactionContext trx, HolderCredentialRequestStore store) {
            var userId = "user1";
            var token = runtime.createParticipant(userId).apiKey();

            var holderPid = UUID.randomUUID().toString();
            var holderRequest = HolderCredentialRequest.Builder.newInstance()
                    .id(holderPid)
                    .participantContextId(userId)
                    .issuerDid("did:web:issuer")
                    .issuerPid("dummy-issuance-id")
                    .credentialType("TestCredential", CredentialFormat.VC2_0_JOSE.toString())
                    .build();

            trx.execute(() -> store.save(holderRequest));

            runtime.getIdentityEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", token))
                    .get("/v1alpha/participants/%s/credentials/request/%s".formatted(toBase64(userId), holderPid))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .body("holderPid", equalTo(holderPid))
                    .body("issuerDid", equalTo("did:web:issuer"))
                    .body("issuerPid", equalTo("dummy-issuance-id"))
                    .body("status", equalTo("CREATED"));
        }

        @Test
        void getRequest_notAuthorized_returns403(IdentityHubRuntime runtime, HolderCredentialRequestStore store, TransactionContext trx) {
            var user1 = "user1";
            var user2 = "user2";
            var token1 = runtime.createParticipant(user1);
            var token2 = runtime.createParticipant(user2).apiKey();

            var holderPid = UUID.randomUUID().toString();
            var holderRequest = HolderCredentialRequest.Builder.newInstance()
                    .id(holderPid)
                    .participantContextId(user1)
                    .issuerDid("did:web:issuer")
                    .issuerPid("dummy-issuance-id")
                    .credentialType("TestCredential", CredentialFormat.VC2_0_JOSE.toString())
                    .build();

            trx.execute(() -> store.save(holderRequest));

            runtime.getIdentityEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", token2)) // user 2 tries to access credential request status for user 1 -> not allowed!
                    .get("/v1alpha/participants/%s/credentials/request/%s".formatted(toBase64(user1), holderPid))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403);
        }

        @Test
        void getRequest_whenNotFound_shouldReturn404(IdentityHubRuntime runtime, HolderCredentialRequestStore store, TransactionContext trx) {
            var userId = "user1";
            var token = runtime.createParticipant(userId).apiKey();

            var holderPid = UUID.randomUUID().toString();

            runtime.getIdentityEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", token))
                    .get("/v1alpha/participants/%s/credentials/request/%s".formatted(toBase64(userId), holderPid))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(404);
        }

        private String toBase64(String s) {
            return Base64.getUrlEncoder().encodeToString(s.getBytes());
        }

        private VerifiableCredentialManifest.Builder createManifest(String participantContextId, VerifiableCredential vc) {
            return VerifiableCredentialManifest.Builder.newInstance()
                    .verifiableCredentialContainer(new VerifiableCredentialContainer("rawVc", CredentialFormat.VC1_0_JWT, vc))
                    .participantContextId(participantContextId);
        }

    }

    @Nested
    @EndToEndTest
    class InMemory extends Tests {

        @RegisterExtension
        static final RuntimeExtension IDENTITY_HUB_EXTENSION = IdentityHubExtension.Builder.newInstance()
                .name(IH_RUNTIME_NAME)
                .id(IH_RUNTIME_ID)
                .modules(IH_RUNTIME_MEM_MODULES)
                .build()
                .registerServiceMock(DidResolverRegistry.class, DID_RESOLVER_REGISTRY);
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
        static final RuntimeExtension IDENTITY_HUB_EXTENSION = IdentityHubExtension.Builder.newInstance()
                .name(IH_RUNTIME_NAME)
                .id(IH_RUNTIME_ID)
                .modules(IH_RUNTIME_SQL_MODULES)
                .configurationProvider(() -> POSTGRESQL_EXTENSION.configFor(DB_NAME))
                .build()
                .registerServiceMock(DidResolverRegistry.class, DID_RESOLVER_REGISTRY);

    }
}
