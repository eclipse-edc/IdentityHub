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
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.identityhub.api.Versions;
import org.eclipse.edc.issuerservice.api.admin.holder.v1.unstable.model.HolderDto;
import org.eclipse.edc.issuerservice.spi.holder.HolderService;
import org.eclipse.edc.issuerservice.spi.holder.model.Holder;
import org.eclipse.edc.spi.query.QuerySpec;

import java.net.URI;
import java.util.Collection;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(Versions.UNSTABLE + "/holders")
public class IssuerHolderAdminApiController implements IssuerHolderAdminApi {

    private final HolderService holderService;

    public IssuerHolderAdminApiController(HolderService holderService) {
        this.holderService = holderService;
    }

    @POST
    @Override
    public Response addHolder(HolderDto holder) {
        return holderService.createHolder(holder.toHolder())
                .map(v -> Response.created(URI.create(Versions.UNSTABLE + "/holders/" + holder.id())).build())
                .orElseThrow(exceptionMapper(Holder.class, holder.id()));
    }

    @PUT
    @Override
    public Response updateHolder(HolderDto holder) {
        return holderService.updateHolder(holder.toHolder())
                .map(v -> Response.ok().build())
                .orElseThrow(exceptionMapper(Holder.class, holder.id()));
    }

    @GET
    @Path("/{holderId}")
    @Override
    public HolderDto getHolderById(@PathParam("holderId") String holderId) {
        return holderService.findById(holderId)
                .map(HolderDto::from)
                .orElseThrow(exceptionMapper(Holder.class, holderId));
    }

    @POST
    @Path("/query")
    @Override
    public Collection<HolderDto> queryHolders(QuerySpec querySpec) {
        return holderService.queryHolders(querySpec)
                .map(collection -> collection.stream().map(HolderDto::from).toList())
                .orElseThrow(exceptionMapper(Holder.class, null));
    }
}
