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

package org.eclipse.edc.identityhub.token.verification;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.identityhub.token.rules.ClaimIsPresentRule;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.TokenValidationRulesRegistryImpl;
import org.eclipse.edc.token.TokenValidationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.ECGenParameterSpec;
import java.util.UUID;

import static org.eclipse.edc.identityhub.DefaultServicesExtension.ACCESS_TOKEN_CLAIM;
import static org.eclipse.edc.identityhub.DefaultServicesExtension.ACCESS_TOKEN_SCOPE_CLAIM;
import static org.eclipse.edc.identityhub.DefaultServicesExtension.IATP_ACCESS_TOKEN_CONTEXT;
import static org.eclipse.edc.identityhub.DefaultServicesExtension.IATP_SELF_ISSUED_TOKEN_CONTEXT;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ComponentTest
class AccessTokenVerifierImplComponentTest {

    private final Monitor monitor = mock();
    private AccessTokenVerifierImpl verifier;
    private KeyPair stsKeyPair; // this is used to sign the acces token
    private KeyPair providerKeyPair; // this is used to sign the incoming SI token
    private KeyPairGenerator generator;

    @BeforeEach
    void setUp() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        stsKeyPair = generator.generateKeyPair();
        providerKeyPair = generator.generateKeyPair();

        var tokenValidationService = new TokenValidationServiceImpl();
        var ruleRegistry = new TokenValidationRulesRegistryImpl();

        // would normally get registered in an extension.
        var accessTokenRule = new ClaimIsPresentRule(ACCESS_TOKEN_CLAIM);
        ruleRegistry.addRule(IATP_SELF_ISSUED_TOKEN_CONTEXT, accessTokenRule);

        var scopeIsPresentRule = new ClaimIsPresentRule(ACCESS_TOKEN_SCOPE_CLAIM);
        ruleRegistry.addRule(IATP_ACCESS_TOKEN_CONTEXT, scopeIsPresentRule);

        verifier = new AccessTokenVerifierImpl(tokenValidationService, stsKeyPair::getPublic, ruleRegistry, monitor, (id) -> Result.success(providerKeyPair.getPublic()));
    }

    @Test
    void selfIssuedTokenNotVerified() {
        var spoofedKey = generator.generateKeyPair().getPrivate();

        var selfIssuedIdToken = createSignedJwt(spoofedKey, new JWTClaimsSet.Builder().claim("foo", "bar").jwtID(UUID.randomUUID().toString()).build());
        assertThat(verifier.verify(selfIssuedIdToken)).isFailed()
                .detail().isEqualTo("Token verification failed");

    }

    @Test
    void selfIssuedToken_noAccessTokenClaim() {
        var selfIssuedIdToken = createSignedJwt(providerKeyPair.getPrivate(), new JWTClaimsSet.Builder()/* missing: claims("access_token", "....") */.build());
        assertThat(verifier.verify(selfIssuedIdToken)).isFailed()
                .detail().isEqualTo("Required claim 'access_token' not present on token.");
    }

    @Test
    void accessToken_notVerified() {
        var spoofedKey = generator.generateKeyPair().getPrivate();
        var accessToken = createSignedJwt(spoofedKey, new JWTClaimsSet.Builder().claim("scope", "foobar").claim("foo", "bar").build());
        var siToken = createSignedJwt(providerKeyPair.getPrivate(), new JWTClaimsSet.Builder().claim("access_token", accessToken).build());

        assertThat(verifier.verify(siToken)).isFailed()
                .detail().isEqualTo("Token verification failed");
    }

    @Test
    void accessToken_noScopeClaim() {
        var accessToken = createSignedJwt(stsKeyPair.getPrivate(), new JWTClaimsSet.Builder()/* missing: .claim("scope", "foobar") */.claim("foo", "bar").build());
        var siToken = createSignedJwt(providerKeyPair.getPrivate(), new JWTClaimsSet.Builder().claim("access_token", accessToken).build());

        assertThat(verifier.verify(siToken)).isFailed()
                .detail().isEqualTo("Required claim 'scope' not present on token.");
    }

    @Test
    void assertWarning_whenSubjectClaimsMismatch() {
        var accessToken = createSignedJwt(stsKeyPair.getPrivate(), new JWTClaimsSet.Builder().claim("scope", "foobar").subject("test-subject").build());
        var siToken = createSignedJwt(providerKeyPair.getPrivate(), new JWTClaimsSet.Builder().claim("access_token", accessToken).subject("mismatching-subject").build());

        assertThat(verifier.verify(siToken)).isSucceeded();
        verify(monitor).warning(startsWith("ID token [sub] claim is not equal to [access_token.sub]"));
    }

    private String createSignedJwt(PrivateKey signingKey, JWTClaimsSet claimsSet) {
        try {
            var signer = new ECDSASigner(signingKey, Curve.P_256);
            var jwsHeader = new JWSHeader(JWSAlgorithm.ES256);
            var jwt = new SignedJWT(jwsHeader, claimsSet);
            jwt.sign(signer);
            return jwt.serialize();

        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }
}