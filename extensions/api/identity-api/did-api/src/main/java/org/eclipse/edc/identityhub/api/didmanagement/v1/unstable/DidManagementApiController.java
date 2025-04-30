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

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.identityhub.api.Versions;
import org.eclipse.edc.identityhub.spi.authorization.AuthorizationService;
import org.eclipse.edc.identityhub.spi.did.DidDocumentService;
import org.eclipse.edc.identityhub.spi.did.model.DidResource;
import org.eclipse.edc.identityhub.spi.did.model.DidState;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;

import java.util.Collection;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.identityhub.spi.authorization.AuthorizationResultHandler.exceptionMapper;
import static org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextId.onEncoded;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(Versions.UNSTABLE + "/participants/{participantContextId}/dids")
public class DidManagementApiController implements DidManagementApi {

    private final DidDocumentService documentService;
    private final AuthorizationService authorizationService;

    public DidManagementApiController(DidDocumentService documentService, AuthorizationService authorizationService) {
        this.documentService = documentService;
        this.authorizationService = authorizationService;
    }

    @Override
    @POST
    @Path("/publish")
    public void publishDid(@PathParam("participantContextId") String participantContextId, DidRequestPayload didRequestPayload, @Context SecurityContext securityContext) {
        authorizationService.isAuthorized(securityContext, didRequestPayload.did(), DidResource.class)
                .compose(u -> documentService.publish(didRequestPayload.did()))
                .orElseThrow(exceptionMapper(DidResource.class, didRequestPayload.did()));
    }

    @Override
    @POST
    @Path("/unpublish")
    public void unpublishDid(@PathParam("participantContextId") String participantContextId, DidRequestPayload didRequestPayload, @Context SecurityContext securityContext) {
        authorizationService.isAuthorized(securityContext, didRequestPayload.did(), DidResource.class)
                .compose(u -> documentService.unpublish(didRequestPayload.did()))
                .orElseThrow(exceptionMapper(DidDocument.class, didRequestPayload.did()));
    }


    @POST
    @Path("/query")
    @Override
    public Collection<DidDocument> queryDids(@PathParam("participantContextId") String participantContextId, QuerySpec querySpec, @Context SecurityContext securityContext) {
        return documentService.queryDocuments(querySpec)
                .orElseThrow(exceptionMapper(DidDocument.class, null))
                .stream().filter(dd -> authorizationService.isAuthorized(securityContext, dd.getId(), DidResource.class).succeeded())
                .toList();
    }

    @Override
    @POST
    @Path("/state")
    public String getDidState(@PathParam("participantContextId") String participantContextId, DidRequestPayload request, @Context SecurityContext securityContext) {
        authorizationService.isAuthorized(securityContext, request.did(), DidResource.class)
                .orElseThrow(exceptionMapper(DidResource.class, request.did()));
        var byId = documentService.findById(request.did());
        return byId != null ? DidState.from(byId.getState()).toString() : null;
    }

    @Override
    @POST
    @Path("/{did}/endpoints")
    public void addDidEndpoint(@PathParam("participantContextId") String participantContextId,
                               @PathParam("did") String did, Service service,
                               @QueryParam("autoPublish") boolean autoPublish,
                               @Context SecurityContext securityContext) {
        var decodedDid = onEncoded(did).orElseThrow(InvalidRequestException::new);
        authorizationService.isAuthorized(securityContext, decodedDid, DidResource.class)
                .compose(u -> documentService.addService(decodedDid, service))
                .compose(v -> autoPublish ? documentService.publish(decodedDid) : ServiceResult.success())
                .orElseThrow(exceptionMapper(Service.class, decodedDid));
    }

    @Override
    @PATCH
    @Path("/{did}/endpoints")
    public void replaceDidEndpoint(@PathParam("participantContextId") String participantContextId,
                                   @PathParam("did") String did,
                                   Service service,
                                   @QueryParam("autoPublish") boolean autoPublish,
                                   @Context SecurityContext securityContext) {
        var decodedDid = onEncoded(did).orElseThrow(InvalidRequestException::new);

        authorizationService.isAuthorized(securityContext, decodedDid, DidResource.class)
                .compose(u -> documentService.replaceService(decodedDid, service))
                .compose(v -> autoPublish ? documentService.publish(decodedDid) : ServiceResult.success())
                .orElseThrow(exceptionMapper(Service.class, decodedDid));
    }

    @Override
    @DELETE
    @Path("/{did}/endpoints")
    public void deleteDidEndpoint(@PathParam("participantContextId") String participantContextId,
                                  @PathParam("did") String did,
                                  @QueryParam("serviceId") String serviceId,
                                  @QueryParam("autoPublish") boolean autoPublish,
                                  @Context SecurityContext securityContext) {
        var decodedDid = onEncoded(did).orElseThrow(InvalidRequestException::new);

        authorizationService.isAuthorized(securityContext, decodedDid, DidResource.class)
                .compose(u -> documentService.removeService(decodedDid, serviceId))
                .compose(v -> autoPublish ? documentService.publish(decodedDid) : ServiceResult.success())
                .orElseThrow(exceptionMapper(Service.class, decodedDid));
    }

}