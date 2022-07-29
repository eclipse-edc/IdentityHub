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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import org.eclipse.dataspaceconnector.iam.did.resolution.DidPublicKeyResolverImpl;
import org.eclipse.dataspaceconnector.iam.did.spi.credentials.CredentialsVerifier;
import org.eclipse.dataspaceconnector.iam.did.spi.document.DidDocument;
import org.eclipse.dataspaceconnector.iam.did.spi.document.Service;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.dataspaceconnector.identityhub.client.IdentityHubClient;
import org.eclipse.dataspaceconnector.identityhub.client.IdentityHubClientImpl;
import org.eclipse.dataspaceconnector.junit.extensions.EdcExtension;
import org.eclipse.dataspaceconnector.junit.testfixtures.TestUtils;
import org.eclipse.dataspaceconnector.spi.monitor.ConsoleMonitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.buildSignedJwt;
import static org.eclipse.dataspaceconnector.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.generateEcKey;
import static org.eclipse.dataspaceconnector.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.generateVerifiableCredential;
import static org.eclipse.dataspaceconnector.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.toMap;
import static org.eclipse.dataspaceconnector.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.toPublicKeyWrapper;
import static org.eclipse.dataspaceconnector.junit.testfixtures.TestUtils.getFreePort;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(EdcExtension.class)
public class CredentialsVerifierExtensionTest {
    private static final Faker FAKER = new Faker();
    private static final int PORT = getFreePort();
    private static final String API_URL = String.format("http://localhost:%d/api/identity-hub", PORT);
    private static final String CREDENTIAL_ISSUER = FAKER.internet().url();
    private static final String SUBJECT = FAKER.internet().url();
    private IdentityHubClient identityHubClient;
    private DidPublicKeyResolver publicKeyResolver;

    @BeforeEach
    void setUp(EdcExtension extension) {
        identityHubClient = new IdentityHubClientImpl(TestUtils.testOkHttpClient(), new ObjectMapper(), new ConsoleMonitor());
        publicKeyResolver = mock(DidPublicKeyResolverImpl.class);
        extension.registerServiceMock(DidPublicKeyResolver.class, publicKeyResolver);
        extension.setConfiguration(Map.of("web.http.port", String.valueOf(PORT)));
    }

    @Test
    public void getVerifiedClaims_getValidClaims(CredentialsVerifier verifier) {
        // Arrange
        var jwk = generateEcKey();
        when(publicKeyResolver.resolvePublicKey(anyString())).thenReturn(Result.success(toPublicKeyWrapper(jwk)));
        var didDocument = DidDocument.Builder.newInstance()
                .id(SUBJECT)
                .service(List.of(new Service("IdentityHub", "IdentityHub", API_URL)))
                .build();
        var credential = generateVerifiableCredential();
        var jwt = buildSignedJwt(credential, CREDENTIAL_ISSUER, SUBJECT, jwk);

        // Act
        identityHubClient.addVerifiableCredential(API_URL, jwt);
        var credentials = verifier.getVerifiedCredentials(didDocument);
        var expectedCredentials = toMap(credential, CREDENTIAL_ISSUER, SUBJECT);

        // Assert
        assertThat(credentials.succeeded()).isTrue();
        assertThat(credentials.getContent())
                .usingRecursiveComparison()
                .isEqualTo(expectedCredentials);
    }
}
