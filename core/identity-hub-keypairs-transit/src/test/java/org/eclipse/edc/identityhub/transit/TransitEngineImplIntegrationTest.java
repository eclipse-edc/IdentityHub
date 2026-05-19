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

package org.eclipse.edc.identityhub.transit;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import org.eclipse.edc.http.client.EdcHttpClientImpl;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.vault.VaultContainer;

import java.io.IOException;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.Mockito.mock;

@ComponentTest
@Testcontainers
class TransitEngineImplIntegrationTest {

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
    void setUp() throws Exception {
        var vaultBaseUrl = "http://%s:%d".formatted(VAULT.getHost(), VAULT.getMappedPort(8200));
        transitEngine = new TransitEngineImpl(() -> VAULT_TOKEN, new ObjectMapper(), client, vaultBaseUrl);
    }

    @Test
    void generateKey() {
        var keyName = "test-key-" + UUID.randomUUID();
        var result = transitEngine.generateKey(keyName, "ed25519");
        assertThat(result).isSucceeded().satisfies(desc -> {
            assertThat(desc.getData().getKeys()).hasSize(1);
            assertThat(desc.getData().getLatestVersion()).isEqualTo(1);
            assertThat(desc.getData().isExportable()).isFalse();
        });
    }

    @Test
    void rotateKey() {
        var keyName = "test-key-" + UUID.randomUUID();
        assertThat(transitEngine.generateKey(keyName, "ed25519")).isSucceeded();
        var result = transitEngine.rotateKey(keyName);
        assertThat(result).isSucceeded();
    }

    @Test
    void rotateKey_notExist() {
        var keyName = "not-exist";
        var result = transitEngine.rotateKey(keyName);
        assertThat(result).isFailed().detail().contains("not found");
    }

    @Test
    void rotateKey_severalTimes() {
        var keyName = "test-key-" + UUID.randomUUID();
        assertThat(transitEngine.generateKey(keyName, "ed25519")).isSucceeded();
        assertThat(transitEngine.rotateKey(keyName)).isSucceeded();
        assertThat(transitEngine.rotateKey(keyName)).isSucceeded();
        assertThat(transitEngine.rotateKey(keyName)).isSucceeded();
    }

    @Test
    void getKey() {
        var keyName = "test-key-" + UUID.randomUUID();
        assertThat(transitEngine.generateKey(keyName, "ed25519")).isSucceeded();

        var result = transitEngine.getKey(keyName);
        assertThat(result).isSucceeded()
                .satisfies(desc -> {
                    assertThat(desc.getData().getKeys()).hasSize(1);
                    assertThat(desc.getData().getName()).isEqualTo(keyName);
                });
    }

    @Test
    void getKey_notExist() {
        assertThat(transitEngine.getKey("not-exist")).isFailed().detail().contains("404");
    }

    @Test
    void generateKey_whenAlreadyExists_shouldOverwrite() {
        var keyName = "test-key-" + UUID.randomUUID();
        assertThat(transitEngine.generateKey(keyName, "ed25519")).isSucceeded();
        assertThat(transitEngine.generateKey(keyName, "ed25519")).isSucceeded();
    }

    @Test
    void getKey_afterRotation_hasMultipleVersions() {
        var keyName = "test-key-" + UUID.randomUUID();
        assertThat(transitEngine.generateKey(keyName, "ed25519")).isSucceeded();
        assertThat(transitEngine.rotateKey(keyName)).isSucceeded();
        assertThat(transitEngine.rotateKey(keyName)).isSucceeded();

        var result = transitEngine.getKey(keyName);
        assertThat(result).isSucceeded().satisfies(desc -> {
            assertThat(desc.getData().getKeys()).hasSize(3);
            assertThat(desc.getData().getLatestVersion()).isEqualTo(3);
        });
    }

    @Test
    void generateKey_whenInvalidToken_shouldFail() {
        var vaultBaseUrl = "http://%s:%d".formatted(VAULT.getHost(), VAULT.getMappedPort(8200));
        var engineWithBadToken = new TransitEngineImpl(() -> "wrong-token", new ObjectMapper(), client, vaultBaseUrl);
        assertThat(engineWithBadToken.generateKey("test-key-" + UUID.randomUUID(), "ed25519")).isFailed();
    }

    @Test
    void rotateKey_severalTimes_incrementsLatestVersion() {
        var keyName = "test-key-" + UUID.randomUUID();
        assertThat(transitEngine.generateKey(keyName, "ed25519")).isSucceeded();
        assertThat(transitEngine.rotateKey(keyName)).isSucceeded();
        assertThat(transitEngine.rotateKey(keyName)).isSucceeded();

        var result = transitEngine.getKey(keyName);
        assertThat(result).isSucceeded().satisfies(desc ->
                assertThat(desc.getData().getLatestVersion()).isEqualTo(3));
    }

    @Test
    void setMinEncryptionKeyVersion() {
        var keyName = "test-key-" + UUID.randomUUID();
        assertThat(transitEngine.generateKey(keyName, "ed25519")).isSucceeded();
        assertThat(transitEngine.rotateKey(keyName)).isSucceeded();

        assertThat(transitEngine.setMinEncryptionKeyVersion(keyName, 2)).isSucceeded();
    }

    @Test
    void setMinEncryptionKeyVersion_whenKeyNotExist() {
        assertThat(transitEngine.setMinEncryptionKeyVersion("not-exist", 1)).isFailed();
    }

    @Test
    void setMinEncryptionKeyVersion_whenVersionHigherThanLatest_shouldFail() {
        var keyName = "test-key-" + UUID.randomUUID();
        assertThat(transitEngine.generateKey(keyName, "ed25519")).isSucceeded();

        assertThat(transitEngine.setMinEncryptionKeyVersion(keyName, 99)).isFailed();
    }

    @Test
    void setMinEncryptionKeyVersion_whenNegativeVersion_shouldThrow() {
        assertThatThrownBy(() -> transitEngine.setMinEncryptionKeyVersion("any-key", -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void setMinDecryptionKeyVersion() {
        var keyName = "test-key-" + UUID.randomUUID();
        assertThat(transitEngine.generateKey(keyName, "ed25519")).isSucceeded();
        assertThat(transitEngine.rotateKey(keyName)).isSucceeded();

        assertThat(transitEngine.setMinDecryptionKeyVersion(keyName, 2)).isSucceeded();
    }

    @Test
    void setMinDecryptionKeyVersion_whenKeyNotExist() {
        assertThat(transitEngine.setMinDecryptionKeyVersion("not-exist", 1)).isFailed();
    }

    @Test
    void setMinDecryptionKeyVersion_whenVersionHigherThanLatest_shouldFail() {
        var keyName = "test-key-" + UUID.randomUUID();
        assertThat(transitEngine.generateKey(keyName, "ed25519")).isSucceeded();

        assertThat(transitEngine.setMinDecryptionKeyVersion(keyName, 99)).isFailed();
    }

    @Test
    void setMinDecryptionKeyVersion_whenNegativeVersion_shouldThrow() {
        assertThatThrownBy(() -> transitEngine.setMinDecryptionKeyVersion("any-key", -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void setMinAvailableVersion_whenMinEncVersionNotSet() {
        var keyName = "test-key-" + UUID.randomUUID();
        assertThat(transitEngine.generateKey(keyName, "ed25519")).isSucceeded();
        assertThat(transitEngine.rotateKey(keyName)).isSucceeded();
        assertThat(transitEngine.rotateKey(keyName)).isSucceeded();
        // min_available_version must be <= min_decryption_version, so raise that first
        assertThat(transitEngine.setMinDecryptionKeyVersion(keyName, 2)).isSucceeded();
        assertThat(transitEngine.setMinAvailableVersion(keyName, 2)).isFailed();
    }

    @Test
    void setMinAvailableVersion() {
        var keyName = "test-key-" + UUID.randomUUID();
        assertThat(transitEngine.generateKey(keyName, "ed25519")).isSucceeded();
        assertThat(transitEngine.rotateKey(keyName)).isSucceeded();
        assertThat(transitEngine.rotateKey(keyName)).isSucceeded();
        // min_available_version must be <= min_decryption_version, so raise that first
        assertThat(transitEngine.setMinDecryptionKeyVersion(keyName, 2)).isSucceeded();
        assertThat(transitEngine.setMinEncryptionKeyVersion(keyName, 2)).isSucceeded();
        assertThat(transitEngine.setMinAvailableVersion(keyName, 2)).isSucceeded();
    }

    @Test
    void setMinAvailableVersion_whenKeyNotExist() {
        assertThat(transitEngine.setMinAvailableVersion("not-exist", 1)).isFailed();
    }

    @Test
    void setMinAvailableVersion_whenNegativeVersion_shouldThrow() {
        assertThatThrownBy(() -> transitEngine.setMinAvailableVersion("any-key", -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sign() {
        var keyName = "test-key-" + UUID.randomUUID();
        assertThat(transitEngine.generateKey(keyName, "ed25519")).isSucceeded();

        var payload = """
                {
                    "testkey": "test-value",
                    "testkey2": 42
                }
                """;
        var result = transitEngine.sign(keyName, payload);
        assertThat(result).isSucceeded().satisfies(sig -> {
            assertThat(sig).isNotBlank().startsWith("vault:v1:");
        });
    }

    @Test
    void sign_whenPayloadB64_shouldDoubleEncode() {
        var keyName = "test-key-" + UUID.randomUUID();
        assertThat(transitEngine.generateKey(keyName, "ed25519")).isSucceeded();

        var payload = """
                {
                    "testkey": "test-value",
                    "testkey2": 42
                }
                """;
        var encoded = Base64.getEncoder().encodeToString(payload.getBytes());
        var result = transitEngine.sign(keyName, encoded);
        assertThat(result).isSucceeded().satisfies(sig -> {
            assertThat(sig).isNotBlank().startsWith("vault:v1:");
        });
    }

    @Test
    void sign_afterRotation_usesLatestKeyVersion() {
        var keyName = "test-key-" + UUID.randomUUID();
        assertThat(transitEngine.generateKey(keyName, "ed25519")).isSucceeded();
        assertThat(transitEngine.rotateKey(keyName)).isSucceeded();

        var payload = Base64.getEncoder().encodeToString("test-payload".getBytes());
        var result = transitEngine.sign(keyName, payload);
        assertThat(result).isSucceeded().satisfies(sig -> {
            assertThat(sig).isNotBlank().startsWith("vault:v2:");
        });
    }

    @Test
    void sign_whenKeyNotExist_shouldFail() {
        var payload = Base64.getEncoder().encodeToString("test-payload".getBytes());
        assertThat(transitEngine.sign("not-exist", payload)).isFailed();
    }

    @Test
    void verify() {
        var keyName = "test-key-" + UUID.randomUUID();
        assertThat(transitEngine.generateKey(keyName, "ed25519")).isSucceeded();

        var payload = "test-payload";
        var signature = transitEngine.sign(keyName, payload).getContent();
        assertThat(transitEngine.verify(keyName, payload, signature)).isSucceeded();
    }

    @Test
    void verify_afterRotation_oldSignatureStillValid() {
        var keyName = "test-key-" + UUID.randomUUID();
        assertThat(transitEngine.generateKey(keyName, "ed25519")).isSucceeded();

        var payload = "test-payload";
        var signature = transitEngine.sign(keyName, payload).getContent();
        assertThat(transitEngine.rotateKey(keyName)).isSucceeded();

        assertThat(transitEngine.verify(keyName, payload, signature)).isSucceeded();
    }

    @Test
    void verify_whenSignatureInvalid_shouldFail() {
        var keyName = "test-key-" + UUID.randomUUID();
        assertThat(transitEngine.generateKey(keyName, "ed25519")).isSucceeded();

        assertThat(transitEngine.verify(keyName, "test-payload", "vault:v1:invalidsignature")).isFailed();
    }

    @Test
    void verify_whenKeyNotExist_shouldFail() {
        assertThat(transitEngine.verify("not-exist", "test-payload", "vault:v1:invalidsignature")).isFailed();
    }

    @Test
    void verify_whenPayloadTampered_shouldFail() {
        var keyName = "test-key-" + UUID.randomUUID();
        assertThat(transitEngine.generateKey(keyName, "ed25519")).isSucceeded();

        var signature = transitEngine.sign(keyName, "original-payload").getContent();
        assertThat(transitEngine.verify(keyName, "tampered-payload", signature)).isFailed();
    }

    @Test
    void signAndVerify_failsOnOldSignature() {
        var keyName = "test-key-" + UUID.randomUUID();
        assertThat(transitEngine.generateKey(keyName, "ed25519")).isSucceeded();

        var payload = "test-payload";
        var signature = transitEngine.sign(keyName, payload).getContent();
        assertThat(transitEngine.rotateKey(keyName)).isSucceeded(); // latest version is now 2
        assertThat(transitEngine.setMinDecryptionKeyVersion(keyName, 2)).isSucceeded();

        assertThat(transitEngine.verify(keyName, payload, signature)).isFailed().detail().contains("ciphertext or signature version is disallowed by policy (too old)");
    }

    @Test
    void signAndVerify_failsOnOldKey() {
        var keyName = "test-key-" + UUID.randomUUID();
        assertThat(transitEngine.generateKey(keyName, "ed25519")).isSucceeded();

        var payload = "test-payload";
        var signature = transitEngine.sign(keyName, payload).getContent();
        assertThat(transitEngine.rotateKey(keyName)).isSucceeded();
        assertThat(transitEngine.setMinEncryptionKeyVersion(keyName, 2)).isSucceeded();
        assertThat(transitEngine.setMinDecryptionKeyVersion(keyName, 2)).isSucceeded();
        assertThat(transitEngine.setMinAvailableVersion(keyName, 2)).isSucceeded();

        assertThat(transitEngine.verify(keyName, payload, signature)).isFailed().detail().contains("ciphertext or signature version is disallowed by policy (too old)");
    }

    @Test
    void deleteKey() {
        var keyName = "test-key-" + UUID.randomUUID();
        assertThat(transitEngine.generateKey(keyName, "ed25519")).isSucceeded();

        assertThat(transitEngine.deleteKey(keyName)).isSucceeded();
        assertThat(transitEngine.getKey(keyName)).isFailed().detail().contains("404");
    }

    @Test
    void deleteKey_whenKeyNotExist_shouldFail() {
        assertThat(transitEngine.deleteKey("not-exist")).isFailed();
    }
}