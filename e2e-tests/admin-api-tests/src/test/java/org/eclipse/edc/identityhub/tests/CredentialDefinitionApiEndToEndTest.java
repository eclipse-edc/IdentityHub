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
import org.eclipse.edc.identityhub.tests.fixtures.IssuerServiceEndToEndExtension;
import org.eclipse.edc.identityhub.tests.fixtures.IssuerServiceEndToEndTestContext;
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
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.hamcrest.Matchers.containsString;


public class CredentialDefinitionApiEndToEndTest {
    abstract static class Tests {


        @AfterEach
        void teardown(CredentialDefinitionService service) {
            service.queryCredentialDefinitions(QuerySpec.max()).getContent()
                    .forEach(p -> service.deleteCredentialDefinition(p.getId()).getContent());

        }

        @Test
        void createCredentialDefinition(IssuerServiceEndToEndTestContext context, CredentialDefinitionService service, AttestationDefinitionStore store) {


            store.create(new AttestationDefinition("test-attestation", "type", Map.of()));

            var definition = CredentialDefinition.Builder.newInstance()
                    .id("test-definition-id")
                    .jsonSchema("{}")
                    .jsonSchemaUrl("https://example.org/membership-credential-schema.json")
                    .credentialType("MembershipCredential")
                    .mapping(new MappingDefinition("input", "output", true))
                    .validity(1000)
                    .rule(new CredentialRuleDefinition("rule", Map.of("key", "value")))
                    .attestation("test-attestation")
                    .build();

            context.getAdminEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .body(definition)
                    .post("/v1alpha/credentialdefinitions")
                    .then()
                    .statusCode(201)
                    .header("Location", Matchers.endsWith("/credentialdefinitions/test-definition-id"));

            assertThat(service.findCredentialDefinitionById(definition.getId())).isSucceeded()
                    .usingRecursiveComparison()
                    .isEqualTo(definition);
        }

        @Test
        void createCredentialDefinition_whenExists(IssuerServiceEndToEndTestContext context, CredentialDefinitionService service) {

            var definition = CredentialDefinition.Builder.newInstance()
                    .id("test-definition-id")
                    .jsonSchema("{}")
                    .jsonSchemaUrl("https://example.org/membership-credential-schema.json")
                    .credentialType("MyType")
                    .build();

            service.createCredentialDefinition(definition);

            context.getAdminEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .body("""
                            {
                              "id": "test-definition-id",
                              "credentialType": "MembershipCredential",
                              "jsonSchema": "{}",
                              "jsonSchemaUrl": "https://example.org/membership-credential-schema.json"
                            }
                            """)
                    .post("/v1alpha/credentialdefinitions")
                    .then()
                    .log().all()
                    .statusCode(409);
        }

        @Test
        void createCredentialDefinition_whenCredentialTypeExists(IssuerServiceEndToEndTestContext context, CredentialDefinitionService service) {

            var definition = CredentialDefinition.Builder.newInstance()
                    .id("id")
                    .jsonSchema("{}")
                    .jsonSchemaUrl("https://example.org/membership-credential-schema.json")
                    .credentialType("MembershipCredential")
                    .build();

            service.createCredentialDefinition(definition);

            context.getAdminEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .body("""
                            {
                              "id": "test-definition-id",
                              "credentialType": "MembershipCredential",
                              "jsonSchema": "{}",
                              "jsonSchemaUrl": "https://example.org/membership-credential-schema.json"
                            }
                            """)
                    .post("/v1alpha/credentialdefinitions")
                    .then()
                    .log().all()
                    .statusCode(409);
        }

        @Test
        void createCredentialDefinition_whenMissingFields(IssuerServiceEndToEndTestContext context) {

            context.getAdminEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .body("""
                            {
                              "id": "test-definition-id"
                            }
                            """)
                    .post("/v1alpha/credentialdefinitions")
                    .then()
                    .statusCode(400);
        }

        @Test
        void createCredentialDefinition_whenMissingAttestations(IssuerServiceEndToEndTestContext context) {

            context.getAdminEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .body("""
                            {
                              "id": "test-definition-id",
                              "credentialType": "MembershipCredential",
                              "jsonSchema": "{}",
                              "jsonSchemaUrl": "https://example.org/membership-credential-schema.json",
                              "attestations": ["notfound"]
                            }
                            """)
                    .post("/v1alpha/credentialdefinitions")
                    .then()
                    .statusCode(400)
                    .body("[0].message", containsString("notfound"));
        }


        @Test
        void queryCredentialDefinitions(IssuerServiceEndToEndTestContext context, CredentialDefinitionService service) {

            var definition = CredentialDefinition.Builder.newInstance()
                    .id("id")
                    .jsonSchema("{}")
                    .jsonSchemaUrl("http://example.com/schema")
                    .credentialType("MembershipCredential")
                    .build();

            service.createCredentialDefinition(definition);

            var res = context.getAdminEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .body(QuerySpec.Builder.newInstance().filter(new Criterion("credentialType", "=", "MembershipCredential")).build())
                    .post("/v1alpha/credentialdefinitions/query")
                    .then()
                    .statusCode(200)
                    .body(Matchers.notNullValue())
                    .extract().body().as(CredentialDefinition[].class);

            assertThat(res).hasSize(1).allSatisfy(d -> assertThat(definition).usingRecursiveComparison().isEqualTo(d));
        }

        @Test
        void queryCredentialDefinitions_noResult(IssuerServiceEndToEndTestContext context) {

            var res = context.getAdminEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .body(QuerySpec.Builder.newInstance().filter(new Criterion("id", "=", "test-credential-definition-id")).build())
                    .post("/v1alpha/credentialdefinitions/query")
                    .then()
                    .statusCode(200)
                    .body(Matchers.notNullValue())
                    .extract().body().as(CredentialDefinition[].class);

            assertThat(res).isEmpty();
        }

        @Test
        void getById(IssuerServiceEndToEndTestContext context, CredentialDefinitionService service) {

            var definition = CredentialDefinition.Builder.newInstance()
                    .id("test-credential-definition-id")
                    .jsonSchema("{}")
                    .jsonSchemaUrl("http://example.com/schema")
                    .credentialType("MembershipCredential")
                    .build();

            service.createCredentialDefinition(definition);

            var res = context.getAdminEndpoint().baseRequest()
                    .get("/v1alpha/credentialdefinitions/test-credential-definition-id")
                    .then()
                    .statusCode(200)
                    .body(Matchers.notNullValue())
                    .extract().body().as(CredentialDefinition.class);

            assertThat(res).usingRecursiveComparison().isEqualTo(definition);
        }

        @Test
        void getById_whenNotFound(IssuerServiceEndToEndTestContext context) {


            context.getAdminEndpoint().baseRequest()
                    .get("/v1alpha/credentialdefinitions/test-credential-definition-id")
                    .then()
                    .statusCode(404);

        }


        @Test
        void updateCredentialDefinition(IssuerServiceEndToEndTestContext context, CredentialDefinitionService service) {

            var definition = CredentialDefinition.Builder.newInstance()
                    .id("test-credential-definition-id")
                    .jsonSchema("{}")
                    .jsonSchemaUrl("http://example.com/schema")
                    .credentialType("MembershipCredential")
                    .build();

            service.createCredentialDefinition(definition);

            definition = CredentialDefinition.Builder.newInstance()
                    .id("test-credential-definition-id")
                    .jsonSchema("{}")
                    .jsonSchemaUrl("http://example.com/schema")
                    .credentialType("MembershipCredential")
                    .build();

            context.getAdminEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .body(definition)
                    .put("/v1alpha/credentialdefinitions")
                    .then()
                    .statusCode(200);

            var updatedDefinition = service.findCredentialDefinitionById(definition.getId()).getContent();

            assertThat(updatedDefinition).usingRecursiveComparison().isEqualTo(definition);
        }

        @Test
        void updateCredentialDefinition_whenNotFound(IssuerServiceEndToEndTestContext context, CredentialDefinitionService service) {

            var definition = CredentialDefinition.Builder.newInstance()
                    .id("test-credential-definition-id")
                    .jsonSchema("{}")
                    .jsonSchemaUrl("http://example.com/schema")
                    .credentialType("MembershipCredential")
                    .build();

            context.getAdminEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .body(definition)
                    .put("/v1alpha/credentialdefinitions")
                    .then()
                    .statusCode(404);

        }

        @Test
        void deleteCredentialDefinition(IssuerServiceEndToEndTestContext context, CredentialDefinitionService service) {

            var definition = CredentialDefinition.Builder.newInstance()
                    .id("test-credential-definition-id")
                    .jsonSchema("{}")
                    .jsonSchemaUrl("http://example.com/schema")
                    .credentialType("MembershipCredential")
                    .build();

            service.createCredentialDefinition(definition);

            context.getAdminEndpoint().baseRequest()
                    .delete("/v1alpha/credentialdefinitions/test-credential-definition-id")
                    .then()
                    .statusCode(204);

            assertThat(service.findCredentialDefinitionById(definition.getId())).isFailed();

        }

        @Test
        void deleteCredentialDefinition_whenNotExists(IssuerServiceEndToEndTestContext context) {


            context.getAdminEndpoint().baseRequest()
                    .delete("/v1alpha/credentialdefinitions/test-credential-definition-id")
                    .then()
                    .statusCode(404);

        }
    }

    @Nested
    @EndToEndTest
    @ExtendWith(IssuerServiceEndToEndExtension.InMemory.class)
    class InMemory extends Tests {
    }

    @Nested
    @PostgresqlIntegrationTest
    @ExtendWith(IssuerServiceEndToEndExtension.Postgres.class)
    class Postgres extends Tests {

    }
}
