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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.gen.OctetKeyPairGenerator;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.identityhub.tests.fixtures.issuerservice.IssuerExtension;
import org.eclipse.edc.identityhub.tests.fixtures.issuerservice.IssuerRuntime;
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
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.integration.ClientAndServer;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.http.ContentType.JSON;
import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat.VC1_0_JWT;
import static org.eclipse.edc.identityhub.tests.dcp.TestData.ISSUER_RUNTIME_ID;
import static org.eclipse.edc.identityhub.tests.dcp.TestData.ISSUER_RUNTIME_MEM_MODULES;
import static org.eclipse.edc.identityhub.tests.dcp.TestData.ISSUER_RUNTIME_NAME;
import static org.eclipse.edc.identityhub.tests.dcp.TestData.ISSUER_RUNTIME_SQL_MODULES;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.JwtCreationUtil.generateJwt;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.VerifiableCredentialTestUtil.generateEcKey;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@SuppressWarnings("JUnitMalformedDeclaration")
public class DcpCredentialRequestApiEndToEndTest {

    protected static final DidPublicKeyResolver DID_PUBLIC_KEY_RESOLVER = mock();
    protected static final DidResolverRegistry DID_RESOLVER_REGISTRY = mock();

    abstract static class Tests {

        public static final String ISSUER_DID = "did:web:issuer";
        public static final String PARTICIPANT_DID = "did:web:participant";
        public static final String DID_WEB_PARTICIPANT_KEY_1 = "did:web:participant#key1";
        public static final ECKey PARTICIPANT_KEY = generateEcKey(DID_WEB_PARTICIPANT_KEY_1);
        protected static final AttestationSourceFactory ATTESTATION_SOURCE_FACTORY = mock();
        protected static final String ISSUER_ID = "issuer";
        private static final String ISSUER_ID_ENCODED = Base64.getUrlEncoder().encodeToString(ISSUER_ID.getBytes());
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
        static void beforeAll(IssuerRuntime issuerRuntime) {
            var pipelineFactory = issuerRuntime.getService(AttestationSourceFactoryRegistry.class);
            var validationRegistry = issuerRuntime.getService(AttestationDefinitionValidatorRegistry.class);
            pipelineFactory.registerFactory("Attestation", ATTESTATION_SOURCE_FACTORY);
            validationRegistry.registerValidator("Attestation", def -> ValidationResult.success());
            issuerRuntime.createParticipant(ISSUER_ID);
        }

        private static @NotNull String issuanceUrl() {
            return "/v1alpha/participants/%s/credentials".formatted(ISSUER_ID_ENCODED);
        }

        @NotNull
        private static KeyDescriptor.Builder createKey() {
            return KeyDescriptor.Builder.newInstance().keyId("test-key")
                    .privateKeyAlias("private-alias")
                    .active(true)
                    .publicKeyJwk(createJwk());
        }


        private static Map<String, Object> createJwk() {
            try {
                return new OctetKeyPairGenerator(Curve.Ed25519)
                        .generate()
                        .toJSONObject();
            } catch (JOSEException e) {
                throw new RuntimeException(e);
            }
        }

        @AfterEach
        void teardown(HolderService holderService, CredentialDefinitionService credentialDefinitionService) {
            holderService.queryHolders(QuerySpec.max()).getContent()
                    .forEach(p -> holderService.deleteHolder(p.getHolderId()).getContent());

            credentialDefinitionService.queryCredentialDefinitions(QuerySpec.max()).getContent()
                    .forEach(c -> credentialDefinitionService.deleteCredentialDefinition(c.getId()).getContent());
        }

        @Test
        void requestCredential(IssuerExtension issuerExtension, HolderService holderService,
                               CredentialDefinitionService credentialDefinitionService,
                               AttestationDefinitionService attestationDefinitionService,
                               IssuanceProcessService issuanceProcessService) throws JOSEException, InterruptedException {

            var port = getFreePort();

            try (var mockedCredentialService = ClientAndServer.startClientAndServer(port)) {

                var issuerPid = "dummy-issuance-id";
                mockedCredentialService.when(request()
                                .withMethod("POST")
                                .withPath("/api/credentials"))
                        .respond(response()
                                .withBody(issuerPid)
                                .withStatusCode(201));


                var endpoint = "http://localhost:%s/api".formatted(mockedCredentialService.getLocalPort());

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

                var location = issuerExtension.getIssuerApiEndpoint().baseRequest()
                        .contentType(JSON)
                        .header(AUTHORIZATION, token)
                        .body(VALID_CREDENTIAL_REQUEST_MESSAGE)
                        .post(issuanceUrl())
                        .then()
                        .log().ifValidationFails()
                        .statusCode(201)
                        .extract()
                        .header("Location");

                assertThat(location).contains("/v1alpha/participants/%s/requests".formatted(ISSUER_ID_ENCODED));

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
            }

        }

        @Test
        void requestCredential_validationError_shouldReturn400(IssuerExtension issuerExtension) {
            var token = generateSiToken();

            issuerExtension.getIssuerApiEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(AUTHORIZATION, token)
                    .body(FAULTY_CREDENTIAL_REQUEST_MESSAGE)
                    .post(issuanceUrl())
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400);

        }

        @Test
        void requestCredential_tokenNotPresent_shouldReturn401(IssuerExtension issuerExtension) {
            issuerExtension.getIssuerApiEndpoint().baseRequest()
                    .contentType(JSON)
                    .body(VALID_CREDENTIAL_REQUEST_MESSAGE)
                    .post(issuanceUrl())
                    .then()
                    .log().ifValidationFails()
                    .statusCode(401);

        }

        @Test
        void requestCredential_participantNotFound_shouldReturn401(IssuerExtension issuerExtension) {
            var token = generateSiToken();

            issuerExtension.getIssuerApiEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(AUTHORIZATION, token)
                    .body(VALID_CREDENTIAL_REQUEST_MESSAGE)
                    .post(issuanceUrl())
                    .then()
                    .log().ifValidationFails()
                    .statusCode(401);

        }

        @Test
        void requestCredential_tokenVerificationFails_shouldReturn401(IssuerExtension issuerExtension, HolderService holderService) throws JOSEException {

            holderService.createHolder(createHolder(PARTICIPANT_DID, PARTICIPANT_DID, "Participant"));

            var spoofedKey = new ECKeyGenerator(Curve.P_256).keyID(DID_WEB_PARTICIPANT_KEY_1).generate();

            var token = generateSiToken();

            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(DID_WEB_PARTICIPANT_KEY_1))).thenReturn(Result.success(spoofedKey.toPublicKey()));

            issuerExtension.getIssuerApiEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(AUTHORIZATION, token)
                    .body(VALID_CREDENTIAL_REQUEST_MESSAGE)
                    .post(issuanceUrl())
                    .then()
                    .log().ifValidationFails()
                    .statusCode(401);

        }

        @Test
        void requestCredential_spoofedKeyId_shouldReturn401(IssuerExtension issuerExtension, HolderService holderService) throws JOSEException {

            var spoofedKeyId = "did:web:spoofed#key1";

            holderService.createHolder(createHolder(PARTICIPANT_DID, PARTICIPANT_DID, "Participant"));

            var spoofedKey = new ECKeyGenerator(Curve.P_256).keyID(spoofedKeyId).generate();

            var token = generateSiToken(spoofedKey);

            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(spoofedKeyId))).thenReturn(Result.success(spoofedKey.toPublicKey()));

            issuerExtension.getIssuerApiEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(AUTHORIZATION, token)
                    .body(VALID_CREDENTIAL_REQUEST_MESSAGE)
                    .post(issuanceUrl())
                    .then()
                    .log().ifValidationFails()
                    .statusCode(401);

        }

        @Test
        void requestCredential_wrongTokenAudience_shouldReturn401(IssuerExtension issuerExtension, HolderService holderService) throws JOSEException {

            holderService.createHolder(createHolder(PARTICIPANT_DID, PARTICIPANT_DID, "Participant"));

            var token = generateSiToken("wrong-audience");

            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(DID_WEB_PARTICIPANT_KEY_1))).thenReturn(Result.success(PARTICIPANT_KEY.toPublicKey()));

            issuerExtension.getIssuerApiEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(AUTHORIZATION, token)
                    .body(VALID_CREDENTIAL_REQUEST_MESSAGE)
                    .post(issuanceUrl())
                    .then()
                    .log().ifValidationFails()
                    .statusCode(401);

        }

        @Test
        void requestCredential_definitionNotFound_shouldReturn400(IssuerExtension issuerExtension, HolderService holderService) throws JOSEException {

            holderService.createHolder(createHolder(PARTICIPANT_DID, PARTICIPANT_DID, "Participant"));

            var token = generateSiToken();

            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(DID_WEB_PARTICIPANT_KEY_1))).thenReturn(Result.success(PARTICIPANT_KEY.toPublicKey()));

            issuerExtension.getIssuerApiEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(AUTHORIZATION, token)
                    .body(VALID_CREDENTIAL_REQUEST_MESSAGE)
                    .post(issuanceUrl())
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400);

        }

        @Test
        void requestCredential_attestationsNotFulfilled_shouldReturn403(IssuerExtension issuerExtension,
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

            issuerExtension.getIssuerApiEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(AUTHORIZATION, token)
                    .body(VALID_CREDENTIAL_REQUEST_MESSAGE)
                    .post(issuanceUrl())
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403);

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
    @Order(1)
    class InMemory extends Tests {


        @RegisterExtension
        static final RuntimeExtension ISSUER_EXTENSION = IssuerExtension.Builder.newInstance()
                .id(ISSUER_RUNTIME_ID)
                .name(ISSUER_RUNTIME_NAME)
                .modules(ISSUER_RUNTIME_MEM_MODULES)
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
                .registerServiceMock(DidPublicKeyResolver.class, DID_PUBLIC_KEY_RESOLVER)
                .registerServiceMock(DidResolverRegistry.class, DID_RESOLVER_REGISTRY);

    }
}
