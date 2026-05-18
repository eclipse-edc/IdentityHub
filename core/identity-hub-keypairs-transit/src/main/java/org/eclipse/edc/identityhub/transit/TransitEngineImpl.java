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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.vault.hashicorp.spi.auth.HashicorpVaultTokenProvider;

import java.io.IOException;
import java.util.Map;

public class TransitEngineImpl implements TransitEngine {
    private static final String VAULT_TOKEN_HEADER = "X-Vault-Token";
    private final HashicorpVaultTokenProvider tokenProvider;
    private final ObjectMapper objectMapper;
    private final EdcHttpClient edcHttpClient;
    private final String vaultBaseUrl;

    public TransitEngineImpl(HashicorpVaultTokenProvider tokenProvider, ObjectMapper objectMapper, EdcHttpClient edcHttpClient, String vaultBaseUrl) {
        this.tokenProvider = tokenProvider;
        this.objectMapper = objectMapper;
        this.edcHttpClient = edcHttpClient;
        this.vaultBaseUrl = vaultBaseUrl;
    }

    @Override
    public Result<TransitKeyDescriptor> generateKey(String keyName) {
        var payload = Map.of("type", "ed25519", "exportable", false);
        var request = vaultRequest()
                .url(vaultBaseUrl + "/v1/transit/keys/" + keyName)
                .post(jsonBody(payload))
                .build();
        try (var response = edcHttpClient.execute(request)) {
            if (!response.isSuccessful()) {
                return response.body() == null ? Result.failure("Creating a key returned empty body") : Result.failure("Failed to create key: code %d, message:  %s".formatted(response.code(), response.body().string()));
            }
            var responseBody = response.body().string();
            return Result.success(objectMapper.readValue(responseBody, TransitKeyDescriptor.class));
        } catch (IOException e) {
            return Result.failure("Failed to generate key with reason: %s".formatted(e.getMessage()));
        }
    }

    @Override
    public Result<Void> rotateKey(String keyName) {
        var request = vaultRequest()
                .url(vaultBaseUrl + "/v1/transit/keys/" + keyName + "/rotate")
                .post(RequestBody.EMPTY)
                .build();

        try (var response = edcHttpClient.execute(request)) {
            if (!response.isSuccessful()) {
                return response.body() == null ? Result.failure("Rotating a key returned empty body") : Result.failure("Failed to rotate key: code %d, message:  %s".formatted(response.code(), response.body().string()));
            }
            return Result.success();
        } catch (IOException e) {
            return Result.failure("Failed to rotate key with reason: %s".formatted(e.getMessage()));
        }

    }

    @Override
    public Result<TransitKeyDescriptor> getKey(String keyName) {
        var request = vaultRequest()
                .url(vaultBaseUrl + "/v1/transit/keys/" + keyName)
                .get()
                .build();

        try (var response = edcHttpClient.execute(request)) {
            if (!response.isSuccessful()) {
                return response.body() == null ? Result.failure("Getting a key returned empty body") : Result.failure("Failed to get key: code %d, message:  %s".formatted(response.code(), response.body().string()));
            }
            return Result.success(objectMapper.readValue(response.body().string(), TransitKeyDescriptor.class));
        } catch (IOException e) {
            return Result.failure("Failed to get key with reason: %s".formatted(e.getMessage()));
        }
    }

    @Override
    public Result<Void> setMinEncryptionKeyVersion(String keyName, int minVersion) {
        if (minVersion < 0) {
            throw new IllegalArgumentException("Min version must be a positive integer");
        }
        return postKeyConfig(keyName, TransitKeyConfig.Builder.newInstance().minEncryptionVersion(minVersion).build());
    }

    @Override
    public Result<Void> setMinDecryptionKeyVersion(String keyName, int minVersion) {
        if (minVersion < 0) {
            throw new IllegalArgumentException("Min version must be a positive integer");
        }
        return postKeyConfig(keyName, TransitKeyConfig.Builder.newInstance().minDecryptionVersion(minVersion).build());
    }

    @Override
    public Result<Void> setMinAvailableVersion(String keyName, int minVersion) {
        if (minVersion < 0) {
            throw new IllegalArgumentException("Min version must be a positive integer");
        }
        var body = Map.of("min_available_version", minVersion);

        var request = vaultRequest()
                .url(vaultBaseUrl + "/v1/transit/keys/" + keyName + "/trim")
                .post(jsonBody(body))
                .build();
        try (var response = edcHttpClient.execute(request)) {
            if (!response.isSuccessful()) {
                return response.body() == null ? Result.failure("Trimming a key returned empty body") : Result.failure("Failed to trim key: code %d, message:  %s".formatted(response.code(), response.body().string()));
            }
            return Result.success();
        } catch (IOException e) {
            return Result.failure("Failed to trim key with reason: %s".formatted(e.getMessage()));
        }
    }

    private Result<Void> postKeyConfig(String keyName, TransitKeyConfig config) {
        var request = vaultRequest()
                .url(vaultBaseUrl + "/v1/transit/keys/" + keyName + "/config")
                .post(jsonBody(config))
                .build();
        try (var response = edcHttpClient.execute(request)) {
            if (!response.isSuccessful()) {
                return response.body() == null ? Result.failure("Configuring key returned empty body") : Result.failure("Failed to configure key: code %d, message: %s".formatted(response.code(), response.body().string()));
            }
            return Result.success();
        } catch (IOException e) {
            return Result.failure("Failed to configure key with reason: %s".formatted(e.getMessage()));
        }
    }

    private Request.Builder vaultRequest() {
        return new Request.Builder()
                .header(VAULT_TOKEN_HEADER, tokenProvider.vaultToken());
    }

    private RequestBody jsonBody(Object body) {
        String jsonRepresentation;
        try {
            jsonRepresentation = objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new EdcException(e);
        }
        return RequestBody.create(jsonRepresentation, MediaType.get("application/json"));
    }
}
