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
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import dev.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import org.assertj.core.api.Assertions;
import org.eclipse.edc.http.client.EdcHttpClientImpl;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.identityhub.transit.TransitEngineImpl;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.spi.TokenValidationRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.vault.VaultContainer;

import java.io.IOException;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Testcontainers
class TransitValidationServiceTest {
    public static final String KEY_NAME = "test-key";
    static final String VAULT_TOKEN = "root";
    @Container
    static final VaultContainer<?> VAULT = new VaultContainer<>("hashicorp/vault")
            .withVaultToken(VAULT_TOKEN)
            .withEnv("SKIP_SETCAP", "true")
            .withEnv("VAULT_DEV_ROOT_TOKEN_ID", VAULT_TOKEN)
            .withExposedPorts(8200);

    private final EdcHttpClient client = new EdcHttpClientImpl(new OkHttpClient.Builder().build(), RetryPolicy.ofDefaults(), mock());
    private final TransitLocalPublicKeyResolver publicKeyResolver = new TransitLocalPublicKeyResolver();
    private TransitValidationService service;
    private TransitEngineImpl transitEngine;

    @BeforeAll
    static void prepare() throws IOException, InterruptedException {
        VAULT.execInContainer("vault", "secrets", "enable", "transit");
    }

    @BeforeEach
    void setup() {
        var vaultBaseUrl = "http://%s:%d".formatted(VAULT.getHost(), VAULT.getMappedPort(8200));
        transitEngine = new TransitEngineImpl(() -> VAULT_TOKEN, new ObjectMapper(), client, vaultBaseUrl);
        service = new TransitValidationService(transitEngine);
        assertThat(transitEngine.generateKey(KEY_NAME, "ed25519")).isSucceeded();
    }

    @Test
    void validate_keyNameContainsVersion() throws JOSEException {
        var signer = new TransitSigner(transitEngine, KEY_NAME);
        var header = new JWSHeader.Builder(JWSAlgorithm.Ed25519).keyID(KEY_NAME).build();
        var claims = createClaims();
        var signedJwt = new SignedJWT(header, claims);
        signedJwt.sign(signer);
        var jwt = signedJwt.serialize();

        var result = service.validate(jwt, publicKeyResolver);
        assertThat(result).isSucceeded();
    }

    @Test
    void validate_whenSigningKeyIsRotatedButStillValid() throws JOSEException {


        var signer = new TransitSigner(transitEngine, KEY_NAME);
        var header = new JWSHeader.Builder(JWSAlgorithm.Ed25519).keyID(KEY_NAME).build();
        var claims = createClaims();
        var signedJwt = new SignedJWT(header, claims);
        signedJwt.sign(signer);
        var jwt = signedJwt.serialize();

        // sign, rotate key, leave old key valid for decryption
        assertThat(transitEngine.rotateKey(KEY_NAME)).isSucceeded(); // new key is now v2
        assertThat(transitEngine.setMinEncryptionKeyVersion(KEY_NAME, 2)).isSucceeded();
        assertThat(transitEngine.setMinDecryptionKeyVersion(KEY_NAME, 0)).isSucceeded();

        var result = service.validate(jwt, publicKeyResolver);
        assertThat(result).isSucceeded();
    }

    @Test
    void validate_whenSigningKeyIsRotatedAndNotValid() throws JOSEException {

        var signer = new TransitSigner(transitEngine, KEY_NAME);
        var header = new JWSHeader.Builder(JWSAlgorithm.Ed25519).keyID(KEY_NAME).build();
        var claims = createClaims();
        var signedJwt = new SignedJWT(header, claims);
        signedJwt.sign(signer);
        var jwt = signedJwt.serialize();

        // sign, rotate, invalidate old key for decryption
        assertThat(transitEngine.rotateKey(KEY_NAME)).isSucceeded(); // new key is now v2
        assertThat(transitEngine.setMinEncryptionKeyVersion(KEY_NAME, 2)).isSucceeded();
        assertThat(transitEngine.setMinDecryptionKeyVersion(KEY_NAME, 2)).isSucceeded();

        var result = service.validate(jwt, publicKeyResolver);
        assertThat(result).isFailed().detail().contains("ciphertext or signature version is disallowed by policy (too old)");
    }

    @Test
    void validate_whenNoKidInHeader() throws JOSEException {
        var signer = new TransitSigner(transitEngine, KEY_NAME);
        var header = new JWSHeader.Builder(JWSAlgorithm.Ed25519).build(); // no keyID
        var claims = createClaims();
        var signedJwt = new SignedJWT(header, claims);
        signedJwt.sign(signer);

        var result = service.validate(signedJwt.serialize(), publicKeyResolver);
        assertThat(result).isFailed().detail().contains("A Key-ID header is required");
    }

    @Test
    void validate_whenKeyNotFound() throws JOSEException {
        var signer = new TransitSigner(transitEngine, KEY_NAME);
        var header = new JWSHeader.Builder(JWSAlgorithm.Ed25519).keyID("non-existent-key").build();
        var claims = createClaims();
        var signedJwt = new SignedJWT(header, claims);
        signedJwt.sign(signer);

        var result = service.validate(signedJwt.serialize(), publicKeyResolver);
        assertThat(result).isFailed();
    }

    @Test
    void validate_whenSignedWithDifferentKey() throws JOSEException {
        var otherKey = "other-key";
        assertThat(transitEngine.generateKey(otherKey, "ed25519")).isSucceeded();

        // sign with otherKey but claim the token was signed with KEY_NAME
        var signer = new TransitSigner(transitEngine, otherKey);
        var header = new JWSHeader.Builder(JWSAlgorithm.Ed25519).keyID(KEY_NAME).build();
        var claims = createClaims();
        var signedJwt = new SignedJWT(header, claims);
        signedJwt.sign(signer);

        var result = service.validate(signedJwt.serialize(), publicKeyResolver);
        assertThat(result).isFailed();
    }

    @Test
    void validate_whenTokenIsTampered() throws JOSEException {
        var signer = new TransitSigner(transitEngine, KEY_NAME);
        var header = new JWSHeader.Builder(JWSAlgorithm.Ed25519).keyID(KEY_NAME).build();
        var claims = createClaims();
        var signedJwt = new SignedJWT(header, claims);
        signedJwt.sign(signer);

        // replace the payload with different claims, keeping the original header and signature
        var parts = signedJwt.serialize().split("\\.");
        var tamperedPayload = Base64URL.encode("{\"iss\":\"evil-issuer\",\"sub\":\"admin\"}").toString();
        var tamperedJwt = parts[0] + "." + tamperedPayload + "." + parts[2];

        var result = service.validate(tamperedJwt, publicKeyResolver);
        assertThat(result).isFailed();
    }


    @Test
    void validationFailure_singleRuleFails() throws JOSEException {
        var ruleMock = mock(TokenValidationRule.class);
        when(ruleMock.checkRule(any(), any())).thenReturn(Result.failure("Rule validation failed!"));
        var signer = new TransitSigner(transitEngine, KEY_NAME);
        var header = new JWSHeader.Builder(JWSAlgorithm.Ed25519).keyID(KEY_NAME).build();
        var claims = createClaims();
        var signedJwt = new SignedJWT(header, claims);
        signedJwt.sign(signer);
        var result = service.validate(signedJwt.serialize(), publicKeyResolver, ruleMock);

        Assertions.assertThat(result.failed()).isTrue();
        Assertions.assertThat(result.getFailureMessages()).containsExactly("Rule validation failed!");
    }

    @Test
    void validationFailure_multipleRulesFail() throws JOSEException {
        var r1 = mock(TokenValidationRule.class);
        var r2 = mock(TokenValidationRule.class);
        var r3 = mock(TokenValidationRule.class);

        when(r1.checkRule(any(), any())).thenReturn(Result.failure("test-failure1"));
        when(r2.checkRule(any(), any())).thenReturn(Result.success());
        when(r3.checkRule(any(), any())).thenReturn(Result.failure("test-failure2"));
        var signer = new TransitSigner(transitEngine, KEY_NAME);
        var header = new JWSHeader.Builder(JWSAlgorithm.Ed25519).keyID(KEY_NAME).build();
        var claims = createClaims();
        var signedJwt = new SignedJWT(header, claims);
        signedJwt.sign(signer);

        var result = service.validate(signedJwt.serialize(), publicKeyResolver, r1, r2, r3);

        Assertions.assertThat(result.failed()).isTrue();
        Assertions.assertThat(result.getFailureMessages()).containsExactlyInAnyOrder("test-failure1", "test-failure2");
    }


    private JWTClaimsSet createClaims() {
        return new JWTClaimsSet.Builder().issuer("test-issuer").subject("test-subject").build();
    }

}