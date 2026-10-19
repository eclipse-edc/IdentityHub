/*
 *  Copyright (c) 2026 Metaform Systems Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.web;

import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;

@ApiTest
class RequestBodySizeLimitFilterApiTest extends RestControllerTestBase {

    private static final int LIMIT = 1024;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @ParameterizedTest
    @ValueSource(strings = { "/json", "/string" })
    void bodyWithinLimit_shouldBeAccepted(String path) throws Exception {
        var response = post(path, jsonOfSize(LIMIT), false);

        assertThat(response.statusCode()).isEqualTo(204);
    }

    @ParameterizedTest
    @ValueSource(strings = { "/json", "/string" })
    void bodyAboveLimit_withContentLength_shouldReturn413(String path) throws Exception {
        var response = post(path, jsonOfSize(LIMIT * 10), false);

        assertThat(response.statusCode()).isEqualTo(413);
        assertThat(response.body()).contains("exceeds the maximum size");
    }

    @ParameterizedTest
    @ValueSource(strings = { "/json", "/string" })
    void bodyAboveLimit_chunked_shouldReturn413(String path) throws Exception {
        var response = post(path, jsonOfSize(LIMIT * 10), true);

        assertThat(response.statusCode()).isEqualTo(413);
        assertThat(response.body()).contains("exceeds the maximum size");
    }

    @Override
    protected Object controller() {
        return new TestController();
    }

    @Override
    protected Object additionalResource() {
        return new RequestBodySizeLimitFilter(LIMIT);
    }

    private HttpResponse<String> post(String path, byte[] body, boolean chunked) throws Exception {
        var publisher = chunked
                ? HttpRequest.BodyPublishers.ofInputStream(() -> new ByteArrayInputStream(body))
                : HttpRequest.BodyPublishers.ofByteArray(body);
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", APPLICATION_JSON)
                .POST(publisher)
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private byte[] jsonOfSize(int size) {
        var prefix = "{\"key\":\"";
        var suffix = "\"}";
        return (prefix + "x".repeat(size - prefix.length() - suffix.length()) + suffix).getBytes(StandardCharsets.UTF_8);
    }

    @Path("/")
    @Consumes(APPLICATION_JSON)
    public static class TestController {

        @POST
        @Path("json")
        public Response json(JsonObject body) {
            return Response.noContent().build();
        }

        @POST
        @Path("string")
        public Response string(String body) {
            return Response.noContent().build();
        }
    }
}
