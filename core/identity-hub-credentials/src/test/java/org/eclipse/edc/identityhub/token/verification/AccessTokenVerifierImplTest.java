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

import org.assertj.core.api.Assertions;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.PublicKeyResolver;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.security.PublicKey;
import java.util.Map;
import java.util.function.Supplier;

import static org.eclipse.edc.identityhub.junit.testfixtures.JwtCreationUtil.CONSUMER_KEY;
import static org.eclipse.edc.identityhub.junit.testfixtures.JwtCreationUtil.PROVIDER_KEY;
import static org.eclipse.edc.identityhub.junit.testfixtures.JwtCreationUtil.TEST_SCOPE;
import static org.eclipse.edc.identityhub.junit.testfixtures.JwtCreationUtil.generateJwt;
import static org.eclipse.edc.identityhub.junit.testfixtures.JwtCreationUtil.generateSiToken;
import static org.eclipse.edc.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.generateEcKey;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
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
            .claim("access_token", "test-at")
            .claim("scope", "org.eclipse.edc.vc.type:SomeTestCredential:read")
            .build();

    @Test
    void verify_validSiToken_validAccessToken() {
        when(tokenValidationSerivce.validate(anyString(), any(), anyList()))
                .thenReturn(Result.success(idToken));
        assertThat(verifier.verify(generateSiToken(OWN_DID, OWN_DID, OTHER_PARTICIPANT_DID, OTHER_PARTICIPANT_DID)))
                .isSucceeded()
                .satisfies(strings -> Assertions.assertThat(strings).containsOnly(TEST_SCOPE));
        verify(tokenValidationSerivce, times(2)).validate(anyString(), any(PublicKeyResolver.class), anyList());

    }

    @Test
    void verify_siTokenValidationFails() {
        when(tokenValidationSerivce.validate(anyString(), any(), anyList()))
                .thenReturn(Result.failure("test-failure"));
        assertThat(verifier.verify(generateSiToken(OWN_DID, OWN_DID, OTHER_PARTICIPANT_DID, OTHER_PARTICIPANT_DID))).isFailed()
                .detail().contains("test-failure");
    }

    @Test
    void verify_noAccessTokenClaim() {
        when(tokenValidationSerivce.validate(anyString(), any(PublicKeyResolver.class), anyList()))
                .thenReturn(Result.failure("no access token"));

        assertThat(verifier.verify(generateSiToken(OWN_DID, OWN_DID, OTHER_PARTICIPANT_DID, OTHER_PARTICIPANT_DID))).isFailed()
                .detail().contains("no access token");
        verify(tokenValidationSerivce).validate(anyString(), any(PublicKeyResolver.class), anyList());
    }

    @Test
    void verify_accessTokenValidationFails() {
        var spoofedKey = generateEcKey("spoofed-key");
        var accessToken = generateJwt(OWN_DID, OWN_DID, OTHER_PARTICIPANT_DID, Map.of("scope", TEST_SCOPE), spoofedKey);
        var siToken = generateJwt(OWN_DID, OTHER_PARTICIPANT_DID, OTHER_PARTICIPANT_DID, Map.of("client_id", OTHER_PARTICIPANT_DID, "access_token", accessToken), PROVIDER_KEY);

        when(tokenValidationSerivce.validate(anyString(), any(), anyList())).thenReturn(Result.failure("test-failure"));
        assertThat(verifier.verify(siToken)).isFailed()
                .detail().isEqualTo("test-failure");
    }

    @Test
    void verify_accessTokenSubNotEqualToSub_shouldFail() {

    }

    @Test
    void verify_accessTokenDoesNotContainScopeClaim() {
        var accessToken = generateJwt(OWN_DID, OWN_DID, OTHER_PARTICIPANT_DID, Map.of(/*scope missing*/), CONSUMER_KEY);
        var siToken = generateJwt(OWN_DID, OTHER_PARTICIPANT_DID, OTHER_PARTICIPANT_DID, Map.of("client_id", OTHER_PARTICIPANT_DID, "access_token", accessToken), PROVIDER_KEY);

        when(tokenValidationSerivce.validate(anyString(), any(), anyList())).thenReturn(Result.success(idToken));
        when(tokenValidationSerivce.validate(anyString(), any(), anyList())).thenReturn(Result.failure("test-failure"));

        assertThat(verifier.verify(siToken))
                .isFailed()
                .detail().contains("test-failure");
    }


}