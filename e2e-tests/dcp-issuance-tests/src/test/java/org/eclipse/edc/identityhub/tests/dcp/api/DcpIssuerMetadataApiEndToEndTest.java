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
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.identityhub.tests.fixtures.issuerservice.IssuerExtension;
import org.eclipse.edc.identityhub.tests.fixtures.issuerservice.IssuerRuntime;
import org.eclipse.edc.issuerservice.spi.holder.HolderService;
import org.eclipse.edc.issuerservice.spi.holder.model.Holder;
import org.eclipse.edc.issuerservice.spi.issuance.credentialdefinition.CredentialDefinitionService;
import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialDefinition;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
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
import java.util.Map;
import java.util.UUID;

import static io.restassured.http.ContentType.JSON;
import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.eclipse.edc.identityhub.tests.dcp.TestData.ISSUER_RUNTIME_ID;
import static org.eclipse.edc.identityhub.tests.dcp.TestData.ISSUER_RUNTIME_MEM_MODULES;
import static org.eclipse.edc.identityhub.tests.dcp.TestData.ISSUER_RUNTIME_NAME;
import static org.eclipse.edc.identityhub.tests.dcp.TestData.ISSUER_RUNTIME_SQL_MODULES;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.JwtCreationUtil.generateJwt;
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
        static void beforeAll(IssuerRuntime issuerRuntime) {
            issuerRuntime.createParticipant(ISSUER_ID);
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
        void issuerMetadata(IssuerExtension issuerExtension, HolderService holderService, CredentialDefinitionService credentialDefinitionService) throws JOSEException {

            var credentialDefinition = CredentialDefinition.Builder.newInstance()
                    .id(UUID.randomUUID().toString())
                    .credentialType("MembershipCredential")
                    .jsonSchema("{}")
                    .participantContextId(ISSUER_ID)
                    .formatFrom(CredentialFormat.VC1_0_JWT)
                    .build();

            credentialDefinitionService.createCredentialDefinition(credentialDefinition);
            holderService.createHolder(createHolder(PARTICIPANT_DID, PARTICIPANT_DID, "Participant"));

            var token = generateSiToken();

            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(DID_WEB_PARTICIPANT_KEY_1))).thenReturn(Result.success(PARTICIPANT_KEY.toPublicKey()));

            issuerExtension.getIssuerApiEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(AUTHORIZATION, token)
                    .get(issuanceMetadataUrl())
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .body("issuer", equalTo(ISSUER_DID))
                    .body("credentialsSupported.size()", equalTo(1))
                    .body("credentialsSupported[0].credentialType", equalTo("MembershipCredential"))
                    .body("credentialsSupported[0].bindingMethods[0]", equalTo("did:web"))
                    .body("credentialsSupported[0].profiles[0]", equalTo("vc11-sl2021/jwt"));

        }

        @Test
        void issuerMetadata_tokenNotPresent_shouldReturn401(IssuerExtension issuerExtension) {
            issuerExtension.getIssuerApiEndpoint().baseRequest()
                    .contentType(JSON)
                    .get(issuanceMetadataUrl())
                    .then()
                    .log().ifValidationFails()
                    .statusCode(401);

        }

        @Test
        void issuerMetadata_participantNotFound_shouldReturn401(IssuerExtension issuerExtension) {
            var token = generateSiToken();

            issuerExtension.getIssuerApiEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(AUTHORIZATION, token)
                    .get(issuanceMetadataUrl())
                    .then()
                    .log().ifValidationFails()
                    .statusCode(401);

        }

        @Test
        void issuerMetadata_tokenVerificationFails_shouldReturn401(IssuerExtension issuerExtension, HolderService holderService) throws JOSEException {

            holderService.createHolder(createHolder(PARTICIPANT_DID, PARTICIPANT_DID, "Participant"));

            holderService.createHolder(createHolder(PARTICIPANT_DID, PARTICIPANT_DID, "Participant"));

            var spoofedKey = new ECKeyGenerator(Curve.P_256).keyID(DID_WEB_PARTICIPANT_KEY_1).generate();

            var token = generateSiToken();

            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(DID_WEB_PARTICIPANT_KEY_1))).thenReturn(Result.success(spoofedKey.toPublicKey()));

            issuerExtension.getIssuerApiEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(AUTHORIZATION, token)
                    .get(issuanceMetadataUrl())
                    .then()
                    .log().ifValidationFails()
                    .statusCode(401);

        }

        @Test
        void issuerMetadata_spoofedKeyId_shouldReturn401(IssuerExtension issuerExtension, HolderService holderService) throws JOSEException {
            holderService.createHolder(createHolder(PARTICIPANT_DID, PARTICIPANT_DID, "Participant"));
            var spoofedKeyId = "did:web:spoofed#key1";
            var spoofedKey = new ECKeyGenerator(Curve.P_256).keyID(spoofedKeyId).generate();

            var token = generateSiToken(spoofedKey);

            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(spoofedKeyId))).thenReturn(Result.success(spoofedKey.toPublicKey()));

            issuerExtension.getIssuerApiEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(AUTHORIZATION, token)
                    .get(issuanceMetadataUrl())
                    .then()
                    .log().ifValidationFails()
                    .statusCode(401);


        }

        @Test
        void issuerMetadata_wrongTokenAudience_shouldReturn401(IssuerExtension issuerExtension, HolderService holderService) throws JOSEException {

            generateEcKey(DID_WEB_PARTICIPANT_KEY_1);

            holderService.createHolder(createHolder(PARTICIPANT_DID, PARTICIPANT_DID, "Participant"));

            var token = generateSiToken("wrong-audience");

            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(DID_WEB_PARTICIPANT_KEY_1))).thenReturn(Result.success(PARTICIPANT_KEY.toPublicKey()));

            issuerExtension.getIssuerApiEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(AUTHORIZATION, token)
                    .get(issuanceMetadataUrl())
                    .then()
                    .log().ifValidationFails()
                    .statusCode(401);

        }


        private String generateSiToken() {
            return generateSiToken(ISSUER_DID);
        }

        private String generateSiToken(String audience) {
            return generateSiToken(audience, PARTICIPANT_DID, PARTICIPANT_KEY);
        }

        private String generateSiToken(ECKey key) {
            return generateSiToken(ISSUER_DID, key);
        }

        private String generateSiToken(String audience, ECKey key) {
            return generateJwt(audience, PARTICIPANT_DID, PARTICIPANT_DID, Map.of(), key);
        }

        private String generateSiToken(String audience, String participantDid, ECKey participantKey) {
            return generateJwt(audience, participantDid, participantDid, Map.of(), participantKey);
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
    @Order(1)
    class InMemory extends Tests {

        @RegisterExtension
        static final RuntimeExtension ISSUER_EXTENSION = IssuerExtension.Builder.newInstance()
                .id(ISSUER_RUNTIME_ID)
                .name(ISSUER_RUNTIME_NAME)
                .modules(ISSUER_RUNTIME_MEM_MODULES)
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
        static final RuntimeExtension ISSUER_EXTENSION = IssuerExtension.Builder.newInstance()
                .id(ISSUER_RUNTIME_ID)
                .name(ISSUER_RUNTIME_NAME)
                .modules(ISSUER_RUNTIME_SQL_MODULES)
                .configurationProvider(() -> POSTGRESQL_EXTENSION.configFor(ISSUER))
                .build()
                .registerServiceMock(DidPublicKeyResolver.class, DID_PUBLIC_KEY_RESOLVER);

    }
}
