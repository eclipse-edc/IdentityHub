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

import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.JsonLdErrorCode;
import com.apicatalog.jsonld.JsonLdOptions;
import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.document.JsonDocument;
import com.apicatalog.jsonld.loader.DocumentLoader;
import com.apicatalog.jsonld.loader.DocumentLoaderOptions;
import jakarta.json.JsonObject;
import org.eclipse.edc.identityhub.protocols.dcp.spi.DcpJsonLd;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.Result;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link DcpJsonLd} that expands JSON-LD with a document loader serving only the documents registered with it. Any other
 * context is rejected before an attempt is made to load it, whatever its scheme is. Compaction and the registration of
 * namespaces and contexts are delegated to the runtime's {@link JsonLd} service, with which the cached documents are
 * registered too.
 */
public class RegisteredContextsJsonLd implements DcpJsonLd {

    private final JsonLd delegate;
    private final Map<String, Document> documents = new ConcurrentHashMap<>();
    private final DocumentLoader documentLoader = this::loadRegisteredDocument;

    public RegisteredContextsJsonLd(JsonLd delegate) {
        this.delegate = delegate;
    }

    @Override
    public Result<JsonObject> expand(JsonObject json) {
        try {
            var expanded = com.apicatalog.jsonld.JsonLd.expand(JsonDocument.of(json))
                    .options(new JsonLdOptions(documentLoader))
                    .get();
            if (expanded.isEmpty()) {
                return Result.failure("Error expanding JSON-LD structure: result was empty, it could be caused by missing '@context'");
            }
            return Result.success(expanded.getJsonObject(0));
        } catch (JsonLdError error) {
            return Result.failure("Error expanding JSON-LD structure: %s".formatted(error.getMessage()));
        }
    }

    @Override
    public Result<JsonObject> compact(JsonObject json, String scope) {
        return delegate.compact(json, scope);
    }

    @Override
    public Result<JsonObject> compact(JsonObject json, String scope, List<Object> additionalContexts) {
        return delegate.compact(json, scope, additionalContexts);
    }

    @Override
    public void registerNamespace(String prefix, String contextIri, String scope) {
        delegate.registerNamespace(prefix, contextIri, scope);
    }

    @Override
    public void registerContext(String contextIri, String scope) {
        delegate.registerContext(contextIri, scope);
    }

    /**
     * Registers the document found at the given URI, which is read right away, as the context identified by the URL.
     * The URI is expected to point to a resource of the runtime, e.g. a file on the classpath.
     */
    @Override
    public void registerCachedDocument(String url, URI uri) {
        try (var stream = uri.toURL().openStream()) {
            register(url, JsonDocument.of(stream));
        } catch (IOException | JsonLdError e) {
            throw new EdcException("Error reading JSON-LD context '%s' from '%s'".formatted(url, uri), e);
        }
        delegate.registerCachedDocument(url, uri);
    }

    @Override
    public void registerCachedDocument(String url, JsonObject document) {
        register(url, JsonDocument.of(document));
        delegate.registerCachedDocument(url, document);
    }

    @Override
    public void unregisterCachedDocument(String url) {
        documents.remove(url);
        delegate.unregisterCachedDocument(url);
    }

    private void register(String url, Document document) {
        document.setDocumentUrl(URI.create(url));
        documents.put(url, document);
    }

    private Document loadRegisteredDocument(URI url, DocumentLoaderOptions options) throws JsonLdError {
        var document = documents.get(url.toString());
        if (document == null) {
            throw new JsonLdError(JsonLdErrorCode.LOADING_REMOTE_CONTEXT_FAILED, "JSON-LD context '%s' is not supported".formatted(url));
        }
        return document;
    }
}
