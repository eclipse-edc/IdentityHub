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
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.identityhub.credentials.jwt.JwtPresentation;
import org.eclipse.edc.identityhub.credentials.jwt.JwtPresentationEnvelope;
import org.eclipse.edc.identityhub.spi.credentials.model.Credential;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.identityhub.credentials.jwt.JwtCredentialConstants.VERIFIABLE_PRESENTATION_KEY;
import static org.eclipse.edc.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.buildSignedJwt;
import static org.eclipse.edc.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.generateCredential;
import static org.eclipse.edc.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.generateEcKey;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtPresentationEnvelopeVerifierTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final JwtCredentialsVerifier jwtCredentialsVerifierMock = mock(JwtCredentialsVerifier.class);
    private final JwtPresentationEnvelope jwtPresentationEnvelopeMock = mock(JwtPresentationEnvelope.class);
    private static final String ISSUER = "http://some.test.url";
    private static final String SUBJECT = "http://some.test.url";
    private static final String HUB_BASE_URL = "https://" + "http://some.test.url";
    private static final DidDocument DID_DOCUMENT = DidDocument.Builder.newInstance()
            .service(List.of(new Service("IdentityHub", "IdentityHub", HUB_BASE_URL))).build();

    @Test
    void shouldVerify_ValidJwt() {
        var credentials = generateCredentials();
        var presentation = generateJwtPresentation(credentials);
        var jws = buildPresentationJwt(presentation);
        setUpMocks(true, true);
        var jwtPresentationEnvelopeVerifier = new JwtPresentationEnvelopeVerifier(jwtCredentialsVerifierMock, OBJECT_MAPPER);
        var jwtPresentationEnvelope = new JwtPresentationEnvelope(jws);

        var credentialVerification = jwtPresentationEnvelopeVerifier.verify(jwtPresentationEnvelope, DID_DOCUMENT);

        assertThat(credentialVerification.succeeded()).isTrue();
        assertThat(credentialVerification.getContent())
                .hasSize(1)
                .satisfies(c -> {
                    assertThat(c).isInstanceOf(List.class);
                    assertThat(c).usingRecursiveComparison().isEqualTo(credentials);
                });
    }

    @Test
    void shouldVerify_InvalidCredentialSignature() {
        var credentialJwt = buildSignedJwt(generateCredential(), ISSUER, SUBJECT, generateEcKey());
        var presentation = JwtPresentation.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .type(JwtPresentation.DEFAULT_TYPE)
                .signedCredentials(List.of(credentialJwt.serialize()))
                .build();
        var jws = buildPresentationJwt(presentation);
        when(jwtCredentialsVerifierMock.verifyClaims(any(), any())).thenReturn(Result.success());
        when(jwtCredentialsVerifierMock.isSignedByIssuer(any())).thenReturn(Result.success()).thenReturn(Result.failure("VC JWT not signed"));
        var jwtPresentationEnvelopeVerifier = new JwtPresentationEnvelopeVerifier(jwtCredentialsVerifierMock, OBJECT_MAPPER);
        var jwtPresentationEnvelope = new JwtPresentationEnvelope(jws);

        var credentialVerification = jwtPresentationEnvelopeVerifier.verify(jwtPresentationEnvelope, DID_DOCUMENT);

        assertThat(credentialVerification.failed()).isTrue();
        assertThat(credentialVerification.getFailureDetail()).contains("VC JWT not signed");
    }

    @Test
    void shouldVerify_InvalidCredentialClaims() {
        var credentialJwt = buildSignedJwt(generateCredential(), ISSUER, SUBJECT, generateEcKey());
        var presentation = JwtPresentation.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .type(JwtPresentation.DEFAULT_TYPE)
                .signedCredentials(List.of(credentialJwt.serialize()))
                .build();
        var jws = buildPresentationJwt(presentation);
        when(jwtCredentialsVerifierMock.verifyClaims(any(), any())).thenReturn(Result.success()).thenReturn(Result.failure("VC Claim not valid"));
        when(jwtCredentialsVerifierMock.isSignedByIssuer(any())).thenReturn(Result.success());
        var jwtPresentationEnvelopeVerifier = new JwtPresentationEnvelopeVerifier(jwtCredentialsVerifierMock, OBJECT_MAPPER);
        var jwtPresentationEnvelope = new JwtPresentationEnvelope(jws);

        var credentialVerification = jwtPresentationEnvelopeVerifier.verify(jwtPresentationEnvelope, DID_DOCUMENT);

        assertThat(credentialVerification.failed()).isTrue();
        assertThat(credentialVerification.getFailureDetail()).contains("VC Claim not valid");
    }

    @Test
    void shouldVerifyJwt_UnsignedJwt() {
        var credentials = generateCredentials();
        var presentation = generateJwtPresentation(credentials);
        var jws = buildPresentationJwt(presentation);
        setUpMocks(false, true);
        var jwtPresentationEnvelopeVerifier = new JwtPresentationEnvelopeVerifier(jwtCredentialsVerifierMock, OBJECT_MAPPER);
        var jwtPresentationEnvelope = new JwtPresentationEnvelope(jws);

        var credentialVerification = jwtPresentationEnvelopeVerifier.verify(jwtPresentationEnvelope, DID_DOCUMENT);

        assertThat(credentialVerification.failed()).isTrue();
    }

    @Test
    void shouldVerifyJwt_InvalidClaims() {
        var credentials = generateCredentials();
        var presentation = generateJwtPresentation(credentials);
        var jws = buildPresentationJwt(presentation);
        setUpMocks(true, false);
        var jwtPresentationEnvelopeVerifier = new JwtPresentationEnvelopeVerifier(jwtCredentialsVerifierMock, OBJECT_MAPPER);
        var jwtPresentationEnvelope = new JwtPresentationEnvelope(jws);

        var credentialVerification = jwtPresentationEnvelopeVerifier.verify(jwtPresentationEnvelope, DID_DOCUMENT);

        assertThat(credentialVerification.failed()).isTrue();
    }

    @Test
    void shouldVerifyJwt_FailedCredential() {
        setUpMocks(true, true);
        when(jwtPresentationEnvelopeMock.toVerifiableCredentials(any())).thenReturn(Result.failure("Missing vp claim"));

        var jwtPresentationEnvelopeVerifier = new JwtPresentationEnvelopeVerifier(jwtCredentialsVerifierMock, OBJECT_MAPPER);
        var credentialVerification = jwtPresentationEnvelopeVerifier.verify(jwtPresentationEnvelopeMock, DID_DOCUMENT);

        assertThat(credentialVerification.failed()).isTrue();
    }

    private void setUpMocks(boolean isSigned, boolean claimsValid) {
        when(jwtCredentialsVerifierMock.isSignedByIssuer(any())).thenReturn(isSigned ? Result.success() : Result.failure("JWT not signed"));
        when(jwtCredentialsVerifierMock.verifyClaims(any(), any())).thenReturn(claimsValid ? Result.success() : Result.failure("VC not valid"));
    }

    private SignedJWT buildPresentationJwt(JwtPresentation presentation) {
        var claims = new JWTClaimsSet.Builder()
                .claim(VERIFIABLE_PRESENTATION_KEY, OBJECT_MAPPER.convertValue(presentation, Map.class))
                .issuer(ISSUER)
                .subject(SUBJECT)
                .expirationTime(null)
                .notBeforeTime(null)
                .build();

        return buildSignedJwt(claims, generateEcKey());
    }

    private JwtPresentation generateJwtPresentation(List<Credential> credentials) {
        var verifiableCredentials = credentials.stream()
                .map(vc -> buildSignedJwt(vc, ISSUER, SUBJECT, generateEcKey()).serialize())
                .toList();
        return JwtPresentation.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .type(JwtPresentation.DEFAULT_TYPE)
                .signedCredentials(verifiableCredentials)
                .build();
    }

    private List<Credential> generateCredentials() {
        return List.of(generateCredential());
    }

}
