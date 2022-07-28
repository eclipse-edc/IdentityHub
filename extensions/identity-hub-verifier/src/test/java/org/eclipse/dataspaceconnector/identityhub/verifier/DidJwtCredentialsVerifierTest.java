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

package org.eclipse.dataspaceconnector.identityhub.verifier;

import com.github.javafaker.Faker;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.dataspaceconnector.identityhub.junit.testfixtures.VerifiableCredentialTestUtil;
import org.eclipse.dataspaceconnector.spi.monitor.ConsoleMonitor;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.junit.jupiter.api.Test;

import java.text.ParseException;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Date.from;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.buildSignedJwt;
import static org.eclipse.dataspaceconnector.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.generateEcKey;
import static org.eclipse.dataspaceconnector.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.generateVerifiableCredential;
import static org.eclipse.dataspaceconnector.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.toPublicKeyWrapper;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DidJwtCredentialsVerifierTest {

    private static final Faker FAKER = new Faker();
    private static final Monitor MONITOR = new ConsoleMonitor();
    private static final ECKey JWK = generateEcKey();
    private static final ECKey ANOTHER_JWK = generateEcKey();
    private static final String ISSUER = FAKER.internet().url();
    private static final String SUBJECT = FAKER.internet().url();
    private static final String OTHER_SUBJECT = FAKER.internet().url() + "other";
    private static final SignedJWT JWT = buildSignedJwt(generateVerifiableCredential(), ISSUER, SUBJECT, JWK);
    private DidPublicKeyResolver didPublicKeyResolver = mock(DidPublicKeyResolver.class);
    private DidJwtCredentialsVerifier didJwtCredentialsVerifier = new DidJwtCredentialsVerifier(didPublicKeyResolver, MONITOR);

    @Test
    public void isSignedByIssuer_jwtSignedByIssuer() {

        // Arrange
        when(didPublicKeyResolver.resolvePublicKey(ISSUER)).thenReturn(Result.success(toPublicKeyWrapper(JWK)));

        // Assert
        assertThat(didJwtCredentialsVerifier.isSignedByIssuer(JWT)).isTrue();
    }

    @Test
    public void isSignedByIssuer_jwtSignedByWrongIssuer() {

        // Arrange
        when(didPublicKeyResolver.resolvePublicKey(ISSUER)).thenReturn(Result.success(toPublicKeyWrapper(ANOTHER_JWK)));

        // Assert
        assertThat(didJwtCredentialsVerifier.isSignedByIssuer(JWT)).isFalse();
    }

    @Test
    public void isSignedByIssuer_PublicKeyCantBeResolved() {

        // Arrange
        when(didPublicKeyResolver.resolvePublicKey(ISSUER)).thenReturn(Result.failure("Failed resolving public key"));

        // Assert
        assertThat(didJwtCredentialsVerifier.isSignedByIssuer(JWT)).isFalse();
    }

    @Test
    public void isSignedByIssuer_issuerDidCantBeResolved() throws ParseException {

        // Arrange
        when(didPublicKeyResolver.resolvePublicKey(JWT.getJWTClaimsSet().getIssuer())).thenReturn(Result.failure(FAKER.lorem().sentence()));

        // Assert
        assertThat(didJwtCredentialsVerifier.isSignedByIssuer(JWT)).isFalse();
    }

    @Test
    public void isSignedByIssuer_cantParsePayload() throws Exception {

        // Arrange
        var jws = mock(SignedJWT.class);
        when(jws.getJWTClaimsSet()).thenThrow(new ParseException("Failed parsing JWT payload", 0));

        // Assert
        assertThat(didJwtCredentialsVerifier.isSignedByIssuer(jws)).isFalse();
    }

    @Test
    void verifyClaims_success() {
        assertThat(didJwtCredentialsVerifier.verifyClaims(JWT, SUBJECT)).isTrue();
    }

    @Test
    void verifyClaims_OnInvalidSubject() {
        assertThat(didJwtCredentialsVerifier.verifyClaims(JWT, OTHER_SUBJECT)).isFalse();
    }

    @Test
    void verifyClaims_OnEmptySubject() {
        var jwt = buildSignedJwt(generateVerifiableCredential(), ISSUER, null, JWK);
        assertThat(didJwtCredentialsVerifier.verifyClaims(jwt, OTHER_SUBJECT)).isFalse();
    }

    @Test
    void verifyClaims_OnEmptyIssuer() {
        var jwt = buildSignedJwt(generateVerifiableCredential(), null, SUBJECT, JWK);
        assertThat(didJwtCredentialsVerifier.verifyClaims(jwt, SUBJECT)).isFalse();
    }

    @Test
    public void verifyClaims_OnInvalidJwt() throws Exception {
        // Arrange
        var jwt = mock(SignedJWT.class);
        var message = FAKER.lorem().sentence();
        when(jwt.getJWTClaimsSet()).thenThrow(new ParseException(message, 0));

        // Act
        assertThat(didJwtCredentialsVerifier.verifyClaims(jwt, SUBJECT)).isFalse();
    }

    @Test
    void verifyClaims_OnValidExp() {
        var claims = new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .subject(SUBJECT)
                .expirationTime(from(now().plus(1, DAYS)))
                .build();

        SignedJWT jwt = VerifiableCredentialTestUtil.buildSignedJwt(claims, JWK);

        assertThat(didJwtCredentialsVerifier.verifyClaims(jwt, SUBJECT)).isTrue();
    }

    @Test
    void verifyClaims_OnInvalidExp() {
        var claims = new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .subject(SUBJECT)
                .expirationTime(from(now().minus(1, DAYS)))
                .build();

        SignedJWT jwt = VerifiableCredentialTestUtil.buildSignedJwt(claims, JWK);

        assertThat(didJwtCredentialsVerifier.verifyClaims(jwt, SUBJECT)).isFalse();
    }

    @Test
    void verifyClaims_OnValidNotBefore() {
        var claims = new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .subject(SUBJECT)
                .notBeforeTime(from(now().minus(1, DAYS)))
                .build();

        SignedJWT jwt = VerifiableCredentialTestUtil.buildSignedJwt(claims, JWK);

        assertThat(didJwtCredentialsVerifier.verifyClaims(jwt, SUBJECT)).isTrue();
    }

    @Test
    void verifyClaims_OnInvalidNotBefore() {
        var claims = new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .subject(SUBJECT)
                .notBeforeTime(from(now().plus(1, DAYS)))
                .build();

        SignedJWT jwt = VerifiableCredentialTestUtil.buildSignedJwt(claims, JWK);

        assertThat(didJwtCredentialsVerifier.verifyClaims(jwt, SUBJECT)).isFalse();
    }
}
