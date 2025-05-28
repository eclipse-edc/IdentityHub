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

package org.eclipse.edc.identityhub.tests;

import io.restassured.http.ContentType;
import io.restassured.http.Header;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.tests.fixtures.issuerservice.IssuerExtension;
import org.eclipse.edc.identityhub.tests.fixtures.issuerservice.IssuerRuntime;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionStore;
import org.eclipse.edc.issuerservice.spi.issuance.credentialdefinition.CredentialDefinitionService;
import org.eclipse.edc.issuerservice.spi.issuance.model.AttestationDefinition;
import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialDefinition;
import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialRuleDefinition;
import org.eclipse.edc.issuerservice.spi.issuance.model.MappingDefinition;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat.VC1_0_JWT;
import static org.eclipse.edc.identityhub.tests.TestData.ISSUER_RUNTIME_ID;
import static org.eclipse.edc.identityhub.tests.TestData.ISSUER_RUNTIME_MEM_MODULES;
import static org.eclipse.edc.identityhub.tests.TestData.ISSUER_RUNTIME_NAME;
import static org.eclipse.edc.identityhub.tests.TestData.ISSUER_RUNTIME_SQL_MODULES;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@SuppressWarnings("JUnitMalformedDeclaration")
public class CredentialDefinitionApiEndToEndTest {
    abstract static class Tests {

        public static final String USER = "user";

        @AfterEach
        void teardown(CredentialDefinitionService service, ParticipantContextService pcService) {
            service.queryCredentialDefinitions(QuerySpec.max()).getContent()
                    .forEach(p -> service.deleteCredentialDefinition(p.getId()).getContent());

            pcService.query(QuerySpec.max()).getContent()
                    .forEach(pc -> pcService.deleteParticipantContext(pc.getParticipantContextId()).getContent());
        }

        @Test
        void createCredentialDefinition(IssuerRuntime runtime, CredentialDefinitionService service, AttestationDefinitionStore store) {
            var token = runtime.createParticipant(USER).apiKey();

            store.create(AttestationDefinition.Builder.newInstance().id("test-attestation").attestationType("type").participantContextId("participantContextId").build());

            Map<String, Object> credentialRuleConfiguration = Map.of(
                    "claim", "onboarding.signedDocuments",
                    "operator", "eq",
                    "value", true);

            var definition = CredentialDefinition.Builder.newInstance()
                    .id("test-definition-id")
                    .jsonSchema("{}")
                    .jsonSchemaUrl("https://example.org/membership-credential-schema.json")
                    .credentialType("MembershipCredential")
                    .mapping(new MappingDefinition("input", "output", true))
                    .validity(1000)
                    .rule(new CredentialRuleDefinition("expression", credentialRuleConfiguration))
                    .attestation("test-attestation")
                    .participantContextId(USER)
                    .format(VC1_0_JWT)
                    .build();

            runtime.getAdminEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header(new Header("x-api-key", token))
                    .body(definition)
                    .post("/v1alpha/participants/%s/credentialdefinitions".formatted(toBase64(USER)))
                    .then()
                    .statusCode(201)
                    .header("Location", Matchers.endsWith("/credentialdefinitions/test-definition-id"));

            assertThat(service.findCredentialDefinitionById(definition.getId())).isSucceeded()
                    .usingRecursiveComparison()
                    .isEqualTo(definition);
        }

        @Test
        void createCredentialDefinition_whenRuleValidationFails(IssuerRuntime runtime, AttestationDefinitionStore store) {
            var token = runtime.createParticipant(USER).apiKey();


            store.create(AttestationDefinition.Builder.newInstance().id("test-attestation").attestationType("type").participantContextId("participantContextId").build());

            var definition = CredentialDefinition.Builder.newInstance()
                    .id("test-definition-id")
                    .jsonSchema("{}")
                    .jsonSchemaUrl("https://example.org/membership-credential-schema.json")
                    .credentialType("MembershipCredential")
                    .mapping(new MappingDefinition("input", "output", true))
                    .validity(1000)
                    .rule(new CredentialRuleDefinition("notFound", Map.of()))
                    .attestation("test-attestation")
                    .participantContextId("participantContextId")
                    .format(VC1_0_JWT)
                    .build();

            runtime.getAdminEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header(new Header("x-api-key", token))
                    .body(definition)
                    .post("/v1alpha/participants/%s/credentialdefinitions".formatted(toBase64(USER)))
                    .then()
                    .statusCode(400);

        }

        @Test
        void createCredentialDefinition_whenExists(IssuerRuntime runtime, CredentialDefinitionService service) {
            var token = runtime.createParticipant(USER).apiKey();

            var definition = CredentialDefinition.Builder.newInstance()
                    .id("test-definition-id")
                    .jsonSchema("{}")
                    .jsonSchemaUrl("https://example.org/membership-credential-schema.json")
                    .credentialType("MyType")
                    .participantContextId("participantContextId")
                    .format(VC1_0_JWT)
                    .build();

            service.createCredentialDefinition(definition);

            runtime.getAdminEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header(new Header("x-api-key", token))
                    .body("""
                            {
                              "id": "test-definition-id",
                              "credentialType": "MembershipCredential",
                              "jsonSchema": "{}",
                              "jsonSchemaUrl": "https://example.org/membership-credential-schema.json",
                              "format": "VC1_0_JWT"
                            }
                            """)
                    .post("/v1alpha/participants/%s/credentialdefinitions".formatted(toBase64(USER)))
                    .then()
                    .log().all()
                    .statusCode(409);
        }

        @Test
        void createCredentialDefinition_whenCredentialTypeExists(IssuerRuntime runtime, CredentialDefinitionService service) {
            var token = runtime.createParticipant(USER).apiKey();

            var definition = CredentialDefinition.Builder.newInstance()
                    .id("id")
                    .jsonSchema("{}")
                    .jsonSchemaUrl("https://example.org/membership-credential-schema.json")
                    .credentialType("MembershipCredential")
                    .participantContextId("participantContextId")
                    .format(VC1_0_JWT)
                    .build();

            service.createCredentialDefinition(definition);

            runtime.getAdminEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header(new Header("x-api-key", token))
                    .body("""
                            {
                              "id": "test-definition-id",
                              "credentialType": "MembershipCredential",
                              "jsonSchema": "{}",
                              "jsonSchemaUrl": "https://example.org/membership-credential-schema.json",
                              "format": "VC1_0_JWT"
                            }
                            """)
                    .post("/v1alpha/participants/%s/credentialdefinitions".formatted(toBase64(USER)))
                    .then()
                    .log().all()
                    .statusCode(409);
        }

        @Test
        void createCredentialDefinition_whenMissingFields(IssuerRuntime runtime) {
            var token = runtime.createParticipant(USER).apiKey();

            runtime.getAdminEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header(new Header("x-api-key", token))
                    .body("""
                            {
                              "id": "test-definition-id"
                            }
                            """)
                    .post("/v1alpha/participants/%s/credentialdefinitions".formatted(toBase64(USER)))
                    .then()
                    .statusCode(400);
        }

        @Test
        void createCredentialDefinition_whenMissingAttestations(IssuerRuntime runtime) {
            var token = runtime.createParticipant(USER).apiKey();

            runtime.getAdminEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header(new Header("x-api-key", token))
                    .body("""
                            {
                              "id": "test-definition-id",
                              "credentialType": "MembershipCredential",
                              "jsonSchema": "{}",
                              "jsonSchemaUrl": "https://example.org/membership-credential-schema.json",
                              "attestations": ["notfound"],
                              "format": "VC1_0_JWT"
                            }
                            """)
                    .post("/v1alpha/participants/%s/credentialdefinitions".formatted(toBase64(USER)))
                    .then()
                    .statusCode(400)
                    .body("[0].message", containsString("notfound"));
        }


        @Test
        void queryCredentialDefinitions(IssuerRuntime runtime, CredentialDefinitionService service) {
            var token = runtime.createParticipant(USER).apiKey();

            var definition = CredentialDefinition.Builder.newInstance()
                    .id("id")
                    .jsonSchema("{}")
                    .jsonSchemaUrl("http://example.com/schema")
                    .credentialType("MembershipCredential")
                    .participantContextId(USER)
                    .format(VC1_0_JWT)
                    .build();

            service.createCredentialDefinition(definition);

            var res = runtime.getAdminEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header(new Header("x-api-key", token))
                    .body(QuerySpec.Builder.newInstance().filter(new Criterion("credentialType", "=", "MembershipCredential")).build())
                    .post("/v1alpha/participants/%s/credentialdefinitions/query".formatted(toBase64(USER)))
                    .then()
                    .statusCode(200)
                    .body(Matchers.notNullValue())
                    .extract().body().as(CredentialDefinition[].class);

            assertThat(res).hasSize(1).allSatisfy(d -> assertThat(definition).usingRecursiveComparison().isEqualTo(d));
        }

        @Test
        void queryCredentialDefinitions_noResult_whenNotAuthorized(IssuerRuntime runtime, CredentialDefinitionService service) {
            var token = runtime.createParticipant(USER).apiKey();

            var definition = CredentialDefinition.Builder.newInstance()
                    .id("id")
                    .jsonSchema("{}")
                    .jsonSchemaUrl("http://example.com/schema")
                    .credentialType("MembershipCredential")
                    .participantContextId("anotherUser")
                    .format(VC1_0_JWT)
                    .build();

            service.createCredentialDefinition(definition);

            var res = runtime.getAdminEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header(new Header("x-api-key", token))
                    .body(QuerySpec.Builder.newInstance().filter(new Criterion("id", "=", definition.getId())).build())
                    .post("/v1alpha/participants/%s/credentialdefinitions/query".formatted(toBase64(USER)))
                    .then()
                    .statusCode(200)
                    .body(Matchers.notNullValue())
                    .extract().body().as(CredentialDefinition[].class);

            assertThat(res).isEmpty();
        }

        @Test
        void queryCredentialDefinitions_noResult(IssuerRuntime runtime) {
            var token = runtime.createParticipant(USER).apiKey();

            var res = runtime.getAdminEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header(new Header("x-api-key", token))
                    .body(QuerySpec.Builder.newInstance().filter(new Criterion("id", "=", "test-credential-definition-id")).build())
                    .post("/v1alpha/participants/%s/credentialdefinitions/query".formatted(toBase64(USER)))
                    .then()
                    .statusCode(200)
                    .body(Matchers.notNullValue())
                    .extract().body().as(CredentialDefinition[].class);

            assertThat(res).isEmpty();
        }

        @Test
        void getById(IssuerRuntime runtime, CredentialDefinitionService service) {
            var token = runtime.createParticipant(USER).apiKey();

            var definition = CredentialDefinition.Builder.newInstance()
                    .id("test-credential-definition-id")
                    .jsonSchema("{}")
                    .jsonSchemaUrl("http://example.com/schema")
                    .credentialType("MembershipCredential")
                    .participantContextId(USER)
                    .format(VC1_0_JWT)
                    .build();

            service.createCredentialDefinition(definition);

            var res = runtime.getAdminEndpoint().baseRequest()
                    .header(new Header("x-api-key", token))
                    .get("/v1alpha/participants/%s/credentialdefinitions/test-credential-definition-id".formatted(toBase64(USER)))
                    .then()
                    .statusCode(200)
                    .body(Matchers.notNullValue())
                    .extract().body().as(CredentialDefinition.class);

            assertThat(res).usingRecursiveComparison().isEqualTo(definition);
        }

        @Test
        void getById_whenNotAuthorized(IssuerRuntime runtime, CredentialDefinitionService service) {
            var token = runtime.createParticipant(USER).apiKey();

            var definition = CredentialDefinition.Builder.newInstance()
                    .id("test-credential-definition-id")
                    .jsonSchema("{}")
                    .jsonSchemaUrl("http://example.com/schema")
                    .credentialType("MembershipCredential")
                    .participantContextId("anotherUser")
                    .format(VC1_0_JWT)
                    .build();

            service.createCredentialDefinition(definition);

            runtime.getAdminEndpoint().baseRequest()
                    .header(new Header("x-api-key", token))
                    .get("/v1alpha/participants/%s/credentialdefinitions/test-credential-definition-id".formatted(toBase64(USER)))
                    .then()
                    .statusCode(403);

        }

        @Test
        void getById_whenNotFound(IssuerRuntime runtime) {
            var token = runtime.createParticipant(USER).apiKey();


            runtime.getAdminEndpoint().baseRequest()
                    .header(new Header("x-api-key", token))
                    .get("/v1alpha/participants/%s/credentialdefinitions/test-credential-definition-id".formatted(toBase64(USER)))
                    .then()
                    .statusCode(404);

        }


        @Test
        void updateCredentialDefinition(IssuerRuntime runtime, CredentialDefinitionService service) {
            var token = runtime.createParticipant(USER).apiKey();

            var definition = CredentialDefinition.Builder.newInstance()
                    .id("test-credential-definition-id")
                    .jsonSchema("{}")
                    .jsonSchemaUrl("http://example.com/schema")
                    .credentialType("MembershipCredential")
                    .participantContextId(USER)
                    .format(VC1_0_JWT)
                    .build();

            service.createCredentialDefinition(definition);

            definition = CredentialDefinition.Builder.newInstance()
                    .id("test-credential-definition-id")
                    .jsonSchema("{}")
                    .jsonSchemaUrl("http://example.com/schema")
                    .credentialType("MembershipCredential")
                    .participantContextId(USER)
                    .format(VC1_0_JWT)
                    .build();

            runtime.getAdminEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header(new Header("x-api-key", token))
                    .body(definition)
                    .put("/v1alpha/participants/%s/credentialdefinitions".formatted(toBase64(USER)))
                    .then()
                    .statusCode(200);

            var updatedDefinition = service.findCredentialDefinitionById(definition.getId()).getContent();

            assertThat(updatedDefinition).usingRecursiveComparison().isEqualTo(definition);
        }

        @Test
        void updateCredentialDefinition_whenNotFound(IssuerRuntime runtime, CredentialDefinitionService service) {
            var token = runtime.createParticipant(USER).apiKey();

            var definition = CredentialDefinition.Builder.newInstance()
                    .id("test-credential-definition-id")
                    .jsonSchema("{}")
                    .jsonSchemaUrl("http://example.com/schema")
                    .credentialType("MembershipCredential")
                    .participantContextId("participantContextId")
                    .format(VC1_0_JWT)
                    .build();


            runtime.getAdminEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header(new Header("x-api-key", token))
                    .body(definition)
                    .put("/v1alpha/participants/%s/credentialdefinitions".formatted(toBase64(USER)))
                    .then()
                    .statusCode(404);

        }

        @Test
        void updateCredentialDefinition_whenNotAuthorized(IssuerRuntime runtime, CredentialDefinitionService service) {
            var token = runtime.createParticipant(USER).apiKey();

            var definition = CredentialDefinition.Builder.newInstance()
                    .id("test-credential-definition-id")
                    .jsonSchema("{}")
                    .jsonSchemaUrl("http://example.com/schema")
                    .credentialType("MembershipCredential")
                    .participantContextId("participantContextId")
                    .format(VC1_0_JWT)
                    .build();

            service.createCredentialDefinition(definition);

            definition = CredentialDefinition.Builder.newInstance()
                    .id("test-credential-definition-id")
                    .jsonSchema("{}")
                    .jsonSchemaUrl("http://example.com/schema")
                    .credentialType("MembershipCredential")
                    .participantContextId("participantContextId")
                    .format(VC1_0_JWT)
                    .build();

            runtime.getAdminEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header(new Header("x-api-key", token))
                    .body(definition)
                    .put("/v1alpha/participants/%s/credentialdefinitions".formatted(toBase64(USER)))
                    .then()
                    .statusCode(403);

        }

        @Test
        void deleteCredentialDefinition(IssuerRuntime runtime, CredentialDefinitionService service) {
            var token = runtime.createParticipant(USER).apiKey();

            var definition = CredentialDefinition.Builder.newInstance()
                    .id("test-credential-definition-id")
                    .jsonSchema("{}")
                    .jsonSchemaUrl("http://example.com/schema")
                    .credentialType("MembershipCredential")
                    .participantContextId(USER)
                    .format(VC1_0_JWT)
                    .build();

            service.createCredentialDefinition(definition);

            runtime.getAdminEndpoint().baseRequest()
                    .header(new Header("x-api-key", token))
                    .delete("/v1alpha/participants/%s/credentialdefinitions/test-credential-definition-id".formatted(toBase64(USER)))
                    .then()
                    .statusCode(204);

            assertThat(service.findCredentialDefinitionById(definition.getId())).isFailed();

        }

        @Test
        void deleteCredentialDefinition_whenNotExists(IssuerRuntime runtime) {
            var token = runtime.createParticipant(USER).apiKey();


            runtime.getAdminEndpoint().baseRequest()
                    .header(new Header("x-api-key", token))
                    .delete("/v1alpha/participants/%s/credentialdefinitions/test-credential-definition-id".formatted(toBase64(USER)))
                    .then()
                    .statusCode(404);

        }

        @Test
        void deleteCredentialDefinition_whenNotAuthorized(IssuerRuntime runtime, CredentialDefinitionService service) {
            runtime.createParticipant(USER);
            var token = runtime.createParticipant("anotherUser").apiKey();

            var definition = CredentialDefinition.Builder.newInstance()
                    .id("test-credential-definition-id")
                    .jsonSchema("{}")
                    .jsonSchemaUrl("http://example.com/schema")
                    .credentialType("MembershipCredential")
                    .participantContextId(USER)
                    .format(VC1_0_JWT)
                    .build();

            service.createCredentialDefinition(definition);

            runtime.getAdminEndpoint().baseRequest()
                    .header(new Header("x-api-key", token))
                    .delete("/v1alpha/participants/%s/credentialdefinitions/test-credential-definition-id".formatted(toBase64(USER)))
                    .then()
                    .statusCode(403);

        }

        private String toBase64(String s) {
            return Base64.getUrlEncoder().encodeToString(s.getBytes());
        }
    }

    @Nested
    @EndToEndTest
    class InMemory extends Tests {

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

        @Order(1)
        @RegisterExtension
        static final BeforeAllCallback POSTGRES_CONTAINER_STARTER = context -> {
            POSTGRESQL_EXTENSION.createDatabase(ISSUER);
        };

        @Order(2)
        @RegisterExtension
        static final IssuerExtension ISSUER_EXTENSION = IssuerExtension.Builder.newInstance()
                .id(ISSUER_RUNTIME_ID)
                .name(ISSUER_RUNTIME_NAME)
                .modules(ISSUER_RUNTIME_SQL_MODULES)
                .configurationProvider(() -> POSTGRESQL_EXTENSION.configFor(ISSUER))
                .build();
    }
}
