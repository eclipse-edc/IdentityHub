/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
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
import com.nimbusds.jose.jwk.ECKey;
import org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.identityhub.tests.fixtures.DefaultRuntimes;
import org.eclipse.edc.identityhub.tests.fixtures.issuerservice.IssuerService;
import org.eclipse.edc.issuerservice.spi.holder.HolderService;
import org.eclipse.edc.issuerservice.spi.holder.model.Holder;
import org.eclipse.edc.issuerservice.spi.issuance.credentialdefinition.CredentialDefinitionService;
import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialDefinition;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Base64;
import java.util.UUID;

import static io.restassured.http.ContentType.JSON;
import static org.eclipse.edc.identityhub.tests.dcp.TestData.ISSUER_RUNTIME_NAME;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.VerifiableCredentialTestUtil.generateEcKey;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("JUnitMalformedDeclaration")
public class DcpIssuerMetadataApiEndToEndTest {

    protected static final DidPublicKeyResolver DID_PUBLIC_KEY_RESOLVER = mock();

    abstract static class Tests {

        public static final String ISSUER_DID = "did:web:issuer";
        public static final String PARTICIPANT_DID = "did:web:participant";
        public static final String DID_WEB_PARTICIPANT_KEY_1 = "did:web:participant#key1";
        public static final ECKey PARTICIPANT_KEY = generateEcKey(DID_WEB_PARTICIPANT_KEY_1);
        protected static final String ISSUER_ID = "issuer";
        private static final String ISSUER_ID_ENCODED = Base64.getUrlEncoder().encodeToString(ISSUER_ID.getBytes());


        @BeforeAll
        static void beforeAll(IssuerService issuer) {
            issuer.createParticipant(ISSUER_ID);
        }

        private static @NotNull String issuanceMetadataUrl() {
            return "/v1alpha/participants/%s/metadata".formatted(ISSUER_ID_ENCODED);
        }


        @AfterEach
        void teardown(HolderService holderService, CredentialDefinitionService credentialDefinitionService) {
            holderService.queryHolders(QuerySpec.max()).getContent()
                    .forEach(p -> holderService.deleteHolder(p.getHolderId()).getContent());

            credentialDefinitionService.queryCredentialDefinitions(QuerySpec.max()).getContent()
                    .forEach(c -> credentialDefinitionService.deleteCredentialDefinition(c.getId()).getContent());
        }

        @Test
        void issuerMetadata(IssuerService issuer, HolderService holderService, CredentialDefinitionService credentialDefinitionService) throws JOSEException {

            var credentialDefinition = CredentialDefinition.Builder.newInstance()
                    .id(UUID.randomUUID().toString())
                    .credentialType("MembershipCredential")
                    .jsonSchema("{}")
                    .participantContextId(ISSUER_ID)
                    .formatFrom(CredentialFormat.VC1_0_JWT)
                    .build();

            credentialDefinitionService.createCredentialDefinition(credentialDefinition);
            holderService.createHolder(createHolder(PARTICIPANT_DID, PARTICIPANT_DID, "Participant"));


            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(DID_WEB_PARTICIPANT_KEY_1))).thenReturn(Result.success(PARTICIPANT_KEY.toPublicKey()));

            issuer.getIssuerApiEndpoint().baseRequest()
                    .contentType(JSON)
                    .get(issuanceMetadataUrl())
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .body("issuer", equalTo(ISSUER_DID))
                    .body("credentialsSupported.size()", equalTo(1))
                    .body("credentialsSupported[0].credentialType", equalTo("MembershipCredential"))
                    .body("credentialsSupported[0].bindingMethods[0]", equalTo("did:web"))
                    .body("credentialsSupported[0].profile", equalTo("vc11-sl2021/jwt"));

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
    class InMemory extends Tests {

        @RegisterExtension
        static final RuntimeExtension ISSUER_EXTENSION = ComponentRuntimeExtension.Builder.newInstance()
                .name(ISSUER_RUNTIME_NAME)
                .modules(DefaultRuntimes.Issuer.MODULES)
                .endpoints(DefaultRuntimes.Issuer.ENDPOINTS.build())
                .configurationProvider(DefaultRuntimes.Issuer::config)
                .paramProvider(IssuerService.class, IssuerService::forContext)
                .build()
                .registerServiceMock(DidPublicKeyResolver.class, DID_PUBLIC_KEY_RESOLVER);
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
                .build()
                .registerServiceMock(DidPublicKeyResolver.class, DID_PUBLIC_KEY_RESOLVER);

    }
}
