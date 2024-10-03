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
import org.eclipse.edc.util.string.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Map;
import java.util.function.Supplier;

import static org.eclipse.edc.spi.result.ServiceResult.success;

/**
 * Manages {@link StsAccount} objects by directly interacting with a (local) storage. This is useful if the STS is directly
 * embedded into IdentityHub.
 */
public class RemoteStsAccountService implements StsAccountService {

    public static final MediaType JSON = MediaType.get("application/json");
    private final String stsAccountApiBaseUrl;
    private final EdcHttpClient edcHttpClient;
    private final Supplier<Map<String, String>> headerSupplier;
    private final Monitor monitor;
    private final ObjectMapper objectMapper;

    public RemoteStsAccountService(String stsAccountApiBaseUrl, EdcHttpClient edcHttpClient, Supplier<Map<String, String>> headerSupplier, Monitor monitor, ObjectMapper objectMapper) {
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


        return stringify(new CreateAccountRequest(account))
                .map(json -> request("/v1alpha/accounts")
                        .post(RequestBody.create(json, JSON))
                        .build())
                .compose(rq -> request(rq, CreateAccountRequest.class))
                .mapEmpty();
    }

    @Override
    public ServiceResult<Void> deleteAccount(String participantId) {
        var rq = request("/v1alpha/accounts/" + participantId)
                .delete()
                .build();
        return execute(rq);
    }

    @Override
    public ServiceResult<Void> updateAccount(StsAccount updatedAccount) {
        return stringify(updatedAccount)
                .map(json -> request("/v1alpha/accounts")
                        .put(RequestBody.create(json, JSON))
                        .build())
                .compose(this::execute);
    }

    @Override
    public ServiceResult<StsAccount> findById(String id) {
        var rq = request("/v1alpha/accounts/%s".formatted(id))
                .get()
                .build();
        return request(rq, StsAccount.class);
    }

    private ServiceResult<Void> execute(Request rq) {
        return request(rq, Void.class).mapEmpty();
    }

    private <T> ServiceResult<T> request(Request rq, Class<T> clazz) {
        try (var response = edcHttpClient.execute(rq)) {
            if (response.isSuccessful()) {
                var body = response.body();
                if (body != null) {
                    var json = body.string();
                    return StringUtils.isNullOrEmpty(json) ?
                            success() :
                            success(objectMapper.readValue(json, clazz));
                }
                return success();
            }
            return failureFromResponse(response);
        } catch (IOException e) {
            monitor.severe("Error deleting account", e);
            return ServiceResult.unexpected(e.getMessage());
        }
    }

    private ServiceResult<String> stringify(Object updatedAccount) {
        try {
            return success(objectMapper.writeValueAsString(updatedAccount));
        } catch (JsonProcessingException e) {
            var msg = "Error while converting StsAccount to JSON";
            monitor.severe(msg, e);
            return ServiceResult.unexpected(msg); // todo: should this be badRequest?
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

    private record CreateAccountRequest(StsAccount account, @Nullable String clientSecret) {
        CreateAccountRequest(StsAccount account) {
            this(account, null);
        }
    }
}
