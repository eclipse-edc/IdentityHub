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
 *       Amadeus - add client for getting Self-Description
 *
 */

package org.eclipse.dataspaceconnector.identityhub.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.eclipse.dataspaceconnector.identityhub.model.Descriptor;
import org.eclipse.dataspaceconnector.identityhub.model.MessageRequestObject;
import org.eclipse.dataspaceconnector.identityhub.model.RequestObject;
import org.eclipse.dataspaceconnector.identityhub.model.ResponseObject;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.response.StatusResult;
import org.eclipse.dataspaceconnector.spi.result.AbstractResult;
import org.eclipse.dataspaceconnector.spi.result.Result;

import java.io.IOException;
import java.text.ParseException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.eclipse.dataspaceconnector.identityhub.model.WebNodeInterfaceMethod.COLLECTIONS_QUERY;
import static org.eclipse.dataspaceconnector.identityhub.model.WebNodeInterfaceMethod.COLLECTIONS_WRITE;

public class IdentityHubClientImpl implements IdentityHubClient {
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Monitor monitor;

    public IdentityHubClientImpl(OkHttpClient httpClient, ObjectMapper objectMapper, Monitor monitor) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.monitor = monitor;
    }

    @Override
    public StatusResult<JsonNode> getSelfDescription(String hubBaseUrl) {
        try (var response = httpClient.newCall(
                        new Request.Builder()
                                .url(hubBaseUrl + "/self-description")
                                .get()
                                .build())
                .execute()) {

            return (response.code() == 200) ?
                    StatusResult.success(objectMapper.readTree(response.body().byteStream())) :
                    identityHubCallError(response);
        } catch (IOException e) {
            return StatusResult.failure(ResponseStatus.FATAL_ERROR, e.getMessage());
        }
    }

    @Override
    public StatusResult<Collection<SignedJWT>> getVerifiableCredentials(String hubBaseUrl) {
        ResponseObject responseObject;
        try (var response = httpClient.newCall(
                        new Request.Builder()
                                .url(hubBaseUrl)
                                .post(buildRequestBody(COLLECTIONS_QUERY.getName()))
                                .build())
                .execute()) {

            if (response.code() != 200) {
                return identityHubCallError(response);
            }

            responseObject = objectMapper.readValue(response.body().byteStream(), ResponseObject.class);
        } catch (IOException e) {
            return StatusResult.failure(ResponseStatus.FATAL_ERROR, e.getMessage());
        }

        var verifiableCredentials = responseObject
                .getReplies()
                .stream()
                .flatMap(r -> r.getEntries().stream())
                .map(this::parse)
                .filter(AbstractResult::succeeded)
                .map(AbstractResult::getContent)
                .collect(Collectors.toList());

        return StatusResult.success(verifiableCredentials);
    }

    @Override
    public StatusResult<Void> addVerifiableCredential(String hubBaseUrl, SignedJWT verifiableCredential) {
        var payload = verifiableCredential.serialize().getBytes(UTF_8);
        try (var response = httpClient.newCall(new Request.Builder()
                        .url(hubBaseUrl)
                        .post(buildRequestBody(COLLECTIONS_WRITE.getName(), payload))
                        .build())
                .execute()) {
            if (response.code() != 200) {
                return StatusResult.failure(ResponseStatus.FATAL_ERROR, String.format("IdentityHub error response code: %s, response headers: %s, response body: %s", response.code(), response.headers(), response.body().string()));
            }
        } catch (IOException e) {
            return StatusResult.failure(ResponseStatus.FATAL_ERROR, e.getMessage());
        }
        return StatusResult.success();
    }

    private Result<SignedJWT> parse(Object entry) {
        try {
            var jwt = new String(objectMapper.convertValue(entry, byte[].class));
            return Result.success(SignedJWT.parse(jwt));
        } catch (ParseException e) {
            monitor.warning("Could not parse JWT", e);
            return Result.failure(e.getMessage());
        }
    }

    private RequestBody buildRequestBody(String method) {
        try {
            return buildRequestBody(method, null);
        } catch (JsonProcessingException e) {
            throw new EdcException(e); // Should never happen.
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

    private static <T> StatusResult<T> identityHubCallError(Response response) throws IOException {
        return StatusResult.failure(ResponseStatus.FATAL_ERROR, String.format("IdentityHub error response code: %s, response headers: %s, response body: %s", response.code(), response.headers(), response.body().string()));
    }
}
