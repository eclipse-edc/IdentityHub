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
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.identityhub.api.Versions;
import org.eclipse.edc.issuerservice.api.admin.credentials.v1.unstable.model.CredentialStatusResponse;
import org.eclipse.edc.issuerservice.api.admin.credentials.v1.unstable.model.VerifiableCredentialResponse;
import org.eclipse.edc.spi.query.QuerySpec;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(Versions.UNSTABLE)
public class IssuerCredentialsAdminApiController implements IssuerCredentialsAdminApi {


    @GET
    @Path("/participants/{participantId}/credentials")
    @Override
    public VerifiableCredentialResponse getAllCredentials(@PathParam("participantId") String participantId) {
        return new VerifiableCredentialResponse(CredentialFormat.VC1_0_JWT, VerifiableCredential.Builder.newInstance().build());
    }

    @POST
    @Path("/credentials")
    @Override
    public VerifiableCredentialResponse queryCredentials(QuerySpec query) {
        return null;
    }

    @POST
    @Path("/participants/{participantId}/credentials/{credentialId}/revoke")
    @Override
    public Response revokeCredential(@PathParam("participantId") String participantId,
                                     @PathParam("credentialId") String credentialId) {
        return Response.noContent().build();
    }

    @POST
    @Path("/participants/{participantId}/credentials/{credentialId}/suspend")
    @Override
    public Response suspendCredential(@PathParam("participantId") String participantId,
                                      @PathParam("credentialId") String credentialId) {
        return null;
    }

    @POST
    @Path("/participants/{participantId}/credentials/{credentialId}/resume")
    @Override
    public Response resumeCredential(@PathParam("participantId") String participantId,
                                     @PathParam("credentialId") String credentialId) {
        return null;
    }

    @GET
    @Path("/participants/{participantId}/credentials/{credentialId}/status")
    @Override
    public CredentialStatusResponse checkRevocationStatus(@PathParam("participantId") String participantId,
                                                          @PathParam("credentialId") String credentialId) {
        return null;
    }
}
