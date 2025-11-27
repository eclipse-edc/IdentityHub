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

import io.restassured.http.Header;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.identityhub.spi.participantcontext.IdentityHubParticipantContextService;
import org.eclipse.edc.identityhub.tests.fixtures.DefaultRuntimes;
import org.eclipse.edc.identityhub.tests.fixtures.issuerservice.IssuerService;
import org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcess;
import org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcessStates;
import org.eclipse.edc.issuerservice.spi.issuance.process.store.IssuanceProcessStore;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.http.ContentType.JSON;
import static org.eclipse.edc.identityhub.tests.TestData.ISSUER_RUNTIME_NAME;
import static org.hamcrest.Matchers.equalTo;

@SuppressWarnings("JUnitMalformedDeclaration")
public class IssuanceProcessApiEndToEndTest {
    abstract static class Tests {

        @AfterEach
        void tearDown(IdentityHubParticipantContextService pcService) {
            pcService.query(QuerySpec.max()).getContent()
                    .forEach(pc -> pcService.deleteParticipantContext(pc.getParticipantContextId()).getContent());

        }

        @Test
        void getIssuanceProcess(IssuerService issuerService, IssuanceProcessStore store) {
            var issuer = "issuer";
            var token = issuerService.createParticipant(issuer).apiKey();

            var process = createIssuanceProcess(issuer);
            store.save(process);

            issuerService.getAdminEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", token))
                    .get("/v1alpha/participants/%s/issuanceprocesses/%s".formatted(toBase64(issuer), process.getId()))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .body("id", equalTo(process.getId()))
                    .body("participantContextId", equalTo(process.getParticipantContextId()))
                    .body("holderPid", equalTo(process.getHolderPid()))
                    .body("claims", equalTo(process.getClaims()))
                    .body("credentialDefinitions", equalTo(process.getCredentialDefinitions()))
                    .body("state", equalTo(process.stateAsString()))
                    .body("holderId", equalTo(process.getHolderId()));
        }

        @Test
        void getIssuanceProcess_notAuthorized(IssuerService issuerService, IssuanceProcessStore store) {
            var issuer = "issuer";
            var issuer2 = "issuer2";
            issuerService.createParticipant(issuer);

            var process = createIssuanceProcess(issuer);
            store.save(process);

            var token = issuerService.createParticipant(issuer2).apiKey();

            issuerService.getAdminEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", token))
                    .get("/v1alpha/participants/%s/issuanceprocesses/%s".formatted(toBase64(issuer), process.getId()))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403);
        }

        @Test
        void queryIssuanceProcesses(IssuerService issuerService, IssuanceProcessStore store) {
            var issuer = "issuer";
            var token = issuerService.createParticipant(issuer).apiKey();

            var process = createIssuanceProcess(issuer);
            store.save(process);

            issuerService.getAdminEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", token))
                    .body(QuerySpec.Builder.newInstance()
                            .sortField("id")
                            .sortOrder(SortOrder.ASC)
                            .filter(new Criterion("holderId", "=", process.getHolderId()))
                            .build())
                    .post("/v1alpha/participants/%s/issuanceprocesses/query".formatted(toBase64(issuer)))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .body("size()", equalTo(1))
                    .body("[0].id", equalTo(process.getId()))
                    .body("[0].participantContextId", equalTo(process.getParticipantContextId()))
                    .body("[0].holderPid", equalTo(process.getHolderPid()))
                    .body("[0].claims", equalTo(process.getClaims()))
                    .body("[0].credentialDefinitions", equalTo(process.getCredentialDefinitions()))
                    .body("[0].state", equalTo(process.stateAsString()))
                    .body("[0].holderId", equalTo(process.getHolderId()));

        }

        @Test
        void queryIssuanceProcesses_notAuthorized(IssuerService issuerService, IssuanceProcessStore store) {
            var issuer = "issuer";
            var issuer2 = "issuer2";
            issuerService.createParticipant(issuer);

            var process = createIssuanceProcess(issuer);
            store.save(process);

            var token = issuerService.createParticipant(issuer2).apiKey();

            issuerService.getAdminEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", token))
                    .body(QuerySpec.Builder.newInstance()
                            .sortField("id")
                            .sortOrder(SortOrder.ASC)
                            .filter(new Criterion("holderId", "=", process.getHolderId()))
                            .build())
                    .post("/v1alpha/participants/%s/issuanceprocesses/query".formatted(toBase64(issuer)))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .body("size()", equalTo(0));

        }

        private String toBase64(String s) {
            return Base64.getUrlEncoder().encodeToString(s.getBytes());
        }

        private IssuanceProcess createIssuanceProcess(String participantContextId) {
            return IssuanceProcess.Builder.newInstance()
                    .id(UUID.randomUUID().toString())
                    .state(IssuanceProcessStates.DELIVERED.code())
                    .holderId("test-participant")
                    .participantContextId(participantContextId)
                    .holderPid("test-holder")
                    .claims(Map.of("test-claim", "test-value"))
                    .credentialDefinitions(List.of("test-cred-def"))
                    .credentialFormats(Map.of("test-format", CredentialFormat.VC1_0_JWT))
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
