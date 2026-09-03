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

package org.eclipse.edc.identityhub.tests.dcp.flow;

import io.restassured.http.Header;
import org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialOfferStatus;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialOfferStore;
import org.eclipse.edc.identityhub.tests.fixtures.DefaultRuntimes;
import org.eclipse.edc.identityhub.tests.fixtures.credentialservice.IdentityHub;
import org.eclipse.edc.identityhub.tests.fixtures.issuerservice.IssuerService;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionValidatorRegistry;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationSource;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationSourceFactory;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationSourceFactoryRegistry;
import org.eclipse.edc.issuerservice.spi.issuance.model.AttestationDefinition;
import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialDefinition;
import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialRuleDefinition;
import org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcessStates;
import org.eclipse.edc.issuerservice.spi.issuance.model.MappingDefinition;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat.VC2_0_JOSE;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject.OFFER_REASON_PROOF_KEY_REVOCATION;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject.OFFER_REASON_REISSUE;
import static org.eclipse.edc.identityhub.tests.dcp.TestData.IH_RUNTIME_NAME;
import static org.eclipse.edc.identityhub.tests.dcp.TestData.ISSUER_RUNTIME_NAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the credential offer flow as it is triggered through the Issuer Admin API
 * ({@code IssuerCredentialsAdminApiController#sendCredentialOffer}), as opposed to
 * {@code DcpCredentialOfferApiEndToEndTest}, which posts the {@code CredentialOfferMessage} directly to the holder's
 * DCP offer endpoint.
 * <p>
 * The flow covered here is: Issuer Admin API -&gt; CredentialOfferMessage over DCP -&gt; holder's offer endpoint -&gt;
 * credential request back to the Issuer.
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class DcpCredentialOfferFlowEndToEndTest {

    protected static final AttestationSourceFactory ATTESTATION_SOURCE_FACTORY = mock();
    private static final String ISSUER = "issuer";
    private static final String IDENTITY_HUB = "identityhub";

    abstract static class Tests {

        protected static final String ISSUER_ID = "issuer";
        // must be "user1", because the DefaultRuntimes.IdentityHub config expects the "user1-alias" private key alias
        protected static final String PARTICIPANT_ID = "user1";
        protected static final String TEST_PROFILE = "vc20-bssl/jwt";
        protected static final Duration TIMEOUT = Duration.ofSeconds(60);
        protected static final Duration INTERVAL = Duration.ofSeconds(1);

        private static String issuerDid;
        private static String participantDid;
        private static Header issuerAdminAuthHeader;

        @BeforeAll
        static void beforeAll(IssuerService issuer, IdentityHub identityHub) {
            issuer.getService(AttestationSourceFactoryRegistry.class).registerFactory("Attestation", ATTESTATION_SOURCE_FACTORY);
            issuer.getService(AttestationDefinitionValidatorRegistry.class).registerValidator("Attestation", def -> ValidationResult.success());

            issuerDid = issuer.didFor(ISSUER_ID);
            var issuerParticipant = issuer.createParticipant(ISSUER_ID, issuerDid, issuerDid + "#key");
            issuerAdminAuthHeader = new Header("x-api-key", issuerParticipant.apiKey());

            participantDid = identityHub.didFor(PARTICIPANT_ID);
            identityHub.createParticipant(PARTICIPANT_ID, participantDid, participantDid + "#key");

            // the holder record the Issuer Admin API refers to by its holderId
            issuer.createHolder(ISSUER_ID, PARTICIPANT_ID, participantDid, "Participant");
        }

        @AfterEach
        void teardown(IdentityHub identityHub) {
            var credentialOfferStore = identityHub.getService(CredentialOfferStore.class);
            credentialOfferStore.query(QuerySpec.max())
                    .forEach(offer -> credentialOfferStore.deleteById(offer.getId()).orElseThrow(f -> new RuntimeException(f.getFailureDetail())));
        }

        @Test
        void sendCredentialOffer(IssuerService issuer, IdentityHub identityHub) {
            var credentialDefinitionId = "credential-definition-" + UUID.randomUUID();
            var credentialType = "MembershipCredential_" + UUID.randomUUID();
            setupIssuer(issuer, credentialDefinitionId, credentialType);

            issuer.getAdminEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(issuerAdminAuthHeader)
                    .body(offerRequestBody(credentialDefinitionId, null))
                    .post("/v1/participants/%s/credentials/offer".formatted(ISSUER_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(204);

            // the offer must have reached the holder, and must have been processed into a credential request
            var credentialOfferStore = identityHub.getService(CredentialOfferStore.class);
            await().pollInterval(INTERVAL).atMost(TIMEOUT).untilAsserted(() -> {
                assertThat(credentialOfferStore.query(QuerySpec.max()))
                        .hasSize(1)
                        .allSatisfy(offer -> {
                            assertThat(offer.getStateAsEnum()).isEqualTo(CredentialOfferStatus.PROCESSED);
                            assertThat(offer.getParticipantContextId()).isEqualTo(PARTICIPANT_ID);
                            assertThat(offer.issuer()).isEqualTo(issuerDid);
                            assertThat(offer.getCredentialObjects()).hasSize(1)
                                    .allSatisfy(credentialObject -> {
                                        assertThat(credentialObject.getId()).isEqualTo(credentialDefinitionId);
                                        assertThat(credentialObject.getCredentialType()).isEqualTo(credentialType);
                                        assertThat(credentialObject.getProfile()).isEqualTo(TEST_PROFILE);
                                        // no offerReason was given in the API request, so it defaults to "reissue"
                                        assertThat(credentialObject.getOfferReason()).isEqualTo(OFFER_REASON_REISSUE);
                                        assertThat(credentialObject.getBindingMethods()).containsExactly("did:web");
                                    });
                        });

                assertThat(identityHub.getCredentialRequestForParticipant(PARTICIPANT_ID))
                        .anySatisfy(request -> {
                            assertThat(request.getIssuerDid()).isEqualTo(issuerDid);
                            assertThat(request.getIdsAndFormats()).hasSize(1)
                                    .allSatisfy(requestedCredential -> {
                                        assertThat(requestedCredential.id()).isEqualTo(credentialDefinitionId);
                                        assertThat(requestedCredential.credentialType()).isEqualTo(credentialType);
                                        assertThat(requestedCredential.format()).isEqualTo(VC2_0_JOSE.name());
                                    });
                            assertThat(request.getState()).isEqualTo(HolderRequestState.ISSUED.code());
                        });
            });

            // the issuance triggered by the offer must have completed on the issuer side
            await().pollInterval(INTERVAL).atMost(TIMEOUT).untilAsserted(() ->
                    assertThat(issuer.getIssuanceProcessesForParticipant(ISSUER_ID))
                            .filteredOn(process -> process.getCredentialDefinitions().contains(credentialDefinitionId))
                            .hasSize(1)
                            .allSatisfy(process -> {
                                assertThat(process.getHolderId()).isEqualTo(PARTICIPANT_ID);
                                assertThat(process.getState()).isEqualTo(IssuanceProcessStates.DELIVERED.code());
                            }));

            // ... and the credential must be stored on the holder side
            assertThat(identityHub.getCredentialsForParticipant(PARTICIPANT_ID))
                    .anySatisfy(credential -> {
                        assertThat(credential.getStateAsEnum()).isEqualTo(VcStatus.ISSUED);
                        assertThat(credential.getIssuerId()).isEqualTo(issuerDid);
                        assertThat(credential.getHolderId()).isEqualTo(participantDid);
                        assertThat(credential.getVerifiableCredential().credential().getType()).contains(credentialType);
                    });
        }

        @Test
        void sendCredentialOffer_withOfferReason(IssuerService issuer, IdentityHub identityHub) {
            var credentialDefinitionId = "credential-definition-" + UUID.randomUUID();
            var credentialType = "MembershipCredential_" + UUID.randomUUID();
            setupIssuer(issuer, credentialDefinitionId, credentialType);

            issuer.getAdminEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(issuerAdminAuthHeader)
                    .body(offerRequestBody(credentialDefinitionId, OFFER_REASON_PROOF_KEY_REVOCATION))
                    .post("/v1/participants/%s/credentials/offer".formatted(ISSUER_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(204);

            // the offer reason given in the API request must be carried through to the holder
            var credentialOfferStore = identityHub.getService(CredentialOfferStore.class);
            await().pollInterval(INTERVAL).atMost(TIMEOUT).untilAsserted(() ->
                    assertThat(credentialOfferStore.query(QuerySpec.max()))
                            .hasSize(1)
                            .allSatisfy(offer -> assertThat(offer.getCredentialObjects())
                                    .hasSize(1)
                                    .allSatisfy(credentialObject -> {
                                        assertThat(credentialObject.getId()).isEqualTo(credentialDefinitionId);
                                        assertThat(credentialObject.getOfferReason()).isEqualTo(OFFER_REASON_PROOF_KEY_REVOCATION);
                                    })));
        }

        @Test
        void sendCredentialOffer_credentialDefinitionNotFound_shouldReturn400(IssuerService issuer, IdentityHub identityHub) {
            issuer.getAdminEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(issuerAdminAuthHeader)
                    .body(offerRequestBody("not-exists", null))
                    .post("/v1/participants/%s/credentials/offer".formatted(ISSUER_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400);

            assertThat(identityHub.getService(CredentialOfferStore.class).query(QuerySpec.max())).isEmpty();
        }

        @Test
        void sendCredentialOffer_holderNotFound_shouldReturn404(IssuerService issuer) {
            issuer.getAdminEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(issuerAdminAuthHeader)
                    .body("""
                            {
                              "holderId": "not-exists",
                              "credentials": ["some-credential-definition"]
                            }
                            """)
                    .post("/v1/participants/%s/credentials/offer".formatted(ISSUER_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(404);
        }

        private String offerRequestBody(String credentialDefinitionId, String offerReason) {
            var reason = offerReason == null ? "" : ",\n  \"offerReason\": \"%s\"".formatted(offerReason);
            return """
                    {
                      "holderId": "%s",
                      "credentials": ["%s"]%s
                    }
                    """.formatted(PARTICIPANT_ID, credentialDefinitionId, reason);
        }

        /**
         * Creates an attestation definition and a credential definition in the Issuer's participant context. The
         * credential definition is what the Credential Offer API refers to by its ID, because credential offers are
         * assembled from the Issuer's metadata.
         */
        private void setupIssuer(IssuerService issuer, String credentialDefinitionId, String credentialType) {
            var attestationDefinition = AttestationDefinition.Builder.newInstance()
                    .id("attestation-" + UUID.randomUUID())
                    .attestationType("Attestation")
                    .participantContextId(ISSUER_ID)
                    .configuration(Map.of())
                    .build();
            issuer.createAttestationDefinition(attestationDefinition);

            issuer.createCredentialDefinition(CredentialDefinition.Builder.newInstance()
                    .id(credentialDefinitionId)
                    .credentialType(credentialType)
                    .jsonSchemaUrl("https://example.com/schema")
                    .jsonSchema("{}")
                    .attestation(attestationDefinition.getId())
                    .validity(Duration.ofDays(365).toSeconds())
                    .mappings(List.of(new MappingDefinition("participant.name", "credentialSubject.name", true),
                            new MappingDefinition("participant.id", "credentialSubject.id", true)))
                    .rule(new CredentialRuleDefinition("expression", Map.of(
                            "claim", "onboarding.signedDocuments",
                            "operator", "eq",
                            "value", true)))
                    .participantContextId(ISSUER_ID)
                    .formatFrom(VC2_0_JOSE)
                    .build());

            var attestationSource = mock(AttestationSource.class);
            when(ATTESTATION_SOURCE_FACTORY.createSource(refEq(attestationDefinition))).thenReturn(attestationSource);
            when(attestationSource.execute(any())).thenReturn(Result.success(Map.of(
                    "onboarding", Map.of("signedDocuments", true),
                    "participant", Map.of("name", "Alice", "id", participantDid))));
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
                .build();
    }

    @Nested
    @PostgresqlIntegrationTest
    class Postgres extends Tests {

        @Order(0)
        @RegisterExtension
        static final PostgresqlEndToEndExtension POSTGRESQL_EXTENSION = new PostgresqlEndToEndExtension();
        @Order(1) // must be evaluated before the runtimes, since it creates the databases
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
                .build();
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
