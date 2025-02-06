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
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.identityhub.api.Versions;
import org.eclipse.edc.issuerservice.api.admin.credentials.v1.unstable.model.CredentialStatusResponse;
import org.eclipse.edc.issuerservice.api.admin.credentials.v1.unstable.model.VerifiableCredentialResponse;
import org.eclipse.edc.issuerservice.spi.statuslist.StatusListService;
import org.eclipse.edc.spi.query.QuerySpec;

import java.util.Collection;
import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(Versions.UNSTABLE + "/credentials")
public class IssuerCredentialsAdminApiController implements IssuerCredentialsAdminApi {
    private final StatusListService statuslistService;

    public IssuerCredentialsAdminApiController(StatusListService statuslistService) {
        this.statuslistService = statuslistService;
    }

    @GET
    @Path("/{participantId}")
    @Override
    public Collection<VerifiableCredentialResponse> getAllCredentials(@PathParam("participantId") String participantId) {
        return List.of();
    }

    @POST
    @Override
    public VerifiableCredentialResponse queryCredentials(QuerySpec query) {
        return null;
    }

    @POST
    @Path("/{credentialId}/revoke")
    @Override
    public Response revokeCredential(@PathParam("credentialId") String credentialId) {
        return statuslistService.revokeCredential(credentialId)
                .map(v -> Response.noContent().build())
                .orElseThrow(exceptionMapper(VerifiableCredential.class, credentialId));
    }

    @POST
    @Path("/{credentialId}/suspend")
    @Override
    public Response suspendCredential(@PathParam("credentialId") String credentialId) {
        return Response.status(501).build();
    }

    @POST
    @Path("/{credentialId}/resume")
    @Override
    public Response resumeCredential(@PathParam("credentialId") String credentialId) {
        return Response.status(501).build();
    }

    @GET
    @Path("/{credentialId}/status")
    @Override
    public CredentialStatusResponse checkRevocationStatus(@PathParam("credentialId") String credentialId) {
        return statuslistService.getCredentialStatus(credentialId)
                .map(status -> new CredentialStatusResponse(credentialId, status, null))
                .orElseThrow(exceptionMapper(VerifiableCredential.class, credentialId));
    }
}
