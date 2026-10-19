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

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.WithSpan;
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
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ObjectNotFoundException;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("{any:.*}")
public class DidWebController {
    private final DidResourceStore didResourceStore;
    private final DidWebParser didWebParser;

    public DidWebController(DidResourceStore didResourceStore, DidWebParser didWebParser) {
        this.didResourceStore = didResourceStore;
        this.didWebParser = didWebParser;
    }

    @WithSpan(value = "did-document.resolve", kind = SpanKind.SERVER)
    @GET
    public DidDocument getDidDocument(@Context ContainerRequestContext context) {

        var httpUrl = context.getUriInfo().getAbsolutePath();

        // did:web always uses UTF-8, see https://w3c-ccg.github.io/did-method-web/#read-resolve
        var did = didWebParser.parse(httpUrl, StandardCharsets.UTF_8);
        if (did == null) {
            throw notFound(httpUrl);
        }

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

        return dids.stream().findFirst().orElseThrow(() -> notFound(httpUrl));
    }

    private static ObjectNotFoundException notFound(URI url) {
        return new ObjectNotFoundException(DidDocument.class, url.getPath());
    }
}
