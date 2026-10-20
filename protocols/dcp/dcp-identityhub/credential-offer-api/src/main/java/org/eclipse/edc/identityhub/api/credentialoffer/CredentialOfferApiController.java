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
import org.eclipse.edc.identityhub.spi.participantcontext.IdentityHubParticipantContextService;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialObject;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialOffer;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialOfferStatus;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.offer.CredentialOfferService;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.exception.AuthenticationFailedException;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.NotAuthorizedException;
import org.eclipse.edc.web.spi.exception.ValidationFailureException;

import java.util.Objects;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.iam.decentralizedclaims.spi.DcpConstants.DSPACE_DCP_NAMESPACE_V_1_0;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.DcpConstants.DCP_SCOPE_V_1_0;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialOfferMessage.CREDENTIAL_OFFER_MESSAGE_TERM;
import static org.eclipse.edc.identityhub.verification.AuthenticationFailures.idTokenVerificationFailed;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v1/participants/{participantContextId}/offers")
public class CredentialOfferApiController implements CredentialOfferApi {

    private final JsonObjectValidatorRegistry validatorRegistry;
    private final TypeTransformerRegistry transformerRegistry;
    private final DcpIssuerTokenVerifier issuerTokenVerifier;
    private final IdentityHubParticipantContextService participantContextService;
    private final CredentialOfferService credentialOfferService;
    private final JsonLd jsonLd;
    private final CredentialObjectResolver credentialObjectResolver;
    private final Monitor monitor;

    public CredentialOfferApiController(JsonObjectValidatorRegistry validatorRegistry,
                                        TypeTransformerRegistry transformerRegistry,
                                        DcpIssuerTokenVerifier issuerTokenVerifier,
                                        IdentityHubParticipantContextService participantContextService, CredentialOfferService credentialOfferService, JsonLd jsonLd,
                                        CredentialObjectResolver credentialObjectResolver, Monitor monitor) {
        this.validatorRegistry = validatorRegistry;
        this.transformerRegistry = transformerRegistry;
        this.issuerTokenVerifier = issuerTokenVerifier;
        this.participantContextService = participantContextService;
        this.credentialOfferService = credentialOfferService;
        this.jsonLd = jsonLd;
        this.credentialObjectResolver = credentialObjectResolver;
        this.monitor = monitor;
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

        var participantContext = participantContextService.rejectIfDeactivated(participantContextId)
                .orElseThrow(idTokenVerificationFailed(monitor, "Participant context '%s' could not be resolved".formatted(participantContextId)));

        // validate Issuer's SI token, before the offer is processed
        var issuerClaims = issuerTokenVerifier.verify(participantContext, authToken)
                .orElseThrow(idTokenVerificationFailed(monitor, "ID token verification failed"));

        var expanded = jsonLd.expand(credentialOfferMessage)
                .orElseThrow(f -> {
                    monitor.debug("Credential offer message could not be expanded: %s".formatted(f.getFailureDetail()));
                    return new InvalidRequestException("Invalid JSON-LD");
                });
        validatorRegistry.validate(DSPACE_DCP_NAMESPACE_V_1_0.toIri(CREDENTIAL_OFFER_MESSAGE_TERM), expanded).orElseThrow(ValidationFailureException::new);
        var protocolRegistry = transformerRegistry.forContext(DCP_SCOPE_V_1_0);

        var offerMessage = protocolRegistry.forContext(DCP_SCOPE_V_1_0).transform(expanded, CredentialOfferMessage.class).orElseThrow(InvalidRequestException::new);

        // the token was verified against the DID document of its issuer, so only the 'iss' claim identifies the sender.
        // An offer made in the name of a different Issuer would otherwise cause credentials to be requested from it.
        var senderDid = issuerClaims.getStringClaim(JwtRegisteredClaimNames.ISSUER);
        if (!Objects.equals(senderDid, offerMessage.getIssuer())) {
            throw new NotAuthorizedException("The offer names issuer '%s', but was sent by '%s'".formatted(offerMessage.getIssuer(), senderDid));
        }

        // sparse credential objects, i.e. those carrying only an ID, are completed from the Issuer's metadata
        var credentialObjects = credentialObjectResolver.resolve(offerMessage.getIssuer(), offerMessage.getCredentials())
                .orElseThrow(f -> new InvalidRequestException(f.getFailureDetail()));

        var credentialOffer = CredentialOffer.Builder.newInstance()
                .participantContextId(participantContextId)
                .issuer(offerMessage.getIssuer())
                .credentialObjects(credentialObjects.stream().map(co -> CredentialObject.Builder.newInstance()
                        .id(co.getId())
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
