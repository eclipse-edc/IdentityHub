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

package org.eclipse.edc.identityhub.accesstoken.verification;

import org.assertj.core.api.Assertions;
import org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.JwtCreationUtil;
import org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.VerifiableCredentialTestUtil;
import org.eclipse.edc.junit.assertions.AbstractResultAssert;
import org.eclipse.edc.keys.spi.PublicKeyResolver;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.security.PublicKey;
import java.util.Map;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccessTokenVerifierImplTest {
    public static final String OWN_DID = "did:web:consumer";
    private static final String OTHER_PARTICIPANT_DID = "did:web:provider";
    private final TokenValidationService tokenValidationSerivce = mock();
    private final Supplier<PublicKey> publicKeySupplier = Mockito::mock;
    private final TokenValidationRulesRegistry tokenValidationRulesRegistry = mock();
    private final PublicKeyResolver pkResolver = mock();
    private final AccessTokenVerifierImpl verifier = new AccessTokenVerifierImpl(tokenValidationSerivce, publicKeySupplier, tokenValidationRulesRegistry, mock(), pkResolver);
    private final ClaimToken idToken = ClaimToken.Builder.newInstance()
            .claim("token", "test-at")
            .claim("scope", "org.eclipse.edc.vc.type:SomeTestCredential:read")
            .build();

    @Test
    void verify_validSiToken_validAccessToken() {
        when(tokenValidationSerivce.validate(anyString(), any(), anyList()))
                .thenReturn(Result.success(idToken));
        AbstractResultAssert.assertThat(verifier.verify(JwtCreationUtil.generateSiToken(OWN_DID, OTHER_PARTICIPANT_DID), "did:web:test_participant"))
                .isSucceeded()
                .satisfies(strings -> Assertions.assertThat(strings).containsOnly(JwtCreationUtil.TEST_SCOPE));
        verify(tokenValidationSerivce, times(2)).validate(anyString(), any(PublicKeyResolver.class), anyList());

    }

    @Test
    void verify_siTokenValidationFails() {
        when(tokenValidationSerivce.validate(anyString(), any(), anyList()))
                .thenReturn(Result.failure("test-failure"));
        AbstractResultAssert.assertThat(verifier.verify(JwtCreationUtil.generateSiToken(OWN_DID, OTHER_PARTICIPANT_DID), "did:web:test_participant")).isFailed()
                .detail().contains("test-failure");
    }

    @Test
    void verify_noAccessTokenClaim() {
        when(tokenValidationSerivce.validate(anyString(), any(PublicKeyResolver.class), anyList()))
                .thenReturn(Result.failure("no access token"));

        AbstractResultAssert.assertThat(verifier.verify(JwtCreationUtil.generateSiToken(OWN_DID, OTHER_PARTICIPANT_DID), "did:web:test_participant")).isFailed()
                .detail().contains("no access token");
        verify(tokenValidationSerivce).validate(anyString(), any(PublicKeyResolver.class), anyList());
    }

    @Test
    void verify_accessTokenValidationFails() {
        var spoofedKey = VerifiableCredentialTestUtil.generateEcKey("spoofed-key");
        var accessToken = JwtCreationUtil.generateJwt(OWN_DID, OWN_DID, OTHER_PARTICIPANT_DID, Map.of("scope", JwtCreationUtil.TEST_SCOPE), spoofedKey);
        var siToken = JwtCreationUtil.generateJwt(OWN_DID, OTHER_PARTICIPANT_DID, OTHER_PARTICIPANT_DID, Map.of("client_id", OTHER_PARTICIPANT_DID, "access_token", accessToken), JwtCreationUtil.PROVIDER_KEY);

        when(tokenValidationSerivce.validate(anyString(), any(), anyList())).thenReturn(Result.failure("test-failure"));
        AbstractResultAssert.assertThat(verifier.verify(siToken, "did:web:test_participant")).isFailed()
                .detail().isEqualTo("test-failure");
    }

    @Test
    void verify_accessTokenSubNotEqualToSub_shouldFail() {

    }

    @Test
    void verify_accessTokenDoesNotContainScopeClaim() {
        var accessToken = JwtCreationUtil.generateJwt(OWN_DID, OWN_DID, OTHER_PARTICIPANT_DID, Map.of(/*scope missing*/), JwtCreationUtil.CONSUMER_KEY);
        var siToken = JwtCreationUtil.generateJwt(OWN_DID, OTHER_PARTICIPANT_DID, OTHER_PARTICIPANT_DID, Map.of("client_id", OTHER_PARTICIPANT_DID, "access_token", accessToken), JwtCreationUtil.PROVIDER_KEY);

        when(tokenValidationSerivce.validate(anyString(), any(), anyList())).thenReturn(Result.success(idToken));
        when(tokenValidationSerivce.validate(anyString(), any(), anyList())).thenReturn(Result.failure("test-failure"));

        AbstractResultAssert.assertThat(verifier.verify(siToken, "did:web:test_participant"))
                .isFailed()
                .detail().contains("test-failure");
    }


}