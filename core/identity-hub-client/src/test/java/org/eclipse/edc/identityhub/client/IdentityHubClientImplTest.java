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

package org.eclipse.edc.identityhub.client;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.edc.identityhub.credentials.jwt.JwtCredentialConstants;
import org.eclipse.edc.identityhub.credentials.jwt.JwtCredentialEnvelope;
import org.eclipse.edc.identityhub.credentials.jwt.JwtCredentialEnvelopeTransformer;
import org.eclipse.edc.identityhub.junit.testfixtures.VerifiableCredentialTestUtil;
import org.eclipse.edc.identityhub.spi.credentials.transformer.CredentialEnvelopeTransformerRegistry;
import org.eclipse.edc.identityhub.spi.model.MessageResponseObject;
import org.eclipse.edc.identityhub.spi.model.MessageStatus;
import org.eclipse.edc.identityhub.spi.model.Record;
import org.eclipse.edc.identityhub.spi.model.RequestStatus;
import org.eclipse.edc.identityhub.spi.model.ResponseObject;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.buildSignedJwt;
import static org.eclipse.edc.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.generateEcKey;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IdentityHubClientImplTest {
    private static final String HUB_URL = "http://some.test.url";
    private static final TypeManager TYPE_MANAGER = new TypeManager();

    private final CredentialEnvelopeTransformerRegistry registry = mock(CredentialEnvelopeTransformerRegistry.class);

    @BeforeEach
    void setup() {
        when(registry.resolve(any())).thenReturn(new JwtCredentialEnvelopeTransformer(TYPE_MANAGER.getMapper()));
    }

    @Test
    void getVerifiableCredentials() {
        var credential = VerifiableCredentialTestUtil.generateCredential();
        var jws = buildSignedJwt(credential, "http://some.test.url", "http://some.test.url", generateEcKey());

        var record = Record.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .dataFormat(JwtCredentialConstants.DATA_FORMAT)
                .createdAt(System.currentTimeMillis())
                .data(jws.serialize().getBytes(StandardCharsets.UTF_8))
                .build();
        Interceptor interceptor = chain -> {
            var request = chain.request();
            var replies = MessageResponseObject.Builder.newInstance()
                    .status(MessageStatus.OK)
                    .entries(List.of(record))
                    .build();
            var responseObject = ResponseObject.Builder.newInstance()
                    .status(RequestStatus.OK)
                    .replies(List.of(replies))
                    .build();
            var body = ResponseBody.create(TYPE_MANAGER.writeValueAsString(responseObject), MediaType.get("application/json"));

            return new Response.Builder()
                    .body(body)
                    .request(request)
                    .protocol(Protocol.HTTP_2)
                    .code(200)
                    .message("")
                    .build();
        };

        var client = createClient(interceptor);
        var statusResult = client.getVerifiableCredentials(HUB_URL);
        assertThat(statusResult.succeeded()).isTrue();
        assertThat(statusResult.getContent()).usingRecursiveFieldByFieldElementComparator().containsExactly(new JwtCredentialEnvelope(jws));
    }

    @Test
    void getVerifiableCredentialsServerError() {

        var errorMessage = "test-message";
        var body = "{\"foo\":\"bar\"}";
        var code = 500;

        Interceptor interceptor = chain -> {
            var request = chain.request();
            return new Response.Builder()
                    .body(ResponseBody.create(body, MediaType.get("application/json")))
                    .request(request)
                    .protocol(Protocol.HTTP_2)
                    .code(code)
                    .message(errorMessage)
                    .build();
        };

        var client = createClient(interceptor);
        var result = client.getVerifiableCredentials(HUB_URL);
        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureDetail()).contains(body);
    }

    @Test
    void getVerifiableCredentialsDeserializationError() {
        Interceptor interceptor = chain -> {
            var request = chain.request();
            var body = ResponseBody.create("{\"replies\": [{}]}", MediaType.get("application/json"));

            return new Response.Builder()
                    .body(body)
                    .request(request)
                    .protocol(Protocol.HTTP_2)
                    .code(200)
                    .message("{}")
                    .build();
        };

        var client = createClient(interceptor);
        var result = client.getVerifiableCredentials(HUB_URL);
        assertThat(result.failed()).isTrue();
    }

    @Test
    void addVerifiableCredentialsServerError() {
        var credential = VerifiableCredentialTestUtil.generateCredential();
        var jws = buildSignedJwt(credential, "http://some.test.url", "http://some.test.url", generateEcKey());
        var errorMessage = "test error message";
        var body = "{}";
        int code = 500;

        Interceptor interceptor = chain -> {
            var request = chain.request();
            return new Response.Builder()
                    .body(ResponseBody.create(body, MediaType.get("application/json")))
                    .request(request)
                    .protocol(Protocol.HTTP_2)
                    .code(code)
                    .message(errorMessage)
                    .build();
        };

        var client = createClient(interceptor);
        var result = client.addVerifiableCredential(HUB_URL, new JwtCredentialEnvelope(jws));
        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureDetail()).contains("500");
    }

    @Test
    void addVerifiableCredentialsIoException() {
        var credential = VerifiableCredentialTestUtil.generateCredential();
        var jws = buildSignedJwt(credential, "http://some.test.url", "http://some.test.url", generateEcKey());
        var exceptionMessage = "test exception message";
        Interceptor interceptor = chain -> {
            throw new IOException(exceptionMessage);
        };

        var client = createClient(interceptor);
        var statusResult = client.addVerifiableCredential(HUB_URL, new JwtCredentialEnvelope(jws));

        var expectedResult = StatusResult.failure(ResponseStatus.FATAL_ERROR, exceptionMessage);
        assertThat(statusResult).usingRecursiveComparison().isEqualTo(expectedResult);
    }

    private IdentityHubClientImpl createClient(Interceptor interceptor) {
        return new IdentityHubClientImpl(TestUtils.testHttpClient(interceptor), TYPE_MANAGER, registry);
    }
}
