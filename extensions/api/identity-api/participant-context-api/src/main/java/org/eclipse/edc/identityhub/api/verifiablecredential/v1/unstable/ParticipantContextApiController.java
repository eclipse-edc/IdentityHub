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

package org.eclipse.edc.identityhub.api.verifiablecredential.v1.unstable;

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
import org.eclipse.edc.api.auth.spi.AuthorizationService;
import org.eclipse.edc.api.auth.spi.RequiredScope;
import org.eclipse.edc.identityhub.api.Versions;
import org.eclipse.edc.identityhub.api.verifiablecredential.validation.ParticipantManifestValidator;
import org.eclipse.edc.identityhub.spi.participantcontext.IdentityHubParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.CreateParticipantContextResponse;
import org.eclipse.edc.identityhub.spi.participantcontext.model.IdentityHubParticipantContext;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.web.spi.exception.ValidationFailureException;

import java.util.Collection;
import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.identityhub.spi.authorization.AuthorizationResultHandler.exceptionMapper;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(Versions.UNSTABLE + "/participants")
public class ParticipantContextApiController implements ParticipantContextApi {

    private final ParticipantManifestValidator participantManifestValidator;
    private final IdentityHubParticipantContextService participantContextService;
    private final AuthorizationService authorizationService;

    public ParticipantContextApiController(ParticipantManifestValidator participantManifestValidator, IdentityHubParticipantContextService participantContextService, AuthorizationService authorizationService) {
        this.participantManifestValidator = participantManifestValidator;
        this.participantContextService = participantContextService;
        this.authorizationService = authorizationService;
    }

    @Override
    @POST
    @RequiredScope("identity-api:admin")
    public CreateParticipantContextResponse createParticipant(ParticipantManifest manifest) {
        participantManifestValidator.validate(manifest).orElseThrow(ValidationFailureException::new);
        return participantContextService.createParticipantContext(manifest)
                .orElseThrow(exceptionMapper(ParticipantManifest.class, manifest.getParticipantContextId()));
    }

    @Override
    @GET
    @RequiredScope("identity-api:participants:read")
    @Path("/{participantContextId}")
    public IdentityHubParticipantContext getParticipant(@PathParam("participantContextId") String participantContextId, @Context SecurityContext securityContext) {
        return authorizationService.authorize(securityContext, participantContextId, participantContextId, IdentityHubParticipantContext.class)
                .compose(u -> participantContextService.getParticipantContext(participantContextId))
                .orElseThrow(exceptionMapper(IdentityHubParticipantContext.class, participantContextId));

    }

    @Override
    @POST
    @RequiredScope("identity-api:participants:write")
    @Path("/{participantContextId}/token")
    public String regenerateParticipantToken(@PathParam("participantContextId") String participantContextId, @Context SecurityContext securityContext) {
        return authorizationService.authorize(securityContext, participantContextId, participantContextId, IdentityHubParticipantContext.class)
                .compose(u -> participantContextService.regenerateApiToken(participantContextId))
                .orElseThrow(exceptionMapper(IdentityHubParticipantContext.class, participantContextId));
    }

    @Override
    @POST
    @RequiredScope("identity-api:participants:write")
    @Path("/{participantContextId}/state")
    public void activateParticipant(@PathParam("participantContextId") String participantContextId, @QueryParam("isActive") boolean isActive) {
        participantContextService.updateParticipant(participantContextId, isActive ? IdentityHubParticipantContext::activate : IdentityHubParticipantContext::deactivate)
                .orElseThrow(exceptionMapper(IdentityHubParticipantContext.class, participantContextId));
    }

    @Override
    @DELETE
    @Path("/{participantContextId}")
    @RequiredScope("identity-api:participants:write")
    public void deleteParticipant(@PathParam("participantContextId") String participantContextId, @Context SecurityContext securityContext) {
        participantContextService.deleteParticipantContext(participantContextId)
                .orElseThrow(exceptionMapper(IdentityHubParticipantContext.class, participantContextId));
    }

    @Override
    @PUT
    @RequiredScope("identity-api:admin")
    @Path("/{participantContextId}/scopes")
    public void updateParticipantScopes(@PathParam("participantContextId") String participantContextId, List<String> scopes) {
        participantContextService.updateParticipant(participantContextId, participantContext -> participantContext.setScopes(scopes))
                .orElseThrow(exceptionMapper(IdentityHubParticipantContext.class, participantContextId));
    }

    @GET
    @RequiredScope("identity-api:admin")
    @Override
    public Collection<IdentityHubParticipantContext> getAllParticipants(@DefaultValue("0") @QueryParam("offset") Integer offset,
                                                                        @DefaultValue("50") @QueryParam("limit") Integer limit) {
        return participantContextService.query(QuerySpec.Builder.newInstance().offset(offset).limit(limit).build())
                .orElseThrow(exceptionMapper(IdentityHubParticipantContext.class));
    }

}
