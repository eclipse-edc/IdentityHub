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

package org.eclipse.edc.identityhub.api.storage;

import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.identityhub.protocols.dcp.spi.DcpIssuerTokenVerifier;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialMessage;
import org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState;
import org.eclipse.edc.identityhub.spi.participantcontext.IdentityHubParticipantContextService;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.generator.CredentialWriteRequest;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.generator.CredentialWriter;
import org.eclipse.edc.identityhub.verification.SelfIssuedTokenPrecheck;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.exception.AuthenticationFailedException;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ValidationFailureException;

import java.util.Objects;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.iam.decentralizedclaims.spi.DcpConstants.DSPACE_DCP_NAMESPACE_V_1_0;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.DcpConstants.DCP_SCOPE_V_1_0;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialMessage.CREDENTIAL_MESSAGE_TERM;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v1/participants/{participantContextId}/credentials")
public class StorageApiController implements StorageApi {

    private final JsonObjectValidatorRegistry validatorRegistry;
    private final TypeTransformerRegistry transformerRegistry;
    private final JsonLd jsonLd;
    private final CredentialWriter credentialWriter;
    private final Monitor monitor;
    private final DcpIssuerTokenVerifier issuerTokenVerifier;
    private final IdentityHubParticipantContextService participantContextService;

    public StorageApiController(JsonObjectValidatorRegistry validatorRegistry,
                                TypeTransformerRegistry transformerRegistry,
                                JsonLd jsonLd,
                                CredentialWriter credentialWriter,
                                Monitor monitor,
                                DcpIssuerTokenVerifier issuerTokenVerifier,
                                IdentityHubParticipantContextService participantContextService) {
        this.validatorRegistry = validatorRegistry;
        this.transformerRegistry = transformerRegistry;
        this.jsonLd = jsonLd;
        this.credentialWriter = credentialWriter;
        this.monitor = monitor;
        this.issuerTokenVerifier = issuerTokenVerifier;
        this.participantContextService = participantContextService;
    }


    @POST
    @Override
    public Response storeCredential(@PathParam("participantContextId") String participantContextId, JsonObject credentialMessageJson, @HeaderParam(AUTHORIZATION) String authHeader) {
        if (authHeader == null) {
            throw new AuthenticationFailedException("Authorization header missing");
        }
        if (!authHeader.startsWith("Bearer ")) {
            throw new AuthenticationFailedException("Invalid authorization header, must start with 'Bearer'");
        }
        var authToken = authHeader.replace("Bearer ", "").trim();

        var participantContext = participantContextService.getParticipantContext(participantContextId)
                .orElseThrow((f) -> new AuthenticationFailedException("Invalid participant"));
        var holderDid = participantContext.getDid();

        // the message is needed for the checks preceding the token verification
        var expanded = jsonLd.expand(credentialMessageJson)
                .orElseThrow(f -> {
                    monitor.debug("Credential message could not be expanded: %s".formatted(f.getFailureDetail()));
                    return new InvalidRequestException("Invalid JSON-LD");
                });
        validatorRegistry.validate(DSPACE_DCP_NAMESPACE_V_1_0.toIri(CREDENTIAL_MESSAGE_TERM), expanded).orElseThrow(ValidationFailureException::new);
        var protocolRegistry = transformerRegistry.forContext(DCP_SCOPE_V_1_0);

        var credentialMessage = protocolRegistry.forContext(DCP_SCOPE_V_1_0).transform(expanded, CredentialMessage.class).orElseThrow(InvalidRequestException::new);

        // credentials are only accepted for a request that was sent to the Issuer delivering them. That is checked on the
        // unverified token before the Issuer's DID is resolved to verify it, and again once it is verified
        SelfIssuedTokenPrecheck.check(authToken)
                .compose(unverified -> {
                    var origin = credentialWriter.checkOrigin(credentialMessage.getHolderPid(), unverified.issuer(), participantContextId);
                    return origin.succeeded() ? Result.<Void>success() : Result.<Void>failure(origin.getFailureDetail());
                })
                .orElseThrow(f -> {
                    monitor.debug("Rejected credential message before verifying its ID token: %s".formatted(f.getFailureDetail()));
                    return new AuthenticationFailedException("ID token verification failed");
                });

        // validate Issuer's SI token
        var issuerClaims = issuerTokenVerifier.verify(participantContext, authToken)
                .orElseThrow(f -> {
                    // the failure may describe the outcome of resolving the sender's DID, which is not for the caller to see
                    monitor.debug("ID token verification failed: %s".formatted(f.getFailureDetail()));
                    return new AuthenticationFailedException("ID token verification failed");
                });

        // the token was verified against the DID document of its issuer, so the 'iss' claim identifies who delivers here
        var issuerDid = issuerClaims.getStringClaim(JwtRegisteredClaimNames.ISSUER);

        var holderPid = credentialMessage.getHolderPid();
        var issuerPid = credentialMessage.getIssuerPid();

        // the Issuer reports that it will not issue the credentials, so the request is failed instead of waiting for a
        // delivery that is never coming
        if (Objects.equals(credentialMessage.getStatus(), CredentialMessage.STATUS_REJECTED)) {
            return credentialWriter.reject(holderPid, issuerPid, issuerDid, credentialMessage.getRejectionReason(), participantContextId)
                    .map(v -> Response.ok().build())
                    .orElseThrow(exceptionMapper(CredentialMessage.class, null));
        }

        var writeRequests = credentialMessage.getCredentials().stream().map(c -> new CredentialWriteRequest(c.payload(), c.format())).toList();
        return credentialWriter.write(holderPid, holderDid, issuerPid, issuerDid, writeRequests, participantContextId)
                .onSuccess(v -> monitor.debug("HolderCredentialRequest %s is now in state %s".formatted(holderPid, HolderRequestState.ISSUED)))
                .map(v -> Response.ok().build())
                .orElseThrow(exceptionMapper(CredentialMessage.class, null));
    }

}
