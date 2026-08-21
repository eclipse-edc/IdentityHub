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
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus;
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
import org.eclipse.edc.issuerservice.spi.issuance.events.CredentialDelivered;
import org.eclipse.edc.issuerservice.spi.issuance.events.CredentialGenerated;
import org.eclipse.edc.issuerservice.spi.issuance.events.IssuanceApproved;
import org.eclipse.edc.issuerservice.spi.issuance.events.IssuanceEvent;
import org.eclipse.edc.issuerservice.spi.issuance.events.IssuanceRequested;
import org.eclipse.edc.issuerservice.spi.issuance.model.AttestationDefinition;
import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialDefinition;
import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialRuleDefinition;
import org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcessStates;
import org.eclipse.edc.issuerservice.spi.issuance.model.MappingDefinition;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat.VC1_0_JWT;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat.VC2_0_JOSE;
import static org.eclipse.edc.identityhub.tests.dcp.TestData.IH_RUNTIME_NAME;
import static org.eclipse.edc.identityhub.tests.dcp.TestData.ISSUER_RUNTIME_NAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.inOrder;
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
        static void beforeAll(IssuerService issuer, IdentityHub credentialService) {
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

        @ParameterizedTest
        @ArgumentsSource(CredentialFormatProvider.class)
        void issuanceFlow(CredentialFormat format, String credentialType, IssuerService issuer, IdentityHub identityHub) {

            var subscriber = mock(EventSubscriber.class);
            issuer.registerListener(IssuanceEvent.class, subscriber);

            var nameMapping = new MappingDefinition("participant.name", "credentialSubject.name", true);
            var idMapping = new MappingDefinition("participant.id", "credentialSubject.id", true);
            var credentialNameMapping = new MappingDefinition("participant.credentialName", "name", true);
            var credentialDescMapping = new MappingDefinition("participant.credentialDescription", "description", true);
            var credentialDefinitionId = UUID.randomUUID().toString();
            var attestationDefinition = setupIssuer(issuer, Map.of(
                    "claim", "onboarding.signedDocuments",
                    "operator", "eq",
                    "value", true), List.of(nameMapping, idMapping, credentialNameMapping, credentialDescMapping), format, credentialDefinitionId, credentialType);

            var attestationSource = mock(AttestationSource.class);
            when(ATTESTATION_SOURCE_FACTORY.createSource(refEq(attestationDefinition))).thenReturn(attestationSource);
            when(attestationSource.execute(any()))
                    .thenReturn(Result.success(Map.of("onboarding", Map.of("signedDocuments", true),
                            "participant", Map.of("name", "Alice",
                                    "id", participantDid,
                                    "credentialName", "test-credential-name",
                                    "credentialDescription", "test-credential-description"))));

            var requestId = UUID.randomUUID().toString();
            var request = """
                    {
                      "issuerDid": "%s",
                      "holderPid": "%s",
                      "credentials": [{ "format": "%s", "id": "%s", "type": "%s" }]
                    }
                    """.formatted(issuerDid, requestId, format.name(), credentialDefinitionId, credentialType);

            identityHub.getIdentityEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", participantToken))
                    .body(request)
                    .post("/v1beta/participants/%s/credentials/request".formatted(PARTICIPANT_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(201)
                    .header("Location", Matchers.endsWith("/credentials/request/" + requestId));

            // wait for the request status to be requested on the holder side
            await().pollInterval(INTERVAL)
                    .atMost(TIMEOUT)
                    .untilAsserted(() -> assertThat(identityHub.getCredentialRequestForParticipant(PARTICIPANT_ID))
                            .hasSizeGreaterThanOrEqualTo(1)
                            .anySatisfy(t -> {
                                assertThat(t.getState()).isEqualTo(HolderRequestState.ISSUED.code());
                                assertThat(t.getHolderPid()).isEqualTo(requestId);
                            }));

            // wait for the issuance process to be delivered on the issuer side
            await().pollInterval(INTERVAL)
                    .atMost(TIMEOUT)
                    .untilAsserted(() -> assertThat(issuer.getIssuanceProcessesForParticipant(ISSUER_ID)).hasSizeGreaterThanOrEqualTo(1)
                            .anySatisfy(t -> {
                                assertThat(t.getHolderPid()).isEqualTo(requestId);
                                assertThat(t.getState()).isEqualTo(IssuanceProcessStates.DELIVERED.code());
                            }));

            // checks that the credential was issued on the holder side
            assertThat(identityHub.getCredentialsForParticipant(PARTICIPANT_ID))
                    .hasSizeGreaterThanOrEqualTo(1)
                    .anySatisfy(vc -> {
                        assertThat(vc.getStateAsEnum()).isEqualTo(VcStatus.ISSUED);
                        assertThat(vc.getIssuerId()).isEqualTo(issuerDid);
                        assertThat(vc.getHolderId()).isEqualTo(participantDid);
                        if (format == VC2_0_JOSE) {
                            assertThat(vc.getVerifiableCredential().credential().getName()).isEqualTo("test-credential-name");
                            assertThat(vc.getVerifiableCredential().credential().getDescription()).isEqualTo("test-credential-description");
                        }
                        assertThat(vc.getVerifiableCredential().credential().getCredentialStatus()).isNotEmpty()
                                .anySatisfy(t -> {
                                    assertThat(t.getProperty("", "statusPurpose").toString()).isEqualTo("revocation");
                                });
                    });

            // checks that the credential was issued on the issuer side
            assertThat(issuer.getCredentialsForParticipant(ISSUER_ID))
                    .hasSizeGreaterThanOrEqualTo(2)
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


            var inOrder = inOrder(subscriber);
            inOrder.verify(subscriber).on(argThat(env -> env.getPayload() instanceof IssuanceRequested));
            inOrder.verify(subscriber).on(argThat(env -> env.getPayload() instanceof IssuanceApproved));
            inOrder.verify(subscriber).on(argThat(env -> env.getPayload() instanceof CredentialGenerated));
            inOrder.verify(subscriber).on(argThat(env -> env.getPayload() instanceof CredentialDelivered));
        }

        @Test
        void issuanceFlow_rejectExistingProcessByHolderPid(IssuerService issuer, IdentityHub identityHub) {
            var subscriber = mock(EventSubscriber.class);
            issuer.registerListener(IssuanceEvent.class, subscriber);

            var nameMapping = new MappingDefinition("participant.name", "credentialSubject.name", true);
            var idMapping = new MappingDefinition("participant.id", "credentialSubject.id", true);
            var credentialDefinitionId = UUID.randomUUID().toString();
            var format = VC2_0_JOSE;
            var credentialType = "MembershipCredential_20_" + UUID.randomUUID();

            var attestationDefinition = setupIssuer(issuer, Map.of(
                    "claim", "onboarding.signedDocuments",
                    "operator", "eq",
                    "value", true), List.of(nameMapping, idMapping), format, credentialDefinitionId, credentialType);

            var attestationSource = mock(AttestationSource.class);
            when(ATTESTATION_SOURCE_FACTORY.createSource(refEq(attestationDefinition))).thenReturn(attestationSource);
            when(attestationSource.execute(any()))
                    .thenReturn(Result.success(Map.of("onboarding", Map.of("signedDocuments", true), "participant", Map.of("name", "Alice", "id", participantDid))));

            var requestId = UUID.randomUUID().toString();
            var request = """
                    {
                      "issuerDid": "%s",
                      "holderPid": "%s",
                      "credentials": [{ "format": "%s", "id": "%s", "type": "%s" }]
                    }
                    """.formatted(issuerDid, requestId, format.name(), credentialDefinitionId, credentialType);

            // make first request - expect it to succeed
            identityHub.getIdentityEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", participantToken))
                    .body(request)
                    .post("/v1beta/participants/%s/credentials/request".formatted(PARTICIPANT_ID))
                    .then()
                    .log().all()
                    .statusCode(201);

            // wait for the issuance process to be approved on the issuer side
            await().pollInterval(INTERVAL)
                    .atMost(TIMEOUT)
                    .untilAsserted(() -> assertThat(issuer.getIssuanceProcessesForParticipant(ISSUER_ID)).hasSizeGreaterThanOrEqualTo(1)
                            .anySatisfy(t -> {
                                assertThat(t.getHolderPid()).isEqualTo(requestId);
                                assertThat(t.getState()).isGreaterThanOrEqualTo(IssuanceProcessStates.APPROVED.code());
                            }));

            // make another request with the same holder-PID, expect a 409
            identityHub.getIdentityEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", participantToken))
                    .body(request)
                    .post("/v1beta/participants/%s/credentials/request".formatted(PARTICIPANT_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(409);
        }

        // A2.6: after a successful issuance round trip, the holder's HolderCredentialRequest.issuerPid must equal the issuer's issuance process id as soon as the request reaches REQUESTED
        @Test
        @Disabled("documents intended behavior, not yet implemented (catalog A2.6)")
        void issuanceFlow_issuerPidCorrelatedAfterRequested(IssuerService issuer, IdentityHub identityHub) {
            // arrange: same issuer setup as the happy path (fulfilled attestation, one credential definition)
            var nameMapping = new MappingDefinition("participant.name", "credentialSubject.name", true);
            var idMapping = new MappingDefinition("participant.id", "credentialSubject.id", true);
            var credentialDefinitionId = UUID.randomUUID().toString();
            var credentialType = "MembershipCredential_A26_" + UUID.randomUUID();
            var attestationDefinition = setupIssuer(issuer, Map.of(
                    "claim", "onboarding.signedDocuments",
                    "operator", "eq",
                    "value", true), List.of(nameMapping, idMapping), VC1_0_JWT, credentialDefinitionId, credentialType);

            var attestationSource = mock(AttestationSource.class);
            when(ATTESTATION_SOURCE_FACTORY.createSource(refEq(attestationDefinition))).thenReturn(attestationSource);
            when(attestationSource.execute(any()))
                    .thenReturn(Result.success(Map.of("onboarding", Map.of("signedDocuments", true),
                            "participant", Map.of("name", "Alice", "id", participantDid))));

            // act: initiate the credential request via the Identity API
            var requestId = UUID.randomUUID().toString();
            var request = """
                    {
                      "issuerDid": "%s",
                      "holderPid": "%s",
                      "credentials": [{ "format": "%s", "id": "%s", "type": "%s" }]
                    }
                    """.formatted(issuerDid, requestId, VC1_0_JWT.name(), credentialDefinitionId, credentialType);

            identityHub.getIdentityEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", participantToken))
                    .body(request)
                    .post("/v1beta/participants/%s/credentials/request".formatted(PARTICIPANT_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(201);

            // assert: once the holder request reaches REQUESTED, its issuerPid must equal the issuer-side issuance process id.
            // BUG being documented: the issuer returns the process id only in the 201 Location header, but the holder reads the
            // response BODY as issuerPid, so issuerPid stays empty until the CredentialMessage arrives.
            await().pollInterval(INTERVAL)
                    .atMost(TIMEOUT)
                    .untilAsserted(() -> assertThat(identityHub.getCredentialRequestForParticipant(PARTICIPANT_ID, requestId))
                            .anySatisfy(r -> assertThat(r.getState()).isGreaterThanOrEqualTo(HolderRequestState.REQUESTED.code())));

            // TODO: pause/intercept delivery so this assertion provably runs BEFORE the CredentialMessage arrives (e.g. slow storage endpoint)
            var issuerProcessId = issuer.getIssuanceProcessesForParticipant(ISSUER_ID, requestId).get(0).getId();
            assertThat(identityHub.getCredentialRequestForParticipant(PARTICIPANT_ID, requestId))
                    .anySatisfy(r -> assertThat(r.getIssuerPid()).isEqualTo(issuerProcessId));
        }

        // B1.16 / C8: one CredentialRequestMessage with TWO credentialObjectIds -> single issuance process, ONE CredentialMessage containing both credentials, both stored on the holder and individually correct
        @Test
        @Disabled("TODO: implement (catalog B1.16 / C8)")
        void issuanceFlow_batchRequest_allCredentialsDeliveredInOneMessage(IssuerService issuer, IdentityHub identityHub) {
            // arrange: TWO credential definitions on the issuer, both attestations fulfilled
            var nameMapping = new MappingDefinition("participant.name", "credentialSubject.name", true);
            var idMapping = new MappingDefinition("participant.id", "credentialSubject.id", true);
            var ruleConfiguration = Map.<String, Object>of(
                    "claim", "onboarding.signedDocuments",
                    "operator", "eq",
                    "value", true);
            var definitionId1 = UUID.randomUUID().toString();
            var definitionId2 = UUID.randomUUID().toString();
            var credentialType1 = "MembershipCredential_Batch1_" + UUID.randomUUID();
            var credentialType2 = "MembershipCredential_Batch2_" + UUID.randomUUID();
            var attestationDefinition1 = setupIssuer(issuer, ruleConfiguration, List.of(nameMapping, idMapping), VC1_0_JWT, definitionId1, credentialType1);
            var attestationDefinition2 = setupIssuer(issuer, ruleConfiguration, List.of(nameMapping, idMapping), VC2_0_JOSE, definitionId2, credentialType2);

            var attestationSource = mock(AttestationSource.class);
            when(ATTESTATION_SOURCE_FACTORY.createSource(refEq(attestationDefinition1))).thenReturn(attestationSource);
            when(ATTESTATION_SOURCE_FACTORY.createSource(refEq(attestationDefinition2))).thenReturn(attestationSource);
            when(attestationSource.execute(any()))
                    .thenReturn(Result.success(Map.of("onboarding", Map.of("signedDocuments", true),
                            "participant", Map.of("name", "Alice", "id", participantDid))));

            // act: send ONE request referencing BOTH credentialObjectIds
            var requestId = UUID.randomUUID().toString();
            var request = """
                    {
                      "issuerDid": "%s",
                      "holderPid": "%s",
                      "credentials": [
                        { "format": "%s", "id": "%s", "type": "%s" },
                        { "format": "%s", "id": "%s", "type": "%s" }
                      ]
                    }
                    """.formatted(issuerDid, requestId,
                    VC1_0_JWT.name(), definitionId1, credentialType1,
                    VC2_0_JOSE.name(), definitionId2, credentialType2);

            identityHub.getIdentityEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", participantToken))
                    .body(request)
                    .post("/v1beta/participants/%s/credentials/request".formatted(PARTICIPANT_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(201);

            // assert:
            // TODO: exactly ONE issuance process exists on the issuer for holderPid == requestId, ending in DELIVERED,
            //       and it references BOTH credential definitions (process.getCredentialDefinitions())
            // TODO: the issuer delivers a single CredentialMessage containing both credentials (e.g. verify via a single
            //       CredentialDelivered event / a single Storage API call)
            // TODO: holder request ends in ISSUED; identityHub.getCredentialsForParticipant(PARTICIPANT_ID) contains one
            //       credential per type (credentialType1 as VC1_0_JWT, credentialType2 as VC2_0_JOSE), each individually
            //       correct (issuer DID, holder DID, status list entry)
        }

        // C9 / A2.8 / A6.5: issuance fails AFTER acceptance -> issuer process ERRORED (status API reports REJECTED); the holder must eventually observe the failure and leave REQUESTED
        @Test
        @Disabled("documents intended behavior, not yet implemented (catalog C9 / A2.8 / A6.5)")
        void issuanceFlow_failureAfterAcceptance_holderObservesError(IssuerService issuer, IdentityHub identityHub) {
            // arrange: credential generation is doomed to fail AFTER acceptance: the rule input is present (request gets
            // accepted with 201), but the REQUIRED mapping input "participant.name" is missing from the attestation result,
            // so credential generation fails on the issuer side (alternative arrange: deactivate the signing key)
            var nameMapping = new MappingDefinition("participant.name", "credentialSubject.name", true);
            var credentialDefinitionId = UUID.randomUUID().toString();
            var credentialType = "MembershipCredential_C9_" + UUID.randomUUID();
            var attestationDefinition = setupIssuer(issuer, Map.of(
                    "claim", "onboarding.signedDocuments",
                    "operator", "eq",
                    "value", true), List.of(nameMapping), VC1_0_JWT, credentialDefinitionId, credentialType);

            var attestationSource = mock(AttestationSource.class);
            when(ATTESTATION_SOURCE_FACTORY.createSource(refEq(attestationDefinition))).thenReturn(attestationSource);
            // rule claim present, mapping input "participant" missing -> generation failure after approval
            when(attestationSource.execute(any()))
                    .thenReturn(Result.success(Map.of("onboarding", Map.of("signedDocuments", true))));

            // act: initiate the request; the issuer accepts it before generation fails
            var requestId = UUID.randomUUID().toString();
            var request = """
                    {
                      "issuerDid": "%s",
                      "holderPid": "%s",
                      "credentials": [{ "format": "%s", "id": "%s", "type": "%s" }]
                    }
                    """.formatted(issuerDid, requestId, VC1_0_JWT.name(), credentialDefinitionId, credentialType);

            identityHub.getIdentityEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", participantToken))
                    .body(request)
                    .post("/v1beta/participants/%s/credentials/request".formatted(PARTICIPANT_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(201);

            // assert (issuer side): the process ends in ERRORED after exhausting retries
            await().pollInterval(INTERVAL)
                    .atMost(TIMEOUT)
                    .untilAsserted(() -> assertThat(issuer.getIssuanceProcessesForParticipant(ISSUER_ID, requestId))
                            .anySatisfy(p -> assertThat(p.getState()).isEqualTo(IssuanceProcessStates.ERRORED.code())));

            // TODO: assert the issuer's Credential Request Status API maps ERRORED -> REJECTED:
            //       GET /v1beta/participants/{ISSUER_ID}/requests/{issuerPid} with a holder SI token -> status "REJECTED"

            // assert (holder side): the request must leave REQUESTED and end in an error state.
            // INTENDED behavior: the holder learns of post-acceptance rejection by polling the issuer's status API (A6.5).
            // Today holder-side status polling is not implemented (no processor for REQUESTED), so the request stays in
            // REQUESTED forever (A2.8).
            // TODO: await identityHub.getCredentialRequestForParticipant(PARTICIPANT_ID, requestId) to reach
            //       HolderRequestState.ERROR (or a dedicated rejected state) with the issuer's error detail
        }

        // C12: issuer rotates its signing key between two issuances -> credential 1 remains verifiable (old verificationMethod retained in DID document), credential 2 is signed with the new key
        @Test
        @Disabled("TODO: implement (catalog C12)")
        void issuanceFlow_keyRotation_previouslyIssuedCredentialsRemainVerifiable(IssuerService issuer, IdentityHub identityHub) {
            // arrange: issuer with one credential definition and a fulfilled attestation
            var nameMapping = new MappingDefinition("participant.name", "credentialSubject.name", true);
            var idMapping = new MappingDefinition("participant.id", "credentialSubject.id", true);
            var credentialDefinitionId = UUID.randomUUID().toString();
            var credentialType = "MembershipCredential_C12_" + UUID.randomUUID();
            var attestationDefinition = setupIssuer(issuer, Map.of(
                    "claim", "onboarding.signedDocuments",
                    "operator", "eq",
                    "value", true), List.of(nameMapping, idMapping), VC1_0_JWT, credentialDefinitionId, credentialType);

            var attestationSource = mock(AttestationSource.class);
            when(ATTESTATION_SOURCE_FACTORY.createSource(refEq(attestationDefinition))).thenReturn(attestationSource);
            when(attestationSource.execute(any()))
                    .thenReturn(Result.success(Map.of("onboarding", Map.of("signedDocuments", true),
                            "participant", Map.of("name", "Alice", "id", participantDid))));

            // act 1: full issuance round trip for credential 1
            // TODO: POST credential request 1, await holder request ISSUED, capture credential 1 (raw VC JWT + its kid header)

            // act 2: rotate the issuer's CREDENTIAL_SIGNING key pair
            // TODO: rotate via the issuer runtime's KeyPairService (issuer.getService(...)): create a new active signing key
            //       and rotate the old one WITHOUT revoking it - the old verificationMethod must remain in the DID document

            // act 3: full issuance round trip for credential 2
            // TODO: POST credential request 2, await holder request ISSUED, capture credential 2

            // assert:
            // TODO: credential 2's JWT kid references the NEW key and its signature verifies against the new verificationMethod
            // TODO: credential 1 remains verifiable: its kid still resolves against the issuer's DID document
            //       (old verificationMethod retained after rotation)
        }

        /**
         * Setup the issuer with an attestation definition and a credential definition
         */
        private AttestationDefinition setupIssuer(IssuerService issuer, Map<String, Object> ruleConfiguration, List<MappingDefinition> mappingDefinitions, CredentialFormat credentialFormat, String credentialDefinitionId, String credentialType) {
            var holderService = issuer.getService(HolderService.class);
            var credentialDefinitionService = issuer.getService(CredentialDefinitionService.class);
            var attestationDefinitionService = issuer.getService(AttestationDefinitionService.class);

            holderService.createHolder(Holder.Builder.newInstance().holderId(PARTICIPANT_ID).did(participantDid).holderName("Participant").participantContextId(PARTICIPANT_ID).build());


            var attestationDefinition = AttestationDefinition.Builder.newInstance()
                    .id("attestation-id-%s".formatted(UUID.randomUUID().toString()))
                    .attestationType("Attestation")
                    .participantContextId(PARTICIPANT_ID)
                    .configuration(Map.of())
                    .build();
            attestationDefinitionService.createAttestation(attestationDefinition)
                    .orElseThrow(f -> new AssertionError("Failed to create attestation definition: " + f.getFailureDetail()));


            var credentialDefinition = CredentialDefinition.Builder.newInstance()
                    .id(credentialDefinitionId)
                    .credentialType(credentialType)
                    .additionalContext(List.of("https://example.com/credentials/membership/v1"))
                    .jsonSchemaUrl("https://example.com/schema")
                    .jsonSchema("{}")
                    .attestation(attestationDefinition.getId())
                    .validity(Duration.ofDays(365).toSeconds()) // one year
                    .mappings(mappingDefinitions)
                    .rule(new CredentialRuleDefinition("expression", ruleConfiguration))
                    .participantContextId(PARTICIPANT_ID)
                    .formatFrom(credentialFormat)
                    .build();

            credentialDefinitionService.createCredentialDefinition(credentialDefinition)
                    .orElseThrow(f -> new AssertionError("Failed to create credential definition: " + f.getFailureDetail()));
            return attestationDefinition;
        }

        static class CredentialFormatProvider implements ArgumentsProvider {
            @Override
            public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
                return Stream.of(
                        Arguments.of(VC1_0_JWT, "MembershipCredential_11"),
                        Arguments.of(VC2_0_JOSE, "MembershipCredential_20")
                );
            }
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
                .build();

        @RegisterExtension
        static final RuntimeExtension ISSUER_EXTENSION = ComponentRuntimeExtension.Builder.newInstance()
                .name(ISSUER_RUNTIME_NAME)
                .modules(DefaultRuntimes.Issuer.MODULES)
                .endpoints(DefaultRuntimes.Issuer.ENDPOINTS.build())
                .configurationProvider(DefaultRuntimes.Issuer::config)
                .paramProvider(IssuerService.class, IssuerService::forContext)
                .modules(DefaultRuntimes.Issuer.MODULES)
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
        static final RuntimeExtension ISSUER_EXTENSION = ComponentRuntimeExtension.Builder.newInstance()
                .name(ISSUER_RUNTIME_NAME)
                .modules(DefaultRuntimes.Issuer.SQL_MODULES)
                .endpoints(DefaultRuntimes.Issuer.ENDPOINTS.build())
                .configurationProvider(DefaultRuntimes.Issuer::config)
                .paramProvider(IssuerService.class, IssuerService::forContext)
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
        static final RuntimeExtension IDENTITY_HUB_EXTENSION = ComponentRuntimeExtension.Builder.newInstance()
                .name(IH_RUNTIME_NAME)
                .modules(DefaultRuntimes.IdentityHub.SQL_MODULES)
                .endpoints(DefaultRuntimes.IdentityHub.ENDPOINTS.build())
                .configurationProvider(DefaultRuntimes.IdentityHub::config)
                .paramProvider(IdentityHub.class, IdentityHub::forContext)
                .configurationProvider(() -> POSTGRESQL_EXTENSION.configFor(IDENTITY_HUB))
                .build();


    }
}
