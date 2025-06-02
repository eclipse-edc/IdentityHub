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

package org.eclipse.edc.issuerservice.api.admin.holder.v1.unstable;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.edc.identityhub.api.Versions;
import org.eclipse.edc.identityhub.spi.authorization.AuthorizationService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.issuerservice.api.admin.holder.v1.unstable.model.HolderDto;
import org.eclipse.edc.issuerservice.spi.holder.HolderService;
import org.eclipse.edc.issuerservice.spi.holder.model.Holder;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;

import java.net.URI;
import java.util.Collection;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextId.onEncoded;
import static org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantResource.filterByParticipantContextId;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(Versions.UNSTABLE + "/participants/{participantContextId}/holders")
public class IssuerHolderAdminApiController implements IssuerHolderAdminApi {

    private final AuthorizationService authorizationService;
    private final HolderService holderService;

    public IssuerHolderAdminApiController(AuthorizationService authorizationService, HolderService holderService) {
        this.authorizationService = authorizationService;
        this.holderService = holderService;
    }

    @POST
    @Override
    public Response addHolder(@PathParam("participantContextId") String participantContextId, HolderDto holder, @Context SecurityContext context) {
        var decodedParticipantId = onEncoded(participantContextId).orElseThrow(InvalidRequestException::new);
        return authorizationService.isAuthorized(context, decodedParticipantId, ParticipantContext.class)
                .compose(u -> holderService.createHolder(holder.toHolder(decodedParticipantId)))
                .map(v -> Response.created(URI.create(Versions.UNSTABLE + "/participants/%s/holders/%s".formatted(participantContextId, holder.id()))).build())
                .orElseThrow(exceptionMapper(Holder.class, holder.id()));
    }

    @PUT
    @Override
    public Response updateHolder(@PathParam("participantContextId") String participantContextId, HolderDto holder, @Context SecurityContext context) {
        var decodedParticipantId = onEncoded(participantContextId).orElseThrow(InvalidRequestException::new);
        return authorizationService.isAuthorized(context, holder.id(), Holder.class)
                .compose(u -> holderService.updateHolder(holder.toHolder(decodedParticipantId)))
                .map(v -> Response.ok().build())
                .orElseThrow(exceptionMapper(Holder.class, holder.id()));
    }

    @GET
    @Path("/{holderId}")
    @Override
    public Holder getHolderById(@PathParam("participantContextId") String participantContextId, @PathParam("holderId") String holderId, @Context SecurityContext context) {
        return authorizationService.isAuthorized(context, holderId, Holder.class)
                .compose(u -> holderService.findById(holderId))
                .orElseThrow(exceptionMapper(Holder.class, holderId));
    }

    @POST
    @Path("/query")
    @Override
    public Collection<Holder> queryHolders(@PathParam("participantContextId") String participantContextId, QuerySpec querySpec, @Context SecurityContext context) {
        var decodedParticipantId = onEncoded(participantContextId).orElseThrow(InvalidRequestException::new);
        var spec = querySpec.toBuilder().filter(filterByParticipantContextId(decodedParticipantId)).build();
        return holderService.queryHolders(spec)
                .map(collection -> collection.stream()
                        .filter(holder -> authorizationService.isAuthorized(context, holder.getHolderId(), Holder.class).succeeded()).toList())
                .orElseThrow(exceptionMapper(Holder.class, null));
    }
}
