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

package org.eclipse.edc.identityhub.api.didmanagement.v1.unstable;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.identityhub.api.Versions;
import org.eclipse.edc.identityhub.spi.authentication.ServicePrincipal;
import org.eclipse.edc.identityhub.spi.did.DidDocumentService;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;

import java.util.Collection;

import static org.eclipse.edc.identityhub.spi.authorization.AuthorizationResultHandler.exceptionMapper;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path(Versions.UNSTABLE + "/dids")
public class GetAllDidsApiController implements GetAllDidsApi {
    private final DidDocumentService documentService;

    public GetAllDidsApiController(DidDocumentService documentService) {
        this.documentService = documentService;
    }

    @Override
    @GET
    @RolesAllowed(ServicePrincipal.ROLE_ADMIN)
    public Collection<DidDocument> getAllDids(@DefaultValue("0") @QueryParam("offset") Integer offset,
                                              @DefaultValue("50") @QueryParam("limit") Integer limit) {
        if (offset < 0 || limit < 0) {
            throw new InvalidRequestException("offset and limit must be > 0");
        }
        return documentService.queryDocuments(QuerySpec.Builder.newInstance().offset(offset).limit(limit).build())
                .orElseThrow(exceptionMapper(DidDocument.class));
    }
}
