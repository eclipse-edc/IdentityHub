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

package org.eclipse.edc.identityhub.tests.dcp.api;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.identityhub.tests.fixtures.DefaultRuntimes;
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
import org.eclipse.edc.issuerservice.spi.issuance.process.IssuanceProcessService;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.restassured.http.ContentType.JSON;
import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat.VC1_0_JWT;
import static org.eclipse.edc.identityhub.tests.dcp.TestData.ISSUER_RUNTIME_NAME;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.JwtCreationUtil.generateJwt;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.VerifiableCredentialTestUtil.generateEcKey;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("JUnitMalformedDeclaration")
public class DcpCredentialRequestApiEndToEndTest {

    protected static final DidPublicKeyResolver DID_PUBLIC_KEY_RESOLVER = mock();
    protected static final DidResolverRegistry DID_RESOLVER_REGISTRY = mock();
    private static final String ISSUER = "issuer";

    abstract static class Tests {

        public static final String ISSUER_DID = "did:web:issuer";
        public static final String PARTICIPANT_DID = "did:web:participant";
        public static final String DID_WEB_PARTICIPANT_KEY_1 = "did:web:participant#key1";
        public static final ECKey PARTICIPANT_KEY = generateEcKey(DID_WEB_PARTICIPANT_KEY_1);
        protected static final AttestationSourceFactory ATTESTATION_SOURCE_FACTORY = mock();
        protected static final String ISSUER_ID = "issuer";
        private static final String VALID_CREDENTIAL_REQUEST_MESSAGE = """
                {
                  "@context": [
                     "https://w3id.org/dspace-dcp/v1.0/dcp.jsonld"
                  ],
                  "@type": "CredentialRequestMessage",
                  "holderPid": "holderPid",
                  "credentials":[
                    {
                        "id": "MembershipCredential-id"
                    }
                  ]
                }
                """;
        private static final String FAULTY_CREDENTIAL_REQUEST_MESSAGE = """
                {
                  "@context": [
                     "https://w3id.org/dspace-dcp/v1.0/dcp.jsonld"
                  ],
                  "@type": "CredentialRequestMessage"
                }
                """;

        @BeforeAll
        static void beforeAll(IssuerService issuer) {
            var pipelineFactory = issuer.getService(AttestationSourceFactoryRegistry.class);
            var validationRegistry = issuer.getService(AttestationDefinitionValidatorRegistry.class);
            pipelineFactory.registerFactory("Attestation", ATTESTATION_SOURCE_FACTORY);
            validationRegistry.registerValidator("Attestation", def -> ValidationResult.success());
            issuer.createParticipant(ISSUER_ID);
        }

        private static @NotNull String issuanceUrl() {
            return "/v1beta/participants/%s/credentials".formatted(ISSUER_ID);
        }

        @AfterEach
        void teardown(HolderService holderService, CredentialDefinitionService credentialDefinitionService, AttestationDefinitionService attestationDefinitionService) {
            holderService.queryHolders(QuerySpec.max()).getContent()
                    .forEach(p -> holderService.deleteHolder(p.getHolderId()).getContent());

            credentialDefinitionService.queryCredentialDefinitions(QuerySpec.max()).getContent()
                    .forEach(c -> credentialDefinitionService.deleteCredentialDefinition(c.getId()).getContent());

            attestationDefinitionService.queryAttestations(QuerySpec.max()).getContent()
                    .forEach(a -> attestationDefinitionService.deleteAttestation(a.getId()).getContent());
        }

        @Test
        void requestCredential(IssuerService issuer, HolderService holderService,
                               CredentialDefinitionService credentialDefinitionService,
                               AttestationDefinitionService attestationDefinitionService,
                               IssuanceProcessService issuanceProcessService) throws JOSEException, InterruptedException {

            var port = getFreePort();

            var mockedCredentialService = new WireMockServer(port);
            mockedCredentialService.start();
            try {

                var issuerPid = "dummy-issuance-id";
                mockedCredentialService.stubFor(post(urlPathEqualTo("/api/credentials"))
                        .willReturn(aResponse()
                                .withBody(issuerPid)
                                .withStatus(201)));


                var endpoint = "http://localhost:%s/api".formatted(mockedCredentialService.port());

                holderService.createHolder(createHolder(PARTICIPANT_DID, PARTICIPANT_DID, "Participant"));

                var attestationDefinition = AttestationDefinition.Builder.newInstance()
                        .id("attestation-id")
                        .attestationType("Attestation")
                        .configuration(Map.of())
                        .participantContextId("participantContextId")
                        .build();
                attestationDefinitionService.createAttestation(attestationDefinition);

                Map<String, Object> credentialRuleConfiguration = Map.of(
                        "claim", "onboarding.signedDocuments",
                        "operator", "eq",
                        "value", true);

                var credentialDefinition = CredentialDefinition.Builder.newInstance()
                        .id("MembershipCredential-id")
                        .credentialType("MembershipCredential")
                        .jsonSchemaUrl("https://example.com/schema")
                        .jsonSchema("{}")
                        .attestation("attestation-id")
                        .validity(3600)
                        .mapping(new MappingDefinition("participant.name", "credentialSubject.name", true))
                        .rule(new CredentialRuleDefinition("expression", credentialRuleConfiguration))
                        .participantContextId("participantContextId")
                        .formatFrom(VC1_0_JWT)
                        .build();


                credentialDefinitionService.createCredentialDefinition(credentialDefinition);

                var token = generateSiToken();

                Map<String, Object> claims = Map.of("onboarding", Map.of("signedDocuments", true), "participant", Map.of("name", "Alice"));

                var attestationSource = mock(AttestationSource.class);

                when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(DID_WEB_PARTICIPANT_KEY_1))).thenReturn(Result.success(PARTICIPANT_KEY.toPublicKey()));
                when(DID_RESOLVER_REGISTRY.resolve(PARTICIPANT_DID)).thenReturn(Result.success(generateDidDocument(endpoint)));
                when(ATTESTATION_SOURCE_FACTORY.createSource(refEq(attestationDefinition))).thenReturn(attestationSource);
                when(attestationSource.execute(any())).thenReturn(Result.success(claims));

                var location = issuer.getIssuerApiEndpoint().baseRequest()
                        .contentType(JSON)
                        .header(AUTHORIZATION, token)
                        .body(VALID_CREDENTIAL_REQUEST_MESSAGE)
                        .post(issuanceUrl())
                        .then()
                        .log().ifValidationFails()
                        .statusCode(201)
                        .extract()
                        .header("Location");

                assertThat(location).contains("/v1beta/participants/%s/requests".formatted(ISSUER_ID));

                var processId = location.substring(location.lastIndexOf('/') + 1);

                await().untilAsserted(() -> {
                    var issuanceProcess = issuanceProcessService.findById(processId);

                    assertThat(issuanceProcess).isNotNull()
                            .satisfies(process -> {
                                assertThat(process.getHolderId()).isEqualTo(PARTICIPANT_DID);
                                assertThat(process.getCredentialDefinitions()).containsExactly("MembershipCredential-id");
                                assertThat(process.getClaims()).containsAllEntriesOf(claims);
                                assertThat(process.getState()).isEqualTo(IssuanceProcessStates.DELIVERED.code());
                                assertThat(process.getParticipantContextId()).isEqualTo(ISSUER_ID);
                                assertThat(process.getHolderPid()).isEqualTo("holderPid");
                            });
                });
            } finally {
                mockedCredentialService.stop();
            }

        }

        @Test
        void requestCredential_validationError_shouldReturn400(IssuerService issuer) {
            var token = generateSiToken();

            issuer.getIssuerApiEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(AUTHORIZATION, token)
                    .body(FAULTY_CREDENTIAL_REQUEST_MESSAGE)
                    .post(issuanceUrl())
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400);

        }

        @Test
        void requestCredential_tokenNotPresent_shouldReturn401(IssuerService issuer) {
            issuer.getIssuerApiEndpoint().baseRequest()
                    .contentType(JSON)
                    .body(VALID_CREDENTIAL_REQUEST_MESSAGE)
                    .post(issuanceUrl())
                    .then()
                    .log().ifValidationFails()
                    .statusCode(401);

        }

        @Test
        void requestCredential_participantNotFound_shouldReturn401(IssuerService issuer) {
            var token = generateSiToken();

            issuer.getIssuerApiEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(AUTHORIZATION, token)
                    .body(VALID_CREDENTIAL_REQUEST_MESSAGE)
                    .post(issuanceUrl())
                    .then()
                    .log().ifValidationFails()
                    .statusCode(401);

        }

        @Test
        void requestCredential_tokenVerificationFails_shouldReturn401(IssuerService issuer, HolderService holderService) throws JOSEException {

            holderService.createHolder(createHolder(PARTICIPANT_DID, PARTICIPANT_DID, "Participant"));

            var spoofedKey = new ECKeyGenerator(Curve.P_256).keyID(DID_WEB_PARTICIPANT_KEY_1).generate();

            var token = generateSiToken();

            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(DID_WEB_PARTICIPANT_KEY_1))).thenReturn(Result.success(spoofedKey.toPublicKey()));

            issuer.getIssuerApiEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(AUTHORIZATION, token)
                    .body(VALID_CREDENTIAL_REQUEST_MESSAGE)
                    .post(issuanceUrl())
                    .then()
                    .log().ifValidationFails()
                    .statusCode(401);

        }

        @Test
        void requestCredential_spoofedKeyId_shouldReturn401(IssuerService issuer, HolderService holderService) throws JOSEException {

            var spoofedKeyId = "did:web:spoofed#key1";

            holderService.createHolder(createHolder(PARTICIPANT_DID, PARTICIPANT_DID, "Participant"));

            var spoofedKey = new ECKeyGenerator(Curve.P_256).keyID(spoofedKeyId).generate();

            var token = generateSiToken(spoofedKey);

            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(spoofedKeyId))).thenReturn(Result.success(spoofedKey.toPublicKey()));

            issuer.getIssuerApiEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(AUTHORIZATION, token)
                    .body(VALID_CREDENTIAL_REQUEST_MESSAGE)
                    .post(issuanceUrl())
                    .then()
                    .log().ifValidationFails()
                    .statusCode(401);

        }

        @Test
        void requestCredential_wrongTokenAudience_shouldReturn401(IssuerService issuer, HolderService holderService) throws JOSEException {

            holderService.createHolder(createHolder(PARTICIPANT_DID, PARTICIPANT_DID, "Participant"));

            var token = generateSiToken("wrong-audience");

            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(DID_WEB_PARTICIPANT_KEY_1))).thenReturn(Result.success(PARTICIPANT_KEY.toPublicKey()));

            issuer.getIssuerApiEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(AUTHORIZATION, token)
                    .body(VALID_CREDENTIAL_REQUEST_MESSAGE)
                    .post(issuanceUrl())
                    .then()
                    .log().ifValidationFails()
                    .statusCode(401);

        }

        @Test
        void requestCredential_definitionNotFound_shouldReturn400(IssuerService issuer, HolderService holderService) throws JOSEException {

            holderService.createHolder(createHolder(PARTICIPANT_DID, PARTICIPANT_DID, "Participant"));

            var token = generateSiToken();

            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(DID_WEB_PARTICIPANT_KEY_1))).thenReturn(Result.success(PARTICIPANT_KEY.toPublicKey()));

            issuer.getIssuerApiEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(AUTHORIZATION, token)
                    .body(VALID_CREDENTIAL_REQUEST_MESSAGE)
                    .post(issuanceUrl())
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400);

        }

        @Test
        void requestCredential_attestationsNotFulfilled_shouldReturn403(IssuerService issuer,
                                                                        HolderService holderService,
                                                                        AttestationDefinitionService attestationDefinitionService,
                                                                        CredentialDefinitionService credentialDefinitionService) throws JOSEException {

            holderService.createHolder(createHolder(PARTICIPANT_DID, PARTICIPANT_DID, "Participant"));
            var attestationDefinition = AttestationDefinition.Builder.newInstance()
                    .id("attestation-id")
                    .attestationType("Attestation")
                    .participantContextId("participantContextId")
                    .configuration(Map.of())
                    .build();
            attestationDefinitionService.createAttestation(attestationDefinition);

            Map<String, Object> credentialRuleConfiguration = Map.of(
                    "claim", "onboarding.signedDocuments",
                    "operator", "eq",
                    "value", true);


            var credentialDefinition = CredentialDefinition.Builder.newInstance()
                    .id("MembershipCredential-id")
                    .credentialType("MembershipCredential")
                    .jsonSchemaUrl("https://example.com/schema")
                    .jsonSchema("{}")
                    .attestation("attestation-id")
                    .rule(new CredentialRuleDefinition("expression", credentialRuleConfiguration))
                    .participantContextId("participantContextId")
                    .formatFrom(VC1_0_JWT)
                    .build();

            credentialDefinitionService.createCredentialDefinition(credentialDefinition);
            var token = generateSiToken();

            Map<String, Object> claims = Map.of("onboarding", Map.of("signedDocuments", false));

            var attestationSource = mock(AttestationSource.class);
            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(DID_WEB_PARTICIPANT_KEY_1))).thenReturn(Result.success(PARTICIPANT_KEY.toPublicKey()));
            when(ATTESTATION_SOURCE_FACTORY.createSource(refEq(attestationDefinition))).thenReturn(attestationSource);
            when(attestationSource.execute(any())).thenReturn(Result.success(claims));

            issuer.getIssuerApiEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(AUTHORIZATION, token)
                    .body(VALID_CREDENTIAL_REQUEST_MESSAGE)
                    .post(issuanceUrl())
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403);

        }

        // B2.9: with edc.iam.accesstoken.jti.validation=true on the issuer runtime, sending two requests with the SAME jti in the SI token -> second request 401.
        // NOTE: the issuer runtime used here already enables jti validation (DefaultRuntimes.Issuer.config() sets
        // edc.iam.accesstoken.jti.validation=true), so no dedicated config-variant runtime is required.
        @Test
        @DisplayName("B2.9: replaying an SI token with the same jti is rejected with 401 on the second request")
        void requestCredential_jtiReplay_shouldReturn401(IssuerService issuer, HolderService holderService) throws JOSEException {
            // registered holder, resolvable key
            holderService.createHolder(createHolder(PARTICIPANT_DID, PARTICIPANT_DID, "Participant"));
            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(DID_WEB_PARTICIPANT_KEY_1))).thenReturn(Result.success(PARTICIPANT_KEY.toPublicKey()));

            // create ONE SI token and send it twice - the identical token carries the identical jti claim
            var token = generateSiToken();

            // the first request passes token validation and records the jti. It then fails with 400 because no
            // credential definition exists - crucially NOT with 401, which proves the token itself was accepted.
            issuer.getIssuerApiEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(AUTHORIZATION, token)
                    .body(VALID_CREDENTIAL_REQUEST_MESSAGE)
                    .post(issuanceUrl())
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400);

            // + assert: the second request replaying the SAME token (same jti) must be rejected with 401
            issuer.getIssuerApiEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(AUTHORIZATION, token)
                    .body(VALID_CREDENTIAL_REQUEST_MESSAGE)
                    .post(issuanceUrl())
                    .then()
                    .log().ifValidationFails()
                    .statusCode(401);
        }

        private DidDocument generateDidDocument(String endpoint) {

            return DidDocument.Builder.newInstance()
                    .id(PARTICIPANT_DID)
                    .service(List.of(new Service("id", "CredentialService", endpoint)))
                    .build();

        }

        private String generateSiToken() {
            return "Bearer " + generateSiToken(ISSUER_DID);
        }

        private String generateSiToken(ECKey key) {
            return generateSiToken(ISSUER_DID, key);
        }

        private String generateSiToken(String audience, ECKey key) {
            return generateJwt(audience, PARTICIPANT_DID, PARTICIPANT_DID, Map.of(), key);
        }

        private String generateSiToken(String audience) {
            return generateJwt(audience, PARTICIPANT_DID, PARTICIPANT_DID, Map.of(), PARTICIPANT_KEY);
        }

        private Holder createHolder(String id, String did, String name) {
            return Holder.Builder.newInstance()
                    .participantContextId(UUID.randomUUID().toString())
                    .holderId(id)
                    .did(did)
                    .holderName(name)
                    .build();
        }
    }


    @Nested
    @EndToEndTest
    class InMemory extends Tests {


        @RegisterExtension
        static final RuntimeExtension ISSUER_EXTENSION = ComponentRuntimeExtension.Builder.newInstance()
                .name(ISSUER_RUNTIME_NAME)
                .modules(DefaultRuntimes.Issuer.MODULES)
                .endpoints(DefaultRuntimes.Issuer.ENDPOINTS.build())
                .configurationProvider(DefaultRuntimes.Issuer::config)
                .paramProvider(IssuerService.class, IssuerService::forContext)
                .build()
                .registerServiceMock(DidPublicKeyResolver.class, DID_PUBLIC_KEY_RESOLVER)
                .registerServiceMock(DidResolverRegistry.class, DID_RESOLVER_REGISTRY);

    }

    @Nested
    @PostgresqlIntegrationTest
    class Postgres extends Tests {

        @Order(0)
        @RegisterExtension
        static final PostgresqlEndToEndExtension POSTGRESQL_EXTENSION = new PostgresqlEndToEndExtension();
        @Order(1)
        @RegisterExtension
        static final BeforeAllCallback POSTGRES_CONTAINER_STARTER = context -> {
            POSTGRESQL_EXTENSION.createDatabase(ISSUER);
        };
        @Order(2)
        @RegisterExtension
        static final RuntimeExtension ISSUER_EXTENSION = ComponentRuntimeExtension.Builder.newInstance()
                .name(ISSUER_RUNTIME_NAME)
                .modules(DefaultRuntimes.Issuer.SQL_MODULES)
                .endpoints(DefaultRuntimes.Issuer.ENDPOINTS.build())
                .configurationProvider(DefaultRuntimes.Issuer::config)
                .configurationProvider(() -> POSTGRESQL_EXTENSION.configFor(ISSUER))
                .paramProvider(IssuerService.class, IssuerService::forContext)
                .build()
                .registerServiceMock(DidPublicKeyResolver.class, DID_PUBLIC_KEY_RESOLVER)
                .registerServiceMock(DidResolverRegistry.class, DID_RESOLVER_REGISTRY);

    }
}
