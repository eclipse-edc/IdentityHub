/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
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

package org.eclipse.edc.identityhub.api.participantcontext.v1;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.edc.identityhub.api.v1.validation.ParticipantManifestValidator;
import org.eclipse.edc.identityhub.spi.AuthorizationService;
import org.eclipse.edc.identityhub.spi.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.authentication.ServicePrincipal;
import org.eclipse.edc.identityhub.spi.model.participant.ParticipantContext;
import org.eclipse.edc.identityhub.spi.model.participant.ParticipantManifest;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ValidationFailureException;

import java.util.Collection;
import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.identityhub.spi.AuthorizationResultHandler.exceptionMapper;
import static org.eclipse.edc.identityhub.spi.ParticipantContextId.onEncoded;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v1/participants")
public class ParticipantContextApiController implements ParticipantContextApi {

    private final ParticipantManifestValidator participantManifestValidator;
    private final ParticipantContextService participantContextService;
    private final AuthorizationService authorizationService;

    public ParticipantContextApiController(ParticipantManifestValidator participantManifestValidator, ParticipantContextService participantContextService, AuthorizationService authorizationService) {
        this.participantManifestValidator = participantManifestValidator;
        this.participantContextService = participantContextService;
        this.authorizationService = authorizationService;
    }

    @Override
    @POST
    @RolesAllowed(ServicePrincipal.ROLE_ADMIN)
    public String createParticipant(ParticipantManifest manifest) {
        participantManifestValidator.validate(manifest).orElseThrow(ValidationFailureException::new);
        return participantContextService.createParticipantContext(manifest)
                .orElseThrow(exceptionMapper(ParticipantManifest.class, manifest.getParticipantId()));
    }

    @Override
    @GET
    @Path("/{participantId}")
    public ParticipantContext getParticipant(@PathParam("participantId") String participantId, @Context SecurityContext securityContext) {
        return onEncoded(participantId)
                .map(decoded -> authorizationService.isAuthorized(securityContext, decoded, ParticipantContext.class)
                        .compose(u -> participantContextService.getParticipantContext(decoded))
                        .orElseThrow(exceptionMapper(ParticipantContext.class, decoded)))
                .orElseThrow(InvalidRequestException::new);
    }

    @Override
    @POST
    @Path("/{participantId}/token")
    public String regenerateToken(@PathParam("participantId") String participantId, @Context SecurityContext securityContext) {
        return onEncoded(participantId)
                .map(decoded -> authorizationService.isAuthorized(securityContext, decoded, ParticipantContext.class)
                        .compose(u -> participantContextService.regenerateApiToken(decoded))
                        .orElseThrow(exceptionMapper(ParticipantContext.class, decoded)))
                .orElseThrow(InvalidRequestException::new);
    }

    @Override
    @POST
    @Path("/{participantId}/state")
    @RolesAllowed(ServicePrincipal.ROLE_ADMIN)
    public void activateParticipant(@PathParam("participantId") String participantId, @QueryParam("isActive") boolean isActive) {
        onEncoded(participantId)
                .onSuccess(decoded -> participantContextService.updateParticipant(decoded, isActive ? ParticipantContext::activate : ParticipantContext::deactivate)
                        .orElseThrow(exceptionMapper(ParticipantContext.class, decoded)))
                .orElseThrow(InvalidRequestException::new);
    }

    @Override
    @DELETE
    @Path("/{participantId}")
    @RolesAllowed(ServicePrincipal.ROLE_ADMIN)
    public void deleteParticipant(@PathParam("participantId") String participantId, @Context SecurityContext securityContext) {
        onEncoded(participantId)
                .onSuccess(decoded -> participantContextService.deleteParticipantContext(decoded)
                        .orElseThrow(exceptionMapper(ParticipantContext.class, decoded)))
                .orElseThrow(InvalidRequestException::new);
    }

    @Override
    @PUT
    @Path("/{participantId}/roles")
    @RolesAllowed(ServicePrincipal.ROLE_ADMIN)
    public void updateRoles(@PathParam("participantId") String participantId, List<String> roles) {
        onEncoded(participantId)
                .onSuccess(decoded -> participantContextService.updateParticipant(decoded, participantContext -> participantContext.setRoles(roles))
                        .orElseThrow(exceptionMapper(ParticipantContext.class, decoded)))
                .orElseThrow(InvalidRequestException::new);
    }

    @GET
    @RolesAllowed(ServicePrincipal.ROLE_ADMIN)
    @Override
    public Collection<ParticipantContext> getAll(@DefaultValue("0") @QueryParam("offset") Integer offset,
                                                 @DefaultValue("50") @QueryParam("limit") Integer limit) {
        return participantContextService.query(QuerySpec.Builder.newInstance().offset(offset).limit(limit).build())
                .orElseThrow(exceptionMapper(ParticipantContext.class));
    }

}
