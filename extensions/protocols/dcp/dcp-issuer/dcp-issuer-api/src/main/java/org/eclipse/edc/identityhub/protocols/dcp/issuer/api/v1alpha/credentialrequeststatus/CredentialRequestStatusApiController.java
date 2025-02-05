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

package org.eclipse.edc.identityhub.protocols.dcp.issuer.api.v1alpha.credentialrequeststatus;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v1alpha/requests")
public class CredentialRequestStatusApiController implements CredentialRequestStatusApi {

    public CredentialRequestStatusApiController(TypeTransformerRegistry dcpRegistry) {
        
    }

    @GET
    @Path("/{credentialRequestId}")
    @Override
    public Response requestCredential(@PathParam("credentialRequestId") String credentialRequestId) {
        if (credentialRequestId == null || credentialRequestId.isEmpty()) {
            return Response.status(400).build();
        }
        return Response.ok()
                .build();
    }
}
