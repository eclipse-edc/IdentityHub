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

import io.restassured.http.Header;
import org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus;
import org.eclipse.edc.identityhub.tests.fixtures.credentialservice.IdentityHubExtension;
import org.eclipse.edc.identityhub.tests.fixtures.credentialservice.IdentityHubRuntime;
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
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.spi.result.Result;
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

import java.time.Duration;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat.VC1_0_JWT;
import static org.eclipse.edc.identityhub.tests.dcp.TestData.IH_RUNTIME_ID;
import static org.eclipse.edc.identityhub.tests.dcp.TestData.IH_RUNTIME_MEM_MODULES;
import static org.eclipse.edc.identityhub.tests.dcp.TestData.IH_RUNTIME_NAME;
import static org.eclipse.edc.identityhub.tests.dcp.TestData.IH_RUNTIME_SQL_MODULES;
import static org.eclipse.edc.identityhub.tests.dcp.TestData.ISSUER_RUNTIME_ID;
import static org.eclipse.edc.identityhub.tests.dcp.TestData.ISSUER_RUNTIME_MEM_MODULES;
import static org.eclipse.edc.identityhub.tests.dcp.TestData.ISSUER_RUNTIME_NAME;
import static org.eclipse.edc.identityhub.tests.dcp.TestData.ISSUER_RUNTIME_SQL_MODULES;
import static org.eclipse.edc.identityhub.tests.fixtures.common.TestFunctions.base64Encode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("JUnitMalformedDeclaration")
public class DcpIssuanceFlowEndToEndTest {


    protected static final AttestationSourceFactory ATTESTATION_SOURCE_FACTORY = mock();

    protected static final Duration TIMEOUT = Duration.ofSeconds(60);
    protected static final Duration INTERVAL = Duration.ofSeconds(1);

    abstract static class Tests {

        protected static final String ISSUER_ID = "issuer";
        protected static final String PARTICIPANT_ID = "user1";

        private static String participantToken;
        private static String issuerDid;
        private static String participantDid;

        @BeforeAll
        static void beforeAll(IssuerRuntime issuer, IdentityHubRuntime credentialService) {
            var pipelineFactory = issuer.getService(AttestationSourceFactoryRegistry.class);
            var validationRegistry = issuer.getService(AttestationDefinitionValidatorRegistry.class);
            pipelineFactory.registerFactory("Attestation", ATTESTATION_SOURCE_FACTORY);
            validationRegistry.registerValidator("Attestation", def -> ValidationResult.success());

            // Create an issuer
            issuerDid = issuer.didFor(ISSUER_ID);
            issuer.createParticipant(ISSUER_ID, issuerDid, issuerDid + "#key");

            // Create a participant and store the token
            participantDid = credentialService.didFor(PARTICIPANT_ID);
            participantToken = credentialService.createParticipant(PARTICIPANT_ID, participantDid, participantDid + "#key").apiKey();
        }

        @Test
        void issuanceFlow(IssuerRuntime issuer, IdentityHubRuntime identityHub) {

            var mappingDefinition = new MappingDefinition("participant.name", "credentialSubject.name", true);
            var attestationDefinition = setupIssuer(issuer, Map.of(
                    "claim", "onboarding.signedDocuments",
                    "operator", "eq",
                    "value", true), mappingDefinition);

            var attestationSource = mock(AttestationSource.class);
            when(ATTESTATION_SOURCE_FACTORY.createSource(refEq(attestationDefinition))).thenReturn(attestationSource);
            when(attestationSource.execute(any()))
                    .thenReturn(Result.success(Map.of("onboarding", Map.of("signedDocuments", true), "participant", Map.of("name", "Alice"))));

            var request = """
                    {
                      "issuerDid": "%s",
                      "holderPid": "test-request-id",
                      "credentials": [{ "format": "VC1_0_JWT", "id": "membershipCredential-id", "type": "MembershipCredential" }]
                    }
                    """.formatted(issuerDid);

            identityHub.getIdentityEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", participantToken))
                    .body(request)
                    .post("/v1alpha/participants/%s/credentials/request".formatted(base64Encode(PARTICIPANT_ID)))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(201)
                    .header("Location", Matchers.endsWith("/credentials/request/test-request-id"));

            // wait for the request status to be requested on the holder side
            await().pollInterval(INTERVAL)
                    .atMost(TIMEOUT)
                    .untilAsserted(() -> assertThat(identityHub.getCredentialRequestForParticipant(PARTICIPANT_ID)).hasSize(1)
                            .allSatisfy(t -> {
                                assertThat(t.getState()).isEqualTo(HolderRequestState.ISSUED.code());
                                assertThat(t.getHolderPid()).isEqualTo("test-request-id");
                            }));

            // wait for the issuance process to be delivered on the issuer side
            await().pollInterval(INTERVAL)
                    .atMost(TIMEOUT)
                    .untilAsserted(() -> assertThat(issuer.getIssuanceProcessesForParticipant(ISSUER_ID)).hasSize(1)
                            .allSatisfy(t -> {
                                assertThat(t.getHolderPid()).isEqualTo("test-request-id");
                                assertThat(t.getState()).isEqualTo(IssuanceProcessStates.DELIVERED.code());
                            }));

            // checks that the credential was issued on the holder side
            assertThat(identityHub.getCredentialsForParticipant(PARTICIPANT_ID))
                    .hasSize(1)
                    .allSatisfy(vc -> {
                        assertThat(vc.getStateAsEnum()).isEqualTo(VcStatus.ISSUED);
                        assertThat(vc.getIssuerId()).isEqualTo(issuerDid);
                        assertThat(vc.getHolderId()).isEqualTo(participantDid);
                        assertThat(vc.getVerifiableCredential().credential().getCredentialStatus()).isNotEmpty()
                                .anySatisfy(t -> {
                                    assertThat(t.getProperty("", "statusPurpose").toString()).isEqualTo("revocation");
                                });
                    });

            // checks that the credential was issued on the issuer side
            assertThat(issuer.getCredentialsForParticipant(ISSUER_ID))
                    .hasSize(2)
                    .anySatisfy(vc -> {
                        assertThat(vc.getStateAsEnum()).isEqualTo(VcStatus.ISSUED);
                        assertThat(vc.getIssuerId()).isEqualTo(issuerDid);
                        assertThat(vc.getHolderId()).isEqualTo(participantDid);
                        assertThat(vc.getVerifiableCredential().credential().getCredentialStatus()).hasSize(1)
                                .allSatisfy(t -> assertThat(t.type()).isEqualTo("BitstringStatusListEntry"));
                    });

            // verify that the status credential on the issuer side is accessible
            assertThat(issuer.getCredentialsForParticipant(ISSUER_ID))
                    .anySatisfy(vc -> {
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

        /**
         * Setup the issuer with an attestation definition and a credential definition
         */
        private @NotNull AttestationDefinition setupIssuer(IssuerRuntime issuer, Map<String, Object> ruleConfiguration, MappingDefinition mappingDefinition) {
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
                    .validity(Duration.ofDays(365).toSeconds()) // one year
                    .mapping(mappingDefinition)
                    .rule(new CredentialRuleDefinition("expression", ruleConfiguration))
                    .participantContextId("participantContextId")
                    .formatFrom(VC1_0_JWT)
                    .build();

            credentialDefinitionService.createCredentialDefinition(credentialDefinition);
            return attestationDefinition;
        }


    }

    @Nested
    @EndToEndTest
    class InMemory extends Tests {

        @RegisterExtension
        static final IdentityHubExtension IDENTITY_HUB_EXTENSION = IdentityHubExtension.Builder.newInstance()
                .id(IH_RUNTIME_ID)
                .name(IH_RUNTIME_NAME)
                .modules(IH_RUNTIME_MEM_MODULES)
                .build();

        @RegisterExtension
        static final IssuerExtension ISSUER_EXTENSION = IssuerExtension.Builder.newInstance()
                .id(ISSUER_RUNTIME_ID)
                .name(ISSUER_RUNTIME_NAME)
                .modules(ISSUER_RUNTIME_MEM_MODULES)
                .build();

    }

    @Nested
    @PostgresqlIntegrationTest
    class Postgres extends Tests {

        @Order(0)
        @RegisterExtension
        static final PostgresqlEndToEndExtension POSTGRESQL_EXTENSION = new PostgresqlEndToEndExtension();
        private static final String ISSUER = "issuer";

        @Order(2)
        @RegisterExtension
        static final IssuerExtension ISSUER_EXTENSION = IssuerExtension.Builder.newInstance()
                .id(ISSUER_RUNTIME_ID)
                .name(ISSUER_RUNTIME_NAME)
                .modules(ISSUER_RUNTIME_SQL_MODULES)
                .configurationProvider(() -> POSTGRESQL_EXTENSION.configFor(ISSUER))
                .build();

        private static final String IDENTITY_HUB = "identityhub";
        @Order(1) // must be the first extension to be evaluated since it starts the DB server
        @RegisterExtension
        static final BeforeAllCallback POSTGRES_CONTAINER_STARTER = context -> {
            POSTGRESQL_EXTENSION.createDatabase(ISSUER);
            POSTGRESQL_EXTENSION.createDatabase(IDENTITY_HUB);
        };

        @Order(2)
        @RegisterExtension
        static final IdentityHubExtension IDENTITY_HUB_EXTENSION = IdentityHubExtension.Builder.newInstance()
                .id(IH_RUNTIME_ID)
                .name(IH_RUNTIME_NAME)
                .modules(IH_RUNTIME_SQL_MODULES)
                .configurationProvider(() -> POSTGRESQL_EXTENSION.configFor(IDENTITY_HUB))
                .build();


    }
}
