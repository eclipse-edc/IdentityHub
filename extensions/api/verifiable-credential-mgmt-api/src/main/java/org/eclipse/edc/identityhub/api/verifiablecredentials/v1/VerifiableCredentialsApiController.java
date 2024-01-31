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

package org.eclipse.edc.identityhub.api.verifiablecredentials.v1;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.edc.identityhub.spi.AuthorizationService;
import org.eclipse.edc.identityhub.spi.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.store.CredentialStore;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.NotAuthorizedException;
import org.eclipse.edc.web.spi.exception.ObjectNotFoundException;

import java.util.Collection;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.identityhub.spi.AuthorizationResultHandler.exceptionMapper;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v1/credentials")
public class VerifiableCredentialsApiController implements VerifiableCredentialsApi {

    private final CredentialStore credentialStore;
    private final AuthorizationService authorizationService;

    public VerifiableCredentialsApiController(CredentialStore credentialStore, AuthorizationService authorizationService) {
        this.credentialStore = credentialStore;
        this.authorizationService = authorizationService;
    }

    @GET
    @Path("/{credentialId}")
    @Override
    public Collection<VerifiableCredentialResource> findById(@PathParam("credentialId") String id, @Context SecurityContext securityContext) {

        if (id == null) {
            if (authorizationService.hasElevatedPrivilege(securityContext.getUserPrincipal())) {
                return ServiceResult.from(credentialStore.query(QuerySpec.max()))
                        .orElseThrow(exceptionMapper(VerifiableCredentialResource.class));
            }
            throw new NotAuthorizedException("Not authorized to get all keypairs.");

        }


        authorizationService.isAuthorized(securityContext.getUserPrincipal(), id, VerifiableCredentialResource.class)
                .orElseThrow(exceptionMapper(VerifiableCredentialResource.class, id));

        var result = credentialStore.query(QuerySpec.Builder.newInstance().filter(new Criterion("id", "=", id)).build())
                .orElseThrow(InvalidRequestException::new);
        if (result.isEmpty()) {
            throw new ObjectNotFoundException(VerifiableCredentialResource.class, id);
        }
        return result;
    }

    @GET
    @Override
    public Collection<VerifiableCredentialResource> findByType(@QueryParam("type") String type, @Context SecurityContext securityContext) {
        var query = QuerySpec.Builder.newInstance()
                .filter(new Criterion("verifiableCredential.credential.types", "contains", type))
                .build();

        return credentialStore.query(query)
                .orElseThrow(InvalidRequestException::new)
                .stream().filter(vcr -> authorizationService.isAuthorized(securityContext.getUserPrincipal(), vcr.getId(), VerifiableCredentialResource.class).succeeded())
                .toList();
    }

    @DELETE
    @Path("/{credentialId}")
    @Override
    public void deleteCredential(@PathParam("credentialId") String id, @Context SecurityContext securityContext) {
        authorizationService.isAuthorized(securityContext.getUserPrincipal(), id, VerifiableCredentialResource.class)
                .orElseThrow(exceptionMapper(VerifiableCredentialResource.class, id));
        var res = credentialStore.deleteById(id);
        if (res.failed()) {
            throw exceptionMapper(VerifiableCredentialResource.class, id).apply(ServiceResult.fromFailure(res).getFailure());
        }
    }
}
