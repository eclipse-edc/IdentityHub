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
import org.eclipse.edc.identityhub.tests.fixtures.allinone.AllInOneExtension;
import org.eclipse.edc.identityhub.tests.fixtures.allinone.AllInOneRuntime;
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
import static org.eclipse.edc.identityhub.tests.dcp.TestData.IH_RUNTIME_MEM_MODULES;
import static org.eclipse.edc.identityhub.tests.dcp.TestData.IH_RUNTIME_SQL_MODULES;
import static org.eclipse.edc.identityhub.tests.dcp.TestData.ISSUER_RUNTIME_MEM_MODULES;
import static org.eclipse.edc.identityhub.tests.dcp.TestData.ISSUER_RUNTIME_SQL_MODULES;
import static org.eclipse.edc.identityhub.tests.fixtures.common.TestFunctions.base64Encode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("JUnitMalformedDeclaration")
public class DcpIssuanceFlowAllInOneTest {


    protected static final AttestationSourceFactory ATTESTATION_SOURCE_FACTORY = mock();

    protected static final Duration TIMEOUT = Duration.ofSeconds(60);
    protected static final Duration INTERVAL = Duration.ofSeconds(1);

    abstract static class Tests {

        protected static final String ISSUER_ID = "issuer";
        protected static final String PARTICIPANT_ID = "issuer"; //issuer and participant use the same ID -> issue to self
        private static String participantToken;
        private static String issuerDid;
        private static String participantDid;

        @BeforeAll
        static void beforeAll(AllInOneRuntime runtime) {
            var pipelineFactory = runtime.getService(AttestationSourceFactoryRegistry.class);
            var validationRegistry = runtime.getService(AttestationDefinitionValidatorRegistry.class);
            pipelineFactory.registerFactory("Attestation", ATTESTATION_SOURCE_FACTORY);
            validationRegistry.registerValidator("Attestation", def -> ValidationResult.success());

            // Create an issuer
            issuerDid = runtime.didFor(ISSUER_ID);
            var response = runtime.createIssuerParticipant(ISSUER_ID, issuerDid, issuerDid + "#key");

            // Create a participant and store the token
            participantDid = runtime.didFor(PARTICIPANT_ID);
            participantToken = response.apiKey();

            // seed attestations, credential definitions, rules, mappings etc. to the Issuer
            prepareIssuer(runtime);

        }

        /**
         * Prepares the issuer facet by creating rules and mappings, credential definitions and a mocked attestation source
         *
         * @param allInOneRuntime the runtime that implements the issuer endpoint etc.
         */
        private static void prepareIssuer(AllInOneRuntime allInOneRuntime) {
            var attestationDefinition = createRulesAndMappingsInIssuer(allInOneRuntime, Map.of(
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
        private static @NotNull AttestationDefinition createRulesAndMappingsInIssuer(AllInOneRuntime issuer, Map<String, Object> ruleConfiguration, MappingDefinition mappingDefinition) {
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

        @Test
        void testCredentialIssuance(AllInOneRuntime allInOneRuntime) {

            var holderRequestId = "test-request-id";
            var issuanceRequest = """
                    {
                      "issuerDid": "%s",
                      "holderPid": "%s",
                      "credentials": [{ "format": "VC1_0_JWT", "id": "membershipCredential-id", "type": "MembershipCredential" }]
                    }
                    """.formatted(issuerDid, holderRequestId);

            allInOneRuntime.getIdentityEndpoint().baseRequest()
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
                    .untilAsserted(() -> assertThat(allInOneRuntime.getCredentialRequestForParticipant(PARTICIPANT_ID)).hasSize(1)
                            .allSatisfy(t -> {
                                assertThat(t.getState()).isEqualTo(HolderRequestState.ISSUED.code());
                                assertThat(t.getHolderPid()).isEqualTo(holderRequestId);
                            }));

            // wait for the issuance process to be delivered on the issuer side
            await().pollInterval(INTERVAL)
                    .atMost(TIMEOUT)
                    .untilAsserted(() -> assertThat(allInOneRuntime.getIssuanceProcessesForParticipant(ISSUER_ID)).hasSize(1)
                            .allSatisfy(t -> {
                                assertThat(t.getHolderPid()).isEqualTo(holderRequestId);
                                assertThat(t.getState()).isEqualTo(IssuanceProcessStates.DELIVERED.code());
                            }));


            // checks that the credential was issued correctly: we expect 1 status list credential, 1 "holder" credential and 1 tracked issuance
            assertThat(allInOneRuntime.getCredentialsForParticipant(ISSUER_ID))
                    .hasSize(3)
                    .anySatisfy(vc -> {
                        assertThat(vc.getStateAsEnum()).isEqualTo(VcStatus.ISSUED);
                        assertThat(vc.getIssuerId()).isEqualTo(issuerDid);
                        assertThat(vc.getHolderId()).isEqualTo(participantDid);
                        assertThat(vc.getVerifiableCredential().credential().getCredentialStatus()).hasSize(1)
                                .allSatisfy(t -> assertThat(t.type()).isEqualTo("BitstringStatusListEntry"));
                    })
                    .anySatisfy(vc -> {
                        assertThat(vc.getVerifiableCredential().rawVc()).isNull(); //that's the credential tracked by the issuer
                    })
                    .anySatisfy(vc -> {
                        assertThat(vc.getStateAsEnum()).isEqualTo(VcStatus.ISSUED);
                        assertThat(vc.getIssuerId()).isEqualTo(issuerDid);
                        assertThat(vc.getHolderId()).isEqualTo(participantDid);
                        assertThat(vc.getVerifiableCredential().credential().getCredentialStatus()).isNotEmpty()
                                .anySatisfy(t -> {
                                    assertThat(t.getProperty("", "statusPurpose").toString()).isEqualTo("revocation");
                                });
                    });

            // verify that the status credential on the issuer side is accessible
            assertThat(allInOneRuntime.getCredentialsForParticipant(ISSUER_ID))
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

        @Test
        void testRenewal(AllInOneRuntime allInOneRuntime) {
            var holderRequestId = "test-request-id";
            var issuanceRequest = """
                    {
                      "issuerDid": "%s",
                      "holderPid": "%s",
                      "credentials": [{ "format": "VC1_0_JWT", "id": "membershipCredential-id", "type": "MembershipCredential" }]
                    }
                    """.formatted(issuerDid, holderRequestId);

            allInOneRuntime.getIdentityEndpoint().baseRequest()
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
                    .untilAsserted(() -> assertThat(allInOneRuntime.getCredentialRequestForParticipant(PARTICIPANT_ID)).hasSize(1)
                            .allSatisfy(t -> {
                                assertThat(t.getState()).isEqualTo(HolderRequestState.ISSUED.code());
                                assertThat(t.getHolderPid()).isEqualTo(holderRequestId);
                            }));

            await().atMost(Duration.ofHours(10)).until(() -> false);

        }

    }

    @Nested
    @EndToEndTest
    class InMemory extends Tests {

        @RegisterExtension
        static final AllInOneExtension IDENTITY_HUB_EXTENSION = AllInOneExtension.Builder.newInstance()
                .id("all-in-one-runtime")
                .name("all-in-one-runtime")
                .module(IH_RUNTIME_MEM_MODULES[0])
                .module(ISSUER_RUNTIME_MEM_MODULES[0])
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
        static final AllInOneExtension IDENTITY_HUB_EXTENSION = AllInOneExtension.Builder.newInstance()
                .id("all-in-one-runtime")
                .name("all-in-one-runtime")
                .modules(new String[]{
                        IH_RUNTIME_SQL_MODULES[0],
                        IH_RUNTIME_SQL_MODULES[1],
                        ISSUER_RUNTIME_SQL_MODULES[0],
                        ISSUER_RUNTIME_SQL_MODULES[1]
                })
                .configurationProvider(() -> POSTGRESQL_EXTENSION.configFor(ISSUER))
                .build();
        @Order(1) // must be the first extension to be evaluated since it starts the DB server
        @RegisterExtension
        static final BeforeAllCallback POSTGRES_CONTAINER_STARTER = context -> {
            POSTGRESQL_EXTENSION.createDatabase(ISSUER);
        };
    }
}
