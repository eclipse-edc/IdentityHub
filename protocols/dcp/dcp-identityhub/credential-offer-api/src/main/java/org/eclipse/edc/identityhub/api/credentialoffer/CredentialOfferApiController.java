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
import org.eclipse.edc.identityhub.protocols.dcp.spi.DcpIssuerTokenVerifier;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialOfferMessage;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialObject;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialOffer;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialOfferStatus;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.offer.CredentialOfferService;
import org.eclipse.edc.jsonld.spi.JsonLd;
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
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialOfferMessage.CREDENTIAL_OFFER_MESSAGE_TERM;
import static org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextId.onEncoded;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v1/participants/{participantContextId}/offers")
public class CredentialOfferApiController implements CredentialOfferApi {

    private final JsonObjectValidatorRegistry validatorRegistry;
    private final TypeTransformerRegistry transformerRegistry;
    private final DcpIssuerTokenVerifier issuerTokenVerifier;
    private final ParticipantContextService participantContextService;
    private final CredentialOfferService credentialOfferService;
    private final JsonLd jsonLd;

    public CredentialOfferApiController(JsonObjectValidatorRegistry validatorRegistry,
                                        TypeTransformerRegistry transformerRegistry,
                                        DcpIssuerTokenVerifier issuerTokenVerifier,
                                        ParticipantContextService participantContextService, CredentialOfferService credentialOfferService, JsonLd jsonLd) {
        this.validatorRegistry = validatorRegistry;
        this.transformerRegistry = transformerRegistry;
        this.issuerTokenVerifier = issuerTokenVerifier;
        this.participantContextService = participantContextService;
        this.credentialOfferService = credentialOfferService;
        this.jsonLd = jsonLd;
    }


    @POST
    @Override
    public void offerCredential(@PathParam("participantContextId") String participantContextId, JsonObject credentialOfferMessage, @HeaderParam(AUTHORIZATION) String authHeader) {
        if (credentialOfferMessage == null) {
            throw new InvalidRequestException("Request body is null");
        }
        if (authHeader == null) {
            throw new AuthenticationFailedException("Authorization header missing");
        }
        if (!authHeader.startsWith("Bearer ")) {
            throw new AuthenticationFailedException("Invalid authorization header, must start with 'Bearer'");
        }
        var authToken = authHeader.replace("Bearer ", "").trim();
        var expanded = jsonLd.expand(credentialOfferMessage).orElseThrow(InvalidRequestException::new);
        validatorRegistry.validate(DSPACE_DCP_NAMESPACE_V_1_0.toIri(CREDENTIAL_OFFER_MESSAGE_TERM), expanded).orElseThrow(ValidationFailureException::new);
        var protocolRegistry = transformerRegistry.forContext(DCP_SCOPE_V_1_0);

        participantContextId = onEncoded(participantContextId).orElseThrow(InvalidRequestException::new);

        var offerMessage = protocolRegistry.forContext(DCP_SCOPE_V_1_0).transform(expanded, CredentialOfferMessage.class).orElseThrow(InvalidRequestException::new);

        var participantContext = participantContextService.getParticipantContext(participantContextId)
                .orElseThrow((f) -> new AuthenticationFailedException("Invalid participant"));

        // validate Issuer's SI token
        issuerTokenVerifier.verify(participantContext, authToken)
                .orElseThrow(f -> new NotAuthorizedException("ID token verification failed: %s".formatted(f.getFailureDetail())));

        var credentialOffer = CredentialOffer.Builder.newInstance()
                .participantContextId(participantContextId)
                .issuer(offerMessage.getIssuer())
                .credentialObjects(offerMessage.getCredentials().stream().map(co -> CredentialObject.Builder.newInstance()
                        .bindingMethods(co.getBindingMethods())
                        .credentialType(co.getCredentialType())
                        .issuancePolicy(co.getIssuancePolicy())
                        .offerReason(co.getOfferReason())
                        .profile(co.getProfile())
                        .build()).toList())
                .state(CredentialOfferStatus.RECEIVED.code())
                .build();
        credentialOfferService.create(credentialOffer).orElseThrow(exceptionMapper(CredentialOffer.class, credentialOffer.getId()));
    }

}
