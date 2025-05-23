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

package org.eclipse.edc.identityhub.tests.dcp.api;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.edc.identityhub.tests.fixtures.issuerservice.IssuerExtension;
import org.eclipse.edc.identityhub.tests.fixtures.issuerservice.IssuerRuntime;
import org.eclipse.edc.issuerservice.spi.holder.HolderService;
import org.eclipse.edc.issuerservice.spi.holder.model.Holder;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionValidatorRegistry;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationSourceFactory;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationSourceFactoryRegistry;
import org.eclipse.edc.issuerservice.spi.issuance.credentialdefinition.CredentialDefinitionService;
import org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcess;
import org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcessStates;
import org.eclipse.edc.issuerservice.spi.issuance.process.store.IssuanceProcessStore;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.eclipse.edc.validator.spi.ValidationResult;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.identityhub.tests.dcp.TestData.ISSUER_RUNTIME_ID;
import static org.eclipse.edc.identityhub.tests.dcp.TestData.ISSUER_RUNTIME_MEM_MODULES;
import static org.eclipse.edc.identityhub.tests.dcp.TestData.ISSUER_RUNTIME_NAME;
import static org.eclipse.edc.identityhub.tests.dcp.TestData.ISSUER_RUNTIME_SQL_MODULES;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.JwtCreationUtil.generateJwt;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.VerifiableCredentialTestUtil.generateEcKey;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("JUnitMalformedDeclaration")
public class DcpCredentialRequestStatusApiEndToEndTest {

    protected static final DidPublicKeyResolver DID_PUBLIC_KEY_RESOLVER = mock();

    abstract static class Tests {

        public static final String ISSUER_DID = "did:web:issuer";
        public static final String PARTICIPANT_DID = "did:web:participant";
        public static final String DID_WEB_PARTICIPANT_KEY_1 = "did:web:participant#key1";
        public static final ECKey PARTICIPANT_KEY = generateEcKey(DID_WEB_PARTICIPANT_KEY_1);
        protected static final AttestationSourceFactory ATTESTATION_SOURCE_FACTORY = mock();
        protected static final String ISSUER_ID = "issuer";
        private static final String ISSUER_ID_ENCODED = Base64.getUrlEncoder().encodeToString(ISSUER_ID.getBytes());


        @BeforeAll
        static void beforeAll(IssuerRuntime issuerRuntime) {
            var pipelineFactory = issuerRuntime.getService(AttestationSourceFactoryRegistry.class);
            var validationRegistry = issuerRuntime.getService(AttestationDefinitionValidatorRegistry.class);
            pipelineFactory.registerFactory("Attestation", ATTESTATION_SOURCE_FACTORY);
            validationRegistry.registerValidator("Attestation", def -> ValidationResult.success());
            issuerRuntime.createParticipant(ISSUER_ID);
        }

        private static @NotNull String issuanceStatusUrl(String id) {
            return "/v1alpha/participants/%s/requests/%s".formatted(ISSUER_ID_ENCODED, id);
        }


        @AfterEach
        void teardown(HolderService holderService, CredentialDefinitionService credentialDefinitionService) {
            holderService.queryHolders(QuerySpec.max()).getContent()
                    .forEach(p -> holderService.deleteHolder(p.getHolderId()).getContent());

            credentialDefinitionService.queryCredentialDefinitions(QuerySpec.max()).getContent()
                    .forEach(c -> credentialDefinitionService.deleteCredentialDefinition(c.getId()).getContent());
        }

        @Test
        void credentialStatus(IssuerExtension issuerExtension, HolderService holderService,
                              IssuanceProcessStore issuanceProcessStore) throws JOSEException {

            var process = createIssuanceProcess();

            holderService.createHolder(createHolder(PARTICIPANT_DID, PARTICIPANT_DID, "Participant"));
            issuanceProcessStore.save(process);

            var token = generateSiToken();


            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(DID_WEB_PARTICIPANT_KEY_1))).thenReturn(Result.success(PARTICIPANT_KEY.toPublicKey()));

            var response = issuerExtension.getIssuerApiEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(AUTHORIZATION, token)
                    .get(issuanceStatusUrl(process.getId()))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .extract()
                    .body().as(Map.class);


            assertThat(response.get("holderPid")).isEqualTo(process.getHolderPid());
            assertThat(response.get("issuerPid")).isEqualTo(process.getId());
            assertThat(response.get("status")).isEqualTo("ISSUED");

        }

        @Test
        void credentialStatus_wrongParticipant_shouldReturn401(IssuerExtension issuerExtension, HolderService holderService,
                                                               IssuanceProcessStore issuanceProcessStore) throws JOSEException {

            var process = createIssuanceProcess();
            var wrongParticipant = "wrong-participant";
            var wrongParticipantDid = "did:web:%s".formatted(wrongParticipant);
            var wrongParticipantKeyId = "%s#key1".formatted(wrongParticipantDid);
            var wrongParticipantKey = generateEcKey(wrongParticipantKeyId);

            holderService.createHolder(createHolder(PARTICIPANT_DID, PARTICIPANT_DID, "Participant"));
            holderService.createHolder(createHolder(wrongParticipant, wrongParticipantDid, "WrongParticipant"));

            issuanceProcessStore.save(process);

            var token = generateSiToken(ISSUER_DID, wrongParticipantDid, wrongParticipantKey);

            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(wrongParticipantKeyId))).thenReturn(Result.success(wrongParticipantKey.toPublicKey()));

            issuerExtension.getIssuerApiEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(AUTHORIZATION, token)
                    .get(issuanceStatusUrl(process.getId()))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(401);

        }

        @Test
        void credentialStatus_tokenNotPresent_shouldReturn401(IssuerExtension issuerExtension) {
            issuerExtension.getIssuerApiEndpoint().baseRequest()
                    .contentType(JSON)
                    .get(issuanceStatusUrl("credentialRequestId"))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(401);

        }

        @Test
        void credentialStatus_participantNotFound_shouldReturn401(IssuerExtension issuerExtension) {
            var token = generateSiToken();

            issuerExtension.getIssuerApiEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(AUTHORIZATION, token)
                    .get(issuanceStatusUrl("credentialRequestId"))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(401);

        }

        @Test
        void credentialStatus_tokenVerificationFails_shouldReturn401(IssuerExtension issuerExtension, HolderService holderService, IssuanceProcessStore issuanceProcessStore) throws JOSEException {

            var process = createIssuanceProcess();

            holderService.createHolder(createHolder(PARTICIPANT_DID, PARTICIPANT_DID, "Participant"));
            issuanceProcessStore.save(process);

            holderService.createHolder(createHolder(PARTICIPANT_DID, PARTICIPANT_DID, "Participant"));

            var spoofedKey = new ECKeyGenerator(Curve.P_256).keyID(DID_WEB_PARTICIPANT_KEY_1).generate();

            var token = generateSiToken();

            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(DID_WEB_PARTICIPANT_KEY_1))).thenReturn(Result.success(spoofedKey.toPublicKey()));

            issuerExtension.getIssuerApiEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(AUTHORIZATION, token)
                    .get(issuanceStatusUrl(process.getId()))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(401);

        }

        @Test
        void credentialStatus_spoofedKeyId_shouldReturn401(IssuerExtension issuerExtension, HolderService holderService, IssuanceProcessStore issuanceProcessStore) throws JOSEException {
            var process = createIssuanceProcess();
            holderService.createHolder(createHolder(PARTICIPANT_DID, PARTICIPANT_DID, "Participant"));
            issuanceProcessStore.save(process);
            var spoofedKeyId = "did:web:spoofed#key1";
            var spoofedKey = new ECKeyGenerator(Curve.P_256).keyID(spoofedKeyId).generate();

            var token = generateSiToken(spoofedKey);

            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(spoofedKeyId))).thenReturn(Result.success(spoofedKey.toPublicKey()));

            issuerExtension.getIssuerApiEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(AUTHORIZATION, token)
                    .get(issuanceStatusUrl(process.getId()))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(401);


        }

        @Test
        void credentialStatus_wrongTokenAudience_shouldReturn401(IssuerExtension issuerExtension, HolderService holderService, IssuanceProcessStore issuanceProcessStore) throws JOSEException {

            var process = createIssuanceProcess();

            generateEcKey(DID_WEB_PARTICIPANT_KEY_1);

            holderService.createHolder(createHolder(PARTICIPANT_DID, PARTICIPANT_DID, "Participant"));
            issuanceProcessStore.save(process);


            var token = generateSiToken("wrong-audience");

            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(DID_WEB_PARTICIPANT_KEY_1))).thenReturn(Result.success(PARTICIPANT_KEY.toPublicKey()));

            issuerExtension.getIssuerApiEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(AUTHORIZATION, token)
                    .get(issuanceStatusUrl(process.getId()))
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
            return "Bearer " + generateJwt(audience, participantDid, participantDid, Map.of(), participantKey);
        }

        private IssuanceProcess createIssuanceProcess() {
            return IssuanceProcess.Builder.newInstance()
                    .id(UUID.randomUUID().toString())
                    .state(IssuanceProcessStates.DELIVERED.code())
                    .holderId(PARTICIPANT_DID)
                    .participantContextId(ISSUER_ID)
                    .holderPid(UUID.randomUUID().toString())
                    .build();
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
