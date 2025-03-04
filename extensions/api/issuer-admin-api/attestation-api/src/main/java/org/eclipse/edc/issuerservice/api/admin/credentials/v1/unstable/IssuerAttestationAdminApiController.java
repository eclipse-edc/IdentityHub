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
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.identityhub.api.Versions;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionService;
import org.eclipse.edc.issuerservice.spi.issuance.model.AttestationDefinition;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;

import java.net.URI;
import java.util.Collection;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(Versions.UNSTABLE + "/attestations")
public class IssuerAttestationAdminApiController implements IssuerAttestationAdminApi {

    private final AttestationDefinitionService attestationDefinitionService;

    public IssuerAttestationAdminApiController(AttestationDefinitionService attestationDefinitionService) {
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
    public Response createAttestationDefinition(AttestationDefinition attestationDefinition) {
        attestationDefinitionService.createAttestation(attestationDefinition)
                .orElseThrow(exceptionMapper(AttestationDefinition.class, null));
        return Response.created(URI.create("/attestations/" + attestationDefinition.id())).build();
    }


    @DELETE
    @Path("/{attestationDefinitionId}")
    @Override
    public Response deleteAttestationDefinition(@PathParam("attestationDefinitionId") String attestationDefinitionId) {
        attestationDefinitionService.deleteAttestation(attestationDefinitionId)
                .orElseThrow(InvalidRequestException::new);
        return Response.noContent().build();
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
    public Collection<AttestationDefinition> queryAttestationDefinitions(QuerySpec query) {
        var result = attestationDefinitionService.queryAttestations(query);
        return result.orElseThrow(exceptionMapper(AttestationDefinition.class, null));
    }
}
