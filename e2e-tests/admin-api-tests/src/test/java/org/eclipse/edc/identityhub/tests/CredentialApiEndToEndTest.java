/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.tests;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import io.restassured.http.Header;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialStatus;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.identityhub.tests.fixtures.issuerservice.IssuerExtension;
import org.eclipse.edc.identityhub.tests.fixtures.issuerservice.IssuerRuntime;
import org.eclipse.edc.issuerservice.spi.holder.model.Holder;
import org.eclipse.edc.issuerservice.spi.holder.store.HolderStore;
import org.eclipse.edc.issuerservice.spi.issuance.credentialdefinition.store.CredentialDefinitionStore;
import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialDefinition;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.integration.ClientAndServer;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.identityhub.tests.TestData.EXAMPLE_REVOCATION_CREDENTIAL;
import static org.eclipse.edc.identityhub.tests.TestData.EXAMPLE_REVOCATION_CREDENTIAL_JWT;
import static org.eclipse.edc.identityhub.tests.TestData.EXAMPLE_REVOCATION_CREDENTIAL_JWT_WITH_STATUS_BIT_SET;
import static org.eclipse.edc.identityhub.tests.TestData.EXAMPLE_REVOCATION_CREDENTIAL_WITH_STATUS_BIT_SET;
import static org.eclipse.edc.identityhub.tests.TestData.ISSUER_RUNTIME_ID;
import static org.eclipse.edc.identityhub.tests.TestData.ISSUER_RUNTIME_MEM_MODULES;
import static org.eclipse.edc.identityhub.tests.TestData.ISSUER_RUNTIME_NAME;
import static org.eclipse.edc.identityhub.tests.TestData.ISSUER_RUNTIME_SQL_MODULES;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.StringBody.subString;
import static org.mockserver.verify.VerificationTimes.exactly;

@SuppressWarnings("JUnitMalformedDeclaration")
public class CredentialApiEndToEndTest {
    public static final String SIGNING_KEY_ALIAS = "signing-key";
    public static final int STATUS_LIST_INDEX = 94567;
    public static final String USER = "user";
    protected static final DidResolverRegistry DID_RESOLVER_REGISTRY = mock();
    private static final String STATUS_LIST_CREDENTIAL_ID = "https://example.com/credentials/status/3";
    private final ObjectMapper objectMapper = new JacksonTypeManager().getMapper()
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private @NotNull VerifiableCredentialResource createCredential(String credentialId) {
        return createCredential(credentialId, USER);
    }

    private @NotNull VerifiableCredentialResource createCredential(String credentialId, String participantContextId) {
        var cred = VerifiableCredential.Builder.newInstance()
                .issuanceDate(Instant.now())
                .id(credentialId)
                .type("VerifiableCredential")
                .credentialSubject(CredentialSubject.Builder.newInstance().id(UUID.randomUUID().toString()).claim("foo", "bar").build())
                .credentialStatus(new CredentialStatus(credentialId + "#status", "BitstringStatusListEntry", Map.of(
                        "statusListIndex", STATUS_LIST_INDEX,
                        "statusPurpose", "revocation",
                        "statusListCredential", STATUS_LIST_CREDENTIAL_ID
                )))
                .issuer(new Issuer(UUID.randomUUID().toString()))
                .build();
        return VerifiableCredentialResource.Builder.newInstance()
                .state(VcStatus.ISSUED)
                .issuerId("issuer-id")
                .holderId("holder-id")
                .id(credentialId)
                .credential(new VerifiableCredentialContainer("JWT_STRING", CredentialFormat.VC1_0_JWT, cred))
                .participantContextId(participantContextId)
                .build();
    }

    private VerifiableCredentialResource createRevocationCredential(String credentialJson, String credentialJwt) {
        try {
            var credential = objectMapper.readValue(credentialJson, VerifiableCredential.class);
            return VerifiableCredentialResource.Builder.newInstance()
                    .state(VcStatus.ISSUED)
                    .issuerId("issuer-id")
                    .holderId("holder-id")
                    .id(STATUS_LIST_CREDENTIAL_ID)
                    .credential(new VerifiableCredentialContainer(credentialJwt, CredentialFormat.VC1_0_JWT, credential))
                    .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    abstract class Tests {


        private static @NotNull String getOfferRequestBody() {
            return """
                    
                        {
                      "holderId": "test-holder-id",
                      "credentials": [
                        "test-credential-id"
                      ]
                    }
                    """;
        }

        @BeforeEach
        void prepare(Vault vault) throws JOSEException {
            // put signing key in vault
            vault.storeSecret(SIGNING_KEY_ALIAS, new ECKeyGenerator(Curve.P_256).generate().toJSONString());

        }

        @AfterEach
        void teardown(CredentialStore credentialStore, ParticipantContextService pcService, HolderStore holderStore) {
            credentialStore.query(QuerySpec.max()).getContent()
                    .forEach(vcr -> credentialStore.deleteById(vcr.getId()));

            pcService.query(QuerySpec.max()).getContent()
                    .forEach(pc -> pcService.deleteParticipantContext(pc.getParticipantContextId()).getContent());

            holderStore.query(QuerySpec.max()).getContent()
                    .forEach(holder -> holderStore.deleteById(holder.getHolderId()).getContent());
        }

        @Test
        void revoke_whenNotYetRevoked(IssuerRuntime runtime, CredentialStore credentialStore) {
            var token = runtime.createParticipant(USER).apiKey();

            // create revocation credential
            var res = createRevocationCredential(EXAMPLE_REVOCATION_CREDENTIAL, EXAMPLE_REVOCATION_CREDENTIAL_JWT);

            // track the original bitstring
            var originalBitstring = res.getVerifiableCredential().credential().getCredentialSubject().get(0).getClaim("", "encodedList");
            credentialStore.create(res).orElseThrow(f -> new RuntimeException("Failed to create credential: " + f.getFailureDetail()));

            credentialStore.create(createCredential("test-cred"));

            runtime.getAdminEndpoint()
                    .baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", token))
                    .post("/v1alpha/participants/%s/credentials/test-cred/revoke".formatted(toBase64(USER)))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(204);

            // verify that the status list credential's bitstring has changed
            var updatedBitstring = credentialStore.findById(STATUS_LIST_CREDENTIAL_ID).getContent()
                    .getVerifiableCredential().credential().getCredentialSubject().get(0).getClaim("", "encodedList");
            assertThat(updatedBitstring).isNotEqualTo(originalBitstring);
        }

        @Test
        void revoke_whenAlreadyRevoked(IssuerRuntime runtime, CredentialStore credentialStore) {
            var token = runtime.createParticipant(USER).apiKey();

            // create a statuslist credential which has the "revocation" bit set
            var res = createRevocationCredential(EXAMPLE_REVOCATION_CREDENTIAL_WITH_STATUS_BIT_SET, EXAMPLE_REVOCATION_CREDENTIAL_JWT_WITH_STATUS_BIT_SET);

            // track the original bitstring
            var originalBitstring = res.getVerifiableCredential().credential().getCredentialSubject().get(0).getClaim("", "encodedList");
            credentialStore.create(res).orElseThrow(f -> new RuntimeException("Failed to create credential: " + f.getFailureDetail()));

            credentialStore.create(createCredential("test-cred"));

            runtime.getAdminEndpoint()
                    .baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", token))
                    .post("/v1alpha/participants/%s/credentials/test-cred/revoke".formatted(toBase64(USER)))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(204);

            // verify that the status list credential's bitstring has NOT changed
            var updatedBitstring = credentialStore.findById(STATUS_LIST_CREDENTIAL_ID).getContent()
                    .getVerifiableCredential().credential().getCredentialSubject().get(0).getClaim("", "encodedList");
            assertThat(updatedBitstring).isEqualTo(originalBitstring);
        }

        @Test
        void revoke_whenCredentialNotFound(IssuerRuntime runtime, CredentialStore credentialStore) {
            var token = runtime.createParticipant(USER).apiKey();

            // create a statuslist credential which has the "revocation" bit set
            var res = createRevocationCredential(EXAMPLE_REVOCATION_CREDENTIAL_WITH_STATUS_BIT_SET, EXAMPLE_REVOCATION_CREDENTIAL_JWT_WITH_STATUS_BIT_SET);

            // track the original bitstring
            credentialStore.create(res).orElseThrow(f -> new RuntimeException("Failed to create credential: " + f.getFailureDetail()));

            // missing: creation of the holder credential

            runtime.getAdminEndpoint()
                    .baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", token))
                    .post("/v1alpha/participants/%s/credentials/test-cred/revoke".formatted(toBase64(USER)))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(404)
                    .body(containsString("was not found"));
        }

        @Test
        void revoke_whenNotAuthorized(IssuerRuntime runtime, CredentialStore credentialStore) {
            runtime.createParticipant(USER);
            var token = runtime.createParticipant("anotherUser").apiKey();

            // create revocation credential
            var res = createRevocationCredential(EXAMPLE_REVOCATION_CREDENTIAL, EXAMPLE_REVOCATION_CREDENTIAL_JWT);

            credentialStore.create(res).orElseThrow(f -> new RuntimeException("Failed to create credential: " + f.getFailureDetail()));

            credentialStore.create(createCredential("test-cred"));

            runtime.getAdminEndpoint()
                    .baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", token))
                    .post("/v1alpha/participants/%s/credentials/test-cred/revoke".formatted(toBase64(USER)))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403);
        }

        @Test
        void revoke_whenStatusListCredentialNotFound(IssuerRuntime runtime, CredentialStore credentialStore) {
            var token = runtime.createParticipant(USER).apiKey();

            //missing: create status list credential

            credentialStore.create(createCredential("test-cred"));

            runtime.getAdminEndpoint()
                    .baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", token))
                    .post("/v1alpha/participants/%s/credentials/test-cred/revoke".formatted(toBase64(USER)))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(404)
                    .body(containsString("was not found"));
        }

        @Test
        void revoke_whenWrongStatusListType(IssuerRuntime runtime, CredentialStore credentialStore) {
            var token = runtime.createParticipant(USER).apiKey();

            // create a statuslist credential which has the "revocation" bit set
            var res = createRevocationCredential(EXAMPLE_REVOCATION_CREDENTIAL, EXAMPLE_REVOCATION_CREDENTIAL_JWT);

            // track the original bitstring
            credentialStore.create(res).orElseThrow(f -> new RuntimeException("Failed to create credential: " + f.getFailureDetail()));

            // create credential with invalid status type
            var credential = createCredential("test-cred");
            var status = credential.getVerifiableCredential().credential().getCredentialStatus();
            status.clear();
            status.add(new CredentialStatus("test-cred#status", "InvalidStatusListType", Map.of(
                    "statusListIndex", STATUS_LIST_INDEX,
                    "statusPurpose", "revocation",
                    "statusListCredential", STATUS_LIST_CREDENTIAL_ID
            )));
            credentialStore.create(credential);

            runtime.getAdminEndpoint()
                    .baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", token))
                    .post("/v1alpha/participants/%s/credentials/test-cred/revoke".formatted(toBase64(USER)))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400)
                    .body(containsString("No StatusList implementation for type 'InvalidStatusListType' found."));

        }

        @Test
        void queryCredentials(IssuerRuntime runtime, CredentialStore credentialStore) {
            var token = runtime.createParticipant(USER).apiKey();

            credentialStore.create(createCredential("test-cred"));
            credentialStore.create(createCredential("test-cred-1", "another-user"));

            runtime.getAdminEndpoint()
                    .baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", token))
                    .body(QuerySpec.Builder.newInstance()
                            .filter(new Criterion("issuerId", "=", "issuer-id"))
                            .build())
                    .post("/v1alpha/participants/%s/credentials/query".formatted(toBase64(USER)))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .body("size()", equalTo(1));

        }

        @Test
        void queryCredentials_notAuthorized(IssuerRuntime runtime, CredentialStore credentialStore) {
            runtime.createParticipant(USER);
            var token = runtime.createParticipant("anotherUser").apiKey();

            credentialStore.create(createCredential("test-cred"));

            runtime.getAdminEndpoint()
                    .baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", token))
                    .body(QuerySpec.Builder.newInstance()
                            .filter(new Criterion("issuerId", "=", "issuer-id"))
                            .build())
                    .post("/v1alpha/participants/%s/credentials/query".formatted(toBase64(USER)))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .body("size()", equalTo(0));

        }

        @Test
        void checkStatus(IssuerRuntime runtime, CredentialStore credentialStore) {
            var token = runtime.createParticipant(USER).apiKey();

            // create revocation credential
            var res = createRevocationCredential(EXAMPLE_REVOCATION_CREDENTIAL, EXAMPLE_REVOCATION_CREDENTIAL_JWT);

            credentialStore.create(res).orElseThrow(f -> new RuntimeException("Failed to create credential: " + f.getFailureDetail()));

            credentialStore.create(createCredential("test-cred"));

            runtime.getAdminEndpoint()
                    .baseRequest()
                    .header(new Header("x-api-key", token))
                    .get("/v1alpha/participants/%s/credentials/test-cred/status".formatted(toBase64(USER)))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .body(Matchers.notNullValue());
        }

        @Test
        void checkStatus_notAuthorized(IssuerRuntime runtime, CredentialStore credentialStore) {
            runtime.createParticipant(USER);
            var token = runtime.createParticipant("anotherUser").apiKey();

            // create revocation credential
            var res = createRevocationCredential(EXAMPLE_REVOCATION_CREDENTIAL, EXAMPLE_REVOCATION_CREDENTIAL_JWT);

            credentialStore.create(res).orElseThrow(f -> new RuntimeException("Failed to create credential: " + f.getFailureDetail()));

            credentialStore.create(createCredential("test-cred"));

            runtime.getAdminEndpoint()
                    .baseRequest()
                    .header(new Header("x-api-key", token))
                    .get("/v1alpha/participants/%s/credentials/test-cred/status".formatted(toBase64(USER)))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403);
        }

        @Test
        void sendCredentialOffer(IssuerRuntime runtime, HolderStore holderStore, CredentialDefinitionStore definitionStore) {
            var token = runtime.createParticipant(USER).apiKey();
            definitionStore.create(CredentialDefinition.Builder.newInstance()
                            .id("test-credential-id")
                            .credentialType("TestCredential")
                            .formatFrom(CredentialFormat.VC1_0_JWT)
                            .jsonSchemaUrl("https://example.com/schemas/test-credential.json")
                            .participantContextId(USER)
                            .build())
                    .orElseThrow(AssertionError::new);


            var port = getFreePort();
            try (var mockedHolderEndpoint = ClientAndServer.startClientAndServer(port)) {

                mockedHolderEndpoint.when(request()
                                .withPath("/api/holder/offers")
                                .withMethod("POST"))
                        .respond(response()
                                .withBody("foobar")
                                .withStatusCode(200));


                holderStore.create(Holder.Builder.newInstance()
                        .holderId("test-holder-id")
                        .participantContextId(USER)
                        .did("did:web:holder")
                        .build());

                when(DID_RESOLVER_REGISTRY.resolve(eq("did:web:holder")))
                        .thenReturn(Result.success(DidDocument.Builder.newInstance()
                                .service(List.of(new Service(UUID.randomUUID().toString(),
                                        "CredentialService",
                                        "http://localhost:%s/api/holder".formatted(port)))).build()));

                runtime.getAdminEndpoint()
                        .baseRequest()
                        .contentType(JSON)
                        .header(new Header("x-api-key", token))
                        .body(getOfferRequestBody())
                        .post("/v1alpha/participants/%s/credentials/offer".formatted(toBase64(USER)))
                        .then()
                        .log().ifValidationFails()
                        .statusCode(204);

                mockedHolderEndpoint.verify(request()
                        .withMethod("POST")
                        .withPath("/api/holder/offers")
                        .withBody(subString("credentialIssuer"))
                        .withBody(subString("credentials"))
                        .withBody(subString("TestCredential")), exactly(1));
            }
        }

        @Test
        void sendCredentialOffer_offerMessageFailure(IssuerRuntime runtime, HolderStore holderStore) {
            var token = runtime.createParticipant(USER).apiKey();

            var port = getFreePort();
            try (var mockedHolderDidServer = ClientAndServer.startClientAndServer(port)) {

                mockedHolderDidServer.when(request()
                                .withPath("/api/holder/offers")
                                .withMethod("POST"))
                        .respond(response()
                                .withBody("foobar")
                                .withStatusCode(404));


                holderStore.create(Holder.Builder.newInstance()
                        .holderId("test-holder-id")
                        .participantContextId(USER)
                        .did("did:web:holder")
                        .build());

                when(DID_RESOLVER_REGISTRY.resolve(eq("did:web:holder")))
                        .thenReturn(Result.success(DidDocument.Builder.newInstance()
                                .service(List.of(new Service(UUID.randomUUID().toString(),
                                        "CredentialService",
                                        "http://localhost:%s/api/holder".formatted(port)))).build()));

                runtime.getAdminEndpoint()
                        .baseRequest()
                        .contentType(JSON)
                        .header(new Header("x-api-key", token))
                        .body(getOfferRequestBody())
                        .post("/v1alpha/participants/%s/credentials/offer".formatted(toBase64(USER)))
                        .then()
                        .log().ifValidationFails()
                        .statusCode(400);
            }
        }

        @Test
        void sendCredentialOffer_holderDidResolutionFailure(IssuerRuntime runtime, HolderStore holderStore) {
            var token = runtime.createParticipant(USER).apiKey();

            var port = getFreePort();
            when(DID_RESOLVER_REGISTRY.resolve(eq("did:web:holder")))
                    .thenReturn(Result.failure("did not found"));

            holderStore.create(Holder.Builder.newInstance()
                    .holderId("test-holder-id")
                    .participantContextId(USER)
                    .did("did:web:holder")
                    .build());

            runtime.getAdminEndpoint()
                    .baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", token))
                    .body(getOfferRequestBody())
                    .post("/v1alpha/participants/%s/credentials/offer".formatted(toBase64(USER)))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400);
        }

        @Test
        void sendCredentialOffer_holderNotFound(IssuerRuntime runtime) {
            var token = runtime.createParticipant(USER).apiKey();

            // missing: holder

            runtime.getAdminEndpoint()
                    .baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", token))
                    .body(getOfferRequestBody())
                    .post("/v1alpha/participants/%s/credentials/offer".formatted(toBase64(USER)))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400)
                    .body(containsString("Holder not found"));
        }

        @Test
        void sendCredentialOffer_participantNotAuthorized(IssuerRuntime runtime, HolderStore holderStore) {
            var token = runtime.createParticipant(USER).apiKey();
            var token2 = runtime.createParticipant("another-issuer");

            holderStore.create(Holder.Builder.newInstance()
                    .holderId("test-holder-id")
                    .participantContextId("another-issuer")
                    .did("did:web:holder")
                    .build());

            runtime.getAdminEndpoint()
                    .baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", token))
                    .body(getOfferRequestBody())
                    .post("/v1alpha/participants/%s/credentials/offer".formatted(toBase64(USER)))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403);
        }

        private String toBase64(String input) {
            return Base64.getUrlEncoder().encodeToString(input.getBytes());
        }
    }

    @Nested
    @EndToEndTest
    class InMemory extends Tests {

        @RegisterExtension
        static final RuntimeExtension ISSUER_EXTENSION = IssuerExtension.Builder.newInstance()
                .id(ISSUER_RUNTIME_ID)
                .name(ISSUER_RUNTIME_NAME)
                .modules(ISSUER_RUNTIME_MEM_MODULES)
                .build()
                .registerServiceMock(DidResolverRegistry.class, DID_RESOLVER_REGISTRY);

    }

    @Nested
    @PostgresqlIntegrationTest
    class Postgres extends Tests {

        @Order(0)
        @RegisterExtension
        static final PostgresqlEndToEndExtension POSTGRESQL_EXTENSION = new PostgresqlEndToEndExtension();
        private static final String ISSUER = "issuer";

        @Order(1)
        @RegisterExtension
        static final BeforeAllCallback POSTGRES_CONTAINER_STARTER = context -> {
            POSTGRESQL_EXTENSION.createDatabase(ISSUER);
        };

        @Order(2)
        @RegisterExtension
        static final RuntimeExtension ISSUER_EXTENSION = IssuerExtension.Builder.newInstance()
                .id(ISSUER_RUNTIME_ID)
                .name(ISSUER_RUNTIME_NAME)
                .modules(ISSUER_RUNTIME_SQL_MODULES)
                .configurationProvider(() -> POSTGRESQL_EXTENSION.configFor(ISSUER))
                .build()
                .registerServiceMock(DidResolverRegistry.class, DID_RESOLVER_REGISTRY);
    }

}
