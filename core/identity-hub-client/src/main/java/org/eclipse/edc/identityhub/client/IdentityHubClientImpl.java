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

package org.eclipse.edc.identityhub.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.eclipse.edc.identityhub.client.spi.IdentityHubClient;
import org.eclipse.edc.identityhub.spi.credentials.model.CredentialEnvelope;
import org.eclipse.edc.identityhub.spi.credentials.transformer.CredentialEnvelopeTransformerRegistry;
import org.eclipse.edc.identityhub.spi.model.Descriptor;
import org.eclipse.edc.identityhub.spi.model.MessageRequestObject;
import org.eclipse.edc.identityhub.spi.model.MessageResponseObject;
import org.eclipse.edc.identityhub.spi.model.Record;
import org.eclipse.edc.identityhub.spi.model.RequestObject;
import org.eclipse.edc.identityhub.spi.model.ResponseObject;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.AbstractResult;
import org.eclipse.edc.spi.result.Result;

import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.eclipse.edc.identityhub.spi.model.WebNodeInterfaceMethod.COLLECTIONS_QUERY;
import static org.eclipse.edc.identityhub.spi.model.WebNodeInterfaceMethod.COLLECTIONS_WRITE;

public class IdentityHubClientImpl implements IdentityHubClient {
    public static final String DATA_FORMAT = "application/vc+jwt";
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Monitor monitor;

    private final CredentialEnvelopeTransformerRegistry transformerRegistry;

    public IdentityHubClientImpl(OkHttpClient httpClient, ObjectMapper objectMapper, Monitor monitor, CredentialEnvelopeTransformerRegistry transformerRegistry) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.monitor = monitor;
        this.transformerRegistry = transformerRegistry;
    }

    private static Descriptor.Builder defaultDescriptor(String method) {
        return Descriptor.Builder.newInstance()
                .method(method);
    }

    private static <T> StatusResult<T> identityHubCallError(Response response) throws IOException {
        return StatusResult.failure(ResponseStatus.FATAL_ERROR, format("IdentityHub error response code: %s, response headers: %s, response body: %s", response.code(), response.headers(), response.body().string()));
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
    public StatusResult<Collection<CredentialEnvelope>> getVerifiableCredentials(String hubBaseUrl) {
        var descriptor = defaultDescriptor(COLLECTIONS_QUERY.getName()).build();
        try (var response = httpClient.newCall(
                        new Request.Builder()
                                .url(hubBaseUrl)
                                .post(buildRequestBody(descriptor))
                                .build())
                .execute()) {

            if (response.code() != 200) {
                return identityHubCallError(response);
            }

            try (var body = response.body()) {
                var responseObject = objectMapper.readValue(body.string(), ResponseObject.class);
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


        } catch (IOException e) {
            return StatusResult.failure(ResponseStatus.FATAL_ERROR, e.getMessage());
        }


    }

    @Override
    public StatusResult<Void> addVerifiableCredential(String hubBaseUrl, CredentialEnvelope verifiableCredential) {

        var transformer = transformerRegistry.resolve(verifiableCredential.format());
        if (transformer == null) {
            return StatusResult.failure(ResponseStatus.FATAL_ERROR, format("Transformer not found for format %s", verifiableCredential.format()));
        }
        Result<byte[]> result = transformer.serialize(verifiableCredential);

        if (result.failed()) {
            return StatusResult.failure(ResponseStatus.FATAL_ERROR, result.getFailureDetail());
        }

        var descriptor = defaultDescriptor(COLLECTIONS_WRITE.getName())
                .recordId(UUID.randomUUID().toString())
                .dataFormat(DATA_FORMAT)
                .dateCreated(Instant.now().getEpochSecond()) // TODO: this should be passed from input
                .build();
        try (var response = httpClient.newCall(new Request.Builder()
                        .url(hubBaseUrl)
                        .post(buildRequestBody(descriptor, result.getContent()))
                        .build())
                .execute()) {
            if (response.code() != 200) {
                return identityHubCallError(response);
            }

            try (var body = response.body()) {
                var responseObject = objectMapper.readValue(body.string(), ResponseObject.class);

                // If the status of Response object is not success return error
                if (responseObject.getStatus() != null && !responseObject.getStatus().isSuccess()) {
                    return StatusResult.failure(ResponseStatus.FATAL_ERROR, responseObject.getStatus().getDetail());
                }

                // If the status of one of the replies is not success return error
                return responseObject.getReplies()
                        .stream()
                        .map(MessageResponseObject::getStatus)
                        .filter(status -> !status.isSuccess())
                        .map(status -> StatusResult.<Void>failure(ResponseStatus.FATAL_ERROR, status.getDetail()))
                        .findFirst()
                        .orElseGet(() -> StatusResult.success());

            }


        } catch (IOException e) {
            return StatusResult.failure(ResponseStatus.FATAL_ERROR, e.getMessage());
        }
    }

    private Result<CredentialEnvelope> parse(Object entry) {
        var record = objectMapper.convertValue(entry, Record.class);
        var t = transformerRegistry.resolve(record.getDataFormat());

        if (t == null) {
            return Result.failure(format("Transformer not found for format %s", record.getDataFormat()));
        }
        return t.parse(record.getData());
    }

    private RequestBody buildRequestBody(Descriptor descriptor) {
        try {
            return buildRequestBody(descriptor, null);
        } catch (JsonProcessingException e) {
            throw new EdcException(e); // Should never happen.
        }
    }

    private RequestBody buildRequestBody(Descriptor descriptor, byte[] data) throws JsonProcessingException {
        var requestObject = RequestObject.Builder.newInstance()
                .messages(List.of(MessageRequestObject.Builder.newInstance()
                        .descriptor(descriptor)
                        .data(data)
                        .build())
                )
                .build();
        var payload = objectMapper.writeValueAsString(requestObject);
        return RequestBody.create(payload, okhttp3.MediaType.get("application/json"));
    }
}
