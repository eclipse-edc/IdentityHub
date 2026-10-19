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
import org.eclipse.edc.api.auth.spi.AuthorizationService;
import org.eclipse.edc.api.auth.spi.RequiredScope;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.identityhub.api.Versions;
import org.eclipse.edc.identityhub.spi.did.DidDocumentService;
import org.eclipse.edc.identityhub.spi.did.model.DidResource;
import org.eclipse.edc.identityhub.spi.did.model.DidState;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.util.string.StringUtils;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.identityhub.spi.authorization.AuthorizationResultHandler.exceptionMapper;
import static org.eclipse.edc.participantcontext.spi.types.ParticipantResource.filterByParticipantContextId;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(Versions.STABLE + "/participants/{participantContextId}/dids")
public class DidManagementApiController implements DidManagementApi {

    private final DidDocumentService documentService;
    private final AuthorizationService authorizationService;

    public DidManagementApiController(DidDocumentService documentService, AuthorizationService authorizationService) {
        this.documentService = documentService;
        this.authorizationService = authorizationService;
    }

    @Override
    @POST
    @RequiredScope("identity-api:dids:write")
    @Path("/publish")
    public void publishDid(@PathParam("participantContextId") String participantContextId, DidRequestPayload didRequestPayload, @Context SecurityContext securityContext) {
        var did = requireDid(didRequestPayload);
        authorizationService.authorize(securityContext, participantContextId, did, DidResource.class)
                .compose(u -> documentService.publish(did))
                .orElseThrow(exceptionMapper(DidResource.class, did));
    }

    @Override
    @POST
    @RequiredScope("identity-api:dids:write")
    @Path("/unpublish")
    public void unpublishDid(@PathParam("participantContextId") String participantContextId, DidRequestPayload didRequestPayload, @Context SecurityContext securityContext) {
        var did = requireDid(didRequestPayload);
        authorizationService.authorize(securityContext, participantContextId, did, DidResource.class)
                .compose(u -> documentService.unpublish(did))
                .orElseThrow(exceptionMapper(DidDocument.class, did));
    }


    @POST
    @Path("/query")
    @RequiredScope("identity-api:dids:read")
    @Override
    public Collection<DidDocument> queryDids(@PathParam("participantContextId") String participantContextId, QuerySpec querySpec, @Context SecurityContext securityContext) {
        var spec = querySpec.toBuilder().filter(filterByParticipantContextId(participantContextId)).build();
        return documentService.queryDocuments(spec)
                .orElseThrow(exceptionMapper(DidDocument.class, null))
                .stream().filter(dd -> authorizationService.authorize(securityContext, participantContextId, dd.getId(), DidResource.class).succeeded())
                .toList();
    }

    @Override
    @POST
    @RequiredScope("identity-api:dids:read")
    @Path("/state")
    public String getDidState(@PathParam("participantContextId") String participantContextId, DidRequestPayload request, @Context SecurityContext securityContext) {
        var did = requireDid(request);
        authorizationService.authorize(securityContext, participantContextId, did, DidResource.class)
                .orElseThrow(exceptionMapper(DidResource.class, did));
        var byId = documentService.findById(did);
        return byId != null ? DidState.from(byId.getState()).toString() : null;
    }

    @Override
    @POST
    @RequiredScope("identity-api:dids:write")
    @Path("/{did}/endpoints")
    public void addDidEndpoint(@PathParam("participantContextId") String participantContextId,
                               @PathParam("did") String did,
                               Service service,
                               @QueryParam("autoPublish") boolean autoPublish,
                               @Context SecurityContext securityContext) {
        var decodedDid = decodeDid(did);
        authorizationService.authorize(securityContext, participantContextId, decodedDid, DidResource.class)
                .compose(u -> documentService.addService(decodedDid, service))
                .compose(v -> autoPublish ? documentService.publish(decodedDid) : ServiceResult.success())
                .orElseThrow(exceptionMapper(Service.class, decodedDid));
    }

    @Override
    @PATCH
    @RequiredScope("identity-api:dids:write")
    @Path("/{did}/endpoints")
    public void replaceDidEndpoint(@PathParam("participantContextId") String participantContextId,
                                   @PathParam("did") String did,
                                   Service service,
                                   @QueryParam("autoPublish") boolean autoPublish,
                                   @Context SecurityContext securityContext) {
        var decodedDid = decodeDid(did);

        authorizationService.authorize(securityContext, participantContextId, decodedDid, DidResource.class)
                .compose(u -> documentService.replaceService(decodedDid, service))
                .compose(v -> autoPublish ? documentService.publish(decodedDid) : ServiceResult.success())
                .orElseThrow(exceptionMapper(Service.class, decodedDid));
    }


    @Override
    @DELETE
    @RequiredScope("identity-api:dids:write")
    @Path("/{did}/endpoints")
    public void deleteDidEndpoint(@PathParam("participantContextId") String participantContextId,
                                  @PathParam("did") String did,
                                  @QueryParam("serviceId") String serviceId,
                                  @QueryParam("autoPublish") boolean autoPublish,
                                  @Context SecurityContext securityContext) {
        var decodedDid = decodeDid(did);

        authorizationService.authorize(securityContext, participantContextId, decodedDid, DidResource.class)
                .compose(u -> documentService.removeService(decodedDid, serviceId))
                .compose(v -> autoPublish ? documentService.publish(decodedDid) : ServiceResult.success())
                .orElseThrow(exceptionMapper(Service.class, decodedDid));
    }

    private static String requireDid(DidRequestPayload payload) {
        if (payload == null || StringUtils.isNullOrBlank(payload.did())) {
            throw new InvalidRequestException("DID cannot be null or empty.");
        }
        return payload.did();
    }

    private static String decodeDid(String encoded) {
        try {
            return new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("DID path parameter is not valid Base64url.");
        }
    }
}