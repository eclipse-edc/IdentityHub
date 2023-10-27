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
 *
 */

package org.eclipse.edc.identityhub.token.verification;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import org.assertj.core.api.Assertions;
import org.eclipse.edc.iam.did.spi.key.PublicKeyWrapper;
import org.eclipse.edc.identitytrust.validation.JwtValidator;
import org.eclipse.edc.identitytrust.verification.JwtVerifier;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static org.eclipse.edc.identityhub.junit.testfixtures.JwtCreationUtil.CONSUMER_KEY;
import static org.eclipse.edc.identityhub.junit.testfixtures.JwtCreationUtil.PROVIDER_KEY;
import static org.eclipse.edc.identityhub.junit.testfixtures.JwtCreationUtil.TEST_SCOPE;
import static org.eclipse.edc.identityhub.junit.testfixtures.JwtCreationUtil.generateJwt;
import static org.eclipse.edc.identityhub.junit.testfixtures.JwtCreationUtil.generateSiToken;
import static org.eclipse.edc.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.generateEcKey;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

class AccessTokenVerifierImplTest {
    public static final String OWN_DID = "did:web:consumer";
    private static final String OTHER_PARTICIPANT_DID = "did:web:provider";
    private final JwtVerifier jwtVerifierMock = mock();
    private final JwtValidator jwtValidatorMock = mock();
    private final PublicKeyWrapper pkWrapper = mock();
    private final AccessTokenVerifierImpl verifier = new AccessTokenVerifierImpl(jwtVerifierMock, jwtValidatorMock, OWN_DID, pkWrapper);

    @BeforeEach
    void setup() throws JOSEException {
        when(jwtValidatorMock.validateToken(any(), any())).thenAnswer(a -> success(convert(a.getArgument(0, TokenRepresentation.class))));
        when(jwtVerifierMock.verify(any(), eq(OWN_DID))).thenReturn(success());
        when(pkWrapper.verifier()).thenReturn(new ECDSAVerifier(CONSUMER_KEY));
    }

    private ClaimToken convert(TokenRepresentation argument) {
        try {
            var ctb = ClaimToken.Builder.newInstance();
            SignedJWT.parse(argument.getToken()).getJWTClaimsSet().getClaims().forEach(ctb::claim);
            return ctb.build();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void verify_validJwt() {
        assertThat(verifier.verify(generateSiToken(OWN_DID, OWN_DID, OTHER_PARTICIPANT_DID, OTHER_PARTICIPANT_DID)))
                .isSucceeded()
                .satisfies(strings -> Assertions.assertThat(strings).containsOnly(TEST_SCOPE));
    }


    @Test
    void verify_jwtVerifierFails() {
        when(jwtVerifierMock.verify(any(), eq(OWN_DID))).thenReturn(failure("test-failure"));
        assertThat(verifier.verify(generateSiToken(OWN_DID, OWN_DID, OTHER_PARTICIPANT_DID, OTHER_PARTICIPANT_DID))).isFailed()
                .detail().contains("test-failure");
    }

    @Test
    void verify_jwtValidatorFails() {
        reset(jwtValidatorMock);
        when(jwtValidatorMock.validateToken(any(), eq(OWN_DID))).thenReturn(failure("test-failure"));
        assertThat(verifier.verify(generateSiToken(OWN_DID, OWN_DID, OTHER_PARTICIPANT_DID, OTHER_PARTICIPANT_DID))).isFailed()
                .detail().contains("test-failure");
    }

    @Test
    void verify_noAccessTokenClaim() {
        var claimToken = ClaimToken.Builder.newInstance()
                .claim("iss", "did:web:provider")
                .claim("aud", OWN_DID)
                .claim("sub", "BPN0001")
                .claim("exp", Instant.now().toString())
                .claim("nbf", Instant.now().minus(1, ChronoUnit.DAYS).toString())
                .claim("jti", UUID.randomUUID().toString())
                // "access_token" claim is missing
                .build();

        reset(jwtValidatorMock);
        when(jwtValidatorMock.validateToken(any(), eq(OWN_DID))).thenReturn(success(claimToken));

        assertThat(verifier.verify(generateSiToken(OWN_DID, OWN_DID, OTHER_PARTICIPANT_DID, OTHER_PARTICIPANT_DID))).isFailed()
                .detail().contains("No 'access_token' claim was found on ID Token.");
    }

    @Test
    void verify_accessTokenSignatureInvalid() {
        var spoofedKey = generateEcKey();
        var accessToken = generateJwt(OWN_DID, OWN_DID, OTHER_PARTICIPANT_DID, Map.of("scope", TEST_SCOPE), spoofedKey);
        var siToken = generateJwt(OWN_DID, OTHER_PARTICIPANT_DID, OTHER_PARTICIPANT_DID, Map.of("client_id", OTHER_PARTICIPANT_DID, "access_token", accessToken), PROVIDER_KEY);


        assertThat(verifier.verify(siToken))
                .isFailed().detail().isEqualTo("Could not verify access_token: Invalid Signature");
    }

    @Test
    void verify_accessTokenSubNotEqualToSub_shouldFail() {

    }

    @Test
    void verify_accessTokenDoesNotContainScope() {
        var accessToken = generateJwt(OWN_DID, OWN_DID, OTHER_PARTICIPANT_DID, Map.of(/*scope missing*/), CONSUMER_KEY);
        var siToken = generateJwt(OWN_DID, OTHER_PARTICIPANT_DID, OTHER_PARTICIPANT_DID, Map.of("client_id", OTHER_PARTICIPANT_DID, "access_token", accessToken), PROVIDER_KEY);

        assertThat(verifier.verify(siToken)).isFailed()
                .detail().contains("No scope claim was found on the access_token");
    }


}