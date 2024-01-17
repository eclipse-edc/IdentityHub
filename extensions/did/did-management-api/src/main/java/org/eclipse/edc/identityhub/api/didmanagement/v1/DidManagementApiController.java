/*
 *  Copyright (c) 2023 Metaform Systems, Inc.
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

package org.eclipse.edc.identityhub.api.didmanagement.v1;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.identithub.did.spi.DidDocumentService;
import org.eclipse.edc.identithub.did.spi.model.DidState;
import org.eclipse.edc.spi.query.QuerySpec;

import java.util.Collection;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v1/dids")
public class DidManagementApiController implements DidManagementApi {

    private final DidDocumentService documentService;

    public DidManagementApiController(DidDocumentService documentService) {
        this.documentService = documentService;
    }


    @Override
    @POST
    @Path("/publish")
    public void publishDidFromBody(DidRequestPayload didRequestPayload) {
        documentService.publish(didRequestPayload.did())
                .orElseThrow(exceptionMapper(DidDocument.class, didRequestPayload.did()));
    }

    @Override
    @POST
    @Path("/unpublish")
    public void unpublishDidFromBody(DidRequestPayload didRequestPayload) {
        documentService.unpublish(didRequestPayload.did())
                .orElseThrow(exceptionMapper(DidDocument.class, didRequestPayload.did()));
    }


    @POST
    @Path("/query")
    @Override
    public Collection<DidDocument> queryDid(QuerySpec querySpec) {
        return documentService.queryDocuments(querySpec)
                .orElseThrow(exceptionMapper(DidDocument.class, null));
    }

    @Override
    @POST
    @Path("/state")
    public String getState(DidRequestPayload request) {
        var byId = documentService.findById(request.did());
        return byId != null ? DidState.from(byId.getState()).toString() : null;
    }

    @Override
    @POST
    @Path("/{did}/endpoints")
    public void addEndpoint(@PathParam("did") String did, Service service) {
        documentService.addService(did, service)
                .orElseThrow(exceptionMapper(Service.class, did));
    }

    @Override
    @PATCH
    @Path("/{did}/endpoints")
    public void replaceEndpoint(@PathParam("did") String did, Service service) {
        documentService.replaceService(did, service)
                .orElseThrow(exceptionMapper(Service.class, did));
    }

    @Override
    @DELETE
    @Path("/{did}/endpoints")
    public void removeEndpoint(@PathParam("did") String did, @QueryParam("serviceId") String serviceId) {
        documentService.removeService(did, serviceId)
                .orElseThrow(exceptionMapper(Service.class, did));
    }

}
