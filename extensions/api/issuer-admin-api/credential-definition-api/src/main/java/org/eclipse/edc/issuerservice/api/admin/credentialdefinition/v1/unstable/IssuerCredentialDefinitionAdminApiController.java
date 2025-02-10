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
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.identityhub.api.Versions;
import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialDefinition;
import org.eclipse.edc.issuerservice.spi.issuance.credentialdefinition.CredentialDefinitionService;
import org.eclipse.edc.spi.query.QuerySpec;

import java.net.URI;
import java.util.Collection;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(Versions.UNSTABLE + "/credentialdefinitions")
public class IssuerCredentialDefinitionAdminApiController implements IssuerCredentialDefinitionAdminApi {

    private final CredentialDefinitionService credentialDefinitionService;

    public IssuerCredentialDefinitionAdminApiController(CredentialDefinitionService credentialDefinitionService) {
        this.credentialDefinitionService = credentialDefinitionService;
    }

    @POST
    @Override
    public Response createCredentialDefinition(CredentialDefinition credentialDefinition) {
        return credentialDefinitionService.createCredentialDefinition(credentialDefinition)
                .map(v -> Response.created(URI.create(Versions.UNSTABLE + "/credentialdefinitions/" + credentialDefinition.getId())).build())
                .orElseThrow(exceptionMapper(CredentialDefinition.class, credentialDefinition.getId()));
    }

    @PUT
    @Override
    public Response updateCredentialDefinition(CredentialDefinition credentialDefinition) {
        return credentialDefinitionService.updateCredentialDefinition(credentialDefinition)
                .map(v -> Response.ok().build())
                .orElseThrow(exceptionMapper(CredentialDefinition.class, credentialDefinition.getId()));
    }

    @GET
    @Path("/{credentialDefinitionId}")
    @Override
    public CredentialDefinition getCredentialDefinitionById(@PathParam("credentialDefinitionId") String credentialDefinitionId) {
        return credentialDefinitionService.findCredentialDefinitionById(credentialDefinitionId)
                .orElseThrow(exceptionMapper(CredentialDefinition.class, credentialDefinitionId));
    }

    @POST
    @Path("/query")
    @Override
    public Collection<CredentialDefinition> queryCredentialDefinitions(QuerySpec querySpec) {
        return credentialDefinitionService.queryCredentialDefinitions(querySpec)
                .orElseThrow(exceptionMapper(CredentialDefinition.class, null));
    }

    @DELETE
    @Path("/{credentialDefinitionId}")
    @Override
    public void deleteCredentialDefinitionById(@PathParam("credentialDefinitionId") String credentialDefinitionId) {
        credentialDefinitionService.deleteCredentialDefinition(credentialDefinitionId)
                .orElseThrow(exceptionMapper(CredentialDefinition.class, credentialDefinitionId));
    }
}
