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

package org.eclipse.edc.identityhub.verifier;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.ECKey;
import org.eclipse.edc.iam.did.spi.credentials.CredentialsVerifier;
import org.eclipse.edc.iam.did.spi.document.DidConstants;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.EllipticCurvePublicKey;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.iam.did.spi.resolution.DidResolver;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.identityhub.client.IdentityHubClientImpl;
import org.eclipse.edc.identityhub.client.spi.IdentityHubClient;
import org.eclipse.edc.junit.extensions.EdcExtension;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.buildSignedJwt;
import static org.eclipse.edc.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.generateEcKey;
import static org.eclipse.edc.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.generateVerifiableCredential;
import static org.eclipse.edc.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.toMap;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(EdcExtension.class)
class CredentialsVerifierExtensionTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final int PORT = getFreePort();
    private static final String API_URL = format("http://localhost:%d/api/identity-hub", PORT);
    private static final String DID_METHOD = "test-method";
    private static final String CREDENTIAL_ISSUER = format("did:%s:%s", DID_METHOD, "http://some.test.url");
    private static final String SUBJECT = "http://some.test.url";
    private IdentityHubClient identityHubClient;

    private static DidDocument createDidDocument(ECKey jwk) throws JsonProcessingException {
        var ecKey = OBJECT_MAPPER.readValue(jwk.toJSONString(), EllipticCurvePublicKey.class);
        return DidDocument.Builder.newInstance()
                .id(SUBJECT)
                .service(List.of(new Service("IdentityHub", "IdentityHub", API_URL)))
                .verificationMethod(UUID.randomUUID().toString(), DidConstants.ECDSA_SECP_256_K_1_VERIFICATION_KEY_2019, ecKey)
                .build();
    }

    @BeforeEach
    void setUp(EdcExtension extension) {
        identityHubClient = new IdentityHubClientImpl(TestUtils.testOkHttpClient(), new ObjectMapper(), mock(Monitor.class));
        extension.setConfiguration(Map.of("web.http.port", String.valueOf(PORT)));
    }

    @Test
    void getVerifiedClaims_getValidClaims(CredentialsVerifier verifier, DidResolverRegistry registry) throws JsonProcessingException {
        // Arrange
        var jwk = generateEcKey();
        var didDocument = createDidDocument(jwk);
        var credential = generateVerifiableCredential();
        var jwt = buildSignedJwt(credential, CREDENTIAL_ISSUER, SUBJECT, jwk);
        var didResolverMock = mock(DidResolver.class);
        when(didResolverMock.getMethod()).thenReturn(DID_METHOD);
        when(didResolverMock.resolve(anyString())).thenReturn(Result.success(didDocument));

        registry.register(didResolverMock);

        // Act
        var addResult = identityHubClient.addVerifiableCredential(API_URL, jwt);
        assertThat(addResult.succeeded()).isTrue();
        var credentials = verifier.getVerifiedCredentials(didDocument);
        var expectedCredentials = toMap(credential, CREDENTIAL_ISSUER, SUBJECT);

        // Assert
        assertThat(credentials.succeeded()).isTrue();
        assertThat(credentials.getContent())
                .usingRecursiveComparison()
                .isEqualTo(expectedCredentials);
    }
}
