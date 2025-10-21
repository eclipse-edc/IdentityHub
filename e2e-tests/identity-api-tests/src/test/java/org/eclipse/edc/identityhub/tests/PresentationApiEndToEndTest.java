/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *       Cofinity-X - Improvements for VC DataModel 2.0
 *
 */

package org.eclipse.edc.identityhub.tests;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.SignedJWT;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.edc.iam.identitytrust.sts.spi.store.StsAccountStore;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialStatus;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.RevocationServiceRegistry;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.identityhub.spi.did.store.DidResourceStore;
import org.eclipse.edc.identityhub.spi.keypair.store.KeyPairResourceStore;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.identityhub.tests.fixtures.TestData;
import org.eclipse.edc.identityhub.tests.fixtures.credentialservice.IdentityHubExtension;
import org.eclipse.edc.identityhub.tests.fixtures.credentialservice.IdentityHubRuntime;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static io.restassured.http.ContentType.JSON;
import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.identitytrust.spi.DcpConstants.DCP_CONTEXT_URL;
import static org.eclipse.edc.iam.identitytrust.spi.DcpConstants.DSPACE_DCP_V_1_0_CONTEXT;
import static org.eclipse.edc.identityhub.tests.fixtures.TestData.IH_RUNTIME_ID;
import static org.eclipse.edc.identityhub.tests.fixtures.TestData.IH_RUNTIME_MEM_MODULES;
import static org.eclipse.edc.identityhub.tests.fixtures.TestData.IH_RUNTIME_NAME;
import static org.eclipse.edc.identityhub.tests.fixtures.TestData.IH_RUNTIME_SQL_MODULES;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.JwtCreationUtil.CONSUMER_DID;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.JwtCreationUtil.CONSUMER_KEY;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.JwtCreationUtil.PROVIDER_DID;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.JwtCreationUtil.PROVIDER_KEY;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.JwtCreationUtil.TEST_SCOPE;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.JwtCreationUtil.generateJwt;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.JwtCreationUtil.generateSiToken;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.VerifiableCredentialTestUtil.generateEcKey;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("JUnitMalformedDeclaration")
public class PresentationApiEndToEndTest {

    abstract static class Tests {

        protected static final DidPublicKeyResolver DID_PUBLIC_KEY_RESOLVER = mock();
        protected static final RevocationServiceRegistry REVOCATION_LIST_REGISTRY = mock();
        private static final String VALID_QUERY_WITH_SCOPE_TEMPLATE = """
                {
                  "@context": [
                    "https://identity.foundation/presentation-exchange/submission/v1",
                     "%s"
                  ],
                  "@type": "PresentationQueryMessage",
                  "scope":[
                    "org.eclipse.edc.vc.type:AlumniCredential:read"
                  ]
                }
                """;
        private static final String VALID_QUERY_WITH_SCOPE = VALID_QUERY_WITH_SCOPE_TEMPLATE.formatted("https://w3id.org/tractusx-trust/v0.8");
        private static final String VALID_QUERY_WITH_ADDITIONAL_SCOPE = """
                {
                  "@context": [
                    "https://identity.foundation/presentation-exchange/submission/v1",
                    "https://w3id.org/tractusx-trust/v0.8"
                  ],
                  "@type": "PresentationQueryMessage",
                  "scope":[
                    "org.eclipse.edc.vc.type:AlumniCredential:read",
                    "org.eclipse.edc.vc.type:SuperSecretCredential:*"
                  ]
                }
                """;
        private static final String TEST_PARTICIPANT_CONTEXT_ID = "consumer";
        private static final String TEST_PARTICIPANT_CONTEXT_ID_ENCODED = Base64.getUrlEncoder().encodeToString(TEST_PARTICIPANT_CONTEXT_ID.getBytes());
        private static final ObjectMapper OBJECT_MAPPER = JacksonJsonLd.createObjectMapper()
                .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                .enable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT);


        @BeforeEach
        void setup(IdentityHubRuntime runtime) {
            createParticipant(runtime);
        }

        @AfterEach
        void teardown(ParticipantContextService contextService, DidResourceStore didResourceStore, KeyPairResourceStore keyPairResourceStore, CredentialStore store, StsAccountStore accountStore) {
            // purge all participant contexts

            contextService.query(QuerySpec.max()).getContent()
                    .forEach(pc -> contextService.deleteParticipantContext(pc.getParticipantContextId()).getContent());

            didResourceStore.query(QuerySpec.max()).forEach(dr -> didResourceStore.deleteById(dr.getDid()).getContent());

            keyPairResourceStore.query(QuerySpec.max()).getContent()
                    .forEach(kpr -> keyPairResourceStore.deleteById(kpr.getId()).getContent());

            // purge all VCs
            store.query(QuerySpec.none())
                    .map(creds -> creds.stream().map(cred -> store.deleteById(cred.getId())).toList())
                    .orElseThrow(f -> new RuntimeException(f.getFailureDetail()));

            accountStore.findAll(QuerySpec.max())
                    .forEach(sts -> accountStore.deleteById(sts.getId()).getContent());
        }

        @Test
        void query_tokenNotPresent_shouldReturn401(IdentityHubRuntime runtime) {

            runtime.getCredentialsEndpoint().baseRequest()
                    .contentType("application/json")
                    .post("/v1/participants/%s/presentations/query".formatted(TEST_PARTICIPANT_CONTEXT_ID_ENCODED))
                    .then()
                    .statusCode(401)
                    .extract().body().asString();
        }

        @Test
        void query_validationError_shouldReturn400(IdentityHubRuntime runtime) {

            var query = """
                    {
                      "@context": [
                        "https://identity.foundation/participants/test-participant/presentation-exchange/submission/v1",
                        "https://w3id.org/tractusx-trust/v0.8"
                      ],
                      "@type": "PresentationQueryMessage"
                    }
                    """;
            runtime.getCredentialsEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(AUTHORIZATION, "Bearer " + generateSiToken())
                    .body(query)
                    .post("/v1/participants/%s/presentations/query".formatted(TEST_PARTICIPANT_CONTEXT_ID_ENCODED))
                    .then()
                    .statusCode(400)
                    .extract().body().asString();

        }

        @Test
        void query_withPresentationDefinition_shouldReturn501(IdentityHubRuntime runtime) {

            var query = """
                    {
                      "@context": [
                        "https://identity.foundation/presentation-exchange/submission/v1",
                        "https://w3id.org/tractusx-trust/v0.8"
                      ],
                      "@type": "PresentationQueryMessage",
                      "presentationDefinition":{
                        "id": "presentation1",
                            "input_descriptors": [
                            ]
                      }
                    }
                    """;
            runtime.getCredentialsEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(AUTHORIZATION, "Bearer " + generateSiToken())
                    .body(query)
                    .post("/v1/participants/%s/presentations/query".formatted(TEST_PARTICIPANT_CONTEXT_ID_ENCODED))
                    .then()
                    .statusCode(501)
                    .extract().body().asString();
        }

        @Test
        void query_tokenVerificationFails_shouldReturn401(IdentityHubRuntime runtime) throws JOSEException {


            var spoofedKey = new ECKeyGenerator(Curve.P_256).keyID("did:web:provider#key1").generate();
            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq("did:web:provider#key1"))).thenReturn(Result.success(spoofedKey.toPublicKey()));

            var token = generateSiToken();
            runtime.getCredentialsEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(AUTHORIZATION, "Bearer " + token)
                    .body(VALID_QUERY_WITH_SCOPE)
                    .post("/v1/participants/%s/presentations/query".formatted(TEST_PARTICIPANT_CONTEXT_ID_ENCODED))
                    .then()
                    .statusCode(401)
                    .log().ifValidationFails()
                    .body("[0].type", equalTo("AuthenticationFailed"))
                    .body("[0].message", equalTo("ID token verification failed: Token verification failed"));
        }

        @Test
        void query_spoofedKeyId_shouldReturn401(IdentityHubRuntime runtime) throws JOSEException {
            var spoofedKey = generateEcKey("did:web:spoofed#key1");

            var accessToken = generateJwt(CONSUMER_DID, CONSUMER_DID, PROVIDER_DID, Map.of("scope", TEST_SCOPE), CONSUMER_KEY);
            var token = generateJwt(CONSUMER_DID, PROVIDER_DID, PROVIDER_DID, Map.of("client_id", PROVIDER_DID, "token", accessToken), spoofedKey);

            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq("did:web:consumer#key1"))).thenReturn(Result.success(CONSUMER_KEY.toPublicKey()));
            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq("did:web:spoofed#key1"))).thenReturn(Result.success(spoofedKey.toPublicKey()));

            runtime.getCredentialsEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(AUTHORIZATION, "Bearer " + token)
                    .body(VALID_QUERY_WITH_SCOPE)
                    .post("/v1/participants/%s/presentations/query".formatted(TEST_PARTICIPANT_CONTEXT_ID_ENCODED))
                    .then()
                    .statusCode(401)
                    .log().ifValidationFails()
                    .body("[0].type", equalTo("AuthenticationFailed"))
                    .body("[0].message", startsWith("ID token verification failed: kid header"));

        }

        @Test
        void query_proofOfPossessionFails_shouldReturn401(IdentityHubRuntime runtime) throws JOSEException {

            var accessToken = generateJwt(CONSUMER_DID, CONSUMER_DID, PROVIDER_DID, Map.of("scope", TEST_SCOPE), CONSUMER_KEY);
            var token = generateJwt(CONSUMER_DID, PROVIDER_DID, "mismatching", Map.of("client_id", PROVIDER_DID, "token", accessToken), PROVIDER_KEY);

            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq("did:web:consumer#key1"))).thenReturn(Result.success(CONSUMER_KEY.toPublicKey()));
            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq("did:web:provider#key1"))).thenReturn(Result.success(PROVIDER_KEY.toPublicKey()));

            runtime.getCredentialsEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(AUTHORIZATION, "Bearer " + token)
                    .body(VALID_QUERY_WITH_SCOPE)
                    .post("/v1/participants/%s/presentations/query".formatted(TEST_PARTICIPANT_CONTEXT_ID_ENCODED))
                    .then()
                    .statusCode(401)
                    .log().ifValidationFails()
                    .body("[0].type", equalTo("AuthenticationFailed"))
                    .body("[0].message", startsWith("ID token verification failed: ID token [sub] claim is not equal to [token.sub] claim"));

        }

        @Test
        void query_credentialQueryResolverFails_shouldReturn403(IdentityHubRuntime runtime, CredentialStore store) throws JOSEException, JsonProcessingException {

            var token = generateSiToken();

            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq("did:web:consumer#key1"))).thenReturn(Result.success(CONSUMER_KEY.toPublicKey()));
            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq("did:web:provider#key1"))).thenReturn(Result.success(PROVIDER_KEY.toPublicKey()));

            // create the credential in the store
            var cred = OBJECT_MAPPER.readValue(TestData.VC_EXAMPLE, VerifiableCredential.class);
            var res = VerifiableCredentialResource.Builder.newHolder()
                    .state(VcStatus.ISSUED)
                    .credential(new VerifiableCredentialContainer(TestData.VC_EXAMPLE, CredentialFormat.VC1_0_JWT, cred))
                    .issuerId("https://example.edu/issuers/565049")
                    .holderId("did:example:ebfeb1f712ebc6f1c276e12ec21")
                    .participantContextId(TEST_PARTICIPANT_CONTEXT_ID)
                    .build();
            store.create(res);

            // create another credential in the store
            var cred2 = OBJECT_MAPPER.readValue(TestData.VC_EXAMPLE_2, VerifiableCredential.class);
            var res2 = VerifiableCredentialResource.Builder.newHolder()
                    .state(VcStatus.ISSUED)
                    .credential(new VerifiableCredentialContainer(TestData.VC_EXAMPLE_2, CredentialFormat.VC1_0_JWT, cred2))
                    .issuerId("https://example.edu/issuers/12345")
                    .holderId("did:example:ebfeb1f712ebc6f1c276e12ec21")
                    .participantContextId(TEST_PARTICIPANT_CONTEXT_ID)
                    .build();
            store.create(res2);


            runtime.getCredentialsEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(AUTHORIZATION, "Bearer " + token)
                    .body(VALID_QUERY_WITH_ADDITIONAL_SCOPE)
                    .post("/v1/participants/%s/presentations/query".formatted(TEST_PARTICIPANT_CONTEXT_ID_ENCODED))
                    .then()
                    .log().ifError()
                    .statusCode(403)
                    .body("[0].type", equalTo("NotAuthorized"))
                    .body("[0].message", equalTo("Invalid query: requested Credentials outside of scope."));
        }

        @ParameterizedTest
        @ValueSource(strings = {DCP_CONTEXT_URL, DSPACE_DCP_V_1_0_CONTEXT})
        void query_success_noCredentials(String dcpContext, IdentityHubRuntime runtime) throws JOSEException {

            var token = generateSiToken();

            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq("did:web:consumer#key1"))).thenReturn(Result.success(CONSUMER_KEY.toPublicKey()));
            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq("did:web:provider#key1"))).thenReturn(Result.success(PROVIDER_KEY.toPublicKey()));

            var response = runtime.getCredentialsEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(AUTHORIZATION, "Bearer " + token)
                    .body(VALID_QUERY_WITH_SCOPE_TEMPLATE.formatted(dcpContext))
                    .post("/v1/participants/%s/presentations/query".formatted(TEST_PARTICIPANT_CONTEXT_ID_ENCODED))
                    .then()
                    .statusCode(200)
                    .log().ifValidationFails()
                    .extract().body().as(JsonObject.class);

            assertThat(response)
                    .hasEntrySatisfying("type", jsonValue -> assertThat(jsonValue.toString()).contains("PresentationResponseMessage"))
                    .hasEntrySatisfying("@context", jsonValue -> assertThat(jsonValue.asJsonArray()).hasSize(1));


            assertThat(vpTokensExtractor(response)).hasSize(0);
        }

        @ParameterizedTest
        @ValueSource(strings = {DCP_CONTEXT_URL, DSPACE_DCP_V_1_0_CONTEXT})
        void query_success_containsCredential(String dcpContext, IdentityHubRuntime runtime, CredentialStore store) throws JOSEException, JsonProcessingException {

            var cred = OBJECT_MAPPER.readValue(TestData.VC_EXAMPLE, VerifiableCredential.class);
            var res = VerifiableCredentialResource.Builder.newHolder()
                    .state(VcStatus.ISSUED)
                    .credential(new VerifiableCredentialContainer(TestData.VC_EXAMPLE, CredentialFormat.VC1_0_JWT, cred))
                    .issuerId("https://example.edu/issuers/565049")
                    .holderId("did:example:ebfeb1f712ebc6f1c276e12ec21")
                    .participantContextId(TEST_PARTICIPANT_CONTEXT_ID)
                    .build();

            store.create(res);
            var token = generateSiToken();

            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq("did:web:consumer#key1"))).thenReturn(Result.success(CONSUMER_KEY.toPublicKey()));
            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq("did:web:provider#key1"))).thenReturn(Result.success(PROVIDER_KEY.toPublicKey()));

            var response = runtime.getCredentialsEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(AUTHORIZATION, "Bearer " + token)
                    .body(VALID_QUERY_WITH_SCOPE_TEMPLATE.formatted(dcpContext))
                    .post("/v1/participants/%s/presentations/query".formatted(TEST_PARTICIPANT_CONTEXT_ID_ENCODED))
                    .then()
                    .statusCode(200)
                    .log().ifValidationFails()
                    .extract().body().as(JsonObject.class);

            assertThat(response)
                    .hasEntrySatisfying("type", jsonValue -> assertThat(jsonValue.toString()).contains("PresentationResponseMessage"))
                    .hasEntrySatisfying("@context", jsonValue -> assertThat(jsonValue.asJsonArray()).hasSize(1))
                    .hasEntrySatisfying("presentation", jsonValue -> {
                        assertThat(vpTokensExtractor(jsonValue)).hasSize(1)
                                .first()
                                .satisfies(vpToken -> {
                                    assertThat(vpToken).isNotNull();
                                    assertThat(extractCredentials(vpToken)).isNotEmpty();
                                });
                    });

        }

        @ParameterizedTest
        @ValueSource(strings = {DCP_CONTEXT_URL, DSPACE_DCP_V_1_0_CONTEXT})
        void query_success_containsMultiplePresentations(String dcpContext, IdentityHubRuntime runtime, CredentialStore store) throws JOSEException, JsonProcessingException {

            var cred = OBJECT_MAPPER.readValue(TestData.VC_EXAMPLE, VerifiableCredential.class);
            var res = VerifiableCredentialResource.Builder.newHolder()
                    .state(VcStatus.ISSUED)
                    .credential(new VerifiableCredentialContainer(TestData.VC_EXAMPLE, CredentialFormat.VC1_0_JWT, cred))
                    .issuerId("https://example.edu/issuers/565049")
                    .holderId("did:example:ebfeb1f712ebc6f1c276e12ec21")
                    .participantContextId(TEST_PARTICIPANT_CONTEXT_ID)
                    .build();
            store.create(res);

            var res1 = VerifiableCredentialResource.Builder.newHolder()
                    .state(VcStatus.ISSUED)
                    .credential(new VerifiableCredentialContainer(TestData.VC_EXAMPLE, CredentialFormat.VC2_0_JOSE, cred))
                    .issuerId("https://example.edu/issuers/565049")
                    .holderId("did:example:ebfeb1f712ebc6f1c276e12ec21")
                    .participantContextId(TEST_PARTICIPANT_CONTEXT_ID)
                    .build();

            store.create(res1);

            var token = generateSiToken();

            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq("did:web:consumer#key1"))).thenReturn(Result.success(CONSUMER_KEY.toPublicKey()));
            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq("did:web:provider#key1"))).thenReturn(Result.success(PROVIDER_KEY.toPublicKey()));

            var response = runtime.getCredentialsEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(AUTHORIZATION, "Bearer " + token)
                    .body(VALID_QUERY_WITH_SCOPE_TEMPLATE.formatted(dcpContext))
                    .post("/v1/participants/%s/presentations/query".formatted(TEST_PARTICIPANT_CONTEXT_ID_ENCODED))
                    .then()
                    .statusCode(200)
                    .log().ifValidationFails()
                    .extract().body().as(JsonObject.class);

            assertThat(response)
                    .hasEntrySatisfying("type", jsonValue -> assertThat(jsonValue.toString()).contains("PresentationResponseMessage"))
                    .hasEntrySatisfying("@context", jsonValue -> assertThat(jsonValue.asJsonArray()).hasSize(1))
                    .hasEntrySatisfying("presentation", jsonValue -> {
                        assertThat(jsonValue.getValueType()).isEqualTo(JsonValue.ValueType.ARRAY);
                        assertThat(jsonValue.asJsonArray()).hasSize(2);
                    });

        }

        @Test
        void query_success_containsEnvelopedCredential(IdentityHubRuntime runtime, CredentialStore store) throws JOSEException, JsonProcessingException {

            var cred = OBJECT_MAPPER.readValue(TestData.VC_EXAMPLE, VerifiableCredential.class);
            var res = VerifiableCredentialResource.Builder.newHolder()
                    .state(VcStatus.ISSUED)
                    .credential(new VerifiableCredentialContainer(TestData.JWT_VC_EXAMPLE, CredentialFormat.VC2_0_JOSE, cred))
                    .issuerId("https://example.edu/issuers/565049")
                    .holderId("did:example:ebfeb1f712ebc6f1c276e12ec21")
                    .participantContextId(TEST_PARTICIPANT_CONTEXT_ID)
                    .build();

            store.create(res);
            var token = generateSiToken();

            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq("did:web:consumer#key1"))).thenReturn(Result.success(CONSUMER_KEY.toPublicKey()));
            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq("did:web:provider#key1"))).thenReturn(Result.success(PROVIDER_KEY.toPublicKey()));

            var response = runtime.getCredentialsEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(AUTHORIZATION, "Bearer " + token)
                    .body(VALID_QUERY_WITH_SCOPE)
                    .post("/v1/participants/%s/presentations/query".formatted(TEST_PARTICIPANT_CONTEXT_ID_ENCODED))
                    .then()
                    .statusCode(200)
                    .log().ifValidationFails()
                    .extract().body().as(JsonObject.class);

            assertThat(response)
                    .hasEntrySatisfying("type", jsonValue -> assertThat(jsonValue.toString()).contains("PresentationResponseMessage"))
                    .hasEntrySatisfying("@context", jsonValue -> assertThat(jsonValue.asJsonArray()).hasSize(1))
                    .hasEntrySatisfying("presentation", jsonValue -> {
                        assertThat(jsonValue.getValueType()).isEqualTo(JsonValue.ValueType.STRING);
                        var vpToken = ((JsonString) jsonValue).getString();
                        assertThat(vpToken).isNotNull();
                    });
        }

        @ParameterizedTest(name = "VcState code: {0}")
        @ValueSource(ints = {600, 700, 800, 900})
        void query_shouldFilterOutInvalidCreds(int vcStateCode, IdentityHubRuntime runtime, CredentialStore store) throws JOSEException, JsonProcessingException {

            // modify VC content, so that it becomes either not-yet-valid or expired
            var vcContent = TestData.VC_EXAMPLE;
            if (vcStateCode == VcStatus.EXPIRED.code()) {
                var expirationInPast = Instant.now().minus(1, ChronoUnit.DAYS).toString();
                vcContent = vcContent.replaceAll("\"expirationDate\": \"2999-01-01T19:23:24Z\",", "\"expirationDate\": \"" + expirationInPast + "\",");
            } else if (vcStateCode == VcStatus.NOT_YET_VALID.code()) {
                var futureIssuance = Instant.now().plus(1, ChronoUnit.DAYS).toString();
                vcContent = vcContent.replaceAll("\"issuanceDate\": \".*\",", "\"issuanceDate\": \"" + futureIssuance + "\",");
            }


            var cred = OBJECT_MAPPER.readValue(vcContent, VerifiableCredential.class);
            // inject a CredentialStatus object, that triggers the revocation check
            if (vcStateCode == VcStatus.SUSPENDED.code()) {
                cred.getCredentialStatus().add(new CredentialStatus("test-cred-stat-id", "StatusList2021Entry",
                        Map.of("statusListCredential", "https://university.example/credentials/status/3",
                                "statusPurpose", "suspension",
                                "statusListIndex", 69)));
                when(REVOCATION_LIST_REGISTRY.checkValidity(any(VerifiableCredential.class)))
                        .thenReturn(Result.failure("suspended"));
            }
            // create the credential in the store
            var res = VerifiableCredentialResource.Builder.newHolder()
                    .state(VcStatus.from(vcStateCode))
                    .credential(new VerifiableCredentialContainer(vcContent, CredentialFormat.VC1_0_JWT, cred))
                    .issuerId("https://example.edu/issuers/565049")
                    .holderId("did:example:ebfeb1f712ebc6f1c276e12ec21")
                    .participantContextId(TEST_PARTICIPANT_CONTEXT_ID)
                    .build();
            store.create(res);

            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq("did:web:consumer#key1"))).thenReturn(Result.success(CONSUMER_KEY.toPublicKey()));
            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq("did:web:provider#key1"))).thenReturn(Result.success(PROVIDER_KEY.toPublicKey()));

            var token = generateSiToken();

            var response = runtime.getCredentialsEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(AUTHORIZATION, "Bearer " + token)
                    .body(VALID_QUERY_WITH_SCOPE)
                    .post("/v1/participants/%s/presentations/query".formatted(TEST_PARTICIPANT_CONTEXT_ID_ENCODED))
                    .then()
                    .statusCode(200)
                    .log().ifError()
                    .extract().body().as(JsonObject.class);

            assertThat(response)
                    .hasEntrySatisfying("type", jsonValue -> assertThat(jsonValue.toString()).contains("PresentationResponseMessage"))
                    .hasEntrySatisfying("@context", jsonValue -> assertThat(jsonValue.asJsonArray()).hasSize(1));

            assertThat(vpTokensExtractor(response)).hasSize(0);
        }

        @Test
        void query_accessTokenKeyIdDoesNotBelongToParticipant_shouldReturn401(IdentityHubRuntime runtime, CredentialStore store) throws JsonProcessingException, JOSEException {

            createParticipant(runtime, "attacker", generateEcKey("did:web:attacker#key-1"));

            var cred = OBJECT_MAPPER.readValue(TestData.VC_EXAMPLE, VerifiableCredential.class);
            var res = VerifiableCredentialResource.Builder.newHolder()
                    .state(VcStatus.ISSUED)
                    .credential(new VerifiableCredentialContainer(TestData.VC_EXAMPLE, CredentialFormat.VC1_0_JWT, cred))
                    .issuerId("https://example.edu/issuers/565049")
                    .holderId("did:example:ebfeb1f712ebc6f1c276e12ec21")
                    .participantContextId(TEST_PARTICIPANT_CONTEXT_ID)
                    .build();

            store.create(res);
            var token = generateSiToken();
            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq("did:web:consumer#key1"))).thenReturn(Result.success(CONSUMER_KEY.toPublicKey()));
            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq("did:web:provider#key1"))).thenReturn(Result.success(PROVIDER_KEY.toPublicKey()));

            runtime.getCredentialsEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(AUTHORIZATION, "Bearer " + token)
                    .body(VALID_QUERY_WITH_SCOPE)
                    // attempt to request the presentation for a different participant than the one who issued the access token
                    .post("/v1/participants/%s/presentations/query".formatted(Base64.getUrlEncoder().encodeToString("attacker".getBytes())))
                    .then()
                    .statusCode(401)
                    .log().ifValidationFails();

        }

        @Test
        void query_accessTokenAudienceDoesNotBelongToParticipant_shouldReturn401(IdentityHubRuntime runtime, CredentialStore store) throws JsonProcessingException, JOSEException {

            var cred = OBJECT_MAPPER.readValue(TestData.VC_EXAMPLE, VerifiableCredential.class);
            var res = VerifiableCredentialResource.Builder.newHolder()
                    .state(VcStatus.ISSUED)
                    .credential(new VerifiableCredentialContainer(TestData.VC_EXAMPLE, CredentialFormat.VC1_0_JWT, cred))
                    .issuerId("https://example.edu/issuers/565049")
                    .holderId("did:example:ebfeb1f712ebc6f1c276e12ec21")
                    .participantContextId(TEST_PARTICIPANT_CONTEXT_ID)
                    .build();

            store.create(res);

            var accessToken = generateJwt("did:web:someone_else", "did:web:someone_else", PROVIDER_DID, Map.of("scope", TEST_SCOPE), CONSUMER_KEY);
            var token = generateJwt(CONSUMER_DID, PROVIDER_DID, PROVIDER_DID, Map.of("client_id", PROVIDER_DID, "token", accessToken), PROVIDER_KEY);

            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq("did:web:consumer#key1"))).thenReturn(Result.success(CONSUMER_KEY.toPublicKey()));
            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq("did:web:provider#key1"))).thenReturn(Result.success(PROVIDER_KEY.toPublicKey()));

            runtime.getCredentialsEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(AUTHORIZATION, "Bearer " + token)
                    .body(VALID_QUERY_WITH_SCOPE)
                    // attempt to request the presentation for a different participant than the one who issued the access token
                    .post("/v1/participants/%s/presentations/query".formatted(TEST_PARTICIPANT_CONTEXT_ID_ENCODED))
                    .then()
                    .statusCode(401)
                    .log().ifValidationFails()
                    .body(Matchers.containsString("The DID associated with the Participant Context ID of this request ('did:web:consumer') must match 'aud' claim in 'access_token' ([did:web:someone_else])."));
        }

        @ParameterizedTest
        @ValueSource(strings = {DCP_CONTEXT_URL, DSPACE_DCP_V_1_0_CONTEXT})
        void query_filterCredentialWithWrongUsage(String dcpContext, IdentityHubRuntime runtime, CredentialStore store) throws JsonProcessingException, JOSEException {

            var cred = OBJECT_MAPPER.readValue(TestData.VC_EXAMPLE, VerifiableCredential.class);
            var res = VerifiableCredentialResource.Builder.newHolder()
                    .state(VcStatus.ISSUED)
                    .credential(new VerifiableCredentialContainer(TestData.VC_EXAMPLE, CredentialFormat.VC1_0_JWT, cred))
                    .issuerId("https://example.edu/issuers/565049")
                    .holderId("did:example:ebfeb1f712ebc6f1c276e12ec21")
                    .participantContextId(TEST_PARTICIPANT_CONTEXT_ID)
                    .build();

            store.create(res);


            var issuedCred = OBJECT_MAPPER.readValue(TestData.VC_EXAMPLE_2, VerifiableCredential.class);
            var resIssuanceTracker = VerifiableCredentialResource.Builder.newIssuanceTracker()
                    .state(VcStatus.ISSUED)
                    .credential(new VerifiableCredentialContainer(TestData.VC_EXAMPLE_2, CredentialFormat.VC1_0_JWT, issuedCred))
                    .issuerId("https://example.edu/issuers/565049")
                    .holderId("did:example:ebfeb1f712ebc6f1c276e12ec21")
                    .participantContextId(TEST_PARTICIPANT_CONTEXT_ID)
                    .build();
            store.create(resIssuanceTracker);

            var token = generateSiToken();

            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq("did:web:consumer#key1"))).thenReturn(Result.success(CONSUMER_KEY.toPublicKey()));
            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq("did:web:provider#key1"))).thenReturn(Result.success(PROVIDER_KEY.toPublicKey()));

            var response = runtime.getCredentialsEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(AUTHORIZATION, "Bearer " + token)
                    .body(VALID_QUERY_WITH_SCOPE_TEMPLATE.formatted(dcpContext))
                    .post("/v1/participants/%s/presentations/query".formatted(TEST_PARTICIPANT_CONTEXT_ID_ENCODED))
                    .then()
                    .statusCode(200)
                    .log().ifValidationFails()
                    .extract().body().as(JsonObject.class);

            assertThat(response)
                    .hasEntrySatisfying("type", jsonValue -> assertThat(jsonValue.toString()).contains("PresentationResponseMessage"))
                    .hasEntrySatisfying("@context", jsonValue -> assertThat(jsonValue.asJsonArray()).hasSize(1))
                    .hasEntrySatisfying("presentation", jsonValue -> {
                        assertThat(vpTokensExtractor(jsonValue)).hasSize(1)
                                .first()
                                .satisfies(vpToken -> {
                                    assertThat(vpToken).isNotNull();
                                    assertThat(extractCredentials(vpToken)).hasSize(1);
                                });
                    });

        }

        /**
         * extracts a (potentially empty) list of verifiable credentials from a JWT-VP
         */
        @SuppressWarnings("unchecked")
        private List<VerifiableCredential> extractCredentials(String vpToken) {
            try {
                var jwt = SignedJWT.parse(vpToken);
                var vpClaim = jwt.getJWTClaimsSet().getClaim("vp");
                if (vpClaim == null) return List.of();

                Map<String, Object> map = (Map<String, Object>) OBJECT_MAPPER.convertValue(vpClaim, Map.class);

                return (List<VerifiableCredential>) map.get("verifiableCredential");

            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }

        private void createParticipant(IdentityHubRuntime runtime) {
            createParticipant(runtime, TEST_PARTICIPANT_CONTEXT_ID, CONSUMER_KEY);
        }

        private void createParticipant(IdentityHubRuntime runtime, String participantContextId, ECKey participantKey) {
            var service = runtime.getService(ParticipantContextService.class);
            var vault = runtime.getService(Vault.class);

            var privateKeyAlias = "%s-privatekey-alias".formatted(participantContextId);
            vault.storeSecret(privateKeyAlias, participantKey.toJSONString());
            var manifest = ParticipantManifest.Builder.newInstance()
                    .participantId(participantContextId)
                    .did("did:web:%s".formatted(participantContextId.replace("did:web:", "")))
                    .active(true)
                    .key(KeyDescriptor.Builder.newInstance()
                            .publicKeyJwk(participantKey.toPublicJWK().toJSONObject())
                            .privateKeyAlias(privateKeyAlias)
                            .keyId(participantKey.getKeyID())
                            .build())
                    .build();
            service.createParticipantContext(manifest)
                    .orElseThrow(f -> new RuntimeException(f.getFailureDetail()));
        }

        private List<String> vpTokensExtractor(JsonObject response) {
            if (!response.containsKey("presentation")) {
                return List.of();
            }

            var value = response.get("presentation");
            return vpTokensExtractor(value);
        }

        private List<String> vpTokensExtractor(JsonValue jsonValue) {
            if (jsonValue.getValueType() == JsonValue.ValueType.ARRAY) {
                return ((JsonArray) jsonValue).stream()
                        .map(JsonString.class::cast)
                        .map(JsonString::getString)
                        .toList();
            } else {
                return List.of(((JsonString) jsonValue).getString());
            }

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
                .registerServiceMock(DidPublicKeyResolver.class, DID_PUBLIC_KEY_RESOLVER)
                .registerServiceMock(RevocationServiceRegistry.class, REVOCATION_LIST_REGISTRY);
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
                .registerServiceMock(DidPublicKeyResolver.class, DID_PUBLIC_KEY_RESOLVER)
                .registerServiceMock(RevocationServiceRegistry.class, REVOCATION_LIST_REGISTRY);


    }
}
