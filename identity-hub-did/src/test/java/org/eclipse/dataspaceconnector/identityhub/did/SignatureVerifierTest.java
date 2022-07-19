/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.identityhub.did;

import com.github.javafaker.Faker;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.dataspaceconnector.spi.monitor.ConsoleMonitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.junit.jupiter.api.Test;

import java.text.ParseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.buildSignedJwt;
import static org.eclipse.dataspaceconnector.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.generateEcKey;
import static org.eclipse.dataspaceconnector.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.generateVerifiableCredential;
import static org.eclipse.dataspaceconnector.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.toPublicKeyWrapper;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SignatureVerifierTest {

    private static final Faker FAKER = new Faker();

    @Test
    public void isSignedByIssuer_jwtSignedByIssuer() throws Exception {

        // Arrange
        var jwk = generateEcKey();
        var issuer = FAKER.internet().url();
        var jwt = buildSignedJwt(generateVerifiableCredential(), issuer, jwk);
        var didPublicKeyResolver = mock(DidPublicKeyResolver.class);
        when(didPublicKeyResolver.resolvePublicKey(issuer)).thenReturn(Result.success(toPublicKeyWrapper(jwk)));

        // Act
        var signatureVerifier = new SignatureVerifier(didPublicKeyResolver, new ConsoleMonitor());

        // Assert
        assertThat(signatureVerifier.isSignedByIssuer(jwt)).isTrue();
    }

    @Test
    public void isSignedByIssuer_JwtSignedByWrongIssuer() throws Exception {

        // Arrange
        var jwk = generateEcKey();
        var issuer = FAKER.internet().url();
        var jwt = buildSignedJwt(generateVerifiableCredential(), issuer, jwk);
        var didPublicKeyResolver = mock(DidPublicKeyResolver.class);
        when(didPublicKeyResolver.resolvePublicKey(issuer)).thenReturn(Result.failure("Failed resolving public key"));

        // Act
        var signatureVerifier = new SignatureVerifier(didPublicKeyResolver, new ConsoleMonitor());

        // Assert
        assertThat(signatureVerifier.isSignedByIssuer(jwt)).isFalse();
    }

    @Test
    public void isSignedByIssuer_issuerDidCantBeResolved() throws Exception {

        // Arrange
        var jwsHeader = new JWSHeader.Builder(JWSAlgorithm.ES256).build();
        var claims = new JWTClaimsSet.Builder().build();
        var jws = new SignedJWT(jwsHeader, claims);

        // Act
        var signatureVerifier = new SignatureVerifier(mock(DidPublicKeyResolver.class), new ConsoleMonitor());

        // Assert
        assertThat(signatureVerifier.isSignedByIssuer(jws)).isFalse();
    }

    @Test
    public void isSignedByIssuer_cantParsePayload() throws Exception {

        // Arrange
        var jws = mock(SignedJWT.class);
        when(jws.getJWTClaimsSet()).thenThrow(new ParseException("Failed parsing JWT payload", 0));

        // Act
        var signatureVerifier = new SignatureVerifier(mock(DidPublicKeyResolver.class), new ConsoleMonitor());

        // Assert
        assertThat(signatureVerifier.isSignedByIssuer(jws)).isFalse();
    }
}
