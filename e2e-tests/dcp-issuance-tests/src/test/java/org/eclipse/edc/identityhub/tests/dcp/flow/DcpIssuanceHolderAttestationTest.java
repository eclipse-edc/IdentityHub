/*
 *  Copyright (c) 2025 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.tests.dcp.flow;

import io.restassured.http.ContentType;
import io.restassured.http.Header;
import jakarta.json.Json;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus;
import org.eclipse.edc.identityhub.tests.fixtures.DefaultRuntimes;
import org.eclipse.edc.identityhub.tests.fixtures.credentialservice.IdentityHub;
import org.eclipse.edc.identityhub.tests.fixtures.issuerservice.IssuerService;
import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialDefinition;
import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialRuleDefinition;
import org.eclipse.edc.issuerservice.spi.issuance.model.MappingDefinition;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static io.restassured.http.ContentType.JSON;
import static jakarta.json.Json.createObjectBuilder;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat.VC1_0_JWT;
import static org.eclipse.edc.identityhub.tests.dcp.TestData.IH_RUNTIME_NAME;
import static org.eclipse.edc.identityhub.tests.dcp.TestData.ISSUER_RUNTIME_NAME;
import static org.eclipse.edc.identityhub.tests.fixtures.common.TestFunctions.base64Encode;

public class DcpIssuanceHolderAttestationTest {

    @Nested
    @EndToEndTest
    class InMemory implements Tests {
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
    class Postgres implements Tests {

        private static final String ISSUER = "issuer";
        private static final String IDENTITY_HUB = "identityhub";

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
        static final RuntimeExtension ISSUER_EXTENSION = ComponentRuntimeExtension.Builder.newInstance()
                .name(ISSUER_RUNTIME_NAME)
                .modules(DefaultRuntimes.Issuer.SQL_MODULES)
                .endpoints(DefaultRuntimes.Issuer.ENDPOINTS.build())
                .configurationProvider(DefaultRuntimes.Issuer::config)
                .paramProvider(IssuerService.class, IssuerService::forContext)
                .configurationProvider(() -> POSTGRESQL_EXTENSION.configFor(ISSUER))
                .build();

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

    @SuppressWarnings("JUnitMalformedDeclaration")
    interface Tests {
        @Test
        default void shouldIssueCredentialUsingHolderAttestation(IssuerService issuer, IdentityHub identityHub) {
            var issuerId = UUID.randomUUID().toString();
            var issuerDid = issuer.didFor(issuerId);
            issuer.createParticipant(issuerId, issuerDid, issuerDid + "#key");

            var participantId = UUID.randomUUID().toString();
            var participantDid = identityHub.didFor(participantId);
            var participantToken = identityHub.createParticipant(participantId, participantDid, participantDid + "#key").apiKey();

            var issuerToken = issuer.createParticipant(issuerDid).apiKey();
            issuer.getAdminEndpoint().baseRequest()
                    .header("x-api-key", issuerToken)
                    .contentType(ContentType.JSON)
                    .body(Map.of(
                            "id", "attestation-id",
                            "attestationType", "holder",
                            "configuration", emptyMap()
                    ))
                    .post("/v1alpha/participants/{participantContextId}/attestations", base64Encode(issuerDid))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(201);

            issuer.getAdminEndpoint().baseRequest()
                    .header("x-api-key", issuerToken)
                    .contentType(ContentType.JSON)
                    .body(Map.of(
                            "holderId", participantId,
                            "did", participantDid,
                            "name", "Participant",
                            "properties", Map.of(
                                    "onboarding", Map.of("signedDocuments", true),
                                    "participant", Map.of("name", "Bob")
                            )
                    ))
                    .post("/v1alpha/participants/{participantContextId}/holders", base64Encode(issuerDid))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(201);

            issuer.createCredentialDefinition(CredentialDefinition.Builder.newInstance()
                    .id("membershipCredential-id")
                    .credentialType("MembershipCredential")
                    .jsonSchemaUrl("https://example.com/schema")
                    .jsonSchema("{}")
                    .attestation("attestation-id")
                    .validity(Duration.ofDays(365).toSeconds()) // one year
                    .mapping(new MappingDefinition("participant.name", "credentialSubject.name", true))
                    .rule(new CredentialRuleDefinition("expression", Map.of(
                            "claim", "onboarding.signedDocuments",
                            "operator", "eq",
                            "value", true)))
                    .participantContextId(participantId)
                    .formatFrom(VC1_0_JWT)
                    .build());

            var request = createObjectBuilder()
                    .add("issuerDid", issuerDid)
                    .add("holderPid", "test-request-id")
                    .add("credentials", Json.createArrayBuilder()
                            .add(createObjectBuilder()
                                    .add("format", "VC1_0_JWT")
                                    .add("id", "membershipCredential-id")
                                    .add("type", "MembershipCredential")
                            )
                    )
                    .build();

            identityHub.getIdentityEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", participantToken))
                    .body(request)
                    .post("/v1alpha/participants/%s/credentials/request".formatted(base64Encode(participantId)))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(201)
                    .header("Location", Matchers.endsWith("/credentials/request/test-request-id"));

            await().untilAsserted(() -> assertThat(identityHub.getCredentialsForParticipant(participantId))
                    .hasSize(1)
                    .allSatisfy(vc -> {
                        assertThat(vc.getStateAsEnum()).isEqualTo(VcStatus.ISSUED);
                        assertThat(vc.getIssuerId()).isEqualTo(issuerDid);
                        assertThat(vc.getHolderId()).isEqualTo(participantDid);
                        var credentialSubject = vc.getVerifiableCredential().credential().getCredentialSubject().get(0);
                        assertThat(credentialSubject.getClaims().get("name")).isEqualTo("Bob");
                    }));

            await().untilAsserted(() -> assertThat(issuer.getCredentialsForParticipant(issuerId))
                    .hasSize(2)
                    .anySatisfy(vc -> {
                        assertThat(vc.getStateAsEnum()).isEqualTo(VcStatus.ISSUED);
                        assertThat(vc.getIssuerId()).isEqualTo(issuerDid);
                        assertThat(vc.getHolderId()).isEqualTo(participantDid);
                        var credentialSubject = vc.getVerifiableCredential().credential().getCredentialSubject().get(0);
                        assertThat(credentialSubject.getClaims().get("name")).isEqualTo("Bob");
                    }));

        }
    }

}
