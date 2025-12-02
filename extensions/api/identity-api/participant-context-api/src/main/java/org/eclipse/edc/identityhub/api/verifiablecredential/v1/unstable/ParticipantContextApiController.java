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
import org.eclipse.edc.api.auth.spi.AuthorizationService;
import org.eclipse.edc.api.auth.spi.ParticipantPrincipal;
import org.eclipse.edc.api.auth.spi.RequiredScope;
import org.eclipse.edc.identityhub.api.Versions;
import org.eclipse.edc.identityhub.api.verifiablecredential.validation.ParticipantManifestValidator;
import org.eclipse.edc.identityhub.spi.participantcontext.IdentityHubParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.CreateParticipantContextResponse;
import org.eclipse.edc.identityhub.spi.participantcontext.model.IdentityHubParticipantContext;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ValidationFailureException;

import java.util.Collection;
import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.identityhub.spi.authorization.AuthorizationResultHandler.exceptionMapper;
import static org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextId.onEncoded;

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
    @RequiredScope("identity-api:write")
    @RolesAllowed({ParticipantPrincipal.ROLE_ADMIN, ParticipantPrincipal.ROLE_PARTICIPANT, ParticipantPrincipal.ROLE_PROVISIONER})
    public CreateParticipantContextResponse createParticipant(ParticipantManifest manifest) {
        participantManifestValidator.validate(manifest).orElseThrow(ValidationFailureException::new);
        return participantContextService.createParticipantContext(manifest)
                .orElseThrow(exceptionMapper(ParticipantManifest.class, manifest.getParticipantContextId()));
    }

    @Override
    @GET
    @RequiredScope("identity-api:read")
    @RolesAllowed({ParticipantPrincipal.ROLE_ADMIN, ParticipantPrincipal.ROLE_PARTICIPANT, ParticipantPrincipal.ROLE_PROVISIONER})
    @Path("/{participantContextId}")
    public IdentityHubParticipantContext getParticipant(@PathParam("participantContextId") String participantContextId, @Context SecurityContext securityContext) {
        return onEncoded(participantContextId)
                .map(decoded -> authorizationService.authorize(securityContext, decoded, decoded, IdentityHubParticipantContext.class)
                        .compose(u -> participantContextService.getParticipantContext(decoded))
                        .orElseThrow(exceptionMapper(IdentityHubParticipantContext.class, decoded)))
                .orElseThrow(InvalidRequestException::new);
    }

    @Override
    @POST
    @RequiredScope("identity-api:write")
    @RolesAllowed({ParticipantPrincipal.ROLE_ADMIN, ParticipantPrincipal.ROLE_PARTICIPANT, ParticipantPrincipal.ROLE_PROVISIONER})
    @Path("/{participantContextId}/token")
    public String regenerateParticipantToken(@PathParam("participantContextId") String participantContextId, @Context SecurityContext securityContext) {
        return onEncoded(participantContextId)
                .map(decoded -> authorizationService.authorize(securityContext, decoded, decoded, IdentityHubParticipantContext.class)
                        .compose(u -> participantContextService.regenerateApiToken(decoded))
                        .orElseThrow(exceptionMapper(IdentityHubParticipantContext.class, decoded)))
                .orElseThrow(InvalidRequestException::new);
    }

    @Override
    @POST
    @RequiredScope("identity-api:write")
    @RolesAllowed({ParticipantPrincipal.ROLE_ADMIN, ParticipantPrincipal.ROLE_PARTICIPANT, ParticipantPrincipal.ROLE_PROVISIONER})
    @Path("/{participantContextId}/state")
    public void activateParticipant(@PathParam("participantContextId") String participantContextId, @QueryParam("isActive") boolean isActive) {
        onEncoded(participantContextId)
                .onSuccess(decoded -> participantContextService.updateParticipant(decoded, isActive ? IdentityHubParticipantContext::activate : IdentityHubParticipantContext::deactivate)
                        .orElseThrow(exceptionMapper(IdentityHubParticipantContext.class, decoded)))
                .orElseThrow(InvalidRequestException::new);
    }

    @Override
    @DELETE
    @Path("/{participantContextId}")
    @RequiredScope("identity-api:write")
    @RolesAllowed({ParticipantPrincipal.ROLE_ADMIN, ParticipantPrincipal.ROLE_PARTICIPANT, ParticipantPrincipal.ROLE_PROVISIONER})
    public void deleteParticipant(@PathParam("participantContextId") String participantContextId, @Context SecurityContext securityContext) {
        onEncoded(participantContextId)
                .onSuccess(decoded -> participantContextService.deleteParticipantContext(decoded)
                        .orElseThrow(exceptionMapper(IdentityHubParticipantContext.class, decoded)))
                .orElseThrow(InvalidRequestException::new);
    }

    @Override
    @PUT
    @RequiredScope("identity-api:write")
    @RolesAllowed({ParticipantPrincipal.ROLE_ADMIN, ParticipantPrincipal.ROLE_PARTICIPANT, ParticipantPrincipal.ROLE_PROVISIONER})
    @Path("/{participantContextId}/roles")
    public void updateParticipantRoles(@PathParam("participantContextId") String participantContextId, List<String> roles) {
        onEncoded(participantContextId)
                .onSuccess(decoded -> participantContextService.updateParticipant(decoded, participantContext -> participantContext.setRoles(roles))
                        .orElseThrow(exceptionMapper(IdentityHubParticipantContext.class, decoded)))
                .orElseThrow(InvalidRequestException::new);
    }

    @GET
    @RequiredScope("identity-api:read")
    @RolesAllowed({ParticipantPrincipal.ROLE_ADMIN, ParticipantPrincipal.ROLE_PARTICIPANT, ParticipantPrincipal.ROLE_PROVISIONER})
    @Override
    public Collection<IdentityHubParticipantContext> getAllParticipants(@DefaultValue("0") @QueryParam("offset") Integer offset,
                                                                        @DefaultValue("50") @QueryParam("limit") Integer limit) {
        return participantContextService.query(QuerySpec.Builder.newInstance().offset(offset).limit(limit).build())
                .orElseThrow(exceptionMapper(IdentityHubParticipantContext.class));
    }

}
