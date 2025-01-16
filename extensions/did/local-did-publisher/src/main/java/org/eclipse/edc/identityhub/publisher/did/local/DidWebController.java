/*
 *  Copyright (c) 2023 Metaform Systems, Inc.
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

package org.eclipse.edc.identityhub.publisher.did.local;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.identityhub.spi.did.DidWebParser;
import org.eclipse.edc.identityhub.spi.did.model.DidResource;
import org.eclipse.edc.identityhub.spi.did.model.DidState;
import org.eclipse.edc.identityhub.spi.did.store.DidResourceStore;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;

import java.nio.charset.Charset;
import java.util.Optional;
import java.util.regex.Pattern;

import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("{any:.*}")
public class DidWebController {
    private static final Charset DEFAULT_CHARSET = Charset.defaultCharset();
    private static final Pattern CHARSET_REGEX_PATTERN = Pattern.compile("(?i)\\bcharset=\\s*\"?([^\\s;\"]*)");
    private final Monitor monitor;
    private final DidResourceStore didResourceStore;
    private final DidWebParser didWebParser;

    public DidWebController(Monitor monitor, DidResourceStore didResourceStore, DidWebParser didWebParser) {
        this.monitor = monitor;
        this.didResourceStore = didResourceStore;
        this.didWebParser = didWebParser;
    }

    @GET
    public DidDocument getDidDocument(@Context ContainerRequestContext context) {

        var httpUrl = context.getUriInfo().getAbsolutePath();

        var charset = extractCharset(context.getHeaderString(CONTENT_TYPE));
        String did;
        did = didWebParser.parse(httpUrl, charset);


        var q = QuerySpec.Builder.newInstance()
                .filter(new Criterion("state", "=", DidState.PUBLISHED.code()))
                .filter(new Criterion("did", "=", did))
                .build();

        var dids = didResourceStore.query(q)
                .stream()
                .map(DidResource::getDocument)
                .toList();

        if (dids.size() > 1) {
            throw new InvalidRequestException("DID '%s' resolved more than one document".formatted(did));
        }

        return dids.stream().findFirst().orElse(null);
    }

    private Charset extractCharset(String contentType) {
        return Optional.ofNullable(contentType)
                .map(this::parseCharsetFromContentType)
                .orElse(DEFAULT_CHARSET);
    }

    /**
     * Parses the "charset" attribute from the Content-Type header. Returns the value of the charset attribute,
     * or {@link Charset#defaultCharset()} if the attribute was not present or represents an invalid charset
     *
     * @param contentType The Content-Type header
     * @return The value of the charset attribute or the default charset
     */
    private Charset parseCharsetFromContentType(String contentType) {
        var m = CHARSET_REGEX_PATTERN.matcher(contentType);
        if (m.find()) {
            var cs = m.group(1).trim().toUpperCase();
            if (Charset.isSupported(cs)) {
                return Charset.forName(cs);
            } else {
                monitor.warning("Charset '%s' is not supported, defaulting to %s".formatted(cs, DEFAULT_CHARSET));
                return DEFAULT_CHARSET;
            }
        }
        return DEFAULT_CHARSET;
    }
}
