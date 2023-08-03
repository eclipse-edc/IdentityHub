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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.ECKey;
import org.assertj.core.api.Assertions;
import org.eclipse.edc.spi.EdcException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.eclipse.edc.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.buildSignedJwt;
import static org.eclipse.edc.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.generateCredential;
import static org.eclipse.edc.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.generateEcKey;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JwtPresentationTest {

    private static final String ISSUER = "issuer";
    private static final String SUBJECT = "subject";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private ECKey privateKey;

    @BeforeEach
    public void setUp() {
        privateKey = generateEcKey();
    }

    @Test
    void verifySerialize() throws JsonProcessingException {
        var presentation = generateJwtPresentation();

        var json = OBJECT_MAPPER.writeValueAsString(presentation);

        assertNotNull(json);

        var result = OBJECT_MAPPER.readValue(json, JwtPresentation.class);
        assertThat(result).usingRecursiveComparison().isEqualTo(presentation);
    }

    @Test
    void verifyDeserialize() throws JsonProcessingException {
        var json = """
                {
                  "@context": ["https://www.w3.org/2018/credentials/v1"],
                  "type": ["VerifiablePresentation"],
                  "verifiableCredential": ["eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9"]
                }
                """;
        var expectedPresentation = JwtPresentation.Builder.newInstance()
                .type(JwtPresentation.DEFAULT_TYPE)
                .context("https://www.w3.org/2018/credentials/v1")
                .signedCredentials(List.of("eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9"))
                .build();
        
        var result = OBJECT_MAPPER.readValue(json, JwtPresentation.class);

        assertThat(result).usingRecursiveComparison().isEqualTo(expectedPresentation);
    }

    @Test
    void verifyNullFieldNotSerialized() throws JsonProcessingException {
        var presentation = JwtPresentation.Builder.newInstance()
                .type(JwtPresentation.DEFAULT_TYPE)
                .signedCredentials(generateSignedCredentials())
                .build();

        var json = OBJECT_MAPPER.writeValueAsString(presentation);

        Assertions.assertThat(json)
                .doesNotContain("`id`")
                .doesNotContain("`context`");
    }

    @Test
    void verifyTypeMandatory() {
        assertThatExceptionOfType(EdcException.class).isThrownBy(() -> JwtPresentation.Builder.newInstance()
                        .id(UUID.randomUUID().toString())
                        .signedCredentials(generateSignedCredentials())
                        .build())
                .withMessageContaining("`type`");
    }

    @Test
    void verifySignedCredentialsMandatory() {
        assertThatExceptionOfType(EdcException.class).isThrownBy(() -> JwtPresentation.Builder.newInstance()
                        .id(UUID.randomUUID().toString())
                        .type(JwtPresentation.DEFAULT_TYPE)
                        .build())
                .withMessageContaining("`verifiableCredential`");
    }

    private JwtPresentation generateJwtPresentation() {
        return JwtPresentation.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .context("https://www.w3.org/2018/credentials/v1")
                .type(JwtPresentation.DEFAULT_TYPE)
                .signedCredentials(generateSignedCredentials())
                .build();
    }

    private List<String> generateSignedCredentials() {
        return List.of(buildSignedJwt(generateCredential(), ISSUER, SUBJECT, privateKey).serialize());
    }
}
