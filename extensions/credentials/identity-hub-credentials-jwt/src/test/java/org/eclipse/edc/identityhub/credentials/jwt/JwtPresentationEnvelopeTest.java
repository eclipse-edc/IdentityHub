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

package org.eclipse.edc.identityhub.credentials.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.identityhub.spi.credentials.model.VerifiableCredential;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.identityhub.credentials.jwt.JwtCredentialConstants.VERIFIABLE_PRESENTATION_KEY;
import static org.eclipse.edc.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.buildSignedJwt;
import static org.eclipse.edc.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.generateCredential;
import static org.eclipse.edc.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.generateEcKey;

class JwtPresentationEnvelopeTest {

    private static final String ISSUER = "issuer";
    private static final String SUBJECT = "subject";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final JWSHeader JWS_HEADER = new JWSHeader.Builder(JWSAlgorithm.ES256).build();
    private ECKey privateKey;

    @BeforeEach
    public void setUp() {
        privateKey = generateEcKey();
    }

    @Test
    void verifyToVerifiablePresentation() {
        var verifiableCredentials = generateCredentials();
        var presentation = generateJwtPresentation(verifiableCredentials);
        var jwtPresentation = buildPresentationJwt(presentation);
        var envelope = new JwtPresentationEnvelope(jwtPresentation);

        var result = envelope.toVerifiableCredentials(OBJECT_MAPPER);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).usingRecursiveComparison().isEqualTo(verifiableCredentials);
    }

    @Test
    void verifyVerifiablePresentation_OnMissingMandatoryClaim() {
        var claims = new JWTClaimsSet.Builder().claim("wrong-claim", "value").build();
        var jws = new SignedJWT(JWS_HEADER, claims);
        var envelope = new JwtPresentationEnvelope(jws);

        var result = envelope.toVerifiableCredentials(OBJECT_MAPPER);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureDetail()).contains(VERIFIABLE_PRESENTATION_KEY);
    }

    @Test
    void verifyVerifiablePresentation_OnWrongJwt() {
        var claims = new JWTClaimsSet.Builder().claim(VERIFIABLE_PRESENTATION_KEY, "wrong-value").build();
        var jws = new SignedJWT(JWS_HEADER, claims);
        var envelope = new JwtPresentationEnvelope(jws);

        var result = envelope.toVerifiableCredentials(OBJECT_MAPPER);

        assertThat(result.failed()).isTrue();
    }

    @Test
    void shouldReturnCorrectFormatAndJwt() {
        var verifiableCredentials = generateCredentials();
        var presentation = generateJwtPresentation(verifiableCredentials);
        var jwtPresentation = buildPresentationJwt(presentation);
        var envelope = new JwtPresentationEnvelope(jwtPresentation);

        var format = envelope.format();
        var resultJwt = envelope.getJwt();

        assertThat(format).isEqualTo("application/vp+ld+jwt");
        assertThat(resultJwt).isEqualTo(jwtPresentation);
    }

    private JwtPresentation generateJwtPresentation(List<VerifiableCredential> credentials) {
        var verifiableCredentials = credentials.stream()
                .map(vc -> buildSignedJwt(vc.getItem(), ISSUER, SUBJECT, privateKey).serialize())
                .toList();
        return JwtPresentation.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .type(JwtPresentation.DEFAULT_TYPE)
                .signedCredentials(verifiableCredentials)
                .build();
    }

    private List<VerifiableCredential> generateCredentials() {
        return List.of(new VerifiableCredential(generateCredential(), null));
    }

    private SignedJWT buildPresentationJwt(JwtPresentation jwtPresentation) {
        var claims = new JWTClaimsSet.Builder()
                .claim(VERIFIABLE_PRESENTATION_KEY, OBJECT_MAPPER.convertValue(jwtPresentation, Map.class))
                .issuer(JwtPresentationEnvelopeTest.ISSUER)
                .subject(JwtPresentationEnvelopeTest.SUBJECT)
                .expirationTime(null)
                .notBeforeTime(null)
                .build();

        return buildSignedJwt(claims, privateKey);
    }
}
