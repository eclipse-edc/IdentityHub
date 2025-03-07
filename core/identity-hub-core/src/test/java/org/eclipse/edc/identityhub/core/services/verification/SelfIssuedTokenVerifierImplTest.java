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

import org.assertj.core.api.Assertions;
import org.eclipse.edc.identityhub.publickey.KeyPairResourcePublicKeyResolver;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.JwtCreationUtil;
import org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.VerifiableCredentialTestUtil;
import org.eclipse.edc.junit.assertions.AbstractResultAssert;
import org.eclipse.edc.keys.spi.PublicKeyResolver;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SelfIssuedTokenVerifierImplTest {
    public static final String OWN_DID = "did:web:consumer";
    public static final String PARTICIPANT_CONTEXT_ID = "did:web:test_participant";
    private static final String OTHER_PARTICIPANT_DID = "did:web:provider";
    private final TokenValidationService tokenValidationSerivce = mock();
    private final TokenValidationRulesRegistry tokenValidationRulesRegistry = mock();
    private final PublicKeyResolver pkResolver = mock();
    private final ClaimToken idToken = ClaimToken.Builder.newInstance()
            .claim("token", "test-at")
            .claim("scope", "org.eclipse.edc.vc.type:AlumniCredential:read")
            .build();
    private final KeyPairResourcePublicKeyResolver localPublicKeyResolver = mock();
    private final ParticipantContextService participantContextService = mock();
    private final SelfIssuedTokenVerifierImpl verifier = new SelfIssuedTokenVerifierImpl(tokenValidationSerivce, localPublicKeyResolver, tokenValidationRulesRegistry, pkResolver, participantContextService);

    @BeforeEach
    void beforeEach() {
        when(participantContextService.getParticipantContext(anyString())).thenReturn(ServiceResult.success(createParticipantContext()));
    }

    @Test
    void verify_validSiToken_validAccessToken() {
        when(tokenValidationSerivce.validate(anyString(), any(), anyList()))
                .thenReturn(Result.success(idToken));
        AbstractResultAssert.assertThat(verifier.verify(JwtCreationUtil.generateSiToken(OWN_DID, OTHER_PARTICIPANT_DID), PARTICIPANT_CONTEXT_ID))
                .isSucceeded()
                .satisfies(strings -> Assertions.assertThat(strings).containsOnly(JwtCreationUtil.TEST_SCOPE));
        verify(tokenValidationSerivce, times(2)).validate(anyString(), any(PublicKeyResolver.class), anyList());

    }

    @Test
    void verify_siTokenValidationFails() {
        when(tokenValidationSerivce.validate(anyString(), any(), anyList()))
                .thenReturn(Result.failure("test-failure"));
        AbstractResultAssert.assertThat(verifier.verify(JwtCreationUtil.generateSiToken(OWN_DID, OTHER_PARTICIPANT_DID), PARTICIPANT_CONTEXT_ID)).isFailed()
                .detail().contains("test-failure");
    }

    @Test
    void verify_noAccessTokenClaim() {
        when(tokenValidationSerivce.validate(anyString(), any(PublicKeyResolver.class), anyList()))
                .thenReturn(Result.failure("no access token"));

        AbstractResultAssert.assertThat(verifier.verify(JwtCreationUtil.generateSiToken(OWN_DID, OTHER_PARTICIPANT_DID), PARTICIPANT_CONTEXT_ID)).isFailed()
                .detail().contains("no access token");
        verify(tokenValidationSerivce).validate(anyString(), any(PublicKeyResolver.class), anyList());
    }

    @Test
    void verify_accessTokenValidationFails() {
        var spoofedKey = VerifiableCredentialTestUtil.generateEcKey("spoofed-key");
        var accessToken = JwtCreationUtil.generateJwt(OWN_DID, OWN_DID, OTHER_PARTICIPANT_DID, Map.of("scope", JwtCreationUtil.TEST_SCOPE), spoofedKey);
        var siToken = JwtCreationUtil.generateJwt(OWN_DID, OTHER_PARTICIPANT_DID, OTHER_PARTICIPANT_DID, Map.of("client_id", OTHER_PARTICIPANT_DID, "access_token", accessToken), JwtCreationUtil.PROVIDER_KEY);

        when(tokenValidationSerivce.validate(anyString(), any(), anyList())).thenReturn(Result.failure("test-failure"));
        AbstractResultAssert.assertThat(verifier.verify(siToken, PARTICIPANT_CONTEXT_ID)).isFailed()
                .detail().isEqualTo("test-failure");
    }

    @Test
    void verify_accessTokenDoesNotContainScopeClaim() {
        var accessToken = JwtCreationUtil.generateJwt(OWN_DID, OWN_DID, OTHER_PARTICIPANT_DID, Map.of(/*scope missing*/), JwtCreationUtil.CONSUMER_KEY);
        var siToken = JwtCreationUtil.generateJwt(OWN_DID, OTHER_PARTICIPANT_DID, OTHER_PARTICIPANT_DID, Map.of("client_id", OTHER_PARTICIPANT_DID, "access_token", accessToken), JwtCreationUtil.PROVIDER_KEY);

        when(tokenValidationSerivce.validate(anyString(), any(), anyList())).thenReturn(Result.success(idToken));
        when(tokenValidationSerivce.validate(anyString(), any(), anyList())).thenReturn(Result.failure("test-failure"));

        AbstractResultAssert.assertThat(verifier.verify(siToken, PARTICIPANT_CONTEXT_ID))
                .isFailed()
                .detail().contains("test-failure");
    }


    private ParticipantContext createParticipantContext(String did) {
        return ParticipantContext.Builder.newInstance()
                .apiTokenAlias("token-alias")
                .participantContextId(did)
                .did(did)
                .build();
    }

    private ParticipantContext createParticipantContext() {
        return createParticipantContext(PARTICIPANT_CONTEXT_ID);
    }
}