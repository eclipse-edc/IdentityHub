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

package org.eclipse.dataspaceconnector.identityhub.credentials;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.buildSignedJwt;
import static org.eclipse.dataspaceconnector.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.generateEcKey;
import static org.eclipse.dataspaceconnector.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.generateVerifiableCredential;
import static org.eclipse.dataspaceconnector.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.toMap;

public class VerifiableCredentialsJWTServiceTest {

    static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    static final VerifiableCredentialsJWTService VERIFIABLE_CREDENTIALS_JWT_SERVICE = new VerifiableCredentialsJWTService(OBJECT_MAPPER);
    static final Faker FAKER = new Faker();
    static final String VERIFIABLE_CREDENTIALS_KEY = "vc";
    static final JWSHeader JWS_HEADER = new JWSHeader.Builder(JWSAlgorithm.ES256).build();

    @Test
    public void extractCredential_OnJwtWithValidCredential() throws Exception {

        // Arrange
        var verifiableCredential = generateVerifiableCredential();
        var issuer = FAKER.lorem().word();
        var subject = FAKER.lorem().word();
        var jwt = buildSignedJwt(verifiableCredential, issuer, subject, generateEcKey());

        // Act
        var result = VERIFIABLE_CREDENTIALS_JWT_SERVICE.extractCredential(jwt);

        // Assert
        assertThat(result.succeeded());
        assertThat(result.getContent())
                .usingRecursiveComparison()
                .ignoringFields(String.format("value.exp", verifiableCredential.getId()))
                .isEqualTo(toMap(verifiableCredential, issuer, subject).entrySet().stream().findFirst().get());
    }

    @Test
    public void extractCredential_OnJwtWithMissingVcField() {

        // Arrange
        var claims = new JWTClaimsSet.Builder().claim(FAKER.lorem().word(), FAKER.lorem().word()).build();
        var jws = new SignedJWT(JWS_HEADER, claims);

        // Act
        var result = VERIFIABLE_CREDENTIALS_JWT_SERVICE.extractCredential(jws);

        // Assert
        assertThat(result.failed());
        assertThat(result.getFailureMessages()).containsExactly(String.format("No %s field found", VERIFIABLE_CREDENTIALS_KEY));
    }

    @Test
    public void extractCredential_OnjJwtWithWrongFormat() {
        // Arrange
        var claims = new JWTClaimsSet.Builder().claim(VERIFIABLE_CREDENTIALS_KEY, FAKER.lorem().word()).build();
        var jws = new SignedJWT(JWS_HEADER, claims);

        // Act
        var result = VERIFIABLE_CREDENTIALS_JWT_SERVICE.extractCredential(jws);

        // Assert
        assertThat(result.failed());
    }
}
