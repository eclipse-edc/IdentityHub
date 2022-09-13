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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.dataspaceconnector.identityhub.junit.testfixtures.VerifiableCredentialTestUtil;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class DidJwtCredentialsVerifierTest {

    private static final ECKey JWK = generateEcKey();
    private static final ECKey ANOTHER_JWK = generateEcKey();
    private static final String ISSUER = "http://some.test.url";
    private static final String SUBJECT = "http://some.test.url";
    private static final String OTHER_SUBJECT = "http://some.test.url" + "other";
    private static final SignedJWT JWT = buildSignedJwt(generateVerifiableCredential(), ISSUER, SUBJECT, JWK);
    private DidPublicKeyResolver didPublicKeyResolver;
    private DidJwtCredentialsVerifier didJwtCredentialsVerifier;

    @BeforeEach
    public void setUp() {
        didPublicKeyResolver = mock(DidPublicKeyResolver.class);
        didJwtCredentialsVerifier = new DidJwtCredentialsVerifier(didPublicKeyResolver, mock(Monitor.class));
    }


    @Test
    void isSignedByIssuer_jwtSignedByIssuer() {

        // Arrange
        when(didPublicKeyResolver.resolvePublicKey(ISSUER)).thenReturn(Result.success(toPublicKeyWrapper(JWK)));

        // Assert
        assertThat(didJwtCredentialsVerifier.isSignedByIssuer(JWT).succeeded()).isTrue();
    }

    @Test
    void isSignedByIssuer_jwtSignedByWrongIssuer() {

        // Arrange
        when(didPublicKeyResolver.resolvePublicKey(ISSUER)).thenReturn(Result.success(toPublicKeyWrapper(ANOTHER_JWK)));

        // Assert
        assertThat(didJwtCredentialsVerifier.isSignedByIssuer(JWT).failed()).isTrue();
    }

    @Test
    void isSignedByIssuer_PublicKeyCantBeResolved() {

        // Arrange
        when(didPublicKeyResolver.resolvePublicKey(ISSUER)).thenReturn(Result.failure("Failed resolving public key"));

        // Assert
        assertThat(didJwtCredentialsVerifier.isSignedByIssuer(JWT).failed()).isTrue();
    }

    @Test
    void isSignedByIssuer_issuerDidCantBeResolved() throws ParseException {

        // Arrange
        when(didPublicKeyResolver.resolvePublicKey(JWT.getJWTClaimsSet().getIssuer())).thenReturn(Result.failure("test failure"));

        // Assert
        assertThat(didJwtCredentialsVerifier.isSignedByIssuer(JWT).failed()).isTrue();
    }

    @Test
    void isSignedByIssuer_cantParsePayload() throws Exception {

        // Arrange
        var jws = mock(SignedJWT.class);
        when(jws.getJWTClaimsSet()).thenThrow(new ParseException("Failed parsing JWT payload", 0));

        // Assert
        assertThat(didJwtCredentialsVerifier.isSignedByIssuer(jws).failed()).isTrue();
    }

    @Test
    void verifyClaims_success() {
        assertThat(didJwtCredentialsVerifier.verifyClaims(JWT, SUBJECT).succeeded()).isTrue();
    }

    @Test
    void verifyClaims_OnInvalidSubject() {
        assertThat(didJwtCredentialsVerifier.verifyClaims(JWT, OTHER_SUBJECT).failed()).isTrue();
    }

    @Test
    void verifyClaims_OnEmptySubject() {
        var jwt = buildSignedJwt(generateVerifiableCredential(), ISSUER, null, JWK);
        assertThat(didJwtCredentialsVerifier.verifyClaims(jwt, OTHER_SUBJECT).failed()).isTrue();
    }

    @Test
    void verifyClaims_OnEmptyIssuer() {
        var jwt = buildSignedJwt(generateVerifiableCredential(), null, SUBJECT, JWK);
        assertThat(didJwtCredentialsVerifier.verifyClaims(jwt, SUBJECT).failed()).isTrue();
    }

    @Test
    void verifyClaims_OnInvalidJwt() throws Exception {
        // Arrange
        var jwt = mock(SignedJWT.class);
        var message = "Test Message";
        when(jwt.getJWTClaimsSet()).thenThrow(new ParseException(message, 0));

        // Act
        assertThat(didJwtCredentialsVerifier.verifyClaims(jwt, SUBJECT).failed()).isTrue();
    }

    @Test
    void verifyClaims_OnValidExp() {
        var claims = new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .subject(SUBJECT)
                .expirationTime(from(now().plus(1, DAYS)))
                .build();

        var jwt = VerifiableCredentialTestUtil.buildSignedJwt(claims, JWK);

        assertThat(didJwtCredentialsVerifier.verifyClaims(jwt, SUBJECT).succeeded()).isTrue();
    }

    @Test
    void verifyClaims_OnInvalidExp() {
        var claims = new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .subject(SUBJECT)
                .expirationTime(from(now().minus(1, DAYS)))
                .build();

        var jwt = VerifiableCredentialTestUtil.buildSignedJwt(claims, JWK);

        assertThat(didJwtCredentialsVerifier.verifyClaims(jwt, SUBJECT).failed()).isTrue();
    }

    @Test
    void verifyClaims_OnValidNotBefore() {
        var claims = new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .subject(SUBJECT)
                .notBeforeTime(from(now().minus(1, DAYS)))
                .build();

        var jwt = VerifiableCredentialTestUtil.buildSignedJwt(claims, JWK);

        assertThat(didJwtCredentialsVerifier.verifyClaims(jwt, SUBJECT).succeeded()).isTrue();
    }

    @Test
    void verifyClaims_OnInvalidNotBefore() {
        var claims = new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .subject(SUBJECT)
                .notBeforeTime(from(now().plus(1, DAYS)))
                .build();

        var jwt = VerifiableCredentialTestUtil.buildSignedJwt(claims, JWK);

        assertThat(didJwtCredentialsVerifier.verifyClaims(jwt, SUBJECT).failed()).isTrue();
    }

    @Test
    void verifyClaims_JwsCantBeVerified() throws Exception {
        // Arrange
        var jwt = spy(JWT);
        when(didPublicKeyResolver.resolvePublicKey(ISSUER)).thenReturn(Result.success(toPublicKeyWrapper(JWK)));
        // JOSEException can occur if JWS algorithm is not supported, or if signature verification failed for some
        // other internal reason
        doThrow(new JOSEException("JWS algorithm is not supported")).when(jwt).verify(any());

        // Act & Assert
        assertThat(didJwtCredentialsVerifier.isSignedByIssuer(jwt).failed()).isTrue();
    }
}
