/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import dev.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import org.eclipse.edc.http.client.EdcHttpClientImpl;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.identityhub.transit.TransitEngineImpl;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.vault.VaultContainer;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.Mockito.mock;

@ComponentTest
@Testcontainers
class TransitSignerIntegrationTest {
    static final String VAULT_TOKEN = "root";
    @Container
    static final VaultContainer<?> VAULT = new VaultContainer<>("hashicorp/vault")
            .withVaultToken(VAULT_TOKEN)
            .withEnv("SKIP_SETCAP", "true")
            .withEnv("VAULT_DEV_ROOT_TOKEN_ID", VAULT_TOKEN)
            .withExposedPorts(8200);

    private final EdcHttpClient client = new EdcHttpClientImpl(new OkHttpClient.Builder().build(), RetryPolicy.ofDefaults(), mock());
    private TransitEngineImpl transitEngine;

    @BeforeAll
    static void prepare() throws IOException, InterruptedException {
        VAULT.execInContainer("vault", "secrets", "enable", "transit");
    }

    @BeforeEach
    void setUp() {
        var vaultBaseUrl = "http://%s:%d".formatted(VAULT.getHost(), VAULT.getMappedPort(8200));
        transitEngine = new TransitEngineImpl(() -> VAULT_TOKEN, new ObjectMapper(), client, vaultBaseUrl);
    }

    @Test
    void sign_withNonExistentKey_throwsException() {
        var signer = new TransitSigner(transitEngine, "nonexistent-key");
        var jwsHeader = new JWSHeader.Builder(JWSAlgorithm.Ed25519).build();
        var claimsSet = new JWTClaimsSet.Builder().issuer("test-issuer").build();
        var signedJwt = new SignedJWT(jwsHeader, claimsSet);

        assertThatThrownBy(() -> signedJwt.sign(signer))
                .isInstanceOf(JOSEException.class)
                .hasMessageContaining("Transit signing failed");
    }

    @Test
    void sign_multipleJwts_allParseable() throws JOSEException, ParseException {
        var keyName = "multi-sign-key";
        assertThat(transitEngine.generateKey(keyName, "ed25519")).isSucceeded();
        var signer = new TransitSigner(transitEngine, keyName);

        for (var i = 0; i < 3; i++) {
            var jwsHeader = new JWSHeader.Builder(JWSAlgorithm.Ed25519).keyID("key-" + i).build();
            var claimsSet = new JWTClaimsSet.Builder().subject("subject-" + i).build();
            var jwt = new SignedJWT(jwsHeader, claimsSet);
            jwt.sign(signer);

            var parsed = SignedJWT.parse(jwt.serialize());
            assertThat(parsed.getJWTClaimsSet().getSubject()).isEqualTo("subject-" + i);
        }
    }

    @Test
    void sign_withRsaKey() throws JOSEException, ParseException {
        var keyName = "rsa-test-key";
        assertThat(transitEngine.generateKey(keyName, "rsa-2048")).isSucceeded();
        var signer = new TransitSigner(transitEngine, keyName);

        var jwsHeader = new JWSHeader.Builder(JWSAlgorithm.PS256).keyID("rsa-key-id").build();
        var claimsSet = new JWTClaimsSet.Builder()
                .issuer("test-issuer")
                .subject("test-subject")
                .build();
        var signedJwt = new SignedJWT(jwsHeader, claimsSet);
        signedJwt.sign(signer);

        var parsed = SignedJWT.parse(signedJwt.serialize());
        assertThat(parsed.getHeader().getKeyID()).isEqualTo("rsa-key-id");
        assertThat(parsed.getJWTClaimsSet().getIssuer()).isEqualTo("test-issuer");
        assertThat(parsed.getJWTClaimsSet().getSubject()).isEqualTo("test-subject");
    }


    @Test
    void sign_withEcKey() throws JOSEException, ParseException {
        var keyName = "rsa-test-key";
        assertThat(transitEngine.generateKey(keyName, "ecdsa-p521")).isSucceeded();
        var signer = new TransitSigner(transitEngine, keyName);

        var jwsHeader = new JWSHeader.Builder(JWSAlgorithm.ES512).keyID("ec-key-id").build();
        var claimsSet = new JWTClaimsSet.Builder()
                .issuer("test-issuer")
                .subject("test-subject")
                .build();
        var signedJwt = new SignedJWT(jwsHeader, claimsSet);
        signedJwt.sign(signer);

        var parsed = SignedJWT.parse(signedJwt.serialize());
        assertThat(parsed.getHeader().getKeyID()).isEqualTo("ec-key-id");
        assertThat(parsed.getJWTClaimsSet().getIssuer()).isEqualTo("test-issuer");
        assertThat(parsed.getJWTClaimsSet().getSubject()).isEqualTo("test-subject");
    }

    @Test
    void sign_withEd25519() throws JOSEException, ParseException {
        var keyName = "test-key";
        var keyId = "test-key-id";
        assertThat(transitEngine.generateKey(keyName, "ed25519")).isSucceeded();
        var signer = new TransitSigner(transitEngine, keyName);

        var jwsHeader = new JWSHeader.Builder(JWSAlgorithm.Ed25519).keyID(keyId).build();
        var claimsSet = new JWTClaimsSet.Builder()
                .issuer("test-issuer")
                .subject("test-subject")
                .audience("test-audience")
                .build();
        var signedJwt = new SignedJWT(jwsHeader, claimsSet);
        signedJwt.sign(signer);

        assertThat(signedJwt).isNotNull();
        assertThat(signedJwt.serialize()).isNotNull();

        // round trip: attempt to parse the JWT
        var parsed = SignedJWT.parse(signedJwt.serialize());
        assertThat(parsed).isNotNull();
        assertThat(parsed.getHeader().getKeyID()).isEqualTo(keyId);
        assertThat(parsed.getJWTClaimsSet().getClaims())
                .containsEntry("iss", "test-issuer")
                .containsEntry("sub", "test-subject")
                .containsEntry("aud", List.of("test-audience"));
    }

    @Test
    void verifyJcaContext() {
        assertThat(new TransitSigner(transitEngine, "foobar").getJCAContext()).isNotNull();
    }

    @Test
    void verifyAlgorithms() {
        assertThat(new TransitSigner(transitEngine, "foobar").supportedJWSAlgorithms()).isNotEmpty();
    }

}