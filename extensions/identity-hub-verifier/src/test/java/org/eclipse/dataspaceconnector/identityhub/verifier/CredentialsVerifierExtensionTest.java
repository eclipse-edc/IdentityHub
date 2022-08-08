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
import com.nimbusds.jose.jwk.ECKey;
import org.eclipse.dataspaceconnector.iam.did.spi.credentials.CredentialsVerifier;
import org.eclipse.dataspaceconnector.iam.did.spi.document.DidDocument;
import org.eclipse.dataspaceconnector.iam.did.spi.document.EllipticCurvePublicKey;
import org.eclipse.dataspaceconnector.iam.did.spi.document.Service;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolver;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.dataspaceconnector.identityhub.client.IdentityHubClient;
import org.eclipse.dataspaceconnector.identityhub.client.IdentityHubClientImpl;
import org.eclipse.dataspaceconnector.junit.extensions.EdcExtension;
import org.eclipse.dataspaceconnector.junit.testfixtures.TestUtils;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
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
import static org.eclipse.dataspaceconnector.junit.testfixtures.TestUtils.getFreePort;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(EdcExtension.class)
public class CredentialsVerifierExtensionTest {
    private static final Faker FAKER = new Faker();
    private static final int PORT = getFreePort();
    private static final String API_URL = String.format("http://localhost:%d/api/identity-hub", PORT);
    private static final String CREDENTIAL_ISSUER = "did:web:" + FAKER.internet().domainName();
    private static final String SUBJECT = "did:web:" + FAKER.internet().domainName();
    private IdentityHubClient identityHubClient;

    @BeforeEach
    void setUp(EdcExtension extension) {
        identityHubClient = new IdentityHubClientImpl(TestUtils.testOkHttpClient(), new ObjectMapper(), mock(Monitor.class));
        extension.registerServiceMock(Monitor.class, mock(Monitor.class));
        extension.setConfiguration(Map.of("web.http.port", String.valueOf(PORT)));
    }

    @Test
    public void getVerifiedClaims_getValidClaims(CredentialsVerifier verifier, DidResolverRegistry registry) {

        var jwk = generateEcKey();
        // Arrange - add did resolver that returns a dummy DID
        var didResolver = mock(DidResolver.class);
        var did = createDidDocument(jwk);
        when(didResolver.resolve(anyString())).thenReturn(Result.success(did));
        when(didResolver.getMethod()).thenReturn("web");
        registry.register(didResolver);

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

    private DidDocument createDidDocument(ECKey jwk) {
        var ecPk = new EllipticCurvePublicKey(jwk.getCurve().getName(), jwk.getKeyType().toString(), jwk.getX().toString(), jwk.getY().toString());
        return DidDocument.Builder.newInstance()
                .verificationMethod("test-id", "test-type", ecPk).build();
    }
}
