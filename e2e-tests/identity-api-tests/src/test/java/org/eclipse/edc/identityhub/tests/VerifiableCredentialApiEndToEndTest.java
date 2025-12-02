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
import org.eclipse.edc.iam.decentralizedclaims.sts.spi.store.StsAccountStore;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.identityhub.spi.credential.request.model.HolderCredentialRequest;
import org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState;
import org.eclipse.edc.identityhub.spi.credential.request.store.HolderCredentialRequestStore;
import org.eclipse.edc.identityhub.spi.did.store.DidResourceStore;
import org.eclipse.edc.identityhub.spi.keypair.store.KeyPairResourceStore;
import org.eclipse.edc.identityhub.spi.participantcontext.IdentityHubParticipantContextService;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialManifest;
import org.eclipse.edc.identityhub.tests.fixtures.DefaultRuntimes;
import org.eclipse.edc.identityhub.tests.fixtures.credentialservice.IdentityHub;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.policy.model.Policy;
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
import static org.eclipse.edc.identityhub.tests.fixtures.TestData.IH_RUNTIME_NAME;
import static org.eclipse.edc.identityhub.tests.fixtures.TestFunctions.createCredential;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.hamcrest.Matchers.endsWith;
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
        void tearDown(IdentityHubParticipantContextService pcService, DidResourceStore didResourceStore, KeyPairResourceStore keyPairResourceStore, StsAccountStore stsAccountStore) {
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
        void findById(IdentityHub identityHub) {
            var superUserKey = identityHub.createSuperUser().apiKey();
            var user = "user1";
            var token = identityHub.createParticipant(user).apiKey();

            var credential = createCredential();
            var resourceId = identityHub.storeCredential(credential, user);

            assertThat(Arrays.asList(token, superUserKey))
                    .allSatisfy(t -> identityHub.getIdentityEndpoint().baseRequest()
                            .contentType(JSON)
                            .header(new Header("x-api-key", t))
                            .get("/v1alpha/participants/%s/credentials/%s".formatted(toBase64(user), resourceId))
                            .then()
                            .log().ifValidationFails()
                            .statusCode(200)
                            .body(notNullValue()));
        }

        @Test
        void findById_doesNotBelongToParticipant(IdentityHub identityHub) {
            var superUserKey = identityHub.createSuperUser().apiKey();
            var user1 = "user1";
            identityHub.createParticipant(user1);

            var user2 = "user2";
            identityHub.createParticipant(user2);

            var credentialUser1 = createCredential();
            var credentialUser2 = createCredential();
            var resourceIdUser1 = identityHub.storeCredential(credentialUser1, user1);
            identityHub.storeCredential(credentialUser2, user1);

            // attempt to get credential1 for user2 -> should fail

            identityHub.getIdentityEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", superUserKey))
                    .get("/v1alpha/participants/%s/credentials/%s".formatted(toBase64(user2), resourceIdUser1))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(404)
                    .body(notNullValue());
        }

        @Test
        void create(IdentityHub identityHub) {
            var superUserKey = identityHub.createSuperUser().apiKey();
            var user = "user1";
            var token = identityHub.createParticipant(user).apiKey();

            assertThat(Arrays.asList(token, superUserKey))
                    .allSatisfy(t -> {
                        var vc = createCredential();
                        var resourceId = UUID.randomUUID().toString();
                        var manifest = createManifest(user, vc).id(resourceId).build();
                        identityHub.getIdentityEndpoint().baseRequest()
                                .contentType(JSON)
                                .header(new Header("x-api-key", t))
                                .body(manifest)
                                .post("/v1alpha/participants/%s/credentials".formatted(toBase64(user)))
                                .then()
                                .log().ifValidationFails()
                                .statusCode(204)
                                .body(notNullValue());

                        var resource = identityHub.getCredential(resourceId).orElseThrow(() -> new EdcException("Failed to credential with id %s".formatted(resourceId)));
                        assertThat(resource.getVerifiableCredential().credential()).usingRecursiveComparison().isEqualTo(vc);
                    });
        }

        @Test
        void update(IdentityHub identityHub) {
            var superUserKey = identityHub.createSuperUser().apiKey();
            var user = "user1";
            var token = identityHub.createParticipant(user).apiKey();

            assertThat(Arrays.asList(token, superUserKey))
                    .allSatisfy(t -> {
                        var credential1 = createCredential();
                        var credential2 = createCredential();
                        var resourceId1 = identityHub.storeCredential(credential1, user);
                        var manifest = createManifest(user, credential2).id(resourceId1).build();
                        identityHub.getIdentityEndpoint().baseRequest()
                                .contentType(JSON)
                                .header(new Header("x-api-key", t))
                                .body(manifest)
                                .put("/v1alpha/participants/%s/credentials".formatted(toBase64(user)))
                                .then()
                                .log().ifValidationFails()
                                .statusCode(204)
                                .body(notNullValue());

                        var resource = identityHub.getCredential(resourceId1).orElseThrow(() -> new EdcException("Failed to retrieve credential with id %s".formatted(resourceId1)));
                        assertThat(resource.getVerifiableCredential().credential()).usingRecursiveComparison().isEqualTo(credential2);
                    });
        }

        @Test
        void update_credentialDoesNotBelongToParticipant(IdentityHub identityHub) {
            var superUserKey = identityHub.createSuperUser().apiKey();
            var owner = "owner";
            identityHub.createParticipant(owner);

            var otherUser = "other-user";
            identityHub.createParticipant(otherUser);

            // store credential for "owner"
            var credential = createCredential();
            var resourceId = identityHub.storeCredential(credential, owner);

            var updateManifest = createManifest(owner, credential)
                    .id(resourceId)
                    .reissuancePolicy(Policy.Builder.newInstance().build()) // update re-issuance policy
                    .build();

            // attempt to update the credential for "modifier", which actually belongs to "owner" -> should fail
            identityHub.getIdentityEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", superUserKey))
                    .body(updateManifest)
                    .put("/v1alpha/participants/%s/credentials".formatted(toBase64(otherUser)))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(404)
                    .body(notNullValue());

            var resource = identityHub.getCredential(resourceId).orElseThrow(() -> new EdcException("Failed to retrieve credential with id %s".formatted(resourceId)));
        }

        @Test
        void delete(IdentityHub identityHub) {
            var superUserKey = identityHub.createSuperUser().apiKey();
            var user = "user1";
            var token = identityHub.createParticipant(user).apiKey();

            assertThat(Arrays.asList(token, superUserKey))
                    .allSatisfy(t -> {
                        var credential = createCredential();
                        var resourceId = identityHub.storeCredential(credential, user);
                        identityHub.getIdentityEndpoint().baseRequest()
                                .contentType(JSON)
                                .header(new Header("x-api-key", t))
                                .delete("/v1alpha/participants/%s/credentials/%s".formatted(toBase64(user), resourceId))
                                .then()
                                .log().ifValidationFails()
                                .statusCode(204)
                                .body(notNullValue());

                        var resource = identityHub.getCredential(resourceId);
                        assertThat(resource.isEmpty()).isTrue();
                    });
        }

        @Test
        void queryByType(IdentityHub identityHub) {
            var superUserKey = identityHub.createSuperUser().apiKey();
            var user = "user1";
            var token = identityHub.createParticipant(user).apiKey();

            var credential = createCredential();
            identityHub.storeCredential(credential, user);

            assertThat(Arrays.asList(token, superUserKey))
                    .allSatisfy(t -> identityHub.getIdentityEndpoint().baseRequest()
                            .contentType(JSON)
                            .header(new Header("x-api-key", t))
                            .get("/v1alpha/participants/%s/credentials?type=%s".formatted(toBase64(user), credential.getType().get(0)))
                            .then()
                            .log().ifValidationFails()
                            .statusCode(200)
                            .body(notNullValue()));
        }

        @Test
        void queryByType_noTypeSpecified(IdentityHub identityHub) {
            var superUserKey = identityHub.createSuperUser().apiKey();
            var user = "user1";
            var token = identityHub.createParticipant(user).apiKey();

            var credential = createCredential();
            identityHub.storeCredential(credential, user);

            assertThat(Arrays.asList(token, superUserKey))
                    .allSatisfy(t -> identityHub.getIdentityEndpoint().baseRequest()
                            .contentType(JSON)
                            .header(new Header("x-api-key", t))
                            .get("/v1alpha/participants/%s/credentials".formatted(toBase64(user)))
                            .then()
                            .log().ifValidationFails()
                            .statusCode(200)
                            .body(notNullValue()));
        }

        @Test
        void createCredentialRequest(IdentityHub identityHub, HolderCredentialRequestStore store) {
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
                identityHub.createSuperUser();
                var user = "user1";
                var token = identityHub.createParticipant(user).apiKey();
                var holderPid = UUID.randomUUID().toString();
                var request =
                        """
                                {
                                  "issuerDid": "did:web:issuer",
                                  "holderPid": "%s",
                                  "credentials": [{ "format": "VC1_0_JWT", "id": "TestCredential-id"}]
                                }
                                """.formatted(holderPid);
                identityHub.getIdentityEndpoint().baseRequest()
                        .contentType(JSON)
                        .header(new Header("x-api-key", token))
                        .body(request)
                        .post("/v1alpha/participants/%s/credentials/request".formatted(toBase64(user)))
                        .then()
                        .log().ifValidationFails()
                        .statusCode(201)
                        .header("Location", endsWith("/v1alpha/participants/%s/credentials/request/%s".formatted(toBase64(user), holderPid)));

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
        void getRequest_success(IdentityHub identityHub, TransactionContext trx, HolderCredentialRequestStore store) {
            var userId = "user1";
            var token = identityHub.createParticipant(userId).apiKey();

            var holderPid = UUID.randomUUID().toString();
            var holderRequest = HolderCredentialRequest.Builder.newInstance()
                    .id(holderPid)
                    .participantContextId(userId)
                    .issuerDid("did:web:issuer")
                    .issuerPid("dummy-issuance-id")
                    .requestedCredential("test-credential-id", "TestCredential", CredentialFormat.VC2_0_JOSE.toString())
                    .build();

            trx.execute(() -> store.save(holderRequest));

            identityHub.getIdentityEndpoint().baseRequest()
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
        void getRequest_notAuthorized_returns403(IdentityHub identityHub, HolderCredentialRequestStore store, TransactionContext trx) {
            var user1 = "user1";
            var user2 = "user2";
            var token1 = identityHub.createParticipant(user1);
            var token2 = identityHub.createParticipant(user2).apiKey();

            var holderPid = UUID.randomUUID().toString();
            var holderRequest = HolderCredentialRequest.Builder.newInstance()
                    .id(holderPid)
                    .participantContextId(user1)
                    .issuerDid("did:web:issuer")
                    .issuerPid("dummy-issuance-id")
                    .requestedCredential("test-credential-id", "TestCredential", CredentialFormat.VC2_0_JOSE.toString())
                    .build();

            trx.execute(() -> store.save(holderRequest));

            identityHub.getIdentityEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", token2)) // user 2 tries to access credential request status for user 1 -> not allowed!
                    .get("/v1alpha/participants/%s/credentials/request/%s".formatted(toBase64(user1), holderPid))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403);
        }

        @Test
        void getRequest_whenNotFound_shouldReturn404(IdentityHub identityHub, HolderCredentialRequestStore store, TransactionContext trx) {
            var userId = "user1";
            var token = identityHub.createParticipant(userId).apiKey();

            var holderPid = UUID.randomUUID().toString();

            identityHub.getIdentityEndpoint().baseRequest()
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
        static final RuntimeExtension IDENTITY_HUB_EXTENSION = ComponentRuntimeExtension.Builder.newInstance()
                .name(IH_RUNTIME_NAME)
                .modules(DefaultRuntimes.IdentityHub.MODULES)
                .endpoints(DefaultRuntimes.IdentityHub.ENDPOINTS.build())
                .configurationProvider(DefaultRuntimes.IdentityHub::config)
                .paramProvider(IdentityHub.class, IdentityHub::forContext)
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
        static final RuntimeExtension IDENTITY_HUB_EXTENSION = ComponentRuntimeExtension.Builder.newInstance()
                .name(IH_RUNTIME_NAME)
                .modules(DefaultRuntimes.IdentityHub.SQL_MODULES)
                .endpoints(DefaultRuntimes.IdentityHub.ENDPOINTS.build())
                .configurationProvider(DefaultRuntimes.IdentityHub::config)
                .configurationProvider(() -> POSTGRESQL_EXTENSION.configFor(DB_NAME))
                .paramProvider(IdentityHub.class, IdentityHub::forContext)
                .build()
                .registerServiceMock(DidResolverRegistry.class, DID_RESOLVER_REGISTRY);

    }
}
