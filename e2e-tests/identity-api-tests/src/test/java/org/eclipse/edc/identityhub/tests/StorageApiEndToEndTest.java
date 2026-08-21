/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *       Cofinity-X - Improvements for VC DataModel 2.0
 *
 */

package org.eclipse.edc.identityhub.tests;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import io.restassured.http.ContentType;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.iam.decentralizedclaims.sts.spi.store.StsAccountStore;
import org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.RevocationServiceRegistry;
import org.eclipse.edc.identityhub.spi.credential.request.model.HolderCredentialRequest;
import org.eclipse.edc.identityhub.spi.credential.request.store.HolderCredentialRequestStore;
import org.eclipse.edc.identityhub.spi.did.store.DidResourceStore;
import org.eclipse.edc.identityhub.spi.keypair.store.KeyPairResourceStore;
import org.eclipse.edc.identityhub.spi.participantcontext.IdentityHubParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyPairUsage;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.identityhub.tests.fixtures.DefaultRuntimes;
import org.eclipse.edc.identityhub.tests.fixtures.credentialservice.IdentityHub;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.decentralizedclaims.spi.DcpConstants.DSPACE_DCP_NAMESPACE_V_1_0;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialMessage.CREDENTIALS_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialMessage.HOLDER_PID_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialMessage.ISSUER_PID_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialMessage.STATUS_TERM;
import static org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState.CREATED;
import static org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState.REQUESTED;
import static org.eclipse.edc.identityhub.tests.fixtures.TestData.IH_RUNTIME_NAME;
import static org.eclipse.edc.identityhub.tests.fixtures.TestData.JWT_VC_EXAMPLE;
import static org.eclipse.edc.identityhub.tests.fixtures.TestData.VC_EXAMPLE_2;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.JwtCreationUtil.CONSUMER_DID;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.JwtCreationUtil.CONSUMER_KEY;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.JwtCreationUtil.PROVIDER_DID;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.JwtCreationUtil.PROVIDER_KEY;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.JwtCreationUtil.generateJwt;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.JwtCreationUtil.generateSiToken;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.VerifiableCredentialTestUtil.generateEcKey;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("JUnitMalformedDeclaration")
public class StorageApiEndToEndTest {


    @SuppressWarnings("JUnitMalformedDeclaration")
    abstract static class Tests {

        protected static final DidPublicKeyResolver DID_PUBLIC_KEY_RESOLVER = mock();
        protected static final RevocationServiceRegistry REVOCATION_LIST_REGISTRY = mock();
        private static final String TEST_PARTICIPANT_CONTEXT_ID = "consumer";

        @BeforeEach
        void setup(IdentityHub identityHub) {
            createParticipant(identityHub);
            identityHub.storeHolderRequest(HolderCredentialRequest.Builder.newInstance()
                    .id("test-holder-id")
                    .issuerDid(PROVIDER_DID)
                    .participantContextId(TEST_PARTICIPANT_CONTEXT_ID)
                    .requestedCredential("test-id1", "ExamplePersonCredential", "VC1_0_JWT")
                    .requestedCredential("test-id2", "SuperSecretCredential", "VC1_0_LD")
                    .state(REQUESTED.code())
                    .participantContextId(PROVIDER_DID)
                    .build());
        }

        @AfterEach
        void teardown(IdentityHubParticipantContextService contextService, DidResourceStore didResourceStore, KeyPairResourceStore keyPairResourceStore, CredentialStore store, StsAccountStore accountStore) {
            // purge all participant contexts

            contextService.query(QuerySpec.max()).getContent()
                    .forEach(pc -> contextService.deleteParticipantContext(pc.getParticipantContextId()).getContent());

            didResourceStore.query(QuerySpec.max()).forEach(dr -> didResourceStore.deleteById(dr.getDid()).getContent());

            keyPairResourceStore.query(QuerySpec.max()).getContent()
                    .forEach(kpr -> keyPairResourceStore.deleteById(kpr.getId()).getContent());

            // purge all VCs
            store.query(QuerySpec.none())
                    .map(creds -> creds.stream().map(cred -> store.deleteById(cred.getId())).toList())
                    .orElseThrow(f -> new RuntimeException(f.getFailureDetail()));

            accountStore.findAll(QuerySpec.max())
                    .forEach(sts -> accountStore.deleteById(sts.getId()).getContent());
        }

        @DisplayName("Store JWT credential successfully")
        @Test
        void storeCredential(IdentityHub identityHub, CredentialStore credentialStore) throws JOSEException {

            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(PROVIDER_DID + "#key1"))).thenReturn(Result.success(PROVIDER_KEY.toPublicKey()));
            var credentialMessage = createCredentialMessage(createCredentialContainer());
            identityHub.getCredentialsEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + generateSiToken())
                    .body(credentialMessage)
                    .post("/v1/participants/" + TEST_PARTICIPANT_CONTEXT_ID + "/credentials")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200);

            assertThat(credentialStore.query(QuerySpec.max()).getContent())
                    .hasSize(1)
                    .allSatisfy(vc -> assertThat(vc.getStateAsEnum()).isEqualTo(VcStatus.ISSUED));
        }

        @DisplayName("Issuer's DID not resolvable, expect HTTP 401")
        @Test
        void storeCredential_didNotResolved(IdentityHub identityHub) {
            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(PROVIDER_DID + "#key1"))).thenReturn(Result.failure("not found"));
            var credentialMessage = createCredentialMessage(createCredentialContainer());
            identityHub.getCredentialsEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + generateSiToken())
                    .body(credentialMessage)
                    .post("/v1/participants/" + TEST_PARTICIPANT_CONTEXT_ID + "/credentials")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(401)
                    .body(containsString("not found"));

        }

        @DisplayName("Issuer's auth token invalid, expect HTTP 401")
        @Test
        void storeCredential_tokenSignedWithWrongKey(IdentityHub identityHub) throws JOSEException {
            var wrongKey = new ECKeyGenerator(Curve.P_256).generate();
            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(PROVIDER_DID + "#key1"))).thenReturn(Result.success(wrongKey.toPublicKey()));

            var credentialMessage = createCredentialMessage(createCredentialContainer());
            identityHub.getCredentialsEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + generateSiToken(CONSUMER_DID, PROVIDER_DID))
                    .body(credentialMessage)
                    .post("/v1/participants/" + TEST_PARTICIPANT_CONTEXT_ID + "/credentials")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(401)
                    .body(containsString("ID token verification failed: JWT signature not valid"));
        }

        @DisplayName("CredentialMessage contains an illegal format, expect HTTP 400")
        @Test
        void storeCredential_wrongCredentialFormat(IdentityHub identityHub) throws JOSEException {
            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(PROVIDER_DID + "#key1"))).thenReturn(Result.success(PROVIDER_KEY.toPublicKey()));

            var credentialContainer = Json.createObjectBuilder()
                    .add("credentialType", "MembershipCredential")
                    .add("payload", JWT_VC_EXAMPLE)
                    .add("format", "illegalFormat")
                    .build();

            var credentialMessage = createCredentialMessage(credentialContainer);
            identityHub.getCredentialsEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + generateSiToken())
                    .body(credentialMessage)
                    .post("/v1/participants/" + TEST_PARTICIPANT_CONTEXT_ID + "/credentials")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400)
                    .body(containsString("Invalid format"));
        }

        @DisplayName("Store LD credential successfully")
        @Test
        void storeCredential_jsonLdCredential(IdentityHub identityHub, CredentialStore credentialStore) throws JOSEException {
            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(PROVIDER_DID + "#key1"))).thenReturn(Result.success(PROVIDER_KEY.toPublicKey()));

            var credentialContainer = Json.createObjectBuilder()
                    .add("credentialType", "MembershipCredential")
                    .add("payload", VC_EXAMPLE_2)
                    .add("format", CredentialFormat.VC1_0_LD.toString())
                    .build();

            var credentialMessage = createCredentialMessage(credentialContainer);
            identityHub.getCredentialsEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + generateSiToken())
                    .body(credentialMessage)
                    .post("/v1/participants/" + TEST_PARTICIPANT_CONTEXT_ID + "/credentials")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200);

            assertThat(credentialStore.query(QuerySpec.max()).getContent())
                    .hasSize(1)
                    .allSatisfy(vc -> assertThat(vc.getVerifiableCredential().format()).isEqualTo(CredentialFormat.VC1_0_LD));
        }

        @DisplayName("No corresponding holder credential request was found, expect HTTP 404")
        @Test
        void storeCredential_whenNoCredentialRequest(IdentityHub identityHub) throws JOSEException {
            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(PROVIDER_DID + "#key1"))).thenReturn(Result.success(PROVIDER_KEY.toPublicKey()));
            var credentialMessage = createCredentialMessage("another_holder_pid", createCredentialContainer());

            identityHub.getCredentialsEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + generateSiToken())
                    .body(credentialMessage)
                    .post("/v1/participants/" + TEST_PARTICIPANT_CONTEXT_ID + "/credentials")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(404);
        }

        @DisplayName("Corresponding holder credential request is not in state REQUESTED, expect 400")
        @Test
        void storeCredential_whenCredentialRequestInWrongState(IdentityHub identityHub) throws JOSEException {

            identityHub.storeHolderRequest(HolderCredentialRequest.Builder.newInstance()
                    .id("test-holder-id")
                    .issuerDid(PROVIDER_DID)
                    .participantContextId(TEST_PARTICIPANT_CONTEXT_ID)
                    .requestedCredential("example-cred-id", "ExamplePersonCredential", CredentialFormat.VC1_0_JWT.toString())
                    .state(CREATED.code())
                    .participantContextId(PROVIDER_DID)
                    .build());

            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(PROVIDER_DID + "#key1"))).thenReturn(Result.success(PROVIDER_KEY.toPublicKey()));
            var credentialMessage = createCredentialMessage(createCredentialContainer());

            identityHub.getCredentialsEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + generateSiToken())
                    .body(credentialMessage)
                    .post("/v1/participants/" + TEST_PARTICIPANT_CONTEXT_ID + "/credentials")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400)
                    .body(containsString("HolderCredentialRequest is expected to be in any of the states '[REQUESTED, ISSUED]' but was 'CREATED'"));
        }

        @DisplayName("Corresponding holder credential request was made for a different credential format, expect 400")
        @Test
        void storeCredential_whenFormatNotRequested(IdentityHub identityHub) throws JOSEException {

            identityHub.storeHolderRequest(HolderCredentialRequest.Builder.newInstance()
                    .id("test-holder-id")
                    .issuerDid(PROVIDER_DID)
                    .participantContextId(TEST_PARTICIPANT_CONTEXT_ID)
                    .requestedCredential("test-cred-id", "TestCredential", CredentialFormat.VC1_0_JWT.toString())
                    .state(REQUESTED.code())
                    .participantContextId(PROVIDER_DID)
                    .build());

            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(PROVIDER_DID + "#key1"))).thenReturn(Result.success(PROVIDER_KEY.toPublicKey()));
            var credentialMessage = createCredentialMessage(createCredentialContainer());

            identityHub.getCredentialsEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + generateSiToken())
                    .body(credentialMessage)
                    .post("/v1/participants/" + TEST_PARTICIPANT_CONTEXT_ID + "/credentials")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403)
                    .body(containsString("No credential request was made for Credentials "));
        }

        // A3.16: CredentialMessage with status=REJECTED -> 2xx, nothing stored, holder request ends in an error/rejected state, NOT in ISSUED (currently still transitions to ISSUED)
        @Disabled("documents intended behavior, not yet implemented (catalog A3.16)")
        @DisplayName("CredentialMessage with status=REJECTED must not store anything and must not transition the request to ISSUED")
        @Test
        void storeCredential_whenStatusRejected_shouldNotStoreAndNotTransitionToIssued(IdentityHub identityHub, CredentialStore credentialStore, HolderCredentialRequestStore requestStore) throws JOSEException {
            // arrange: valid issuer key + a REJECTED message correlating to the pending request from setup()
            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(PROVIDER_DID + "#key1"))).thenReturn(Result.success(PROVIDER_KEY.toPublicKey()));

            var rejectedMessage = Json.createObjectBuilder()
                    .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(STATUS_TERM), "REJECTED")
                    .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(ISSUER_PID_TERM), "test-request-id")
                    .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(HOLDER_PID_TERM), "test-holder-id")
                    .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(CREDENTIALS_TERM), Json.createArrayBuilder()
                            .add(Json.createObjectBuilder()
                                    .add(JsonLdKeywords.TYPE, JsonLdKeywords.JSON)
                                    .add(JsonLdKeywords.VALUE, Json.createArrayBuilder().build())))
                    .build();

            // act: rejection notices are accepted with a 2xx status
            identityHub.getCredentialsEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + generateSiToken())
                    .body(rejectedMessage)
                    .post("/v1/participants/" + TEST_PARTICIPANT_CONTEXT_ID + "/credentials")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200);

            // assert: nothing was stored
            assertThat(credentialStore.query(QuerySpec.max()).getContent()).isEmpty();
            // TODO: assert requestStore.findById("test-holder-id") ends in an error/rejected state, NOT in ISSUED
        }

        // A3.18: delivery signed by a DIFFERENT issuer DID than the one the request was addressed to (valid SI token for that other DID, correct aud) -> 4xx, nothing stored (currently accepted when type/format match)
        @Disabled("documents intended behavior, not yet implemented (catalog A3.18)")
        @DisplayName("Delivery from a different issuer than the request's issuerDid must be rejected")
        @Test
        void storeCredential_fromDifferentIssuerThanRequested_shouldReject(IdentityHub identityHub, CredentialStore credentialStore) throws JOSEException {
            // arrange: the pending request from setup() was addressed to PROVIDER_DID, but the delivery comes from another (resolvable) issuer DID
            var otherIssuerDid = "did:web:other-issuer";
            var otherIssuerKey = generateEcKey(otherIssuerDid + "#key1");
            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(otherIssuerDid + "#key1"))).thenReturn(Result.success(otherIssuerKey.toPublicKey()));

            // formally valid SI token (iss=sub=other issuer, correct aud), just not from the request's issuer
            var accessToken = generateJwt(CONSUMER_DID, CONSUMER_DID, otherIssuerDid, Map.of(), CONSUMER_KEY);
            var siToken = generateJwt(CONSUMER_DID, otherIssuerDid, otherIssuerDid, Map.of("token", accessToken), otherIssuerKey);

            var credentialMessage = createCredentialMessage(createCredentialContainer());

            // act + assert: must be rejected, because credentials may only be accepted from the issuer the request was sent to
            identityHub.getCredentialsEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + siToken)
                    .body(credentialMessage)
                    .post("/v1/participants/" + TEST_PARTICIPANT_CONTEXT_ID + "/credentials")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403); // TODO: exact 4xx status to be defined

            // assert: nothing was stored
            assertThat(credentialStore.query(QuerySpec.max()).getContent()).isEmpty();
        }

        // A3.19: delivery from an issuer that is not trusted by the holder -> 4xx, nothing stored (no trusted-issuer list exists yet; CredentialWriterImpl has a "todo: only allow trusted issuers")
        @Disabled("documents intended behavior, not yet implemented (catalog A3.19)")
        @DisplayName("Delivery from an untrusted issuer must be rejected")
        @Test
        void storeCredential_fromUntrustedIssuer_shouldReject(IdentityHub identityHub, CredentialStore credentialStore) throws JOSEException {
            // arrange: token verification succeeds, but the issuer is not on the holder's trusted-issuer list
            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(PROVIDER_DID + "#key1"))).thenReturn(Result.success(PROVIDER_KEY.toPublicKey()));
            var credentialMessage = createCredentialMessage(createCredentialContainer());

            // TODO: configure the holder's trusted-issuer list (feature does not exist yet) such that PROVIDER_DID is NOT trusted
            // TODO: act - POST the message with a valid SI token (generateSiToken()), expect 4xx
            // TODO: assert - credentialStore contains no credentials
        }

        // A3.22: credential payload whose JWT signature does not verify against the issuer's DID key (tampered payload) -> 4xx, nothing stored (currently stored without cryptographic verification)
        @Disabled("documents intended behavior, not yet implemented (catalog A3.22)")
        @DisplayName("Credential payload with an invalid JWT signature must be rejected")
        @Test
        void storeCredential_tamperedCredentialSignature_shouldReject(IdentityHub identityHub, CredentialStore credentialStore) throws JOSEException {
            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(PROVIDER_DID + "#key1"))).thenReturn(Result.success(PROVIDER_KEY.toPublicKey()));

            // arrange: corrupt the signature part of the credential JWT
            var tamperedPayload = JWT_VC_EXAMPLE.substring(0, JWT_VC_EXAMPLE.length() - 4) + "AAAA";
            var credentialContainer = Json.createObjectBuilder()
                    .add("credentialType", "MembershipCredential")
                    .add("payload", tamperedPayload)
                    .add("format", CredentialFormat.VC1_0_JWT.toString())
                    .build();
            var credentialMessage = createCredentialMessage(credentialContainer);

            // act + assert: the credential must be verified against the issuer's public key resolved from its DID
            identityHub.getCredentialsEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + generateSiToken())
                    .body(credentialMessage)
                    .post("/v1/participants/" + TEST_PARTICIPANT_CONTEXT_ID + "/credentials")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400); // TODO: exact 4xx status to be defined

            // assert: nothing was stored
            assertThat(credentialStore.query(QuerySpec.max()).getContent()).isEmpty();
        }

        // A3.23: credential whose credentialSubject.id is NOT the holder participant's DID -> 4xx, nothing stored (currently stored)
        @Disabled("documents intended behavior, not yet implemented (catalog A3.23)")
        @DisplayName("Credential whose credentialSubject.id is not the holder's DID must be rejected")
        @Test
        void storeCredential_subjectNotHolderDid_shouldReject(IdentityHub identityHub, CredentialStore credentialStore) throws JOSEException {
            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(PROVIDER_DID + "#key1"))).thenReturn(Result.success(PROVIDER_KEY.toPublicKey()));

            // TODO: arrange - create a correctly signed VC (PROVIDER_KEY) whose credentialSubject.id is a DID other than the holder's (e.g. "did:web:someone-else")
            // TODO: act - POST the CredentialMessage with a valid SI token, expect 4xx (subject-binding check)
            // TODO: assert - credentialStore contains no credentials
        }

        // A3.10: replaying the exact same SI token for a second delivery -> 401. NOTE: requires a runtime/config variant with "edc.iam.accesstoken.jti.validation=true" (not set on the shared runtime used here)
        @Disabled("TODO: implement (catalog A3.10)")
        @DisplayName("Replaying the same SI token for a second delivery must return 401 when jti validation is enabled")
        @Test
        void storeCredential_replayedSiToken_shouldReturn401(IdentityHub identityHub, CredentialStore credentialStore) throws JOSEException {
            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(PROVIDER_DID + "#key1"))).thenReturn(Result.success(PROVIDER_KEY.toPublicKey()));

            // arrange: a single SI token that will be used for two deliveries
            var siToken = generateSiToken();
            var credentialMessage = createCredentialMessage(createCredentialContainer());

            // act: first delivery succeeds
            identityHub.getCredentialsEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + siToken)
                    .body(credentialMessage)
                    .post("/v1/participants/" + TEST_PARTICIPANT_CONTEXT_ID + "/credentials")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200);

            // TODO: act - replay the exact same siToken for a second delivery (requests in state ISSUED still accept deliveries)
            // TODO: assert - second delivery returns 401 because the token's jti was already consumed
        }

        private void createParticipant(IdentityHub identityHub) {
            createParticipant(identityHub, TEST_PARTICIPANT_CONTEXT_ID, CONSUMER_KEY);
        }

        private void createParticipant(IdentityHub identityHub, String participantContextId, ECKey participantKey) {
            var service = identityHub.getService(IdentityHubParticipantContextService.class);
            var vault = identityHub.getService(Vault.class);

            var privateKeyAlias = "%s-privatekey-alias".formatted(participantContextId);
            vault.storeSecret(privateKeyAlias, participantKey.toJSONString());
            var manifest = ParticipantManifest.Builder.newInstance()
                    .participantContextId(participantContextId)
                    .did("did:web:%s".formatted(participantContextId.replace("did:web:", "")))
                    .active(true)
                    .key(KeyDescriptor.Builder.newInstance()
                            .usage(Set.of(KeyPairUsage.PRESENTATION_SIGNING))
                            .publicKeyJwk(participantKey.toPublicJWK().toJSONObject())
                            .privateKeyAlias(privateKeyAlias)
                            .keyId(participantKey.getKeyID())
                            .build())
                    .build();
            service.createParticipantContext(manifest)
                    .orElseThrow(f -> new RuntimeException(f.getFailureDetail()));
        }

        private JsonObject createCredentialMessage(JsonObject... credentials) {
            return createCredentialMessage("test-holder-id", credentials);
        }

        private JsonObject createCredentialMessage(String holderPid, JsonObject... credentials) {
            var credentialContainers = Json.createArrayBuilder();

            Arrays.stream(credentials).forEach(credentialContainers::add);
            var credentialsJsonArray = Json.createArrayBuilder()
                    .add(Json.createObjectBuilder()
                            .add(JsonLdKeywords.TYPE, JsonLdKeywords.JSON)
                            .add(JsonLdKeywords.VALUE, credentialContainers.build()));

            return Json.createObjectBuilder()
                    .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(STATUS_TERM), "ISSUED")
                    .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(ISSUER_PID_TERM), "test-request-id")
                    .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(HOLDER_PID_TERM), holderPid)
                    .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(CREDENTIALS_TERM), credentialsJsonArray)
                    .build();
        }

        private JsonObject createCredentialContainer() {
            return Json.createObjectBuilder()
                    .add("credentialType", "MembershipCredential")
                    .add("payload", JWT_VC_EXAMPLE)
                    .add("format", CredentialFormat.VC1_0_JWT.toString())
                    .build();
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
                .build()
                .registerServiceMock(DidPublicKeyResolver.class, DID_PUBLIC_KEY_RESOLVER)
                .registerServiceMock(RevocationServiceRegistry.class, REVOCATION_LIST_REGISTRY);

    }

    @Nested
    @PostgresqlIntegrationTest
    class Postgres extends Tests {

        @Order(0)
        @RegisterExtension
        static final PostgresqlEndToEndExtension POSTGRESQL_EXTENSION = new PostgresqlEndToEndExtension();
        private static final String DB_NAME = "runtime";
        @Order(1)
        @RegisterExtension
        static final BeforeAllCallback POSTGRES_CONTAINER_STARTER = context -> {
            POSTGRESQL_EXTENSION.createDatabase(DB_NAME);
        };


        @Order(2)
        @RegisterExtension
        static final RuntimeExtension IDENTITY_HUB_EXTENSION = ComponentRuntimeExtension.Builder.newInstance()
                .name(IH_RUNTIME_NAME)
                .modules(DefaultRuntimes.IdentityHub.SQL_MODULES)
                .endpoints(DefaultRuntimes.IdentityHub.ENDPOINTS.build())
                .configurationProvider(DefaultRuntimes.IdentityHub::config)
                .configurationProvider(() -> POSTGRESQL_EXTENSION.configFor(DB_NAME))
                .paramProvider(IdentityHub.class, IdentityHub::forContext)
                .build()
                .registerServiceMock(DidPublicKeyResolver.class, DID_PUBLIC_KEY_RESOLVER)
                .registerServiceMock(RevocationServiceRegistry.class, REVOCATION_LIST_REGISTRY);


    }
}
