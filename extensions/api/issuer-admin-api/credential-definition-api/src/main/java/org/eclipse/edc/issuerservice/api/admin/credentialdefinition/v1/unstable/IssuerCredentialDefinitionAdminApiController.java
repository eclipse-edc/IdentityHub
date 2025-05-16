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

package org.eclipse.edc.issuerservice.api.admin.credentialdefinition.v1.unstable;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
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
import org.eclipse.edc.issuerservice.api.admin.credentialdefinition.v1.unstable.model.CredentialDefinitionDto;
import org.eclipse.edc.issuerservice.spi.issuance.credentialdefinition.CredentialDefinitionService;
import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialDefinition;
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
@Path(Versions.UNSTABLE + "/participants/{participantContextId}/credentialdefinitions")
public class IssuerCredentialDefinitionAdminApiController implements IssuerCredentialDefinitionAdminApi {

    private final AuthorizationService authorizationService;
    private final CredentialDefinitionService credentialDefinitionService;


    public IssuerCredentialDefinitionAdminApiController(AuthorizationService authorizationService, CredentialDefinitionService credentialDefinitionService) {
        this.authorizationService = authorizationService;
        this.credentialDefinitionService = credentialDefinitionService;
    }

    @POST
    @Override
    public Response createCredentialDefinition(@PathParam("participantContextId") String participantContextId, CredentialDefinitionDto definitionDto, @Context SecurityContext context) {
        var decodedParticipantId = onEncoded(participantContextId).orElseThrow(InvalidRequestException::new);
        return authorizationService.isAuthorized(context, decodedParticipantId, ParticipantContext.class)
                .compose(u -> credentialDefinitionService.createCredentialDefinition(definitionDto.toCredentialDefinition(decodedParticipantId)))
                .map(v -> Response.created(URI.create(Versions.UNSTABLE + "/participants/%s/credentialdefinitions/%s".formatted(participantContextId, definitionDto.getId()))).build())
                .orElseThrow(exceptionMapper(CredentialDefinition.class, definitionDto.getId()));
    }

    @PUT
    @Override
    public Response updateCredentialDefinition(@PathParam("participantContextId") String participantContextId, CredentialDefinitionDto credentialDefinition, @Context SecurityContext context) {
        var decodedParticipantId = onEncoded(participantContextId).orElseThrow(InvalidRequestException::new);
        return authorizationService.isAuthorized(context, credentialDefinition.getId(), CredentialDefinition.class)
                .compose(u -> credentialDefinitionService.updateCredentialDefinition(credentialDefinition.toCredentialDefinition(decodedParticipantId)))
                .map(v -> Response.ok().build())
                .orElseThrow(exceptionMapper(CredentialDefinition.class, credentialDefinition.getId()));
    }

    @GET
    @Path("/{credentialDefinitionId}")
    @Override
    public CredentialDefinition getCredentialDefinitionById(@PathParam("participantContextId") String participantContextId, @PathParam("credentialDefinitionId") String credentialDefinitionId, @Context SecurityContext context) {
        return authorizationService.isAuthorized(context, credentialDefinitionId, CredentialDefinition.class)
                .compose(u -> credentialDefinitionService.findCredentialDefinitionById(credentialDefinitionId))
                .orElseThrow(exceptionMapper(CredentialDefinition.class, credentialDefinitionId));
    }

    @POST
    @Path("/query")
    @Override
    public Collection<CredentialDefinition> queryCredentialDefinitions(@PathParam("participantContextId") String participantContextId, QuerySpec querySpec, @Context SecurityContext context) {
        var decodedParticipantId = onEncoded(participantContextId).orElseThrow(InvalidRequestException::new);
        var spec = querySpec.toBuilder().filter(filterByParticipantContextId(decodedParticipantId)).build();
        var definitions = credentialDefinitionService.queryCredentialDefinitions(spec)
                .orElseThrow(exceptionMapper(CredentialDefinition.class, null));

        return definitions.stream()
                .filter(definition -> authorizationService.isAuthorized(context, definition.getId(), CredentialDefinition.class).succeeded())
                .toList();
    }

    @DELETE
    @Path("/{credentialDefinitionId}")
    @Override
    public void deleteCredentialDefinitionById(@PathParam("participantContextId") String participantContextId, @PathParam("credentialDefinitionId") String credentialDefinitionId, @Context SecurityContext context) {
        authorizationService.isAuthorized(context, credentialDefinitionId, CredentialDefinition.class)
                .compose(u -> credentialDefinitionService.deleteCredentialDefinition(credentialDefinitionId))
                .orElseThrow(exceptionMapper(CredentialDefinition.class, credentialDefinitionId));
    }
}
