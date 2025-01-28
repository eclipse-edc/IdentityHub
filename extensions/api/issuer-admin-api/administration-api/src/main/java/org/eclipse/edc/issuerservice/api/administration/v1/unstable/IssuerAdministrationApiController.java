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

package org.eclipse.edc.issuerservice.api.administration.v1.unstable;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.identityhub.api.Versions;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairResource;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.issuerservice.api.administration.v1.unstable.model.RotationRequest;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(Versions.UNSTABLE + "/admin")
public class IssuerAdministrationApiController implements IssuerAdministrationApi {


    @POST
    @Path("/keypairs/{keyPairId}/revoke")
    @Override
    public Response revokeKeyPair(@PathParam("keyPairId") String keyPairId, KeyDescriptor newKey) {
        return Response.noContent().build();
    }

    @POST
    @Path("/keypairs/{keyPairId}/rotate")
    @Override
    public String initiateKeyRotation(@PathParam("keyPairId") String keyId, RotationRequest request) {
        return "";
    }

    @POST
    @Path("/keypairs")
    @Override
    public Response addKeyPair(KeyDescriptor request) {
        return null;
    }

    @GET
    @Path("/keypairs/{keyPairId}")
    @Override
    public KeyPairResource getKeyPair(@PathParam("keyPairId") String keyPairId) {
        return null;
    }
}
