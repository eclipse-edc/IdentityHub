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

package org.eclipse.edc.identityhub.api.verifiablecredentials.v1.unstable;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import org.eclipse.edc.identityhub.api.Versions;
import org.eclipse.edc.identityhub.spi.authentication.ServicePrincipal;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;

import java.util.Collection;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.identityhub.spi.authorization.AuthorizationResultHandler.exceptionMapper;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(Versions.UNSTABLE + "/credentials")
public class GetAllCredentialsApiController implements GetAllCredentialsApi {
    private final CredentialStore credentialStore;

    public GetAllCredentialsApiController(CredentialStore credentialStore) {
        this.credentialStore = credentialStore;
    }

    @GET
    @RolesAllowed(ServicePrincipal.ROLE_ADMIN)
    @Override
    public Collection<VerifiableCredentialResource> getAllCredentials(@DefaultValue("0") @QueryParam("offset") Integer offset,
                                                                      @DefaultValue("50") @QueryParam("limit") Integer limit) {
        var res = credentialStore.query(QuerySpec.Builder.newInstance().limit(limit).offset(offset).build());
        return ServiceResult.from(res).orElseThrow(exceptionMapper(VerifiableCredentialResource.class));
    }
}
