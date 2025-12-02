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
import org.eclipse.edc.issuerservice.api.admin.credentials.v1.unstable.model.CredentialOfferDto;
import org.eclipse.edc.issuerservice.api.admin.credentials.v1.unstable.model.CredentialStatusResponse;
import org.eclipse.edc.issuerservice.api.admin.credentials.v1.unstable.model.VerifiableCredentialResourceDto;
import org.eclipse.edc.issuerservice.spi.credentials.CredentialStatusService;
import org.eclipse.edc.issuerservice.spi.credentials.IssuerCredentialOfferService;
import org.eclipse.edc.issuerservice.spi.holder.model.Holder;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextId.onEncoded;
import static org.eclipse.edc.participantcontext.spi.types.ParticipantResource.filterByParticipantContextId;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(Versions.UNSTABLE + "/participants/{participantContextId}/credentials")
public class IssuerCredentialsAdminApiController implements IssuerCredentialsAdminApi {

    private final AuthorizationService authorizationService;
    private final CredentialStatusService credentialStatusService;
    private final IssuerCredentialOfferService credentialOfferService;

    public IssuerCredentialsAdminApiController(AuthorizationService authorizationService, CredentialStatusService credentialStatusService, IssuerCredentialOfferService issuerCredentialOfferService) {
        this.authorizationService = authorizationService;
        this.credentialStatusService = credentialStatusService;
        this.credentialOfferService = issuerCredentialOfferService;
    }

    @POST
    @Path("/query")
    @Override
    public Collection<VerifiableCredentialResourceDto> queryCredentials(@PathParam("participantContextId") String participantContextId, QuerySpec query, @Context SecurityContext context) {
        var decodedParticipantContextId = onEncoded(participantContextId).orElseThrow(InvalidRequestException::new);
        var spec = query.toBuilder().filter(filterByParticipantContextId(decodedParticipantContextId)).build();
        return credentialStatusService.queryCredentials(spec)
                .map(resources -> resources.stream()
                        .filter(resource -> authorizationService
                                .isAuthorized(context, decodedParticipantContextId, resource.getId(), VerifiableCredentialResource.class)
                                .succeeded())
                        .map(this::toDto)
                        .toList())
                .orElseThrow(exceptionMapper(VerifiableCredential.class, null));
    }

    @POST
    @Path("/{credentialId}/revoke")
    @Override
    public void revokeCredential(@PathParam("participantContextId") String participantContextId, @PathParam("credentialId") String credentialId, @Context SecurityContext context) {
        authorizationService.isAuthorized(context, onEncoded(participantContextId).orElseThrow(InvalidRequestException::new), credentialId, VerifiableCredentialResource.class)
                .compose(u -> credentialStatusService.revokeCredential(credentialId))
                .orElseThrow(exceptionMapper(VerifiableCredential.class, credentialId));
    }

    @POST
    @Path("/{credentialId}/suspend")
    @Override
    public Response suspendCredential(@PathParam("participantContextId") String participantContextId, @PathParam("credentialId") String credentialId) {
        return Response.status(501).build();
    }

    @POST
    @Path("/{credentialId}/resume")
    @Override
    public Response resumeCredential(@PathParam("participantContextId") String participantContextId, @PathParam("credentialId") String credentialId) {
        return Response.status(501).build();
    }

    @GET
    @Path("/{credentialId}/status")
    @Override
    public CredentialStatusResponse checkRevocationStatus(@PathParam("participantContextId") String participantContextId, @PathParam("credentialId") String credentialId, @Context SecurityContext context) {
        return authorizationService.isAuthorized(context, onEncoded(participantContextId).orElseThrow(InvalidRequestException::new), credentialId, VerifiableCredentialResource.class)
                .compose(u -> credentialStatusService.getCredentialStatus(credentialId))
                .map(status -> new CredentialStatusResponse(credentialId, status, null))
                .orElseThrow(exceptionMapper(VerifiableCredential.class, credentialId));
    }

    @POST
    @Path("/offer")
    @Override
    public void sendCredentialOffer(@PathParam("participantContextId") String participantContextId, CredentialOfferDto credentialOffer, @Context SecurityContext context) {

        var decodedParticipantContextId = onEncoded(participantContextId).orElseThrow(InvalidRequestException::new);
        var holderId = credentialOffer.holderId();
        var result = authorizationService.isAuthorized(context, decodedParticipantContextId, holderId, Holder.class);
        result.orElseThrow(exceptionMapper(Holder.class, holderId));

        credentialOfferService.sendCredentialOffer(decodedParticipantContextId, holderId, credentialOffer.credentials())
                .orElseThrow(InvalidRequestException::new);
    }

    private @NotNull VerifiableCredentialResourceDto toDto(VerifiableCredentialResource resource) {
        return new VerifiableCredentialResourceDto(
                resource.getId(),
                resource.getParticipantContextId(),
                resource.getVerifiableCredential().format(),
                resource.getVerifiableCredential().credential());
    }
}
