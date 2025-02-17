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
import org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState;
import org.eclipse.edc.identityhub.spi.credential.request.store.HolderCredentialRequestStore;
import org.eclipse.edc.identityhub.spi.did.store.DidResourceStore;
import org.eclipse.edc.identityhub.spi.keypair.store.KeyPairResourceStore;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialManifest;
import org.eclipse.edc.identityhub.tests.fixtures.IdentityHubCustomizableEndToEndExtension;
import org.eclipse.edc.identityhub.tests.fixtures.IdentityHubEndToEndExtension;
import org.eclipse.edc.identityhub.tests.fixtures.IdentityHubEndToEndTestContext;
import org.eclipse.edc.identityhub.tests.fixtures.PostgresSqlService;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.integration.ClientAndServer;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.util.io.Ports.getFreePort;
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
        void findById(IdentityHubEndToEndTestContext context) {
            var superUserKey = context.createSuperUser();
            var user = "user1";
            var token = context.createParticipant(user);

            var credential = context.createCredential();
            var resourceId = context.storeCredential(credential, user);

            assertThat(Arrays.asList(token, superUserKey))
                    .allSatisfy(t -> context.getIdentityApiEndpoint().baseRequest()
                            .contentType(JSON)
                            .header(new Header("x-api-key", t))
                            .get("/v1alpha/participants/%s/credentials/%s".formatted(toBase64(user), resourceId))
                            .then()
                            .log().ifValidationFails()
                            .statusCode(200)
                            .body(notNullValue()));
        }

        @Test
        void create(IdentityHubEndToEndTestContext context) {
            var superUserKey = context.createSuperUser();
            var user = "user1";
            var token = context.createParticipant(user);

            assertThat(Arrays.asList(token, superUserKey))
                    .allSatisfy(t -> {
                        var vc = context.createCredential();
                        var resourceId = UUID.randomUUID().toString();
                        var manifest = createManifest(user, vc).id(resourceId).build();
                        context.getIdentityApiEndpoint().baseRequest()
                                .contentType(JSON)
                                .header(new Header("x-api-key", t))
                                .body(manifest)
                                .post("/v1alpha/participants/%s/credentials".formatted(toBase64(user)))
                                .then()
                                .log().ifValidationFails()
                                .statusCode(204)
                                .body(notNullValue());

                        var resource = context.getCredential(resourceId).orElseThrow(() -> new EdcException("Failed to credential with id %s".formatted(resourceId)));
                        assertThat(resource.getVerifiableCredential().credential()).usingRecursiveComparison().isEqualTo(vc);
                    });
        }

        @Test
        void update(IdentityHubEndToEndTestContext context) {
            var superUserKey = context.createSuperUser();
            var user = "user1";
            var token = context.createParticipant(user);

            assertThat(Arrays.asList(token, superUserKey))
                    .allSatisfy(t -> {
                        var credential1 = context.createCredential();
                        var credential2 = context.createCredential();
                        var resourceId1 = context.storeCredential(credential1, user);
                        var manifest = createManifest(user, credential2).id(resourceId1).build();
                        context.getIdentityApiEndpoint().baseRequest()
                                .contentType(JSON)
                                .header(new Header("x-api-key", t))
                                .body(manifest)
                                .put("/v1alpha/participants/%s/credentials".formatted(toBase64(user)))
                                .then()
                                .log().ifValidationFails()
                                .statusCode(204)
                                .body(notNullValue());

                        var resource = context.getCredential(resourceId1).orElseThrow(() -> new EdcException("Failed to retrieve credential with id %s".formatted(resourceId1)));
                        assertThat(resource.getVerifiableCredential().credential()).usingRecursiveComparison().isEqualTo(credential2);
                    });
        }

        @Test
        void delete(IdentityHubEndToEndTestContext context) {
            var superUserKey = context.createSuperUser();
            var user = "user1";
            var token = context.createParticipant(user);

            assertThat(Arrays.asList(token, superUserKey))
                    .allSatisfy(t -> {
                        var credential = context.createCredential();
                        var resourceId = context.storeCredential(credential, user);
                        context.getIdentityApiEndpoint().baseRequest()
                                .contentType(JSON)
                                .header(new Header("x-api-key", t))
                                .delete("/v1alpha/participants/%s/credentials/%s".formatted(toBase64(user), resourceId))
                                .then()
                                .log().ifValidationFails()
                                .statusCode(204)
                                .body(notNullValue());

                        var resource = context.getCredential(resourceId);
                        assertThat(resource.isEmpty()).isTrue();
                    });
        }

        @Test
        void queryByType(IdentityHubEndToEndTestContext context) {
            var superUserKey = context.createSuperUser();
            var user = "user1";
            var token = context.createParticipant(user);

            var credential = context.createCredential();
            context.storeCredential(credential, user);

            assertThat(Arrays.asList(token, superUserKey))
                    .allSatisfy(t -> context.getIdentityApiEndpoint().baseRequest()
                            .contentType(JSON)
                            .header(new Header("x-api-key", t))
                            .get("/v1alpha/participants/%s/credentials?type=%s".formatted(toBase64(user), credential.getType().get(0)))
                            .then()
                            .log().ifValidationFails()
                            .statusCode(200)
                            .body(notNullValue()));
        }

        @Test
        void queryByType_noTypeSpecified(IdentityHubEndToEndTestContext context) {
            var superUserKey = context.createSuperUser();
            var user = "user1";
            var token = context.createParticipant(user);

            var credential = context.createCredential();
            context.storeCredential(credential, user);

            assertThat(Arrays.asList(token, superUserKey))
                    .allSatisfy(t -> context.getIdentityApiEndpoint().baseRequest()
                            .contentType(JSON)
                            .header(new Header("x-api-key", t))
                            .get("/v1alpha/participants/%s/credentials".formatted(toBase64(user)))
                            .then()
                            .log().ifValidationFails()
                            .statusCode(200)
                            .body(notNullValue()));
        }

        @Test
        void createCredentialRequest(IdentityHubEndToEndTestContext context, HolderCredentialRequestStore store) {
            var port = getFreePort();
            try (var mockedIssuer = ClientAndServer.startClientAndServer(port)) {
                var issuanceProcessId = "dummy-issuance-id";
                mockedIssuer.when(request()
                                .withMethod("POST")
                                .withPath("/api/issuance/credentials"))
                        .respond(response()
                                .withBody(issuanceProcessId)
                                .withStatusCode(201));

                when(DID_RESOLVER_REGISTRY.resolve(eq("did:web:issuer")))
                        .thenReturn(Result.success(DidDocument.Builder.newInstance()
                                .service(List.of(new Service(UUID.randomUUID().toString(),
                                        "CredentialRequest",
                                        "http://localhost:%s/api/issuance/credentials".formatted(mockedIssuer.getLocalPort())))).build()));
                context.createSuperUser();
                var user = "user1";
                var token = context.createParticipant(user);
                var request =
                        """
                                {
                                  "issuerDid": "did:web:issuer",
                                  "requestId": "test-request-id",
                                  "credentials": [{ "format": "VC1_0_JWT", "credentialType": "TestCredential"}]
                                }
                                """;
                context.getIdentityApiEndpoint().baseRequest()
                        .contentType(JSON)
                        .header(new Header("x-api-key", token))
                        .body(request)
                        .post("/v1alpha/participants/%s/credentials/request".formatted(toBase64(user)))
                        .then()
                        .log().ifValidationFails()
                        .statusCode(201)
                        .body(Matchers.equalTo(issuanceProcessId));

                var requests = store.query(QuerySpec.max());
                assertThat(requests).hasSize(1)
                        .allSatisfy(t -> {
                            assertThat(t.getState()).isEqualTo(HolderRequestState.REQUESTED.code());
                            assertThat(t.getIssuanceProcessId()).isEqualTo(issuanceProcessId);
                            assertThat(t.getRequestId()).isEqualTo("test-request-id");
                        });

            }

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
        static IdentityHubCustomizableEndToEndExtension runtime;

        static {
            var ctx = IdentityHubEndToEndExtension.InMemory.context();
            ctx.getRuntime().registerServiceMock(DidResolverRegistry.class, DID_RESOLVER_REGISTRY);
            runtime = new IdentityHubCustomizableEndToEndExtension(ctx);
        }
    }

    @Nested
    @PostgresqlIntegrationTest
    class Postgres extends Tests {
        private static final String DB_NAME = "runtime";
        private static final Integer DB_PORT = getFreePort();

        @RegisterExtension
        @Order(1)
        static IdentityHubCustomizableEndToEndExtension runtime;
        static PostgresSqlService server = new PostgresSqlService(DB_NAME, DB_PORT);

        @Order(0) // must be the first extension to be evaluated since it starts the DB server
        @RegisterExtension
        static final BeforeAllCallback POSTGRES_CONTAINER_STARTER = context -> {
            server.start();
        };

        static {
            var ctx = IdentityHubEndToEndExtension.Postgres.context(DB_NAME, DB_PORT);
            ctx.getRuntime().registerServiceMock(DidResolverRegistry.class, DID_RESOLVER_REGISTRY);
            runtime = new IdentityHubCustomizableEndToEndExtension(ctx);
        }
    }
}
