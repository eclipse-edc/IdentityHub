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
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.buildSignedJwt;
import static org.eclipse.edc.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.generateEcKey;

class JwtPresentationEnvelopeTransformerTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ECKey privateKey;

    @BeforeEach
    public void setUp() {
        privateKey = generateEcKey();
    }

    @Test
    void shouldParseJwtPresentationEnvelope() {
        var transformer = new JwtPresentationEnvelopeTransformer(OBJECT_MAPPER);
        var jwt = generateJwt();
        var envelope = new JwtPresentationEnvelope(jwt);
        var serializedEnvelope = jwt.serialize().getBytes(StandardCharsets.UTF_8);

        var result = transformer.parse(serializedEnvelope);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).usingRecursiveComparison().isEqualTo(envelope);
    }

    @Test
    void shouldFailResult_OnInvalidEnvelope() {
        var transformer = new JwtPresentationEnvelopeTransformer(OBJECT_MAPPER);
        var jwt = "wrong-value";
        var serializedEnvelope = jwt.getBytes(StandardCharsets.UTF_8);

        var result = transformer.parse(serializedEnvelope);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureDetail()).contains("Failed to parse Verifiable Presentation");
    }

    @Test
    void shouldSerializeJwt() {
        var transformer = new JwtPresentationEnvelopeTransformer(OBJECT_MAPPER);
        var jwt = generateJwt();
        var envelope = new JwtPresentationEnvelope(jwt);

        var result = transformer.serialize(envelope);

        var expectedSerializedEnvelope = jwt.serialize().getBytes(StandardCharsets.UTF_8);
        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).isEqualTo(expectedSerializedEnvelope);
    }

    @Test
    void shouldReturnCorrectDataFormat() {
        var transformer = new JwtPresentationEnvelopeTransformer(OBJECT_MAPPER);

        var format = transformer.dataFormat();

        assertThat(format).isEqualTo("application/vp+ld+jwt");
    }


    private SignedJWT generateJwt() {
        var claims = new JWTClaimsSet.Builder()
                .claim("test-claim", new Object())
                .issuer("issuer")
                .subject("subject")
                .expirationTime(null)
                .notBeforeTime(null)
                .build();

        return buildSignedJwt(claims, privateKey);
    }
}
