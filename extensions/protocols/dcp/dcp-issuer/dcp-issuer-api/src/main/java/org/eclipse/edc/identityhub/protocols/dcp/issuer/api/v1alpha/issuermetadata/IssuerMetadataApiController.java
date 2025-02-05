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

package org.eclipse.edc.identityhub.protocols.dcp.issuer.api.v1alpha.issuermetadata;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v1alpha/.well-known/vci")
public class IssuerMetadataApiController implements IssuerMetadataApi {

    private final TypeTransformerRegistry dcpRegistry;

    public IssuerMetadataApiController(TypeTransformerRegistry dcpRegistry) {
        this.dcpRegistry = dcpRegistry;
    }

    @GET
    @Path("/")
    @Override
    public Response getIssuerMetadata() {
        return Response.ok().build();
    }
}
