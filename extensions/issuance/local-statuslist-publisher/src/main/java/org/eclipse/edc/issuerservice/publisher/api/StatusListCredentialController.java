/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.issuerservice.publisher.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ObjectConflictException;

import java.util.List;
import java.util.function.Supplier;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("{any:.*}")
public class StatusListCredentialController {
    private static final List<String> SUPPORTED_TYPES = List.of(APPLICATION_JSON, "application/vc+jwt", MediaType.WILDCARD);
    private final CredentialStore store;
    private final Monitor monitor;
    private final Supplier<ObjectMapper> mapper;

    public StatusListCredentialController(CredentialStore store, Monitor monitor, Supplier<ObjectMapper> mapper) {
        this.store = store;
        this.monitor = monitor;
        this.mapper = mapper;
    }

    @GET
    public Response resolveStatusListCredential(@HeaderParam("Accept") @DefaultValue(MediaType.WILDCARD) String acceptHeader,
                                                @Context ContainerRequestContext context) {

        var httpUrl = context.getUriInfo().getAbsolutePath();
        if (!SUPPORTED_TYPES.contains(acceptHeader)) {
            return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE)
                    .entity("Supported media types are: %s".formatted(SUPPORTED_TYPES))
                    .build();
        }

        var split = httpUrl.getPath().split("/");
        var credentialId = split[split.length - 1];

        var query = QuerySpec.Builder.newInstance()
                .filter(new Criterion("verifiableCredential.credential.id", "=", credentialId))
                .filter(new Criterion("metadata.published", "=", true))
                .build();
        var statusListCredential = store.query(query)
                .orElseThrow(InvalidRequestException::new);

        if (statusListCredential.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        if (statusListCredential.size() > 1) {
            throw new ObjectConflictException("Multiple active status list credentials found for the same ID");
        }

        var selectedCredential = statusListCredential.iterator().next();

        var publicUrl = selectedCredential.getMetadata().get("publicUrl");
        if (!httpUrl.toString().equals(publicUrl)) {
            monitor.warning("The status list credential's public URL property is not equal to the request URL: '%s' <> '%s'"
                    .formatted(publicUrl, httpUrl));
        }
        var body = selectedCredential.getVerifiableCredential().rawVc();

        var contentType = "application/vc+jwt";
        if (acceptHeader.equals(APPLICATION_JSON)) {
            try {
                body = mapper.get().writeValueAsString(selectedCredential.getVerifiableCredential().credential());
                contentType = APPLICATION_JSON;
            } catch (JsonProcessingException e) {
                throw new EdcException(e);
            }
        }

        return Response.ok()
                .header("Content-Type", contentType)
                .entity(body).build();
    }
}
