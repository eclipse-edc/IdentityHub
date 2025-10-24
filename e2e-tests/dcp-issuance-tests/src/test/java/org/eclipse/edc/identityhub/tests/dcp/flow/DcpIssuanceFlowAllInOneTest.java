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

package org.eclipse.edc.identityhub.tests.dcp.flow;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.SignedJWT;
import io.restassured.http.Header;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.VerificationMethod;
import org.eclipse.edc.iam.did.spi.resolution.DidResolver;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialUsage;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.identityhub.tests.fixtures.DefaultRuntimes;
import org.eclipse.edc.identityhub.tests.fixtures.credentialservice.IdentityHub;
import org.eclipse.edc.identityhub.tests.fixtures.issuerservice.IssuerService;
import org.eclipse.edc.issuerservice.spi.holder.HolderService;
import org.eclipse.edc.issuerservice.spi.holder.model.Holder;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionService;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionValidatorRegistry;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationSource;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationSourceFactory;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationSourceFactoryRegistry;
import org.eclipse.edc.issuerservice.spi.issuance.credentialdefinition.CredentialDefinitionService;
import org.eclipse.edc.issuerservice.spi.issuance.model.AttestationDefinition;
import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialDefinition;
import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialRuleDefinition;
import org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcessStates;
import org.eclipse.edc.issuerservice.spi.issuance.model.MappingDefinition;
import org.eclipse.edc.issuerservice.spi.issuance.process.store.IssuanceProcessStore;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.utils.Endpoints;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.net.URI;
import java.text.ParseException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat.VC1_0_JWT;
import static org.eclipse.edc.identityhub.tests.fixtures.common.TestFunctions.base64Encode;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.JwtCreationUtil.generateJwt;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("JUnitMalformedDeclaration")
public class DcpIssuanceFlowAllInOneTest {


    protected static final AttestationSourceFactory ATTESTATION_SOURCE_FACTORY = mock();

    protected static final Duration TIMEOUT = Duration.ofSeconds(60);
    protected static final Duration INTERVAL = Duration.ofSeconds(1);
    private static final ObjectMapper OBJECT_MAPPER = JacksonJsonLd.createObjectMapper()
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .enable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT);

    abstract static class Tests {

        protected static final String ISSUER_ID = "issuer";
        protected static final String PARTICIPANT_ID = "issuer"; //issuer and participant use the same ID -> issue to self

        // Endpoints for issuer and identity hub runtimes
        protected static final Endpoints.Builder ENDPOINTS = Endpoints.Builder.newInstance()
                .endpoint("issueradmin", () -> URI.create("http://localhost:" + getFreePort() + "/api/admin"))
                .endpoint("issuance", () -> URI.create("http://localhost:" + getFreePort() + "/api/issuance"))
                .endpoint("sts", () -> URI.create("http://localhost:" + getFreePort() + "/api/sts"))
                .endpoint("identity", () -> URI.create("http://localhost:" + getFreePort() + "/api/identity"))
                .endpoint("did", () -> URI.create("http://localhost:" + getFreePort() + "/"))
                .endpoint("statuslist", () -> URI.create("http://localhost:" + getFreePort() + "/statuslist"))
                .endpoint("credentials", () -> URI.create("http://localhost:" + getFreePort() + "/api/credentials"));

        private static String participantToken;
        private static String issuerDid;
        private static String participantDid;

        @BeforeAll
        static void beforeAll(IssuerService issuer, IdentityHub identityHub) {
            var pipelineFactory = issuer.getService(AttestationSourceFactoryRegistry.class);
            var validationRegistry = issuer.getService(AttestationDefinitionValidatorRegistry.class);
            pipelineFactory.registerFactory("Attestation", ATTESTATION_SOURCE_FACTORY);
            validationRegistry.registerValidator("Attestation", def -> ValidationResult.success());

            // Create an issuer
            issuerDid = issuer.didFor(ISSUER_ID);

            var services = List.of(
                    issuer.createServiceEndpoint(ISSUER_ID),
                    identityHub.createServiceEndpoint(PARTICIPANT_ID));

            var response = issuer.createParticipant(ISSUER_ID, issuerDid, issuerDid + "#key", services);

            // Create a participant and store the token
            participantDid = issuer.didFor(PARTICIPANT_ID);
            participantToken = response.apiKey();

            // seed attestations, credential definitions, rules, mappings etc. to the Issuer
            prepareIssuer(issuer);

        }

        /**
         * Prepares the issuer facet by creating rules and mappings, credential definitions and a mocked attestation source
         *
         * @param issuer the runtime that implements the issuer endpoint etc.
         */
        private static void prepareIssuer(IssuerService issuer) {
            var attestationDefinition = createRulesAndMappingsInIssuer(issuer, Map.of(
                    "claim", "onboarding.signedDocuments",
                    "operator", "eq",
                    "value", true), new MappingDefinition("participant.name", "credentialSubject.name", true));

            var attestationSource = mock(AttestationSource.class);
            when(ATTESTATION_SOURCE_FACTORY.createSource(refEq(attestationDefinition))).thenReturn(attestationSource);
            when(attestationSource.execute(any()))
                    .thenReturn(Result.success(Map.of("onboarding", Map.of("signedDocuments", true), "participant", Map.of("name", "Alice"))));
        }

        /**
         * Set the issuer up with an attestation definition and a credential definition
         */
        private static @NotNull AttestationDefinition createRulesAndMappingsInIssuer(IssuerService issuer, Map<String, Object> ruleConfiguration, MappingDefinition mappingDefinition) {
            var participantService = issuer.getService(HolderService.class);
            var credentialDefinitionService = issuer.getService(CredentialDefinitionService.class);
            var attestationDefinitionService = issuer.getService(AttestationDefinitionService.class);

            participantService.createHolder(Holder.Builder.newInstance().holderId(PARTICIPANT_ID).did(participantDid).holderName("Participant").participantContextId("participantContextId").build());


            var attestationDefinition = AttestationDefinition.Builder.newInstance().id("attestation-id")
                    .attestationType("Attestation")
                    .participantContextId("participantContextId")
                    .configuration(Map.of())
                    .build();
            attestationDefinitionService.createAttestation(attestationDefinition);


            var credentialDefinition = CredentialDefinition.Builder.newInstance()
                    .id("membershipCredential-id")
                    .credentialType("MembershipCredential")
                    .jsonSchemaUrl("https://example.com/schema")
                    .jsonSchema("{}")
                    .attestation(attestationDefinition.getId())
                    .validity(Duration.ofSeconds(5).toSeconds()) // one second - trigger renewal
                    .mapping(mappingDefinition)
                    .rule(new CredentialRuleDefinition("expression", ruleConfiguration))
                    .participantContextId("participantContextId")
                    .formatFrom(VC1_0_JWT)
                    .build();

            credentialDefinitionService.createCredentialDefinition(credentialDefinition);
            return attestationDefinition;
        }

        @Test
        void testCredentialIssuance(IssuerService issuer, IdentityHub identityHub) {

            var holderRequestId = UUID.randomUUID().toString();
            var issuanceRequest = """
                    {
                      "issuerDid": "%s",
                      "holderPid": "%s",
                      "credentials": [{ "format": "VC1_0_JWT", "id": "membershipCredential-id", "type": "MembershipCredential" }]
                    }
                    """.formatted(issuerDid, holderRequestId);

            identityHub.getIdentityEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", participantToken))
                    .body(issuanceRequest)
                    .post("/v1alpha/participants/%s/credentials/request".formatted(base64Encode(PARTICIPANT_ID)))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(201)
                    .header("Location", Matchers.endsWith("/credentials/request/" + holderRequestId));

            // wait for the request status to be requested on the holder side
            await().pollInterval(INTERVAL)
                    .atMost(TIMEOUT)
                    .untilAsserted(() -> assertThat(identityHub.getCredentialRequestForParticipant(PARTICIPANT_ID, holderRequestId)).hasSize(1)
                            .allSatisfy(t -> {
                                assertThat(t.getState()).isEqualTo(HolderRequestState.ISSUED.code());
                                assertThat(t.getHolderPid()).isEqualTo(holderRequestId);
                            }));

            // wait for the issuance process to be delivered on the issuer side
            await().pollInterval(INTERVAL)
                    .atMost(TIMEOUT)
                    .untilAsserted(() -> assertThat(issuer.getIssuanceProcessesForParticipant(ISSUER_ID, holderRequestId))
                            .hasSizeGreaterThanOrEqualTo(1)
                            .allSatisfy(t -> {
                                assertThat(t.getHolderPid()).isEqualTo(holderRequestId);
                                assertThat(t.getState()).isEqualTo(IssuanceProcessStates.DELIVERED.code());
                            }));


            // checks that the credential was issued correctly: we expect 1 status list credential, 1 "holder" credential and 1 tracked issuance
            var credentials = issuer.getCredentialsForParticipant(ISSUER_ID);
            assertThat(credentials)
                    .hasSizeGreaterThanOrEqualTo(3);
            assertThat(credentials).anySatisfy(vc -> {
                assertThat(vc.getUsage()).isEqualTo(CredentialUsage.Holder);
                assertThat(vc.getStateAsEnum()).isEqualTo(VcStatus.ISSUED);
                assertThat(vc.getIssuerId()).isEqualTo(issuerDid);
                assertThat(vc.getHolderId()).isEqualTo(participantDid);
                assertThat(vc.getVerifiableCredential().credential().getCredentialStatus())
                        .hasSize(1)
                        .allSatisfy(t -> assertThat(t.type()).isEqualTo("BitstringStatusListEntry"));
            });
            assertThat(credentials).anySatisfy(vc -> {
                assertThat(vc.getUsage()).isEqualTo(CredentialUsage.IssuanceTracking);
                assertThat(vc.getVerifiableCredential().rawVc()).isNull();
            });
            assertThat(credentials).anySatisfy(vc -> {
                assertThat(vc.getUsage()).isEqualTo(CredentialUsage.StatusList);
                assertThat(vc.getIssuerId()).isEqualTo(issuerDid);
                assertThat(vc.getHolderId()).isEqualTo(participantDid);
                assertThat(vc.getVerifiableCredential().credential().getCredentialSubject()).isNotEmpty()
                        .anySatisfy(t -> {
                            assertThat(t.getClaim("", "statusPurpose").toString()).isEqualTo("revocation");
                        });
            });
            // verify that the status credential on the issuer side is accessible
            assertThat(credentials)
                    .anySatisfy(vc -> {
                        assertThat(vc.getUsage()).isEqualTo(CredentialUsage.StatusList);
                        assertThat(vc.getMetadata()).isNotNull().isNotEmpty().containsKey("publicUrl");

                        var url = vc.getMetadata().get("publicUrl");
                        given()
                                .baseUri(url.toString())
                                .header("Accept", "application/vc+jwt")
                                .get()
                                .then()
                                .log().ifValidationFails()
                                .statusCode(200)
                                .header("Content-Type", "application/vc+jwt")
                                .body(Matchers.notNullValue());
                    });

        }

        @Test
        void testRenewal(IssuerService issuer, IdentityHub identityHub) {
            var holderRequestId = UUID.randomUUID().toString();
            var issuanceRequest = """
                    {
                      "issuerDid": "%s",
                      "holderPid": "%s",
                      "credentials": [{ "format": "VC1_0_JWT", "id": "membershipCredential-id", "type": "MembershipCredential" }]
                    }
                    """.formatted(issuerDid, holderRequestId);

            issuer.getIdentityEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", participantToken))
                    .body(issuanceRequest)
                    .post("/v1alpha/participants/%s/credentials/request".formatted(base64Encode(PARTICIPANT_ID)))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(201)
                    .header("Location", Matchers.endsWith("/credentials/request/" + holderRequestId));

            // wait for the request status to be ISSUED on the holder side
            await().pollInterval(INTERVAL)
                    .atMost(TIMEOUT)
                    .untilAsserted(() -> assertThat(identityHub.getCredentialRequestForParticipant(PARTICIPANT_ID, holderRequestId))
                            .hasSize(1)
                            .allSatisfy(t -> {
                                assertThat(t.getState()).isEqualTo(HolderRequestState.ISSUED.code());
                                assertThat(t.getHolderPid()).isEqualTo(holderRequestId);
                            }));


            //we expect each one new entry for CredentialUsage.Holder and CredentialUsage.IssuanceTracking
            var store = issuer.getService(CredentialStore.class);
            var issuanceProcessStore = issuer.getService(IssuanceProcessStore.class);
            await().pollInterval(INTERVAL)
                    .atMost(TIMEOUT)
                    .untilAsserted(() -> {
                        assertThat(store.query(QuerySpec.Builder.newInstance()
                                        .filter(new Criterion("usage", "=", CredentialUsage.IssuanceTracking.toString()))
                                        .build())
                                .getContent())
                                .hasSizeGreaterThanOrEqualTo(2);

                        assertThat(store.query(QuerySpec.Builder.newInstance()
                                        .filter(new Criterion("usage", "=", CredentialUsage.Holder.toString()))
                                        .build())
                                .getContent())
                                .hasSizeGreaterThanOrEqualTo(2);

                        // no issuance process should be in a state _other than_ DELIVERED
                        var query = QuerySpec.Builder.newInstance()
                                .filter(new Criterion("state", "!=", IssuanceProcessStates.DELIVERED.code()))
                                .build();
                        assertThat(issuanceProcessStore.query(query))
                                .isEmpty();

                    });
        }

        @Test
        void testPresentationQuery(IssuerService issuer, IdentityHub identityHub) throws JOSEException, ParseException {
            // set up consumer DID
            var consumerDid = "did:example:consumer";
            var consumerKey = new ECKeyGenerator(Curve.P_256).keyID(consumerDid + "#key").generate();
            //  and a mocked DID resolver
            var exampleResolverMock = mock(DidResolver.class);
            when(exampleResolverMock.getMethod()).thenReturn("example");
            when(exampleResolverMock.resolve(startsWith("did:example"))).thenReturn(Result.success(DidDocument.Builder.newInstance()
                    .verificationMethod(List.of(VerificationMethod.Builder.newInstance()
                            .id(consumerDid + "#key")
                            .publicKeyJwk(consumerKey.toPublicJWK().toJSONObject())
                            .controller(consumerDid)
                            .type("JsonWebKey2020")
                            .build()))
                    .build()));
            issuer.getService(DidResolverRegistry.class).register(exampleResolverMock);

            // issue credential to holder

            var holderRequestId = UUID.randomUUID().toString();
            var issuanceRequest = """
                    {
                      "issuerDid": "%s",
                      "holderPid": "%s",
                      "credentials": [{ "format": "VC1_0_JWT", "id": "membershipCredential-id", "type": "MembershipCredential" }]
                    }
                    """.formatted(issuerDid, holderRequestId);

            issuer.getIdentityEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", participantToken))
                    .body(issuanceRequest)
                    .post("/v1alpha/participants/%s/credentials/request".formatted(base64Encode(PARTICIPANT_ID)))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(201)
                    .header("Location", Matchers.endsWith("/credentials/request/" + holderRequestId));

            // wait for the request status to be requested on the holder side
            await().pollInterval(INTERVAL)
                    .atMost(TIMEOUT)
                    .untilAsserted(() -> assertThat(identityHub.getCredentialRequestForParticipant(PARTICIPANT_ID, holderRequestId)).hasSize(1)
                            .allSatisfy(t -> {
                                assertThat(t.getState()).isEqualTo(HolderRequestState.ISSUED.code());
                                assertThat(t.getHolderPid()).isEqualTo(holderRequestId);
                            }));

            // create token, for that we need the provider's private key
            var providerJwk = issuer.getService(Vault.class).resolveSecret(PARTICIPANT_ID + "-alias");
            assertThat(providerJwk).isNotNull();
            var accessToken = generateJwt(participantDid, participantDid, consumerDid, Map.of("scope", "org.eclipse.edc.vc.type:MembershipCredential:read"), ECKey.parse(providerJwk));
            var token = generateJwt(participantDid, consumerDid, consumerDid, Map.of("client_id", consumerDid, "token", accessToken), consumerKey);


            var response = identityHub.getCredentialsEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(AUTHORIZATION, "Bearer " + token)
                    .body("""
                            {
                              "@context": [
                                "https://identity.foundation/presentation-exchange/submission/v1",
                                 "https://w3id.org/dspace-dcp/v1.0/dcp.jsonld"
                              ],
                              "@type": "PresentationQueryMessage",
                              "scope":[
                                "org.eclipse.edc.vc.type:MembershipCredential:read"
                              ]
                            }
                            """)
                    .post("/v1/participants/%s/presentations/query".formatted(base64Encode(PARTICIPANT_ID)))
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
    }

    @Nested
    @EndToEndTest
    class InMemory extends Tests {

        @RegisterExtension
        static final RuntimeExtension RUNTIME_EXTENSION = ComponentRuntimeExtension.Builder.newInstance()
                .name("all-in-one-runtime")
                .modules(DefaultRuntimes.IdentityHub.MODULES)
                .modules(DefaultRuntimes.Issuer.MODULES)
                .endpoints(ENDPOINTS.build())
                .configurationProvider(DefaultRuntimes.Issuer::config)
                .configurationProvider(DefaultRuntimes.IdentityHub::config)
                .paramProvider(IdentityHub.class, IdentityHub::forContext)
                .paramProvider(IssuerService.class, IssuerService::forContext)
                .build();
    }

    @Nested
    @EndToEndTest
    class Postgres extends Tests {

        @Order(0)
        @RegisterExtension
        static final PostgresqlEndToEndExtension POSTGRESQL_EXTENSION = new PostgresqlEndToEndExtension();
        private static final String ISSUER = "issuer";

        @Order(2)
        @RegisterExtension
        static final RuntimeExtension RUNTIME_EXTENSION = ComponentRuntimeExtension.Builder.newInstance()
                .name("all-in-one-runtime")
                .modules(DefaultRuntimes.IdentityHub.SQL_MODULES)
                .modules(DefaultRuntimes.Issuer.SQL_MODULES)
                .endpoints(ENDPOINTS.build())
                .configurationProvider(DefaultRuntimes.Issuer::config)
                .configurationProvider(DefaultRuntimes.IdentityHub::config)
                .configurationProvider(() -> POSTGRESQL_EXTENSION.configFor(ISSUER))
                .paramProvider(IdentityHub.class, IdentityHub::forContext)
                .paramProvider(IssuerService.class, IssuerService::forContext)
                .build();

        @Order(1) // must be the first extension to be evaluated since it starts the DB server
        @RegisterExtension
        static final BeforeAllCallback POSTGRES_CONTAINER_STARTER = context -> {
            POSTGRESQL_EXTENSION.createDatabase(ISSUER);
        };
    }
}
