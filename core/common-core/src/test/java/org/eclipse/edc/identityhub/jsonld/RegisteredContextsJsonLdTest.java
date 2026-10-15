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

package org.eclipse.edc.identityhub.jsonld;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.iam.decentralizedclaims.spi.DcpConstants.DSPACE_DCP_V_1_0_CONTEXT;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegisteredContextsJsonLdTest {

    private static final String DCP_NAMESPACE = "https://w3id.org/dspace-dcp/v1.0/";

    private final JsonLd delegate = mock();
    private final RegisteredContextsJsonLd jsonLd = new RegisteredContextsJsonLd(delegate);

    @BeforeEach
    void setUp() throws URISyntaxException {
        jsonLd.registerCachedDocument(DSPACE_DCP_V_1_0_CONTEXT, getClass().getClassLoader().getResource("dcp.v1.0.jsonld").toURI());
    }

    @Test
    void expand_withRegisteredContext() {
        var result = jsonLd.expand(presentationQuery(Json.createValue(DSPACE_DCP_V_1_0_CONTEXT)));

        assertThat(result).isSucceeded().satisfies(expanded -> {
            assertThat(expanded.getJsonArray("@type").getString(0)).isEqualTo(DCP_NAMESPACE + "PresentationQueryMessage");
            assertThat(expanded.getJsonArray(DCP_NAMESPACE + "scope")).hasSize(1);
        });
    }

    @Test
    void expand_withUnregisteredContext() {
        var result = jsonLd.expand(presentationQuery(createArrayBuilder()
                .add(DSPACE_DCP_V_1_0_CONTEXT)
                .add("https://example.com/context.jsonld")
                .build()));

        assertThat(result).isFailed().detail().contains("https://example.com/context.jsonld");
    }

    @Test
    void expand_withJarContext_shouldNotConnect() throws IOException {
        try (var server = new ServerSocket(0)) {
            var context = "jar:http://127.0.0.1:%d/context.jar!/context.json".formatted(server.getLocalPort());

            var result = jsonLd.expand(presentationQuery(Json.createValue(context)));

            assertThat(result).isFailed();
            server.setSoTimeout(500);
            assertThatThrownBy(server::accept).isInstanceOf(SocketTimeoutException.class);
        }
    }

    @Test
    void expand_withFileContext(@TempDir Path tempDir) throws IOException {
        var file = tempDir.resolve("context.jsonld");
        Files.writeString(file, "{\"@context\": {\"scope\": \"https://example.com/scope\"}}");

        var result = jsonLd.expand(presentationQuery(Json.createValue(file.toUri().toString())));

        assertThat(result).isFailed();
    }

    @Test
    void expand_withUnregisteredImport() {
        var context = createArrayBuilder()
                .add(DSPACE_DCP_V_1_0_CONTEXT)
                .add(createObjectBuilder().add("@import", "https://example.com/context.jsonld"))
                .build();

        assertThat(jsonLd.expand(presentationQuery(context))).isFailed();
    }

    @Test
    void expand_withUnregisteredContextInNestedNode() {
        var query = createObjectBuilder(presentationQuery(Json.createValue(DSPACE_DCP_V_1_0_CONTEXT)))
                .add("https://example.com/nested", createObjectBuilder()
                        .add("@context", "https://example.com/context.jsonld")
                        .add("@id", "https://example.com/node"))
                .build();

        assertThat(jsonLd.expand(query)).isFailed();
    }

    @Test
    void expand_withUnregisteredScopedContext() {
        var context = createArrayBuilder()
                .add(DSPACE_DCP_V_1_0_CONTEXT)
                .add(createObjectBuilder().add("https://example.com/term", createObjectBuilder()
                        .add("@id", "https://example.com/term")
                        .add("@context", "https://example.com/context.jsonld")))
                .build();
        var query = createObjectBuilder(presentationQuery(context))
                .add("https://example.com/term", createObjectBuilder().add("@id", "https://example.com/node"))
                .build();

        assertThat(jsonLd.expand(query)).isFailed();
    }

    @Test
    void expand_withContextRegisteredAsObject() {
        var context = createObjectBuilder()
                .add("@context", createObjectBuilder().add("foo", "https://example.com/foo"))
                .build();
        jsonLd.registerCachedDocument("https://example.com/context.jsonld", context);

        var result = jsonLd.expand(createObjectBuilder()
                .add("@context", "https://example.com/context.jsonld")
                .add("foo", "bar")
                .build());

        assertThat(result).isSucceeded().satisfies(expanded -> assertThat(expanded).containsKey("https://example.com/foo"));
        verify(delegate).registerCachedDocument("https://example.com/context.jsonld", context);
    }

    @Test
    void expand_afterUnregistering() {
        jsonLd.unregisterCachedDocument(DSPACE_DCP_V_1_0_CONTEXT);

        assertThat(jsonLd.expand(presentationQuery(Json.createValue(DSPACE_DCP_V_1_0_CONTEXT)))).isFailed();
        verify(delegate).unregisterCachedDocument(DSPACE_DCP_V_1_0_CONTEXT);
    }

    @Test
    void registerCachedDocument_shouldRegisterWithDelegate() {
        verify(delegate).registerCachedDocument(eq(DSPACE_DCP_V_1_0_CONTEXT), any(URI.class));
    }

    @Test
    void compact_shouldDelegate() {
        var json = createObjectBuilder().build();
        var compacted = createObjectBuilder().add("foo", "bar").build();
        when(delegate.compact(json, "scope", List.of())).thenReturn(Result.success(compacted));

        assertThat(jsonLd.compact(json, "scope", List.of())).isSucceeded().isEqualTo(compacted);
    }

    private JsonObject presentationQuery(JsonValue context) {
        return createObjectBuilder()
                .add("@context", context)
                .add("type", "PresentationQueryMessage")
                .add("scope", createArrayBuilder().add("org.eclipse.edc.vc.type:SomeCredential:read"))
                .build();
    }
}
