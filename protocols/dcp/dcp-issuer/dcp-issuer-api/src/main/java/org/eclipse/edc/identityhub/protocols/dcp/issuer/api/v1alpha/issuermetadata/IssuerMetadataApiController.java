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

package org.eclipse.edc.identityhub.protocols.dcp.issuer.api.v1alpha.issuermetadata;

import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import org.eclipse.edc.identityhub.protocols.dcp.issuer.spi.DcpIssuerMetadataService;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.exception.AuthenticationFailedException;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextId.onEncoded;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v1alpha/participants/{participantContextId}/metadata")
public class IssuerMetadataApiController implements IssuerMetadataApi {

    private final ParticipantContextService participantContextService;
    private final DcpIssuerMetadataService issuerMetadataService;
    private final TypeTransformerRegistry dcpRegistry;


    public IssuerMetadataApiController(ParticipantContextService participantContextService, DcpIssuerMetadataService issuerMetadataService, TypeTransformerRegistry dcpRegistry) {
        this.participantContextService = participantContextService;
        this.issuerMetadataService = issuerMetadataService;
        this.dcpRegistry = dcpRegistry;
    }

    @GET
    @Path("/")
    @Override
    public JsonObject getIssuerMetadata(@PathParam("participantContextId") String participantContextId, @HeaderParam(AUTHORIZATION) String authHeader) {
        var decodedParticipantContextId = onEncoded(participantContextId).orElseThrow(InvalidRequestException::new);

        var participantContext = participantContextService.getParticipantContext(decodedParticipantContextId)
                .orElseThrow((f) -> new AuthenticationFailedException("Invalid issuer"));

        var metadata = issuerMetadataService.getIssuerMetadata(participantContext)
                .orElseThrow(f -> new EdcException("Error creating response body: " + f.getFailureDetail()));


        return dcpRegistry.transform(metadata, JsonObject.class)
                .orElseThrow(f -> new EdcException("Error creating response body: " + f.getFailureDetail()));

    }
}
