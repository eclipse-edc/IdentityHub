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

package org.eclipse.edc.identityhub.credentials.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.identityhub.spi.credentials.model.Credential;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.identityhub.credentials.jwt.JwtCredentialConstants.VERIFIABLE_CREDENTIALS_KEY;
import static org.eclipse.edc.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.buildSignedJwt;
import static org.eclipse.edc.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.generateCredential;
import static org.eclipse.edc.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.generateEcKey;

class JwtCredentialEnvelopeTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Credential CREDENTIAL = generateCredential();
    private static final JWSHeader JWS_HEADER = new JWSHeader.Builder(JWSAlgorithm.ES256).build();
    private ECKey privateKey;

    @BeforeEach
    public void setUp() {
        privateKey = generateEcKey();
    }

    @Test
    void verifyToVerifiableCredential() {
        // Arrange
        var issuer = "test-issuer";
        var subject = "test-subject";
        var jwt = buildSignedJwt(CREDENTIAL, issuer, subject, privateKey);
        var envelope = new JwtCredentialEnvelope(jwt);

        // Act
        var result = envelope.toVerifiableCredentials(OBJECT_MAPPER);

        // Assert
        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).hasSize(1).anySatisfy(verifiableCredential -> {
            assertThat(verifiableCredential.getProof()).isNull();
            assertThat(verifiableCredential.getItem()).usingRecursiveComparison().isEqualTo(CREDENTIAL);
        });
    }

    @Test
    void verifyToVerifiableCredential_OnJwtWithMissingVcField() {
        // Arrange
        var claims = new JWTClaimsSet.Builder().claim("test-name", "test-value").build();
        var jws = new SignedJWT(JWS_HEADER, claims);
        var envelope = new JwtCredentialEnvelope(jws);

        // Act
        var result = envelope.toVerifiableCredentials(OBJECT_MAPPER);

        // Assert
        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureDetail()).contains(VERIFIABLE_CREDENTIALS_KEY);
    }

    @Test
    void verifyToVerifiableCredential_OnJwtWithWrongFormat() {
        // Arrange
        var claims = new JWTClaimsSet.Builder().claim(VERIFIABLE_CREDENTIALS_KEY, "test-value").build();
        var jws = new SignedJWT(JWS_HEADER, claims);
        var envelope = new JwtCredentialEnvelope(jws);

        // Act
        var result = envelope.toVerifiableCredentials(OBJECT_MAPPER);

        // Assert
        assertThat(result.failed()).isTrue();
    }
}
