/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.identityhub.client;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.eclipse.dataspaceconnector.identityhub.dtos.Descriptor;
import org.eclipse.dataspaceconnector.identityhub.dtos.MessageRequestObject;
import org.eclipse.dataspaceconnector.identityhub.dtos.RequestObject;
import org.eclipse.dataspaceconnector.identityhub.dtos.ResponseObject;
import org.eclipse.dataspaceconnector.identityhub.dtos.credentials.VerifiableCredential;
import org.eclipse.dataspaceconnector.spi.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.response.StatusResult;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.eclipse.dataspaceconnector.identityhub.dtos.WebNodeInterfaceMethod.COLLECTIONS_QUERY;
import static org.eclipse.dataspaceconnector.identityhub.dtos.WebNodeInterfaceMethod.COLLECTIONS_WRITE;

public class IdentityHubClientImpl implements IdentityHubClient {

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public IdentityHubClientImpl(OkHttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public StatusResult<Collection<VerifiableCredential>> getVerifiableCredentials(String hubBaseUrl) throws IOException {
        ResponseObject responseObject;
        try (var response = httpClient.newCall(
                        new Request.Builder()
                                .url(hubBaseUrl)
                                .post(buildRequestBody(COLLECTIONS_QUERY.getName()))
                                .build())
                .execute()) {

            if (response.code() != 200) {
                return StatusResult.failure(ResponseStatus.FATAL_ERROR, String.format("IdentityHub error response code: %s, response headers: %s, response body: %s", response.code(), response.headers(), response.body()));
            }

            responseObject = objectMapper.readValue(response.body().byteStream(), ResponseObject.class);
        }

        var verifiableCredentials = responseObject.getReplies()
                .stream().flatMap(x -> x.getEntries().stream())
                .map(e -> objectMapper.convertValue(e, VerifiableCredential.class))
                .collect(Collectors.toList());

        return StatusResult.success(verifiableCredentials);
    }

    @Override
    public void addVerifiableCredential(String hubBaseUrl, VerifiableCredential verifiableCredential) throws ApiException, IOException {
        var payload = objectMapper.writeValueAsString(verifiableCredential);
        try (var response = httpClient.newCall(new Request.Builder()
                        .url(hubBaseUrl)
                        .post(buildRequestBody(COLLECTIONS_WRITE.getName(), payload.getBytes(UTF_8)))
                        .build())
                .execute()) {
            if (response.code() != 200) {
                throw new ApiException("IdentityHub error", response.code(), response.headers(), response.body());
            }
        }
    }

    private RequestBody buildRequestBody(String method) {
        try {
            return buildRequestBody(method, null);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e); // Should never happen.
        }
    }

    private RequestBody buildRequestBody(String method, byte[] data) throws JsonProcessingException {
        var requestId = UUID.randomUUID().toString();
        var nonce = UUID.randomUUID().toString();
        var requestObject = RequestObject.Builder.newInstance()
                .requestId(requestId)
                .target("target")
                .messages(List.of(MessageRequestObject.Builder.newInstance()
                        .descriptor(Descriptor.Builder.newInstance()
                                .nonce(nonce)
                                .method(method)
                                .build())
                        .data(data)
                        .build())
                )
                .build();
        var payload = objectMapper.writeValueAsString(requestObject);
        return RequestBody.create(payload, okhttp3.MediaType.get("application/json"));
    }
}
