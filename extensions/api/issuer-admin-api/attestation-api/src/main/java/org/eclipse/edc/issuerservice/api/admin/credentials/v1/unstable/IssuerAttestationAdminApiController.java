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

package org.eclipse.edc.issuerservice.api.admin.credentials.v1.unstable;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.identityhub.api.Versions;
import org.eclipse.edc.issuerservice.api.admin.credentials.v1.unstable.model.AttestationRequest;
import org.eclipse.edc.issuerservice.api.admin.credentials.v1.unstable.model.AttestationResponse;
import org.eclipse.edc.spi.query.QuerySpec;

import java.net.URI;
import java.util.UUID;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(Versions.UNSTABLE)
public class IssuerAttestationAdminApiController implements IssuerAttestationAdminApi {


    @POST
    @Path("/participants/{participantId}/attestations/{attestationId}")
    @Override
    public Response linkAttestation(@PathParam("participantId") String participantId,
                                    @PathParam("attestationId") String attestationId) {
        var id = UUID.randomUUID().toString();
        return Response.created(URI.create("/participants/" + participantId + "/attestations/" + id)).build();
    }

    @POST
    @Path("/attestations")
    @Override
    public Response createAttestation(AttestationRequest attestationRequest) {
        return null;
    }


    @DELETE
    @Path("/participants/{participantId}/attestations/{attestationId}")
    @Override
    public Response deleteAttestation(@PathParam("participantId") String participantId,
                                      @PathParam("attestationId") String attestationId) {
        return Response.noContent().build();
    }

    @GET
    @Path("/participants/{participantId}/attestations")
    @Override
    public AttestationResponse getAttestations(@PathParam("participantId") String participantId) {
        return new AttestationResponse("dummy-attestation-id", "dummy-participant-id");
    }

    @POST
    @Path("/attestations")
    @Override
    public AttestationResponse queryAttestations(QuerySpec query) {
        return new AttestationResponse("dummy-attestation-id", "dummy-participant-id");
    }
}
