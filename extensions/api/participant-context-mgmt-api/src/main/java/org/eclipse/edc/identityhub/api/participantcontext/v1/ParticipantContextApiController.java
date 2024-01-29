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
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.edc.identityhub.api.participantcontext.v1.validation.ParticipantManifestValidator;
import org.eclipse.edc.identityhub.spi.AuthorizationService;
import org.eclipse.edc.identityhub.spi.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.model.participant.ParticipantContext;
import org.eclipse.edc.identityhub.spi.model.participant.ParticipantManifest;
import org.eclipse.edc.web.spi.exception.ValidationFailureException;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.identityhub.spi.AuthorizationResultHandler.exceptionMapper;

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
    @RolesAllowed("admin")
    public String createParticipant(ParticipantManifest manifest) {
        participantManifestValidator.validate(manifest).orElseThrow(ValidationFailureException::new);
        return participantContextService.createParticipantContext(manifest)
                .orElseThrow(exceptionMapper(ParticipantManifest.class, manifest.getParticipantId()));
    }

    @Override
    @GET
    @Path("/{participantId}")
    public ParticipantContext getParticipant(@PathParam("participantId") String participantId, @Context SecurityContext securityContext) {
        return authorizationService.isAuthorized(securityContext.getUserPrincipal(), participantId, ParticipantContext.class)
                .compose(u -> participantContextService.getParticipantContext(participantId))
                .orElseThrow(exceptionMapper(ParticipantContext.class, participantId));
    }

    @Override
    @POST
    @Path("/{participantId}/token")
    public String regenerateToken(@PathParam("participantId") String participantId, @Context SecurityContext securityContext) {
        return authorizationService.isAuthorized(securityContext.getUserPrincipal(), participantId, ParticipantContext.class)
                .compose(u -> participantContextService.regenerateApiToken(participantId))
                .orElseThrow(exceptionMapper(ParticipantContext.class, participantId));
    }

    @Override
    @POST
    @Path("/{participantId}/state")
    @RolesAllowed("admin")
    public void activateParticipant(@PathParam("participantId") String participantId, @QueryParam("isActive") boolean isActive) {
        if (isActive) {
            participantContextService.updateParticipant(participantId, ParticipantContext::activate);
        } else {
            participantContextService.updateParticipant(participantId, ParticipantContext::deactivate);
        }

    }

    @Override
    @DELETE
    @Path("/{participantId}")
    @RolesAllowed("admin")
    public void deleteParticipant(@PathParam("participantId") String participantId, @Context SecurityContext securityContext) {
        participantContextService.deleteParticipantContext(participantId)
                .orElseThrow(exceptionMapper(ParticipantContext.class, participantId));
    }

}
