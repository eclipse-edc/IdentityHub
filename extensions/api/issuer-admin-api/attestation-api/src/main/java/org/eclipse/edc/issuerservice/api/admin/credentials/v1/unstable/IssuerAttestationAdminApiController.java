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
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.edc.identityhub.api.Versions;
import org.eclipse.edc.identityhub.spi.authorization.AuthorizationResultHandler;
import org.eclipse.edc.identityhub.spi.authorization.AuthorizationService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.issuerservice.api.admin.credentials.v1.unstable.model.AttestationDefinitionRequest;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionService;
import org.eclipse.edc.issuerservice.spi.issuance.model.AttestationDefinition;
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
@Path(Versions.UNSTABLE + "/participants/{participantContextId}/attestations")
public class IssuerAttestationAdminApiController implements IssuerAttestationAdminApi {

    private final AuthorizationService authorizationService;
    private final AttestationDefinitionService attestationDefinitionService;

    public IssuerAttestationAdminApiController(AuthorizationService authorizationService, AttestationDefinitionService attestationDefinitionService) {
        this.authorizationService = authorizationService;
        this.attestationDefinitionService = attestationDefinitionService;
    }

    @POST
    @Path("/{attestationDefinitionId}/link")
    @Override
    public Response linkAttestation(@PathParam("attestationDefinitionId") String attestationDefinitionId,
                                    @QueryParam("holderId") String holderId) {

        if (holderId == null) {
            throw new InvalidRequestException("holderId is null");
        }

        var wasCreated = attestationDefinitionService.linkAttestation(attestationDefinitionId, holderId)
                .orElseThrow(InvalidRequestException::new);

        return wasCreated
                ? Response.created(URI.create("/attestations/" + attestationDefinitionId)).build()
                : Response.noContent().build();


    }

    @POST
    @Path("/{attestationDefinitionId}/unlink")
    @Override
    public Response unlinkAttestation(@PathParam("attestationDefinitionId") String attestationDefinitionId,
                                      @QueryParam("holderId") String holderId) {

        if (holderId == null) {
            throw new InvalidRequestException("holderId is null");
        }


        var wasCreated = attestationDefinitionService.unlinkAttestation(holderId, attestationDefinitionId)
                .orElseThrow(InvalidRequestException::new);

        return wasCreated
                ? Response.ok().build()
                : Response.noContent().build();
    }

    @POST
    @Override
    public Response createAttestationDefinition(@PathParam("participantContextId") String participantContextId, AttestationDefinitionRequest attestationRequest, @Context SecurityContext securityContext) {

        var decodedParticipantId = onEncoded(participantContextId).orElseThrow(InvalidRequestException::new);

        return authorizationService.isAuthorized(securityContext, decodedParticipantId, ParticipantContext.class)
                .compose(u -> attestationDefinitionService.createAttestation(createAttestationDefinition(decodedParticipantId, attestationRequest)))
                .map(u -> Response.created(URI.create(Versions.UNSTABLE + "/participants/%s/attestations/%s".formatted(participantContextId, attestationRequest.id()))).build())
                .orElseThrow(AuthorizationResultHandler.exceptionMapper(AttestationDefinition.class));
    }

    @DELETE
    @Path("/{attestationDefinitionId}")
    @Override
    public void deleteAttestationDefinition(@PathParam("attestationDefinitionId") String attestationDefinitionId, @Context SecurityContext context) {
        authorizationService.isAuthorized(context, attestationDefinitionId, AttestationDefinition.class)
                .compose(u -> attestationDefinitionService.deleteAttestation(attestationDefinitionId))
                .orElseThrow(exceptionMapper(AttestationDefinition.class, attestationDefinitionId));
    }

    @GET
    @Override
    public Collection<AttestationDefinition> getAttestationDefinitionsForHolder(@QueryParam("holderId") String holderId) {
        if (holderId == null) {
            throw new InvalidRequestException("holderId is null");
        }
        return attestationDefinitionService.getAttestationsForHolder(holderId)
                .orElseThrow(InvalidRequestException::new);
    }

    @POST
    @Path("/query")
    @Override
    public Collection<AttestationDefinition> queryAttestationDefinitions(@PathParam("participantContextId") String participantContextId, QuerySpec query, @Context SecurityContext context) {
        var decodedParticipantId = onEncoded(participantContextId).orElseThrow(InvalidRequestException::new);
        var spec = query.toBuilder().filter(filterByParticipantContextId(decodedParticipantId)).build();

        var attestations = attestationDefinitionService.queryAttestations(spec)
                .orElseThrow(exceptionMapper(AttestationDefinition.class, null));

        return attestations.stream()
                .filter(attestation -> authorizationService.isAuthorized(context, attestation.getId(), AttestationDefinition.class).succeeded())
                .toList();
    }

    private AttestationDefinition createAttestationDefinition(String participantContextId, AttestationDefinitionRequest attestationRequest) {
        return AttestationDefinition.Builder.newInstance()
                .participantContextId(participantContextId)
                .attestationType(attestationRequest.attestationType())
                .id(attestationRequest.id())
                .configuration(attestationRequest.configuration())
                .build();
    }
}
