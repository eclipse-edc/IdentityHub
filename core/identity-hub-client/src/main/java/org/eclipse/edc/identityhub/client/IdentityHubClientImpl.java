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
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.result.AbstractResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.TypeManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.eclipse.edc.identityhub.spi.model.WebNodeInterfaceMethod.COLLECTIONS_QUERY;
import static org.eclipse.edc.identityhub.spi.model.WebNodeInterfaceMethod.COLLECTIONS_WRITE;
import static org.eclipse.edc.spi.http.FallbackFactories.statusMustBe;
import static org.eclipse.edc.spi.result.Result.failure;

public class IdentityHubClientImpl implements IdentityHubClient {

    private final EdcHttpClient httpClient;
    private final TypeManager typeManager;

    private final CredentialEnvelopeTransformerRegistry transformerRegistry;

    public IdentityHubClientImpl(EdcHttpClient httpClient, TypeManager typeManager, CredentialEnvelopeTransformerRegistry transformerRegistry) {
        this.httpClient = httpClient;
        this.typeManager = typeManager;
        this.transformerRegistry = transformerRegistry;
    }

    @Override
    public Result<Collection<CredentialEnvelope>> getVerifiableCredentials(String hubBaseUrl) {
        var descriptor = Descriptor.Builder.newInstance()
                .method(COLLECTIONS_QUERY.getName())
                .build();
        var body = toRequestBody(descriptor);
        var request = new Request.Builder()
                .url(hubBaseUrl)
                .post(body)
                .build();

        return httpClient.execute(request, List.of(statusMustBe(200)), this::extractCredentials);
    }
    
    @Override
    public Result<Void> addVerifiableCredential(String hubBaseUrl, CredentialEnvelope verifiableCredential) {
        var transformer = transformerRegistry.resolve(verifiableCredential.format());
        if (transformer == null) {
            return failure(format("Transformer not found for format %s", verifiableCredential.format()));
        }
        Result<byte[]> result = transformer.serialize(verifiableCredential);
        if (result.failed()) {
            return failure(result.getFailureDetail());
        }

        var descriptor = Descriptor.Builder.newInstance()
                .method(COLLECTIONS_WRITE.getName())
                .recordId(UUID.randomUUID().toString())
                .dataFormat(verifiableCredential.format())
                .dateCreated(Instant.now().getEpochSecond()) // TODO: this should be passed from input
                .build();

        var request = new Request.Builder()
                .url(hubBaseUrl)
                .post(toRequestBody(descriptor, result.getContent()))
                .build();

        return httpClient.execute(request, List.of(statusMustBe(200)), this::handleAddResponse);
    }

    private Result<CredentialEnvelope> parse(Object entry) {
        var record = typeManager.getMapper().convertValue(entry, Record.class);
        var t = transformerRegistry.resolve(record.getDataFormat());

        if (t == null) {
            return failure(format("Transformer not found for format %s", record.getDataFormat()));
        }
        return t.parse(record.getData());
    }

    private Result<Void> handleAddResponse(Response response) {
        var result = extractResponseObject(response);
        if (result.failed()) {
            return Result.failure(result.getFailureMessages());
        }
        var responseObject = result.getContent();
        // If the status of Response object is not success return error
        if (responseObject.getStatus() != null && !responseObject.getStatus().isSuccess()) {
            return Result.<Void>failure(responseObject.getStatus().getDetail());
        }

        // If the status of one of the replies is not success return error
        return responseObject.getReplies()
                .stream()
                .map(MessageResponseObject::getStatus)
                .filter(status -> !status.isSuccess())
                .map(status -> Result.<Void>failure(status.getDetail()))
                .findFirst()
                .orElseGet(Result::success);
    }

    private Result<Collection<CredentialEnvelope>> extractCredentials(Response response) {
        return extractResponseObject(response)
                .map(ResponseObject::getReplies)
                .map(replies -> replies.stream()
                        .flatMap(r -> r.getEntries().stream())
                        .map(this::parse)
                        .filter(AbstractResult::succeeded)
                        .map(AbstractResult::getContent)
                        .collect(Collectors.toList()));
    }

    private RequestBody toRequestBody(Descriptor descriptor) {
        return toRequestBody(descriptor, null);
    }

    private RequestBody toRequestBody(Descriptor descriptor, byte[] data) {
        var requestObject = RequestObject.Builder.newInstance()
                .messages(List.of(MessageRequestObject.Builder.newInstance()
                        .descriptor(descriptor)
                        .data(data)
                        .build())
                )
                .build();
        var payload = typeManager.writeValueAsString(requestObject);
        return RequestBody.create(payload, okhttp3.MediaType.get("application/json"));
    }

    @NotNull
    private Result<ResponseObject> extractResponseObject(Response response) {
        try (var body = response.body()) {
            if (body != null) {
                return Result.success(typeManager.readValue(body.string(), ResponseObject.class));
            } else {
                return failure("Body is null");
            }
        } catch (IOException e) {
            return failure("Cannot read response body as String: " + e.getMessage());
        }

    }
}
