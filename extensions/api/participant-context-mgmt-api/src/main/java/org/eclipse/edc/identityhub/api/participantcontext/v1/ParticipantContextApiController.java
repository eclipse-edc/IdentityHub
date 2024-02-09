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
import org.eclipse.edc.web.spi.exception.ValidationFailureException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

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
    @RolesAllowed(ServicePrincipal.ROLE_ADMIN)
    public String createParticipant(ParticipantManifest manifest) {
        participantManifestValidator.validate(manifest).orElseThrow(ValidationFailureException::new);
        return participantContextService.createParticipantContext(manifest)
                .orElseThrow(exceptionMapper(ParticipantManifest.class, manifest.getParticipantId()));
    }

    @Override
    @GET
    @Path("/{encodedParticipantId}")
    public ParticipantContext getParticipant(@PathParam("encodedParticipantId") String encodedParticipantId, @Context SecurityContext securityContext) {
        var participantId = decodeParticipantId(encodedParticipantId);
        return authorizationService.isAuthorized(securityContext, participantId, ParticipantContext.class)
                .compose(u -> participantContextService.getParticipantContext(participantId))
                .orElseThrow(exceptionMapper(ParticipantContext.class, participantId));
    }

    @Override
    @POST
    @Path("/{encodedParticipantId}/token")
    public String regenerateToken(@PathParam("encodedParticipantId") String encodedParticipantId, @Context SecurityContext securityContext) {
        var participantId = decodeParticipantId(encodedParticipantId);
        return authorizationService.isAuthorized(securityContext, participantId, ParticipantContext.class)
                .compose(u -> participantContextService.regenerateApiToken(participantId))
                .orElseThrow(exceptionMapper(ParticipantContext.class, participantId));
    }

    @Override
    @POST
    @Path("/{encodedParticipantId}/state")
    @RolesAllowed(ServicePrincipal.ROLE_ADMIN)
    public void activateParticipant(@PathParam("encodedParticipantId") String encodedParticipantId, @QueryParam("isActive") boolean isActive) {
        var participantId = decodeParticipantId(encodedParticipantId);
        if (isActive) {
            participantContextService.updateParticipant(participantId, ParticipantContext::activate);
        } else {
            participantContextService.updateParticipant(participantId, ParticipantContext::deactivate);
        }

    }

    @Override
    @DELETE
    @Path("/{encodedParticipantId}")
    @RolesAllowed(ServicePrincipal.ROLE_ADMIN)
    public void deleteParticipant(@PathParam("encodedParticipantId") String encodedParticipantId, @Context SecurityContext securityContext) {
        var participantId = decodeParticipantId(encodedParticipantId);
        participantContextService.deleteParticipantContext(participantId)
                .orElseThrow(exceptionMapper(ParticipantContext.class, participantId));
    }

    @Override
    @PUT
    @Path("/{encodedParticipantId}/roles")
    @RolesAllowed(ServicePrincipal.ROLE_ADMIN)
    public void updateRoles(@PathParam("encodedParticipantId") String encodedParticipantId, List<String> roles) {
        var participantId = decodeParticipantId(encodedParticipantId);
        participantContextService.updateParticipant(participantId, participantContext -> participantContext.setRoles(roles))
                .orElseThrow(exceptionMapper(ParticipantContext.class, participantId));
    }

    /**
     * Decode the base64-url encoded participantId
     *
     * @param encoded participantId encoded in base64 url
     * @return participantId
     */
    private String decodeParticipantId(String encoded) {
        return new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
    }
}
