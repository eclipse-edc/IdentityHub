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

package org.eclipse.edc.issuerservice.api.admin.credentials.v1.unstable;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.identityhub.api.Versions;
import org.eclipse.edc.identityhub.spi.authorization.AuthorizationService;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.issuerservice.api.admin.credentials.v1.unstable.model.CredentialStatusResponse;
import org.eclipse.edc.issuerservice.api.admin.credentials.v1.unstable.model.VerifiableCredentialDto;
import org.eclipse.edc.issuerservice.spi.credentials.CredentialService;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;

import java.util.Collection;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextId.onEncoded;
import static org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantResource.filterByParticipantContextId;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(Versions.UNSTABLE + "/participants/{participantContextId}/credentials")
public class IssuerCredentialsAdminApiController implements IssuerCredentialsAdminApi {

    private final AuthorizationService authorizationService;
    private final CredentialService credentialService;

    public IssuerCredentialsAdminApiController(AuthorizationService authorizationService, CredentialService credentialService) {
        this.authorizationService = authorizationService;
        this.credentialService = credentialService;
    }

    @POST
    @Path("/query")
    @Override
    public Collection<VerifiableCredentialDto> queryCredentials(@PathParam("participantContextId") String participantContextId, QuerySpec query, @Context SecurityContext context) {
        var decodedParticipantId = onEncoded(participantContextId).orElseThrow(InvalidRequestException::new);
        var spec = query.toBuilder().filter(filterByParticipantContextId(decodedParticipantId)).build();
        return credentialService.queryCredentials(spec).map(coll -> coll.stream()
                        .filter(resource -> authorizationService.isAuthorized(context, resource.getId(), VerifiableCredentialResource.class).succeeded())
                        .map(resource -> new VerifiableCredentialDto(resource.getParticipantContextId(), resource.getVerifiableCredential().format(), resource.getVerifiableCredential().credential())).toList())
                .orElseThrow(exceptionMapper(VerifiableCredential.class, null));
    }

    @POST
    @Path("/{credentialId}/revoke")
    @Override
    public Response revokeCredential(@PathParam("participantContextId") String participantContextId, @PathParam("credentialId") String credentialId, @Context SecurityContext context) {
        return authorizationService.isAuthorized(context, credentialId, VerifiableCredentialResource.class)
                .compose(u -> credentialService.revokeCredential(credentialId))
                .map(v -> Response.noContent().build())
                .orElseThrow(exceptionMapper(VerifiableCredential.class, credentialId));
    }

    @POST
    @Path("/{credentialId}/suspend")
    @Override
    public Response suspendCredential(@PathParam("participantContextId") String participantContextId, @PathParam("credentialId") String credentialId, @Context SecurityContext context) {
        return Response.status(501).build();
    }

    @POST
    @Path("/{credentialId}/resume")
    @Override
    public Response resumeCredential(@PathParam("participantContextId") String participantContextId, @PathParam("credentialId") String credentialId, @Context SecurityContext context) {
        return Response.status(501).build();
    }

    @GET
    @Path("/{credentialId}/status")
    @Override
    public CredentialStatusResponse checkRevocationStatus(@PathParam("participantContextId") String participantContextId, @PathParam("credentialId") String credentialId, @Context SecurityContext context) {
        return authorizationService.isAuthorized(context, credentialId, VerifiableCredentialResource.class)
                .compose(u -> credentialService.getCredentialStatus(credentialId))
                .map(status -> new CredentialStatusResponse(credentialId, status, null))
                .orElseThrow(exceptionMapper(VerifiableCredential.class, credentialId));
    }
}
