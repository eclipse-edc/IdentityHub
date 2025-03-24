/*
 *  Copyright (c) 2025 Metaform Systems Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.api.credentialoffer;

import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.identityhub.protocols.dcp.spi.DcpIssuerTokenVerifier;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialOfferMessage;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.exception.AuthenticationFailedException;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.NotAuthorizedException;
import org.eclipse.edc.web.spi.exception.ValidationFailureException;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.iam.identitytrust.spi.DcpConstants.DSPACE_DCP_NAMESPACE_V_1_0;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.DcpConstants.DCP_SCOPE_V_1_0;
import static org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextId.onEncoded;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v1/participants/{participantContextId}/offers")
public class CredentialOfferApiController implements CredentialOfferApi {

    private final JsonObjectValidatorRegistry validatorRegistry;
    private final TypeTransformerRegistry transformerRegistry;
    private final Monitor monitor;
    private final DcpIssuerTokenVerifier issuerTokenVerifier;
    private final ParticipantContextService participantContextService;

    public CredentialOfferApiController(JsonObjectValidatorRegistry validatorRegistry,
                                        TypeTransformerRegistry transformerRegistry,
                                        Monitor monitor,
                                        DcpIssuerTokenVerifier issuerTokenVerifier,
                                        ParticipantContextService participantContextService) {
        this.validatorRegistry = validatorRegistry;
        this.transformerRegistry = transformerRegistry;
        this.monitor = monitor;
        this.issuerTokenVerifier = issuerTokenVerifier;
        this.participantContextService = participantContextService;
    }


    @POST
    @Override
    public Response offerCredential(@PathParam("participantContextId") String participantContextId, JsonObject credentialOfferMessage, @HeaderParam(AUTHORIZATION) String authHeader) {
        if (credentialOfferMessage == null) {
            throw new InvalidRequestException("Request body is null");
        }
        if (authHeader == null) {
            throw new AuthenticationFailedException("Authorization header missing");
        }
        var authToken = authHeader.replace("Bearer", "").trim();
        validatorRegistry.validate(DSPACE_DCP_NAMESPACE_V_1_0.toIri(CredentialOfferMessage.CREDENTIAL_OFFER_MESSAGE_TERM), credentialOfferMessage).orElseThrow(ValidationFailureException::new);
        var protocolRegistry = transformerRegistry.forContext(DCP_SCOPE_V_1_0);

        participantContextId = onEncoded(participantContextId).orElseThrow(InvalidRequestException::new);

        var offerMessage = protocolRegistry.forContext(DCP_SCOPE_V_1_0).transform(credentialOfferMessage, CredentialOfferMessage.class).orElseThrow(InvalidRequestException::new);

        var participantContext = participantContextService.getParticipantContext(participantContextId)
                .orElseThrow((f) -> new AuthenticationFailedException("Invalid participant"));

        // validate Issuer's SI token
        issuerTokenVerifier.verify(participantContext, authToken)
                .orElseThrow(f -> new NotAuthorizedException("ID token verification failed: %s".formatted(f.getFailureDetail())));


        //todo: process credential offer message
        monitor.warning("Credential offer was received but processing it is not yet implemented");
        return Response.ok().build();
    }

}
