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
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspaceconnector.iam.did.crypto.key.EcPrivateKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.crypto.key.EcPublicKeyWrapper;
import org.eclipse.dataspaceconnector.identityhub.spi.credentials.VerifiableCredentialsJwtService;
import org.eclipse.dataspaceconnector.identityhub.spi.credentials.VerifiableCredentialsJwtServiceImpl;
import org.eclipse.dataspaceconnector.identityhub.spi.credentials.model.VerifiableCredential;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import static org.eclipse.dataspaceconnector.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.generateEcKey;
import static org.eclipse.dataspaceconnector.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.generateVerifiableCredential;
import static org.eclipse.dataspaceconnector.identityhub.spi.credentials.VerifiableCredentialsJwtService.VERIFIABLE_CREDENTIALS_KEY;
import static org.mockito.Mockito.mock;

public class VerifiableCredentialsJwtServiceTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final VerifiableCredential VERIFIABLE_CREDENTIAL = generateVerifiableCredential();
    private static final JWSHeader JWS_HEADER = new JWSHeader.Builder(JWSAlgorithm.ES256).build();
    private EcPrivateKeyWrapper privateKey;
    private EcPublicKeyWrapper publicKey;
    private VerifiableCredentialsJwtService service;

    @BeforeEach
    public void setUp() {
        var key = generateEcKey();
        privateKey = new EcPrivateKeyWrapper(key);
        publicKey = new EcPublicKeyWrapper(key);
        service = new VerifiableCredentialsJwtServiceImpl(OBJECT_MAPPER, mock(Monitor.class));
    }

    @Test
    public void buildSignedJwt_success() throws Exception {
        // Arrange
        var issuer = "test-issuer";
        var subject = "test-subject";
        var startTime = Instant.now().truncatedTo(SECONDS);

        // Act
        var signedJwt = service.buildSignedJwt(VERIFIABLE_CREDENTIAL, issuer, subject, privateKey);

        // Assert
        boolean result = signedJwt.verify(publicKey.verifier());
        assertThat(result).isTrue();

        assertThat(signedJwt.getJWTClaimsSet().getClaims())
                .containsEntry("iss", issuer)
                .containsEntry("sub", subject)
                .extractingByKey(VERIFIABLE_CREDENTIALS_KEY)
                .satisfies(c -> {
                    assertThat(OBJECT_MAPPER.convertValue(c, VerifiableCredential.class))
                            .usingRecursiveComparison()
                            .isEqualTo(VERIFIABLE_CREDENTIAL);
                });
        assertThat(signedJwt.getJWTClaimsSet().getIssueTime()).isBetween(startTime, Instant.now());
    }

    @Test
    public void extractCredential_OnJwtWithValidCredential() throws Exception {
        // Arrange
        var issuer = "test-issuer";
        var subject = "test-subject";
        var jwt = service.buildSignedJwt(VERIFIABLE_CREDENTIAL, issuer, subject, privateKey);

        // Act
        var result = service.extractCredential(jwt);

        // Assert
        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent().getKey()).isEqualTo(VERIFIABLE_CREDENTIAL.getId());
        assertThat(result.getContent().getValue())
                .asInstanceOf(map(String.class, Object.class))
                .containsEntry("iss", issuer)
                .containsEntry("sub", subject)
                .extractingByKey(VERIFIABLE_CREDENTIALS_KEY)
                .satisfies(c -> {
                    assertThat(OBJECT_MAPPER.convertValue(c, VerifiableCredential.class))
                            .usingRecursiveComparison()
                            .isEqualTo(VERIFIABLE_CREDENTIAL);
                });
    }

    @Test
    public void extractCredential_OnJwtWithMissingVcField() {
        // Arrange
        var claims = new JWTClaimsSet.Builder().claim("test-name", "test-value").build();
        var jws = new SignedJWT(JWS_HEADER, claims);

        // Act
        var result = service.extractCredential(jws);

        // Assert
        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).containsExactly(String.format("No %s field found", VERIFIABLE_CREDENTIALS_KEY));
    }

    @Test
    public void extractCredential_OnJwtWithWrongFormat() {
        // Arrange
        var claims = new JWTClaimsSet.Builder().claim(VERIFIABLE_CREDENTIALS_KEY, "test-value").build();
        var jws = new SignedJWT(JWS_HEADER, claims);

        // Act
        var result = service.extractCredential(jws);

        // Assert
        assertThat(result.failed()).isTrue();
    }

}
