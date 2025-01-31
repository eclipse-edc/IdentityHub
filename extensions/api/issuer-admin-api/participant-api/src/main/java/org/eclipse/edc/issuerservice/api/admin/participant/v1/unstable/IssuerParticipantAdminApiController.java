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
import org.eclipse.edc.issuerservice.api.admin.participant.v1.unstable.model.ParticipantDto;
import org.eclipse.edc.issuerservice.spi.participant.ParticipantService;
import org.eclipse.edc.issuerservice.spi.participant.models.Participant;
import org.eclipse.edc.spi.query.QuerySpec;

import java.net.URI;
import java.util.Collection;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(Versions.UNSTABLE + "/participants")
public class IssuerParticipantAdminApiController implements IssuerParticipantAdminApi {

    private final ParticipantService participantService;

    public IssuerParticipantAdminApiController(ParticipantService participantService) {
        this.participantService = participantService;
    }

    @POST
    @Override
    public Response addParticipant(ParticipantDto dto) {
        return participantService.createParticipant(dto.toParticipant())
                .map(v -> Response.created(URI.create(Versions.UNSTABLE + "/participants/" + dto.id())).build())
                .orElseThrow(exceptionMapper(Participant.class, dto.id()));
    }

    @PUT
    @Override
    public Response updateParticipant(ParticipantDto dto) {
        return participantService.updateParticipant(dto.toParticipant())
                .map(v -> Response.ok().build())
                .orElseThrow(exceptionMapper(Participant.class, dto.id()));
    }

    @GET
    @Path("/{participantId}")
    @Override
    public ParticipantDto getParticipantById(@PathParam("participantId") String participantId) {
        return participantService.findById(participantId)
                .map(ParticipantDto::from)
                .orElseThrow(exceptionMapper(Participant.class, participantId));
    }

    @POST
    @Path("/query")
    @Override
    public Collection<ParticipantDto> queryParticipants(QuerySpec querySpec) {
        return participantService.queryParticipants(querySpec)
                .map(collection -> collection.stream().map(ParticipantDto::from).toList())
                .orElseThrow(exceptionMapper(Participant.class, null));
    }
}
