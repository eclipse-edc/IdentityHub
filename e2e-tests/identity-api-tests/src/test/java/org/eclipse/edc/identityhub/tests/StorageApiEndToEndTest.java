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
import com.nimbusds.jwt.JWTClaimsSet;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.decentralizedclaims.spi.DcpConstants.DSPACE_DCP_NAMESPACE_V_1_0;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialMessage.CREDENTIALS_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialMessage.HOLDER_PID_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialMessage.ISSUER_PID_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialMessage.REJECTION_REASON_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialMessage.STATUS_TERM;
import static org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState.CREATED;
import static org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState.ERROR;
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
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.VerifiableCredentialTestUtil.buildSignedJwt;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.VerifiableCredentialTestUtil.generateEcKey;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("JUnitMalformedDeclaration")
public class StorageApiEndToEndTest {

    private static final String DB_NAME = "runtime";

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

            // same credential as VC_EXAMPLE_2, but issued to the holder participant
            var ldCredential = VC_EXAMPLE_2.replace("did:example:ebfeb1f712ebc6f1c276e12ec21", CONSUMER_DID);

            var credentialContainer = Json.createObjectBuilder()
                    .add("credentialType", "MembershipCredential")
                    .add("payload", ldCredential)
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
        @DisplayName("A3.16: A CredentialMessage with status=REJECTED stores nothing and fails the request")
        @Test
        void storeCredential_whenStatusRejected_shouldNotStoreAndTransitionToError(IdentityHub identityHub, CredentialStore credentialStore, HolderCredentialRequestStore requestStore) throws JOSEException {
            // valid issuer key + a REJECTED message correlating to the pending request from setup()
            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(PROVIDER_DID + "#key1"))).thenReturn(Result.success(PROVIDER_KEY.toPublicKey()));

            var rejectedMessage = Json.createObjectBuilder()
                    .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(STATUS_TERM), "REJECTED")
                    .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(ISSUER_PID_TERM), "test-request-id")
                    .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(REJECTION_REASON_TERM), "attestation could not be satisfied")
                    .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(HOLDER_PID_TERM), "test-holder-id")
                    .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(CREDENTIALS_TERM), Json.createArrayBuilder()
                            .add(Json.createObjectBuilder()
                                    .add(JsonLdKeywords.TYPE, JsonLdKeywords.JSON)
                                    .add(JsonLdKeywords.VALUE, Json.createArrayBuilder().build())))
                    .build();

            // rejection notices are accepted with a 2xx status
            identityHub.getCredentialsEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + generateSiToken())
                    .body(rejectedMessage)
                    .post("/v1/participants/" + TEST_PARTICIPANT_CONTEXT_ID + "/credentials")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200);

            // nothing was stored
            assertThat(credentialStore.query(QuerySpec.max()).getContent()).isEmpty();

            // the holder request is failed, so the holder stops waiting for credentials that will never arrive
            var holderRequest = requestStore.findById("test-holder-id");
            assertThat(holderRequest).isNotNull();
            assertThat(holderRequest.stateAsEnum()).isEqualTo(ERROR);
            assertThat(holderRequest.getErrorDetail()).contains("attestation could not be satisfied");
        }

        // A3.18: delivery signed by a DIFFERENT issuer DID than the one the request was addressed to (valid SI token for that other DID, correct aud) -> 4xx, nothing stored (currently accepted when type/format match)
        @DisplayName("A3.18: A delivery from a different issuer than the request's issuerDid is rejected")
        @Test
        void storeCredential_fromDifferentIssuerThanRequested_shouldReject(IdentityHub identityHub, CredentialStore credentialStore) throws JOSEException {
            // the pending request from setup() was addressed to PROVIDER_DID, but the delivery comes from another (resolvable) issuer DID
            var otherIssuerDid = "did:web:other-issuer";
            var otherIssuerKey = generateEcKey(otherIssuerDid + "#key1");
            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(otherIssuerDid + "#key1"))).thenReturn(Result.success(otherIssuerKey.toPublicKey()));

            // formally valid SI token (iss=sub=other issuer, correct aud), just not from the request's issuer
            var accessToken = generateJwt(CONSUMER_DID, CONSUMER_DID, otherIssuerDid, Map.of(), CONSUMER_KEY);
            var siToken = generateJwt(CONSUMER_DID, otherIssuerDid, otherIssuerDid, Map.of("token", accessToken), otherIssuerKey);

            var credentialMessage = createCredentialMessage(createCredentialContainer());

            // must be rejected, because credentials may only be accepted from the issuer the request was sent to
            identityHub.getCredentialsEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + siToken)
                    .body(credentialMessage)
                    .post("/v1/participants/" + TEST_PARTICIPANT_CONTEXT_ID + "/credentials")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403); // TODO: exact 4xx status to be defined

            // nothing was stored
            assertThat(credentialStore.query(QuerySpec.max()).getContent()).isEmpty();
        }

        // A3.19: delivery from an issuer that is not trusted by the holder -> 4xx, nothing stored. An Issuer is trusted for a
        // request exactly when the Holder addressed that request to it, so an Issuer that was never asked is not trusted.
        @DisplayName("A3.19: A delivery from an issuer that is not trusted by the holder is rejected")
        @Test
        void storeCredential_fromUntrustedIssuer_shouldReject(IdentityHub identityHub, CredentialStore credentialStore) throws JOSEException {
            // token verification succeeds for this issuer, but no credential request was ever sent to it
            var untrustedIssuerDid = "did:web:untrusted-issuer";
            var untrustedIssuerKey = generateEcKey(untrustedIssuerDid + "#key1");
            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(untrustedIssuerDid + "#key1"))).thenReturn(Result.success(untrustedIssuerKey.toPublicKey()));

            var accessToken = generateJwt(CONSUMER_DID, CONSUMER_DID, untrustedIssuerDid, Map.of(), CONSUMER_KEY);
            var siToken = generateJwt(CONSUMER_DID, untrustedIssuerDid, untrustedIssuerDid, Map.of("token", accessToken), untrustedIssuerKey);

            var credentialMessage = createCredentialMessage(createCredentialContainer());

            // only trusted issuers may write credentials
            identityHub.getCredentialsEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + siToken)
                    .body(credentialMessage)
                    .post("/v1/participants/" + TEST_PARTICIPANT_CONTEXT_ID + "/credentials")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403);

            // nothing was stored
            assertThat(credentialStore.query(QuerySpec.max()).getContent()).isEmpty();
        }

        // A3.22: credential payload whose JWT signature does not verify against the issuer's DID key (tampered payload) -> 4xx, nothing stored (currently stored without cryptographic verification)
        @DisplayName("A3.22: A credential payload whose JWT signature does not verify is rejected")
        @Test
        void storeCredential_tamperedCredentialSignature_shouldReject(IdentityHub identityHub, CredentialStore credentialStore) throws JOSEException {
            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(PROVIDER_DID + "#key1"))).thenReturn(Result.success(PROVIDER_KEY.toPublicKey()));

            // a credential that would be accepted, but its signature segment is corrupted
            var credential = credentialIssuedTo(CONSUMER_DID);
            var tamperedPayload = credential.substring(0, credential.length() - 4) + "AAAA";
            var credentialContainer = Json.createObjectBuilder()
                    .add("credentialType", "MembershipCredential")
                    .add("payload", tamperedPayload)
                    .add("format", CredentialFormat.VC1_0_JWT.toString())
                    .build();
            var credentialMessage = createCredentialMessage(credentialContainer);

            // the credential must be verified against the issuer's public key resolved from its DID
            identityHub.getCredentialsEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + generateSiToken())
                    .body(credentialMessage)
                    .post("/v1/participants/" + TEST_PARTICIPANT_CONTEXT_ID + "/credentials")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400); // TODO: exact 4xx status to be defined

            // nothing was stored
            assertThat(credentialStore.query(QuerySpec.max()).getContent()).isEmpty();
        }

        // A3.22: only token-based credentials can be verified, so a credential carrying an embedded Linked-Data proof is
        // stored as it is - even one whose proof does not hold up
        @DisplayName("A3.22: A credential that is not token-based is stored without verifying its proof")
        @Test
        void storeCredential_nonTokenBasedCredential_shouldPass(IdentityHub identityHub, CredentialStore credentialStore) throws JOSEException {
            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(PROVIDER_DID + "#key1"))).thenReturn(Result.success(PROVIDER_KEY.toPublicKey()));

            // issued to the holder, but with a corrupted embedded proof, which goes unnoticed for this format
            var ldCredential = VC_EXAMPLE_2
                    .replace("did:example:ebfeb1f712ebc6f1c276e12ec21", CONSUMER_DID)
                    .replace("TCYt5XsITJX1CxPCT8yAV", "AAAAAAAAAAAAAAAAAAAAA");

            var credentialContainer = Json.createObjectBuilder()
                    .add("credentialType", "MembershipCredential")
                    .add("payload", ldCredential)
                    .add("format", CredentialFormat.VC1_0_LD.toString())
                    .build();

            identityHub.getCredentialsEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + generateSiToken())
                    .body(createCredentialMessage(credentialContainer))
                    .post("/v1/participants/" + TEST_PARTICIPANT_CONTEXT_ID + "/credentials")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200);

            assertThat(credentialStore.query(QuerySpec.max()).getContent())
                    .hasSize(1)
                    .allSatisfy(vc -> assertThat(vc.getVerifiableCredential().format()).isEqualTo(CredentialFormat.VC1_0_LD));
        }

        // A3.23: credential whose credentialSubject.id is NOT the holder participant's DID -> 4xx, nothing stored (currently stored)
        @DisplayName("A3.23: A credential whose credentialSubject.id is not the holder's DID is rejected")
        @Test
        void storeCredential_subjectNotHolderDid_shouldReject(IdentityHub identityHub, CredentialStore credentialStore) throws JOSEException {
            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(PROVIDER_DID + "#key1"))).thenReturn(Result.success(PROVIDER_KEY.toPublicKey()));

            // a correctly signed VC (PROVIDER_KEY) of the requested type, but issued to a subject
            // that is NOT the holder participant's DID (the holder's DID is did:web:consumer)
            var vcClaims = new JWTClaimsSet.Builder()
                    .claim("@context", List.of("https://www.w3.org/ns/credentials/v2"))
                    .claim("id", "http://issuer.example/credentials/foreign-subject")
                    .claim("type", List.of("VerifiableCredential", "ExamplePersonCredential"))
                    .claim("issuer", PROVIDER_DID)
                    .claim("validFrom", "2020-01-01T00:00:00Z")
                    .claim("credentialSubject", Map.of("id", "did:web:someone-else", "name", "Some Body"))
                    .build();
            var foreignSubjectVc = buildSignedJwt(vcClaims, PROVIDER_KEY).serialize();

            var credentialContainer = Json.createObjectBuilder()
                    .add("credentialType", "ExamplePersonCredential")
                    .add("payload", foreignSubjectVc)
                    .add("format", CredentialFormat.VC1_0_JWT.toString())
                    .build();
            var credentialMessage = createCredentialMessage(credentialContainer);

            // subject-binding check - the credential was issued to someone else
            identityHub.getCredentialsEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + generateSiToken())
                    .body(credentialMessage)
                    .post("/v1/participants/" + TEST_PARTICIPANT_CONTEXT_ID + "/credentials")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403);

            // nothing was stored
            assertThat(credentialStore.query(QuerySpec.max()).getContent()).isEmpty();
        }

        // A3.10: replaying the exact same SI token for a second delivery -> 401. The shared runtime enables "edc.iam.accesstoken.jti.validation=true" (see DefaultRuntimes.IdentityHub.config())
        @DisplayName("A3.10: Replaying the same SI token for a second delivery returns 401 (jti replay protection)")
        @Test
        void storeCredential_replayedSiToken_shouldReturn401(IdentityHub identityHub, CredentialStore credentialStore) throws JOSEException {
            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(PROVIDER_DID + "#key1"))).thenReturn(Result.success(PROVIDER_KEY.toPublicKey()));

            // a single SI token that will be used for two deliveries
            var siToken = generateSiToken();
            var credentialMessage = createCredentialMessage(createCredentialContainer());

            // first delivery succeeds and consumes the token's jti
            identityHub.getCredentialsEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + siToken)
                    .body(credentialMessage)
                    .post("/v1/participants/" + TEST_PARTICIPANT_CONTEXT_ID + "/credentials")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200);

            // replaying the exact same SI token must be rejected, its jti was already used
            // (the request is in state ISSUED now, which still accepts deliveries - the rejection is purely token-based)
            identityHub.getCredentialsEndpoint().baseRequest()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + siToken)
                    .body(credentialMessage)
                    .post("/v1/participants/" + TEST_PARTICIPANT_CONTEXT_ID + "/credentials")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(401);

            // only the first delivery stored a credential
            assertThat(credentialStore.query(QuerySpec.max()).getContent()).hasSize(1);
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
                    .add("payload", credentialIssuedTo(CONSUMER_DID))
                    .add("format", CredentialFormat.VC1_0_JWT.toString())
                    .build();
        }

        /**
         * A correctly signed VC of the requested type, issued to the given subject.
         */
        private String credentialIssuedTo(String subjectDid) {
            var vcClaims = new JWTClaimsSet.Builder()
                    .claim("@context", List.of("https://www.w3.org/ns/credentials/v2"))
                    .claim("id", "http://issuer.example/credentials/3732")
                    .claim("type", List.of("VerifiableCredential", "ExamplePersonCredential"))
                    .claim("issuer", PROVIDER_DID)
                    .claim("validFrom", "2020-01-01T00:00:00Z")
                    .claim("credentialSubject", Map.of("id", subjectDid, "name", "Test Person"))
                    .build();
            return buildSignedJwt(vcClaims, PROVIDER_KEY).serialize();
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
