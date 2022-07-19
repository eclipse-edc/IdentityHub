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
import org.eclipse.dataspaceconnector.iam.did.spi.document.DidDocument;
import org.eclipse.dataspaceconnector.iam.did.spi.document.Service;
import org.eclipse.dataspaceconnector.identityhub.client.IdentityHubClient;
import org.eclipse.dataspaceconnector.spi.monitor.ConsoleMonitor;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.response.StatusResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.buildSignedJwt;
import static org.eclipse.dataspaceconnector.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.generateVerifiableCredential;
import static org.eclipse.dataspaceconnector.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.toMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IdentityHubCredentialsVerifierTest {

    private static final Faker FAKER = new Faker();
    private static final Monitor MONITOR = new ConsoleMonitor();

    @Test
    public void getVerifiedClaims_getValidClaims() throws Exception {

        // Arrange
        var hubBaseUrl = "https://" + FAKER.internet().url();
        var issuer = FAKER.internet().url();
        var identityHubClient = mock(IdentityHubClient.class);
        var monitor = new ConsoleMonitor();
        var signatureVerifier = mock(SignatureVerifier.class);
        var credentialsVerifier = new IdentityHubCredentialsVerifier(identityHubClient, monitor, signatureVerifier);
        var didDocument = DidDocument.Builder.newInstance()
                .service(List.of(new Service("IdentityHub", "IdentityHub", hubBaseUrl))).build();
        var credential = generateVerifiableCredential();
        var jws = buildSignedJwt(credential, issuer);
        when(identityHubClient.getVerifiableCredentials(hubBaseUrl)).thenReturn(StatusResult.success(List.of(jws)));
        when(signatureVerifier.isSignedByIssuer(jws)).thenReturn(true);

        // Act
        var credentials = credentialsVerifier.getVerifiedCredentials(didDocument);

        // Assert
        assertThat(credentials.succeeded());
        assertThat(credentials.getContent())
                .usingRecursiveComparison()
                .ignoringFields(String.format("%s.exp", credential.getId()))
                .isEqualTo(toMap(credential, issuer));
    }

    @Test
    public void getVerifiedClaims_filtersSignedByWrongIssuer() throws Exception {

        // Arrange
        var hubBaseUrl = "https://" + FAKER.internet().url();
        var issuer = FAKER.internet().url();
        var identityHubClient = mock(IdentityHubClient.class);
        var signatureVerifier = mock(SignatureVerifier.class);
        var credentialsVerifier = new IdentityHubCredentialsVerifier(identityHubClient, MONITOR, signatureVerifier);
        var didDocument = DidDocument.Builder.newInstance()
                .service(List.of(new Service("IdentityHub", "IdentityHub", hubBaseUrl))).build();
        var credential = generateVerifiableCredential();
        var jws = buildSignedJwt(credential, issuer);
        when(identityHubClient.getVerifiableCredentials(hubBaseUrl))
                .thenReturn(StatusResult.success(List.of(jws)));
        when(signatureVerifier.isSignedByIssuer(jws)).thenReturn(false);

        // Act
        var credentials = credentialsVerifier.getVerifiedCredentials(didDocument);

        // Assert
        assertThat(credentials.succeeded());
        assertThat(credentials.getContent().size()).isEqualTo(0);
    }

    @Test
    public void getVerifiedClaims_hubUrlNotResolved() {
        // Arrange
        var credentialsVerifier = new IdentityHubCredentialsVerifier(mock(IdentityHubClient.class), MONITOR, mock(SignatureVerifier.class));
        var didDocument = DidDocument.Builder.newInstance().build();

        // Act
        var credentials = credentialsVerifier.getVerifiedCredentials(didDocument);

        // Assert
        assertThat(credentials.failed());
    }

    @Test
    public void getVerifiedClaims_idHubCallFails() {

        // Arrange
        var hubBaseUrl = "https://" + FAKER.internet().url();
        var identityHubClient = mock(IdentityHubClient.class);
        var monitor = new ConsoleMonitor();
        var credentialsVerifier = new IdentityHubCredentialsVerifier(identityHubClient, MONITOR, mock(SignatureVerifier.class));
        var didDocument = DidDocument.Builder.newInstance().service(List.of(new Service("IdentityHub", "IdentityHub", hubBaseUrl))).build();
        when(identityHubClient.getVerifiableCredentials(hubBaseUrl)).thenReturn(StatusResult.failure(ResponseStatus.FATAL_ERROR));

        // Act
        var credentials = credentialsVerifier.getVerifiedCredentials(didDocument);

        // Assert
        assertThat(credentials.failed());
    }

    @Test
    public void getVerifiedClaims_verifiableCredentialsWithWrongFormat() {

        // Arrange
        var hubBaseUrl = "https://" + FAKER.internet().url();
        var identityHubClient = mock(IdentityHubClient.class);
        var signatureVerifier = mock(SignatureVerifier.class);
        var credentialsVerifier = new IdentityHubCredentialsVerifier(identityHubClient, MONITOR, signatureVerifier);
        var didDocument = DidDocument.Builder.newInstance()
                .service(List.of(new Service("IdentityHub", "IdentityHub", hubBaseUrl))).build();
        var jws = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).build(), new JWTClaimsSet.Builder().build());
        when(identityHubClient.getVerifiableCredentials(hubBaseUrl)).thenReturn(StatusResult.success(List.of(jws)));
        when(signatureVerifier.isSignedByIssuer(jws)).thenReturn(true);

        // Act
        var credentials = credentialsVerifier.getVerifiedCredentials(didDocument);

        // Assert
        assertThat(credentials.succeeded());
        assertThat(credentials.getContent().isEmpty());
    }
}
