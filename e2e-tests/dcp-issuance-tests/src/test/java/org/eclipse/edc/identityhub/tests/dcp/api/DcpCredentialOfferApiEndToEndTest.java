/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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

package org.eclipse.edc.identityhub.tests.dcp.api;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialOfferStatus;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialOfferStore;
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
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.http.ContentType.JSON;
import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.iam.decentralizedclaims.spi.DcpConstants.DSPACE_DCP_NAMESPACE_V_1_0;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat.VC2_0_JOSE;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.DcpConstants.CREDENTIALS_NAMESPACE_W3C;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject.CREDENTIAL_OBJECT_BINDING_METHODS_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject.CREDENTIAL_OBJECT_CREDENTIAL_TYPE_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject.CREDENTIAL_OBJECT_OFFER_REASON_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject.CREDENTIAL_OBJECT_PROFILE_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialOfferMessage.CREDENTIALS_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialOfferMessage.CREDENTIAL_ISSUER_TERM;
import static org.eclipse.edc.identityhub.tests.dcp.TestData.IH_RUNTIME_NAME;
import static org.eclipse.edc.identityhub.tests.dcp.TestData.ISSUER_RUNTIME_NAME;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.JwtCreationUtil.PROVIDER_DID;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.JwtCreationUtil.PROVIDER_KEY;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.JwtCreationUtil.generateJwt;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.JwtCreationUtil.generateSiToken;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.VerifiableCredentialTestUtil.generateEcKey;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("JUnitMalformedDeclaration")
public class DcpCredentialOfferApiEndToEndTest {

    protected static final DidPublicKeyResolver DID_PUBLIC_KEY_RESOLVER = mock();
    protected static final AttestationSourceFactory ATTESTATION_SOURCE_FACTORY = mock();
    private static final String ISSUER = "issuer";
    private static final String IDENTITY_HUB = "identityhub";

    abstract static class Tests {

        public static final String ISSUER_ID = "issuer";
        // must be "user1", because the DefaultRuntimes.IdentityHub config expects the "user1-alias" private key alias
        public static final String PARTICIPANT_CONTEXT_ID = "user1";
        public static final String PROVIDER_KEY_ID = PROVIDER_DID + "#key1";
        public static final String TEST_PROFILE = "vc20-bssl/jwt";
        public static final String TEST_CREDENTIAL_TYPE = "MembershipCredential";
        protected static final Duration TIMEOUT = Duration.ofSeconds(60);
        protected static final Duration INTERVAL = Duration.ofSeconds(1);

        private static String issuerDid;
        private static String participantDid;
        private static ECKey issuerKey;

        @BeforeAll
        static void beforeAll(IssuerService issuer, IdentityHub identityHub) {
            var pipelineFactory = issuer.getService(AttestationSourceFactoryRegistry.class);
            var validationRegistry = issuer.getService(AttestationDefinitionValidatorRegistry.class);
            pipelineFactory.registerFactory("Attestation", ATTESTATION_SOURCE_FACTORY);
            validationRegistry.registerValidator("Attestation", def -> ValidationResult.success());

            issuerDid = issuer.didFor(ISSUER_ID);
            issuer.createParticipant(ISSUER_ID, issuerDid, issuerDid + "#key");
            issuerKey = generateEcKey(issuerDid + "#key1");

            participantDid = identityHub.didFor(PARTICIPANT_CONTEXT_ID);
            identityHub.createParticipant(PARTICIPANT_CONTEXT_ID, participantDid, participantDid + "#key");

            // fallback for key ids that are not explicitly stubbed in the individual tests
            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(any())).thenReturn(Result.failure("not found"));
        }

        @AfterEach
        void teardown(IdentityHub identityHub) {
            var credentialOfferStore = identityHub.getService(CredentialOfferStore.class);
            credentialOfferStore.query(QuerySpec.max())
                    .forEach(offer -> credentialOfferStore.deleteById(offer.getId()).orElseThrow(f -> new RuntimeException(f.getFailureDetail())));
        }

        @Test
        void offerCredential(IssuerService issuer, IdentityHub identityHub) throws JOSEException {
            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(issuerKey.getKeyID()))).thenReturn(Result.success(issuerKey.toPublicKey()));

            var credentialObjectId = "credential-object-id-" + UUID.randomUUID();

            // set up the issuer service so it can serve the credential request triggered by the offer
            var holderService = issuer.getService(HolderService.class);
            var attestationDefinitionService = issuer.getService(AttestationDefinitionService.class);
            var credentialDefinitionService = issuer.getService(CredentialDefinitionService.class);

            holderService.createHolder(Holder.Builder.newInstance()
                    .participantContextId(ISSUER_ID)
                    .holderId(participantDid)
                    .did(participantDid)
                    .holderName("Participant")
                    .build());

            var attestationDefinition = AttestationDefinition.Builder.newInstance()
                    .id("attestation-id")
                    .attestationType("Attestation")
                    .participantContextId(ISSUER_ID)
                    .configuration(Map.of())
                    .build();
            attestationDefinitionService.createAttestation(attestationDefinition);

            var credentialDefinition = CredentialDefinition.Builder.newInstance()
                    .id(credentialObjectId)
                    .credentialType(TEST_CREDENTIAL_TYPE)
                    .jsonSchemaUrl("https://example.com/schema")
                    .jsonSchema("{}")
                    .attestation(attestationDefinition.getId())
                    .validity(3600)
                    .mapping(new MappingDefinition("participant.name", "credentialSubject.name", true))
                    .mapping(new MappingDefinition("participant.id", "credentialSubject.id", true))
                    .rule(new CredentialRuleDefinition("expression", Map.of(
                            "claim", "onboarding.signedDocuments",
                            "operator", "eq",
                            "value", true)))
                    .participantContextId(ISSUER_ID)
                    .formatFrom(VC2_0_JOSE)
                    .build();
            credentialDefinitionService.createCredentialDefinition(credentialDefinition);

            var attestationSource = mock(AttestationSource.class);
            when(ATTESTATION_SOURCE_FACTORY.createSource(refEq(attestationDefinition))).thenReturn(attestationSource);
            when(attestationSource.execute(any())).thenReturn(Result.success(Map.of(
                    "onboarding", Map.of("signedDocuments", true),
                    "participant", Map.of("name", "Alice", "id", "test-participant-with-name-alice"))));

            // make credential offer
            identityHub.getCredentialsEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(AUTHORIZATION, "Bearer " + generateJwt(participantDid, issuerDid, issuerDid, Map.of(), issuerKey))
                    .body(createCredentialOfferMessage(createCredentialObject(credentialObjectId)))
                    .post(offerUrl(PARTICIPANT_CONTEXT_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(204);


            var credentialOfferStore = identityHub.getService(CredentialOfferStore.class);

            // verify the offer has reached IdentityHub
            await().pollInterval(INTERVAL).atMost(TIMEOUT).untilAsserted(() -> {
                assertThat(credentialOfferStore.query(QuerySpec.max()))
                        .hasSize(1)
                        .allSatisfy(offer -> {
                            assertThat(offer.getStateAsEnum()).isEqualTo(CredentialOfferStatus.PROCESSED);
                            assertThat(offer.getParticipantContextId()).isEqualTo(PARTICIPANT_CONTEXT_ID);
                            assertThat(offer.issuer()).isEqualTo(issuerDid);
                            assertThat(offer.getCredentialObjects()).hasSize(1)
                                    .allSatisfy(credentialObject -> {
                                        assertThat(credentialObject.getId()).isEqualTo(credentialObjectId);
                                        assertThat(credentialObject.getCredentialType()).isEqualTo(TEST_CREDENTIAL_TYPE);
                                        assertThat(credentialObject.getProfile()).isEqualTo(TEST_PROFILE);
                                        assertThat(credentialObject.getOfferReason()).isEqualTo("reissuance");
                                        assertThat(credentialObject.getBindingMethods()).containsExactly("did:web");
                                    });
                        });

                // the offer handler must initiate a credential request referencing the offered credential object
                assertThat(identityHub.getCredentialRequestForParticipant(PARTICIPANT_CONTEXT_ID))
                        .hasSize(1)
                        .allSatisfy(request -> {
                            assertThat(request.getIssuerDid()).isEqualTo(issuerDid);
                            assertThat(request.getIdsAndFormats()).hasSize(1)
                                    .allSatisfy(requestedCredential -> {
                                        assertThat(requestedCredential.id()).isEqualTo(credentialObjectId);
                                        assertThat(requestedCredential.credentialType()).isEqualTo(TEST_CREDENTIAL_TYPE);
                                        assertThat(requestedCredential.format()).isEqualTo(VC2_0_JOSE.name());
                                    });
                        });
            });

            // the credential request triggered by the offer must be registered on the issuer service
            await().pollInterval(INTERVAL).atMost(TIMEOUT).untilAsserted(() ->
                    assertThat(issuer.getIssuanceProcessesForParticipant(ISSUER_ID))
                            .hasSize(1)
                            .allSatisfy(process -> {
                                assertThat(process.getHolderId()).isEqualTo(participantDid);
                                // actual delivery will not work, because we are using a mock did resolver, and that does
                                // not resolve the participant's DID
                                assertThat(process.getState()).isEqualTo(IssuanceProcessStates.APPROVED.code());
                                assertThat(process.getCredentialDefinitions()).containsExactly(credentialObjectId);
                            }));

        }

        @Test
        void offerCredential_missingIssuer_shouldReturn400(IdentityHub identityHub) {
            var message = Json.createObjectBuilder()
                    .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(CREDENTIALS_TERM), Json.createArrayBuilder().add(createCredentialObject(UUID.randomUUID().toString())))
                    .build();

            identityHub.getCredentialsEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(AUTHORIZATION, "Bearer " + generateSiToken())
                    .body(message)
                    .post(offerUrl(PARTICIPANT_CONTEXT_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400);
        }

        @Test
        void offerCredential_missingCredentialType_shouldReturn400(IdentityHub identityHub) {
            var credentialObject = Json.createObjectBuilder()
                    .add(JsonLdKeywords.ID, UUID.randomUUID().toString())
                    .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(CREDENTIAL_OBJECT_PROFILE_TERM), Json.createArrayBuilder(List.of(TEST_PROFILE)))
                    .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(CREDENTIAL_OBJECT_OFFER_REASON_TERM), "reissuance")
                    //missing: credentialType
                    .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(CREDENTIAL_OBJECT_BINDING_METHODS_TERM), Json.createArrayBuilder(List.of("did:web")))
                    .build();

            identityHub.getCredentialsEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(AUTHORIZATION, "Bearer " + generateSiToken())
                    .body(createCredentialOfferMessage(credentialObject))
                    .post(offerUrl(PARTICIPANT_CONTEXT_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400);
        }

        @Test
        void offerCredential_tokenNotPresent_shouldReturn401(IdentityHub identityHub) {
            identityHub.getCredentialsEndpoint().baseRequest()
                    .contentType(JSON)
                    .body(createCredentialOfferMessage(createCredentialObject(UUID.randomUUID().toString())))
                    .post(offerUrl(PARTICIPANT_CONTEXT_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(401);
        }

        @Test
        void offerCredential_participantNotFound_shouldReturn401(IdentityHub identityHub) {
            identityHub.getCredentialsEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(AUTHORIZATION, "Bearer " + generateSiToken())
                    .body(createCredentialOfferMessage(createCredentialObject(UUID.randomUUID().toString())))
                    .post(offerUrl("not-exists"))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(401);
        }

        @Test
        void offerCredential_tokenVerificationFails_shouldReturn403(IdentityHub identityHub) throws JOSEException {
            var spoofedKey = new ECKeyGenerator(Curve.P_256).keyID(PROVIDER_KEY_ID).generate();
            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(PROVIDER_KEY_ID))).thenReturn(Result.success(spoofedKey.toPublicKey()));

            identityHub.getCredentialsEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(AUTHORIZATION, "Bearer " + generateSiToken())
                    .body(createCredentialOfferMessage(createCredentialObject(UUID.randomUUID().toString())))
                    .post(offerUrl(PARTICIPANT_CONTEXT_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403);
        }

        @Test
        void offerCredential_wrongTokenAudience_shouldReturn403(IdentityHub identityHub) throws JOSEException {
            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(PROVIDER_KEY_ID))).thenReturn(Result.success(PROVIDER_KEY.toPublicKey()));

            identityHub.getCredentialsEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(AUTHORIZATION, "Bearer " + generateSiToken("did:web:wrong-audience", PROVIDER_DID))
                    .body(createCredentialOfferMessage(createCredentialObject(UUID.randomUUID().toString())))
                    .post(offerUrl(PARTICIPANT_CONTEXT_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403);
        }

        private String offerUrl(String participantContextId) {
            return "/v1/participants/%s/offers".formatted(participantContextId);
        }

        private JsonObject createCredentialOfferMessage(JsonObject... credentials) {
            var credentialsArray = Json.createArrayBuilder();
            Arrays.stream(credentials).forEach(credentialsArray::add);
            return Json.createObjectBuilder()
                    .add(CREDENTIALS_NAMESPACE_W3C.toIri(CREDENTIAL_ISSUER_TERM), issuerDid)
                    .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(CREDENTIALS_TERM), credentialsArray)
                    .build();
        }

        private JsonObject createCredentialObject(String id) {
            return Json.createObjectBuilder()
                    .add(JsonLdKeywords.ID, id)
                    .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(CREDENTIAL_OBJECT_PROFILE_TERM), Json.createArrayBuilder(List.of(TEST_PROFILE)))
                    .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(CREDENTIAL_OBJECT_OFFER_REASON_TERM), "reissuance")
                    .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(CREDENTIAL_OBJECT_CREDENTIAL_TYPE_TERM), TEST_CREDENTIAL_TYPE)
                    .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(CREDENTIAL_OBJECT_BINDING_METHODS_TERM), Json.createArrayBuilder(List.of("did:web")))
                    .build();
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
                .registerServiceMock(DidPublicKeyResolver.class, DID_PUBLIC_KEY_RESOLVER);

        @RegisterExtension
        static final RuntimeExtension ISSUER_EXTENSION = ComponentRuntimeExtension.Builder.newInstance()
                .name(ISSUER_RUNTIME_NAME)
                .modules(DefaultRuntimes.Issuer.MODULES)
                .endpoints(DefaultRuntimes.Issuer.ENDPOINTS.build())
                .configurationProvider(DefaultRuntimes.Issuer::config)
                .paramProvider(IssuerService.class, IssuerService::forContext)
                .build();

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
            POSTGRESQL_EXTENSION.createDatabase(IDENTITY_HUB);
        };
        @Order(2)
        @RegisterExtension
        static final RuntimeExtension IDENTITY_HUB_EXTENSION = ComponentRuntimeExtension.Builder.newInstance()
                .name(IH_RUNTIME_NAME)
                .modules(DefaultRuntimes.IdentityHub.SQL_MODULES)
                .endpoints(DefaultRuntimes.IdentityHub.ENDPOINTS.build())
                .configurationProvider(DefaultRuntimes.IdentityHub::config)
                .configurationProvider(() -> POSTGRESQL_EXTENSION.configFor(IDENTITY_HUB))
                .paramProvider(IdentityHub.class, IdentityHub::forContext)
                .build()
                .registerServiceMock(DidPublicKeyResolver.class, DID_PUBLIC_KEY_RESOLVER);
        @Order(2)
        @RegisterExtension
        static final RuntimeExtension ISSUER_EXTENSION = ComponentRuntimeExtension.Builder.newInstance()
                .name(ISSUER_RUNTIME_NAME)
                .modules(DefaultRuntimes.Issuer.SQL_MODULES)
                .endpoints(DefaultRuntimes.Issuer.ENDPOINTS.build())
                .configurationProvider(DefaultRuntimes.Issuer::config)
                .configurationProvider(() -> POSTGRESQL_EXTENSION.configFor(ISSUER))
                .paramProvider(IssuerService.class, IssuerService::forContext)
                .build();


    }
}
