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
import okhttp3.Response;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.vault.hashicorp.spi.auth.HashicorpVaultTokenProvider;

import java.io.IOException;
import java.util.Base64;
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
    public Result<TransitKeyDescriptor> generateKey(String keyName, String keyType) {
        var request = vaultRequest()
                .url(vaultBaseUrl + "/v1/transit/keys/" + keyName)
                .post(jsonBody(Map.of("type", keyType,
                        "exportable", false)))
                .build();
        // by default, keys are not deletable, so it needs to be configured explicitly
        return execute(request, TransitKeyDescriptor.class)
                .compose(tkd -> postKeyConfig(keyName, TransitKeyConfig.Builder.newInstance().deletionAllowed(true).build()).map(u -> tkd));
    }

    @Override
    public Result<Void> rotateKey(String keyName) {
        var request = vaultRequest()
                .url(vaultBaseUrl + "/v1/transit/keys/" + keyName + "/rotate")
                .post(RequestBody.EMPTY)
                .build();
        return execute(request);
    }

    @Override
    public Result<TransitKeyDescriptor> getKey(String keyName) {
        var request = vaultRequest()
                .url(vaultBaseUrl + "/v1/transit/keys/" + keyName)
                .get()
                .build();
        return execute(request, TransitKeyDescriptor.class);
    }

    @Override
    public Result<Void> setMinEncryptionKeyVersion(String keyName, int minVersion) {
        requireNonNegative(minVersion);
        return postKeyConfig(keyName, TransitKeyConfig.Builder.newInstance().minEncryptionVersion(minVersion).build());
    }

    @Override
    public Result<Void> setMinDecryptionKeyVersion(String keyName, int minVersion) {
        requireNonNegative(minVersion);
        return postKeyConfig(keyName, TransitKeyConfig.Builder.newInstance().minDecryptionVersion(minVersion).build());
    }

    @Override
    public Result<Void> setMinAvailableVersion(String keyName, int minVersion) {
        requireNonNegative(minVersion);
        var request = vaultRequest()
                .url(vaultBaseUrl + "/v1/transit/keys/" + keyName + "/trim")
                .post(jsonBody(Map.of("min_available_version", minVersion)))
                .build();
        return execute(request);
    }

    @Override
    public Result<String> sign(String keyName, String payload) {
        var encoded = Base64.getEncoder().encodeToString(payload.getBytes());
        var request = vaultRequest()
                .url(vaultBaseUrl + "/v1/transit/sign/" + keyName)
                .post(jsonBody(Map.of("input", encoded)))
                .build();
        var result = execute(request, SignResult.class);
        return result.map(SignResult::getSignature);
    }

    @Override
    public Result<Void> verify(String keyName, String payload, String signature) {
        var encoded = Base64.getEncoder().encodeToString(payload.getBytes());
        var request = vaultRequest()
                .url(vaultBaseUrl + "/v1/transit/verify/" + keyName)
                .post(jsonBody(Map.of("input", encoded, "signature", signature)))
                .build();
        return execute(request, VerifyResult.class)
                .compose(r -> r.isValid() ? Result.success() : Result.failure("Signature is invalid"));
    }

    @Override
    public Result<Void> deleteKey(String keyName) {
        var request = vaultRequest()
                .url(vaultBaseUrl + "/v1/transit/keys/" + keyName)
                .delete()
                .build();
        return execute(request);
    }

    private Result<Void> postKeyConfig(String keyName, TransitKeyConfig config) {
        var request = vaultRequest()
                .url(vaultBaseUrl + "/v1/transit/keys/" + keyName + "/config")
                .post(jsonBody(config))
                .build();
        return execute(request);
    }

    private Result<Void> execute(Request request) {
        try (var response = edcHttpClient.execute(request)) {
            return response.isSuccessful() ? Result.success() : Result.failure(errorMessage(response));
        } catch (IOException e) {
            return Result.failure(e.getMessage());
        }
    }

    private <T> Result<T> execute(Request request, Class<T> responseType) {
        try (var response = edcHttpClient.execute(request)) {
            if (!response.isSuccessful()) {
                return Result.failure(errorMessage(response));
            }
            return Result.success(objectMapper.readValue(response.body().string(), responseType));
        } catch (IOException e) {
            return Result.failure(e.getMessage());
        }
    }

    private String errorMessage(Response response) throws IOException {
        return "Vault responded with code %d: %s".formatted(response.code(), response.body().string());
    }

    private void requireNonNegative(int minVersion) {
        if (minVersion < 0) {
            throw new IllegalArgumentException("Min version must be a positive integer");
        }
    }

    private Request.Builder vaultRequest() {
        return new Request.Builder()
                .header(VAULT_TOKEN_HEADER, tokenProvider.vaultToken());
    }

    private RequestBody jsonBody(Object body) {
        try {
            return RequestBody.create(objectMapper.writeValueAsString(body), MediaType.get("application/json"));
        } catch (JsonProcessingException e) {
            throw new EdcException(e);
        }
    }
}
