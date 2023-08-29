/*
 *  Copyright (c) 2023 GAIA-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       GAIA-X - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.verifier.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.identityhub.credentials.jwt.JwtCredentialEnvelope;
import org.eclipse.edc.identityhub.spi.credentials.model.Credential;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.buildSignedJwt;
import static org.eclipse.edc.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.generateCredential;
import static org.eclipse.edc.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.generateEcKey;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtCredentialEnvelopeVerifierTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final JwtCredentialsVerifier jwtCredentialsVerifierMock = mock(JwtCredentialsVerifier.class);
    private final JwtCredentialEnvelope jwtCredentialEnvelopeMock = mock(JwtCredentialEnvelope.class);
    private static final String ISSUER = "http://some.test.url";
    private static final String SUBJECT = "http://some.test.url";
    private static final String HUB_BASE_URL = "https://" + "http://some.test.url";
    private static final DidDocument DID_DOCUMENT = DidDocument.Builder.newInstance()
            .service(List.of(new Service("IdentityHub", "IdentityHub", HUB_BASE_URL))).build();

    @Test
    void shouldVerify_ValidJwt() {
        var credential = generateCredential();
        var jws = buildSignedJwt(credential, ISSUER, SUBJECT, generateEcKey());
        setUpMocks(true, true);
        var jwtCredentialEnvelopeVerifier = new JwtCredentialEnvelopeVerifier(jwtCredentialsVerifierMock, OBJECT_MAPPER);
        var jwtCredentialEnvelope = new JwtCredentialEnvelope(jws);

        var credentialVerification = jwtCredentialEnvelopeVerifier.verify(jwtCredentialEnvelope, DID_DOCUMENT);

        assertThat(credentialVerification.succeeded()).isTrue();
        assertThat(credentialVerification.getContent())
                .hasSize(1)
                .anySatisfy(c -> {
                    assertThat(c).isInstanceOf(Credential.class);
                    assertThat(c).usingRecursiveComparison().isEqualTo(credential);
                });
    }

    @Test
    void shouldVerifyJwt_UnsignedJwt() {
        var credential = generateCredential();
        var jws = buildSignedJwt(credential, ISSUER, SUBJECT, generateEcKey());
        setUpMocks(false, true);
        var jwtCredentialEnvelopeVerifier = new JwtCredentialEnvelopeVerifier(jwtCredentialsVerifierMock, OBJECT_MAPPER);
        var jwtCredentialEnvelope = new JwtCredentialEnvelope(jws);

        var credentialVerification = jwtCredentialEnvelopeVerifier.verify(jwtCredentialEnvelope, DID_DOCUMENT);

        assertThat(credentialVerification.failed()).isTrue();
    }

    @Test
    void shouldVerifyJwt_InvalidClaims() {
        var credential = generateCredential();
        var jws = buildSignedJwt(credential, ISSUER, SUBJECT, generateEcKey());
        setUpMocks(true, false);
        var jwtCredentialEnvelopeVerifier = new JwtCredentialEnvelopeVerifier(jwtCredentialsVerifierMock, OBJECT_MAPPER);
        var jwtCredentialEnvelope = new JwtCredentialEnvelope(jws);

        var credentialVerification = jwtCredentialEnvelopeVerifier.verify(jwtCredentialEnvelope, DID_DOCUMENT);

        assertThat(credentialVerification.failed()).isTrue();
    }

    @Test
    void shouldVerifyJwt_FailedCredential() {
        setUpMocks(true, true);
        when(jwtCredentialEnvelopeMock.toVerifiableCredentials(any())).thenReturn(Result.failure("Missing vc claim"));

        var jwtCredentialEnvelopeVerifier = new JwtCredentialEnvelopeVerifier(jwtCredentialsVerifierMock, OBJECT_MAPPER);
        var credentialVerification = jwtCredentialEnvelopeVerifier.verify(jwtCredentialEnvelopeMock, DID_DOCUMENT);

        assertThat(credentialVerification.failed()).isTrue();
    }

    private void setUpMocks(boolean isSigned, boolean claimsValid) {
        when(jwtCredentialsVerifierMock.isSignedByIssuer(any())).thenReturn(isSigned ? Result.success() : Result.failure("JWT not signed"));
        when(jwtCredentialsVerifierMock.verifyClaims(any(), any())).thenReturn(claimsValid ? Result.success() : Result.failure("VC not valid"));
    }

}
