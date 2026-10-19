/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
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

package org.eclipse.edc.identityhub.core.services.verification;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.identityhub.accesstoken.rules.ClaimIsPresentRule;
import org.eclipse.edc.identityhub.publickey.KeyPairResourcePublicKeyResolver;
import org.eclipse.edc.identityhub.spi.participantcontext.IdentityHubParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.IdentityHubParticipantContext;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.jwt.validation.jti.JtiValidationEntry;
import org.eclipse.edc.jwt.validation.jti.JtiValidationStore;
import org.eclipse.edc.keys.spi.PublicKeyResolver;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.token.TokenValidationRulesRegistryImpl;
import org.eclipse.edc.token.TokenValidationServiceImpl;
import org.eclipse.edc.token.rules.ExpirationIssuedAtValidationRule;
import org.eclipse.edc.token.rules.JtiValidationRule;
import org.eclipse.edc.token.rules.NotBeforeValidationRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.eclipse.edc.identityhub.spi.verification.SelfIssuedTokenConstants.ACCESS_TOKEN_SCOPE_CLAIM;
import static org.eclipse.edc.identityhub.spi.verification.SelfIssuedTokenConstants.DCP_PRESENTATION_ACCESS_TOKEN_CONTEXT;
import static org.eclipse.edc.identityhub.spi.verification.SelfIssuedTokenConstants.DCP_PRESENTATION_SELF_ISSUED_TOKEN_CONTEXT;
import static org.eclipse.edc.identityhub.spi.verification.SelfIssuedTokenConstants.TOKEN_CLAIM;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ComponentTest
class SelfIssuedTokenVerifierImplComponentTest {

    public static final String STS_PUBLIC_KEY_ID = "did:web:test_participant#sts-key-123";
    public static final String PARTICIPANT_CONTEXT_ID = "test_participant";
    public static final String PARTICIPANT_DID = "did:web:test_participant";
    private final IdentityHubParticipantContextService participantContextService = mock();
    private SelfIssuedTokenVerifierImpl verifier;
    private KeyPair stsKeyPair; // this is used to sign the access token
    private KeyPair providerKeyPair; // this is used to sign the incoming SI token
    private KeyPairGenerator generator;
    private JtiValidationStore jtiValidationStore;
    private PublicKeyResolver senderKeyResolver; // resolves the key of the SI token from the sender's DID

    @BeforeEach
    void setUp() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        stsKeyPair = generator.generateKeyPair();
        providerKeyPair = generator.generateKeyPair();

        var tokenValidationService = new TokenValidationServiceImpl();
        TokenValidationRulesRegistryImpl ruleRegistry = new TokenValidationRulesRegistryImpl();

        // would normally get registered in an extension.
        var accessTokenRule = new ClaimIsPresentRule(TOKEN_CLAIM);
        ruleRegistry.addRule(DCP_PRESENTATION_SELF_ISSUED_TOKEN_CONTEXT, accessTokenRule);
        ruleRegistry.addRule(DCP_PRESENTATION_SELF_ISSUED_TOKEN_CONTEXT, new ExpirationIssuedAtValidationRule(Clock.systemUTC(), 5, false));

        var scopeIsPresentRule = new ClaimIsPresentRule(ACCESS_TOKEN_SCOPE_CLAIM);
        ruleRegistry.addRule(DCP_PRESENTATION_ACCESS_TOKEN_CONTEXT, scopeIsPresentRule);
        ruleRegistry.addRule(DCP_PRESENTATION_ACCESS_TOKEN_CONTEXT, new ExpirationIssuedAtValidationRule(Clock.systemUTC(), 5, false));
        ruleRegistry.addRule(DCP_PRESENTATION_ACCESS_TOKEN_CONTEXT, new NotBeforeValidationRule(Clock.systemUTC(), 5, true));

        var entry = new JtiValidationEntry("test-jti", null);
        jtiValidationStore = mock(JtiValidationStore.class);
        when(jtiValidationStore.findById(anyString())).thenReturn(entry);
        when(jtiValidationStore.storeEntry(entry)).thenReturn(StoreResult.success());
        ruleRegistry.addRule(DCP_PRESENTATION_ACCESS_TOKEN_CONTEXT, new JtiValidationRule(jtiValidationStore, mock()));

        var resolverMock = mock(KeyPairResourcePublicKeyResolver.class);
        when(resolverMock.resolveKey(anyString(), anyString())).thenReturn(Result.success(stsKeyPair.getPublic()));

        when(participantContextService.getParticipantContext(anyString()))
                .thenReturn(ServiceResult.success(IdentityHubParticipantContext.Builder.newInstance().did(PARTICIPANT_DID).participantContextId(PARTICIPANT_CONTEXT_ID).apiTokenAlias("foobar").build()));
        when(participantContextService.rejectIfDeactivated(any())).thenCallRealMethod();
        senderKeyResolver = mock();
        when(senderKeyResolver.resolveKey(anyString())).thenReturn(Result.success(providerKeyPair.getPublic()));
        verifier = new SelfIssuedTokenVerifierImpl(tokenValidationService, resolverMock, ruleRegistry, senderKeyResolver, participantContextService, Clock.systemUTC());
    }


    @Test
    void selfIssuedTokenNotVerified() {
        var spoofedKey = generator.generateKeyPair().getPrivate();

        var selfIssuedIdToken = createSignedJwt(spoofedKey, new JWTClaimsSet.Builder().claim("foo", "bar").jwtID(UUID.randomUUID().toString())
                .claim("token", validAccessToken())
                .issuer(PARTICIPANT_DID)
                .build());
        assertThat(verifier.verify(selfIssuedIdToken, PARTICIPANT_CONTEXT_ID)).isFailed()
                .detail().isEqualTo("JWT signature not valid");

    }

    @Test
    void selfIssuedToken_noAccessTokenClaim() {
        var selfIssuedIdToken = createSignedJwt(providerKeyPair.getPrivate(), new JWTClaimsSet.Builder()
                .issuer(PARTICIPANT_DID)
                .subject(PARTICIPANT_DID)
                .expirationTime(inOneHour())
                .audience(PARTICIPANT_DID)
                /* missing: claims("access_token", "....") */
                .build());
        assertThat(verifier.verify(selfIssuedIdToken, PARTICIPANT_CONTEXT_ID)).isFailed()
                .detail().isEqualTo("Required claim 'token' not present on token.");
    }

    @Test
    void selfIssuedToken_audClaimDoesNotBelongToParticipant() {
        var selfIssuedIdToken = createSignedJwt(providerKeyPair.getPrivate(), new JWTClaimsSet.Builder().claim("token", validAccessToken())
                .issuer(PARTICIPANT_DID)
                .subject(PARTICIPANT_DID)
                .expirationTime(inOneHour())
                .audience("did:web:someone_else")
                .build());

        when(participantContextService.getParticipantContext(eq(PARTICIPANT_CONTEXT_ID)))
                .thenReturn(ServiceResult.success(IdentityHubParticipantContext.Builder.newInstance()
                        .did(PARTICIPANT_DID)
                        .participantContextId(PARTICIPANT_CONTEXT_ID)
                        .apiTokenAlias("foobar")
                        .build()));

        assertThat(verifier.verify(selfIssuedIdToken, PARTICIPANT_CONTEXT_ID)).isFailed()
                .detail()
                .isEqualTo("Token audience claim (aud -> [did:web:someone_else]) did not contain expected audience: %s".formatted(PARTICIPANT_DID));
    }

    @Test
    void selfIssuedToken_issuerMismatch() {

        var selfIssuedIdToken = createSignedJwt(providerKeyPair.getPrivate(), new JWTClaimsSet.Builder()
                .issuer("did:web:someone_else")
                .subject("did:web:someone_else")
                .expirationTime(inOneHour())
                .audience(PARTICIPANT_DID)
                .claim("token", "accessToken")
                .build());
        assertThat(verifier.verify(selfIssuedIdToken, PARTICIPANT_CONTEXT_ID)).isFailed()
                .detail().startsWith("kid header");
    }

    @Test
    void selfIssuedToken_issuerIsComparedLiterally() {
        // the '.' in the issuer must not match the '_' in the 'kid' header
        var selfIssuedIdToken = createSignedJwt(providerKeyPair.getPrivate(), new JWTClaimsSet.Builder()
                .issuer("did:web:test.participant")
                .audience(PARTICIPANT_DID)
                .claim("token", "accessToken")
                .build());
        assertThat(verifier.verify(selfIssuedIdToken, PARTICIPANT_CONTEXT_ID)).isFailed()
                .detail().startsWith("kid header");
    }

    @Test
    void selfIssuedToken_noAudience() {
        var selfIssuedIdToken = createSignedJwt(providerKeyPair.getPrivate(), new JWTClaimsSet.Builder().claim("token", validAccessToken())
                .issuer(PARTICIPANT_DID)
                .subject(PARTICIPANT_DID)
                .expirationTime(inOneHour())
                .build());
        assertThat(verifier.verify(selfIssuedIdToken, PARTICIPANT_CONTEXT_ID)).isFailed()
                .detail().isEqualTo("Required audience (aud) claim is missing in token");
    }

    @Test
    void selfIssuedToken_noAccessTokenAudienceClaim() {
        var accessToken = createSignedJwt(stsKeyPair.getPrivate(), new JWTClaimsSet.Builder()
                .claim("scope", "foobar")
                .expirationTime(inOneHour())
                .subject(PARTICIPANT_DID)
                .build());
        var selfIssuedIdToken = createSignedJwt(providerKeyPair.getPrivate(), new JWTClaimsSet.Builder().claim("token", accessToken)
                .issuer(PARTICIPANT_DID)
                .subject(PARTICIPANT_DID)
                .expirationTime(inOneHour())
                .audience(PARTICIPANT_DID)
                .build());
        assertThat(verifier.verify(selfIssuedIdToken, PARTICIPANT_CONTEXT_ID)).isFailed()
                .detail().isEqualTo("Required audience (aud) claim is missing in token");
    }

    @Test
    void validation_successful_withJti() {
        when(jtiValidationStore.findById(anyString())).thenReturn(null); //JTI not known
        when(jtiValidationStore.storeEntry(any())).thenReturn(StoreResult.success());

        var accessToken = createSignedJwt(stsKeyPair.getPrivate(), new JWTClaimsSet.Builder()
                .claim("scope", "foobar")
                .expirationTime(inOneHour())
                .subject(PARTICIPANT_DID)
                .audience(PARTICIPANT_DID)
                .claim("jti", UUID.randomUUID().toString())
                .build());
        var selfIssuedIdToken = createSignedJwt(providerKeyPair.getPrivate(), new JWTClaimsSet.Builder()
                .claim("token", accessToken)
                .issuer(PARTICIPANT_DID)
                .subject(PARTICIPANT_DID)
                .expirationTime(inOneHour())
                .audience(PARTICIPANT_DID)
                .build());
        assertThat(verifier.verify(selfIssuedIdToken, PARTICIPANT_CONTEXT_ID)).isSucceeded();
    }

    @Test
    void accessToken_audClaimDoesNotBelongToParticipant() {
        var accessToken = createSignedJwt(stsKeyPair.getPrivate(), new JWTClaimsSet.Builder()
                .claim("scope", "foobar")
                .expirationTime(inOneHour())
                .subject(PARTICIPANT_DID)
                .audience(PARTICIPANT_DID)
                .build());
        var selfIssuedIdToken = createSignedJwt(providerKeyPair.getPrivate(), new JWTClaimsSet.Builder().claim("token", accessToken)
                .issuer(PARTICIPANT_DID)
                .subject(PARTICIPANT_DID)
                .expirationTime(inOneHour())
                .audience("did:web:someone_else")
                .build());

        when(participantContextService.getParticipantContext(eq(PARTICIPANT_CONTEXT_ID)))
                .thenReturn(ServiceResult.success(IdentityHubParticipantContext.Builder.newInstance()
                        .did("did:web:someone_else")
                        .participantContextId(PARTICIPANT_CONTEXT_ID)
                        .apiTokenAlias("foobar")
                        .build()));

        assertThat(verifier.verify(selfIssuedIdToken, PARTICIPANT_CONTEXT_ID)).isFailed()
                .detail()
                .isEqualTo("Token audience claim (aud -> [%s]) did not contain expected audience: did:web:someone_else".formatted(PARTICIPANT_DID));
    }

    @Test
    void accessToken_participantServiceError() {
        var accessToken = createSignedJwt(stsKeyPair.getPrivate(), new JWTClaimsSet.Builder()
                .claim("scope", "foobar")
                .subject(PARTICIPANT_DID)
                .audience(PARTICIPANT_DID)
                .build());
        var selfIssuedIdToken = createSignedJwt(providerKeyPair.getPrivate(), new JWTClaimsSet.Builder().claim("token", accessToken)
                .issuer(PARTICIPANT_DID)
                .subject(PARTICIPANT_DID)
                .expirationTime(inOneHour())
                .build());
        when(participantContextService.getParticipantContext(eq(PARTICIPANT_CONTEXT_ID))).thenReturn(ServiceResult.notFound("foobar not found barbaz"));

        assertThat(verifier.verify(selfIssuedIdToken, PARTICIPANT_CONTEXT_ID)).isFailed()
                .detail()
                .isEqualTo("foobar not found barbaz");
    }

    @Test
    void accessToken_notVerified() {
        var spoofedKey = generator.generateKeyPair().getPrivate();
        var accessToken = createSignedJwt(spoofedKey, new JWTClaimsSet.Builder().claim("scope", "foobar").claim("foo", "bar").build());
        var siToken = createSignedJwt(providerKeyPair.getPrivate(), new JWTClaimsSet.Builder()
                .claim("token", accessToken)
                .issuer(PARTICIPANT_DID)
                .subject(PARTICIPANT_DID)
                .expirationTime(inOneHour())
                .audience(PARTICIPANT_DID)
                .build());

        assertThat(verifier.verify(siToken, PARTICIPANT_CONTEXT_ID)).isFailed()
                .detail().isEqualTo("JWT signature not valid");
    }

    @Test
    void accessToken_noScopeClaim() {
        var accessToken = createSignedJwt(stsKeyPair.getPrivate(), new JWTClaimsSet.Builder()
                /* missing: .claim("scope", "foobar") */
                .claim("foo", "bar")
                .issuer(PARTICIPANT_DID)
                .subject(PARTICIPANT_DID)
                .expirationTime(inOneHour())
                .audience(PARTICIPANT_DID)
                .build());
        var siToken = createSignedJwt(providerKeyPair.getPrivate(), new JWTClaimsSet.Builder().claim("token", accessToken)
                .issuer("did:web:test_participant")
                .subject("did:web:test_participant")
                .expirationTime(inOneHour())
                .audience("did:web:test_participant")
                .build());

        assertThat(verifier.verify(siToken, PARTICIPANT_CONTEXT_ID)).isFailed()
                .detail().isEqualTo("Required claim 'scope' not present on token.");
    }

    @Test
    void accessToken_noAudClaim() {
        var accessToken = createSignedJwt(stsKeyPair.getPrivate(), new JWTClaimsSet.Builder()
                .issuer("did:web:test_participant")
                .subject("did:web:test_participant")
                .expirationTime(inOneHour())
                .claim("scope", "foobar")
                .claim("foo", "bar")
                /*missing: .audience("did:web:test_participant") */
                .build());
        var siToken = createSignedJwt(providerKeyPair.getPrivate(), new JWTClaimsSet.Builder().claim("token", accessToken)
                .issuer("did:web:test_participant")
                .subject("did:web:test_participant")
                .expirationTime(inOneHour())
                .audience("did:web:test_participant")
                .build());

        assertThat(verifier.verify(siToken, PARTICIPANT_CONTEXT_ID)).isFailed()
                .detail().isEqualTo("Required audience (aud) claim is missing in token");
    }

    @Test
    void accessToken_jtiValidationFails() {
        when(jtiValidationStore.findById(anyString())).thenReturn(new JtiValidationEntry("tokenId"));
        when(jtiValidationStore.storeEntry(any())).thenReturn(StoreResult.alreadyExists("The JWT id 'tokenId' was already used."));

        var accessToken = createSignedJwt(stsKeyPair.getPrivate(), new JWTClaimsSet.Builder()
                .claim("scope", "foobar")
                .expirationTime(inOneHour())
                .subject(PARTICIPANT_DID)
                .audience(PARTICIPANT_DID)
                .claim("jti", UUID.randomUUID().toString())
                .build());
        var selfIssuedIdToken = createSignedJwt(providerKeyPair.getPrivate(), new JWTClaimsSet.Builder()
                .claim("token", accessToken)
                .issuer(PARTICIPANT_DID)
                .subject(PARTICIPANT_DID)
                .expirationTime(inOneHour())
                .audience(PARTICIPANT_DID)
                .build());
        assertThat(verifier.verify(selfIssuedIdToken, PARTICIPANT_CONTEXT_ID)).isFailed()
                .detail().matches("The JWT id '.*' was already used.");
    }

    @Test
    void accessToken_expired() {
        var accessToken = createSignedJwt(stsKeyPair.getPrivate(), new JWTClaimsSet.Builder()
                .claim("scope", "foobar")
                .subject(PARTICIPANT_DID)
                .audience(PARTICIPANT_DID)
                .issueTime(Date.from(Instant.now().minusSeconds(7200)))
                .expirationTime(Date.from(Instant.now().minusSeconds(3600)))
                .build());
        var siToken = createSignedJwt(providerKeyPair.getPrivate(), new JWTClaimsSet.Builder()
                .claim("token", accessToken)
                .issuer(PARTICIPANT_DID)
                .subject(PARTICIPANT_DID)
                .expirationTime(inOneHour())
                .audience(PARTICIPANT_DID)
                .build());

        assertThat(verifier.verify(siToken, PARTICIPANT_CONTEXT_ID)).isFailed()
                .detail().isEqualTo("Token has expired (exp)");
    }

    @Test
    void accessToken_noExpClaim() {
        var accessToken = createSignedJwt(stsKeyPair.getPrivate(), new JWTClaimsSet.Builder()
                .claim("scope", "foobar")
                .subject(PARTICIPANT_DID)
                .audience(PARTICIPANT_DID)
                /* missing: .expirationTime(...) */
                .build());
        var siToken = createSignedJwt(providerKeyPair.getPrivate(), new JWTClaimsSet.Builder()
                .claim("token", accessToken)
                .issuer(PARTICIPANT_DID)
                .subject(PARTICIPANT_DID)
                .expirationTime(inOneHour())
                .audience(PARTICIPANT_DID)
                .build());

        assertThat(verifier.verify(siToken, PARTICIPANT_CONTEXT_ID)).isFailed()
                .detail().isEqualTo("Required expiration time (exp) claim is missing in token");
    }

    @Test
    void accessToken_notYetValid() {
        var accessToken = createSignedJwt(stsKeyPair.getPrivate(), new JWTClaimsSet.Builder()
                .claim("scope", "foobar")
                .subject(PARTICIPANT_DID)
                .audience(PARTICIPANT_DID)
                .expirationTime(Date.from(Instant.now().plusSeconds(7200)))
                .notBeforeTime(Date.from(Instant.now().plusSeconds(3600)))
                .build());
        var siToken = createSignedJwt(providerKeyPair.getPrivate(), new JWTClaimsSet.Builder()
                .claim("token", accessToken)
                .issuer(PARTICIPANT_DID)
                .subject(PARTICIPANT_DID)
                .expirationTime(inOneHour())
                .audience(PARTICIPANT_DID)
                .build());

        assertThat(verifier.verify(siToken, PARTICIPANT_CONTEXT_ID)).isFailed()
                .detail().isEqualTo("Current date/time with leeway before the not before (nbf) claim in token");
    }

    @Test
    void assertWarning_whenSubjectClaimsMismatch() {
        var accessToken = createSignedJwt(stsKeyPair.getPrivate(), new JWTClaimsSet.Builder()
                .claim("scope", "foobar")
                .expirationTime(inOneHour())
                .audience(PARTICIPANT_DID)
                .subject("test-subject")
                .build());
        var siToken = createSignedJwt(providerKeyPair.getPrivate(), new JWTClaimsSet.Builder()
                .issuer(PARTICIPANT_DID)
                .subject(PARTICIPANT_DID)
                .expirationTime(inOneHour())
                .audience(PARTICIPANT_DID)
                .claim("token", accessToken)
                .build());

        assertThat(verifier.verify(siToken, PARTICIPANT_CONTEXT_ID)).isFailed()
                .detail()
                .startsWith("ID token [sub] claim is not equal to [token.sub] claim");
    }


    @Test
    void selfIssuedToken_issuerNotEqualToSubject() {
        var accessToken = createSignedJwt(stsKeyPair.getPrivate(), new JWTClaimsSet.Builder()
                .claim("scope", "foobar")
                .subject(PARTICIPANT_DID)
                .audience(PARTICIPANT_DID)
                .expirationTime(inOneHour())
                .build());
        var selfIssuedIdToken = createSignedJwt(providerKeyPair.getPrivate(), new JWTClaimsSet.Builder()
                .claim("token", accessToken)
                .issuer(PARTICIPANT_DID)
                .subject("did:web:someone_else")
                .audience(PARTICIPANT_DID)
                .expirationTime(inOneHour())
                .build());

        assertThat(verifier.verify(selfIssuedIdToken, PARTICIPANT_CONTEXT_ID)).isFailed()
                .detail().isEqualTo("The 'iss' and 'sub' claims must be non-null and identical.");
    }

    @Test
    void selfIssuedToken_noSubject() {
        var selfIssuedIdToken = createSignedJwt(providerKeyPair.getPrivate(), new JWTClaimsSet.Builder()
                .claim("token", validAccessToken())
                .issuer(PARTICIPANT_DID)
                .audience(PARTICIPANT_DID)
                .expirationTime(inOneHour())
                .build());

        assertThat(verifier.verify(selfIssuedIdToken, PARTICIPANT_CONTEXT_ID)).isFailed()
                .detail().isEqualTo("The 'iss' and 'sub' claims must be non-null and identical.");
    }

    @Test
    void selfIssuedToken_noExpiration() {
        var selfIssuedIdToken = createSignedJwt(providerKeyPair.getPrivate(), new JWTClaimsSet.Builder()
                .claim("token", validAccessToken())
                .issuer(PARTICIPANT_DID)
                .subject(PARTICIPANT_DID)
                .audience(PARTICIPANT_DID)
                .build());

        assertThat(verifier.verify(selfIssuedIdToken, PARTICIPANT_CONTEXT_ID)).isFailed()
                .detail().isEqualTo("Required expiration time (exp) claim is missing in token");
    }

    @Test
    void selfIssuedToken_issuedAtWithoutExpiration() {
        var selfIssuedIdToken = createSignedJwt(providerKeyPair.getPrivate(), new JWTClaimsSet.Builder()
                .claim("token", validAccessToken())
                .issuer(PARTICIPANT_DID)
                .subject(PARTICIPANT_DID)
                .audience(PARTICIPANT_DID)
                .issueTime(new Date())
                .build());

        assertThat(verifier.verify(selfIssuedIdToken, PARTICIPANT_CONTEXT_ID)).isFailed()
                .detail().isEqualTo("Required expiration time (exp) claim is missing in token");
    }

    @Test
    void selfIssuedToken_subJwkPresent() {
        var selfIssuedIdToken = createSignedJwt(providerKeyPair.getPrivate(), new JWTClaimsSet.Builder()
                .claim("token", validAccessToken())
                .claim("sub_jwk", "some-jwk")
                .issuer(PARTICIPANT_DID)
                .subject(PARTICIPANT_DID)
                .audience(PARTICIPANT_DID)
                .expirationTime(inOneHour())
                .build());

        assertThat(verifier.verify(selfIssuedIdToken, PARTICIPANT_CONTEXT_ID)).isFailed()
                .detail().isEqualTo("The 'sub_jwk' claim must not be present.");
    }

    @Test
    void accessToken_noSubjectClaim() {
        var accessToken = createSignedJwt(stsKeyPair.getPrivate(), new JWTClaimsSet.Builder()
                .claim("scope", "foobar")
                .expirationTime(inOneHour())
                .audience(PARTICIPANT_DID)
                .build());
        var selfIssuedIdToken = createSignedJwt(providerKeyPair.getPrivate(), new JWTClaimsSet.Builder()
                .claim("token", accessToken)
                .issuer(PARTICIPANT_DID)
                .subject(PARTICIPANT_DID)
                .audience(PARTICIPANT_DID)
                .expirationTime(inOneHour())
                .build());

        assertThat(verifier.verify(selfIssuedIdToken, PARTICIPANT_CONTEXT_ID)).isFailed()
                .detail()
                .isEqualTo("ID token [sub] claim is not equal to [token.sub] claim: expected '%s', got 'null'.".formatted(PARTICIPANT_DID));
    }

    @Test
    void accessTokenNotIssuedByParticipant_senderDidNotResolved() {
        var foreignKey = generator.generateKeyPair().getPrivate();
        var accessToken = createSignedJwt(foreignKey, new JWTClaimsSet.Builder()
                .claim("scope", "foobar")
                .expirationTime(inOneHour())
                .audience(PARTICIPANT_DID)
                .build());
        var siToken = createSignedJwt(providerKeyPair.getPrivate(), "did:web:attacker.internal#key1", new JWTClaimsSet.Builder()
                .claim("token", accessToken)
                .issuer("did:web:attacker.internal")
                .audience(PARTICIPANT_DID)
                .build());

        assertThat(verifier.verify(siToken, PARTICIPANT_CONTEXT_ID)).isFailed()
                .detail().isEqualTo("JWT signature not valid");
        verifyNoInteractions(senderKeyResolver);
    }

    @Test
    void accessTokenIssuedForOtherSubject_senderDidNotResolved() {
        var accessToken = createSignedJwt(stsKeyPair.getPrivate(), new JWTClaimsSet.Builder()
                .claim("scope", "foobar")
                .expirationTime(inOneHour())
                .audience(PARTICIPANT_DID)
                .subject("did:web:verifier")
                .build());
        var siToken = createSignedJwt(providerKeyPair.getPrivate(), "did:web:attacker.internal#key1", new JWTClaimsSet.Builder()
                .claim("token", accessToken)
                .issuer("did:web:attacker.internal")
                .subject("did:web:attacker.internal")
                .audience(PARTICIPANT_DID)
                .build());

        assertThat(verifier.verify(siToken, PARTICIPANT_CONTEXT_ID)).isFailed();
        verifyNoInteractions(senderKeyResolver);
    }

    @Test
    void kidNotBoundToIssuer_senderDidNotResolved() {
        var siToken = createSignedJwt(providerKeyPair.getPrivate(), "did:web:attacker.internal#key1", new JWTClaimsSet.Builder()
                .claim("token", validAccessToken())
                .issuer(PARTICIPANT_DID)
                .audience(PARTICIPANT_DID)
                .build());

        assertThat(verifier.verify(siToken, PARTICIPANT_CONTEXT_ID)).isFailed()
                .detail().startsWith("kid header");
        verifyNoInteractions(senderKeyResolver);
    }

    @Test
    void malformedDidWebIssuer_senderDidNotResolved() {
        var issuer = "did:web:example.com%2Fadmin%3F";
        var siToken = createSignedJwt(providerKeyPair.getPrivate(), issuer + "#key1", new JWTClaimsSet.Builder()
                .claim("token", validAccessToken())
                .issuer(issuer)
                .audience(PARTICIPANT_DID)
                .build());

        assertThat(verifier.verify(siToken, PARTICIPANT_CONTEXT_ID)).isFailed()
                .detail().startsWith("Malformed did:web identifier");
        verifyNoInteractions(senderKeyResolver);
    }

    private Date inOneHour() {
        return Date.from(Instant.now().plusSeconds(3600));
    }

    private String validAccessToken() {
        return createSignedJwt(stsKeyPair.getPrivate(), new JWTClaimsSet.Builder()
                .claim("scope", "foobar")
                .expirationTime(inOneHour())
                .audience(PARTICIPANT_DID)
                .subject(PARTICIPANT_DID)
                .build());
    }

    private String createSignedJwt(PrivateKey signingKey, JWTClaimsSet claimsSet) {
        return createSignedJwt(signingKey, STS_PUBLIC_KEY_ID, claimsSet);
    }

    private String createSignedJwt(PrivateKey signingKey, String keyId, JWTClaimsSet claimsSet) {
        try {
            var signer = new ECDSASigner(signingKey, Curve.P_256);
            var jwsHeader = new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(keyId).build();
            var jwt = new SignedJWT(jwsHeader, claimsSet);
            jwt.sign(signer);
            return jwt.serialize();

        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }
}