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

package org.eclipse.edc.issuerservice.api.admin.participant.v1.unstable;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.identityhub.api.Versions;
import org.eclipse.edc.issuerservice.api.admin.participant.v1.unstable.model.AddParticipantRequest;
import org.eclipse.edc.issuerservice.api.admin.participant.v1.unstable.model.GetParticipantResponse;
import org.eclipse.edc.issuerservice.api.admin.participant.v1.unstable.model.UpdateParticipantRequest;
import org.eclipse.edc.spi.query.QuerySpec;

import java.net.URI;
import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(Versions.UNSTABLE + "/participants")
public class IssuerParticipantAdminApiController implements IssuerParticipantAdminApi {


    @POST
    @Override
    public Response addParticipant(AddParticipantRequest participant) {
        return Response.created(URI.create("/participants/dummy-id")).build();
    }

    @PUT
    @Override
    public Response updateParticipant(UpdateParticipantRequest participant) {
        return Response.ok().build();
    }

    @GET
    @Path("/{participantId}")
    @Override
    public GetParticipantResponse getParticipantById(@PathParam("participantId") String participantId) {
        return new GetParticipantResponse(participantId);
    }

    @POST
    @Path("/query")
    @Override
    public List<GetParticipantResponse> queryParticipants(QuerySpec querySpec) {
        return List.of(new GetParticipantResponse("dummy-id"));
    }
}
