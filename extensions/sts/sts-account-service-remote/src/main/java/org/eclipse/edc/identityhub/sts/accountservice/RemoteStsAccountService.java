/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
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

package org.eclipse.edc.identityhub.sts.accountservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.identitytrust.sts.spi.model.StsAccount;
import org.eclipse.edc.identityhub.spi.participantcontext.StsAccountService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.ServiceResult;

import java.io.IOException;
import java.util.Map;
import java.util.function.Supplier;

import static org.eclipse.edc.spi.result.ServiceResult.success;

/**
 * Manages {@link StsAccount} objects by directly interacting with a (local) storage. This is useful if the STS is directly
 * embedded into IdentityHub.
 */
class RemoteStsAccountService implements StsAccountService {

    public static final MediaType JSON = MediaType.get("application/json");
    private final String stsAccountApiBaseUrl;
    private final EdcHttpClient edcHttpClient;
    private final Supplier<Map<String, String>> headerSupplier;
    private final Monitor monitor;
    private final ObjectMapper objectMapper;

    RemoteStsAccountService(String stsAccountApiBaseUrl, EdcHttpClient edcHttpClient, Supplier<Map<String, String>> headerSupplier, Monitor monitor, ObjectMapper objectMapper) {
        this.stsAccountApiBaseUrl = stsAccountApiBaseUrl;
        this.edcHttpClient = edcHttpClient;
        this.headerSupplier = headerSupplier;
        this.monitor = monitor;
        this.objectMapper = objectMapper;
    }

    @Override
    public ServiceResult<Void> createAccount(ParticipantManifest manifest, String secretAlias) {
        var account = StsAccount.Builder.newInstance()
                .id(manifest.getParticipantId())
                .name(manifest.getParticipantId())
                .clientId(manifest.getDid())
                .did(manifest.getDid())
                .privateKeyAlias(manifest.getKey().getPrivateKeyAlias())
                .publicKeyReference(manifest.getKey().getKeyId())
                .secretAlias(secretAlias)
                .build();

        return stringify(account)
                .map(json -> request("/v1alpha/accounts")
                        .post(RequestBody.create(json, JSON))
                        .build())
                .compose(this::executeRequest);
    }

    @Override
    public ServiceResult<Void> deleteAccount(String participantId) {
        var rq = request("/v1alpha/accounts/")
                .delete()
                .build();
        return executeRequest(rq);
    }

    @Override
    public ServiceResult<Void> updateAccount(StsAccount updatedAccount) {
        return stringify(updatedAccount)
                .map(json -> request("/v1alpha/accounts/")
                        .put(RequestBody.create(json, JSON))
                        .build())
                .compose(this::executeRequest);
    }

    @Override
    public ServiceResult<StsAccount> findById(String id) {
        var rq = request("/v1alpha/accounts/%s".formatted(id))
                .get()
                .build();

        return executeRequest(rq);
    }

    private ServiceResult<String> stringify(StsAccount updatedAccount) {
        try {
            return success(objectMapper.writeValueAsString(updatedAccount));
        } catch (JsonProcessingException e) {
            var msg = "Error while converting StsAccount to JSON";
            monitor.severe(msg, e);
            return ServiceResult.unexpected(msg); // todo: should this be badRequest?
        }
    }

    private <T> ServiceResult<T> executeRequest(Request rq) {
        try (var response = edcHttpClient.execute(rq)) {
            if (response.isSuccessful()) {
                var body = response.body();
                if (body != null) {
                    return success(objectMapper.readValue(body.string(), new TypeReference<>() {
                    }));
                }
                return success();
            }
            return failureFromResponse(response);
        } catch (IOException e) {
            monitor.severe("Error deleting account", e);
            return ServiceResult.unexpected(e.getMessage());
        }
    }

    private <T> ServiceResult<T> failureFromResponse(Response response) {
        return switch (response.code()) {
            case 400 -> ServiceResult.badRequest(response.message());
            case 401 -> ServiceResult.unauthorized(response.message());
            case 404 -> ServiceResult.notFound(response.message());
            case 409 -> ServiceResult.conflict(response.message());
            default -> ServiceResult.unexpected("Unexpected HTTP status code: %d".formatted(response.code()));
        };
    }

    private Request.Builder request(String path) {
        var builder = new Request.Builder();
        headerSupplier.get().forEach(builder::header);
        return builder.header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .url(stsAccountApiBaseUrl + path);
    }
}
