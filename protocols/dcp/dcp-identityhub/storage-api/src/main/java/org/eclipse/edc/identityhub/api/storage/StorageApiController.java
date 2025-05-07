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
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.generator.CredentialWriteRequest;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.generator.CredentialWriter;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.exception.AuthenticationFailedException;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ValidationFailureException;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.iam.identitytrust.spi.DcpConstants.DSPACE_DCP_NAMESPACE_V_1_0;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.DcpConstants.DCP_SCOPE_V_1_0;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialMessage.CREDENTIAL_MESSAGE_TERM;
import static org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextId.onEncoded;
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
    private final ParticipantContextService participantContextService;

    public StorageApiController(JsonObjectValidatorRegistry validatorRegistry,
                                TypeTransformerRegistry transformerRegistry,
                                JsonLd jsonLd,
                                CredentialWriter credentialWriter,
                                Monitor monitor,
                                DcpIssuerTokenVerifier issuerTokenVerifier,
                                ParticipantContextService participantContextService) {
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
        var expanded = jsonLd.expand(credentialMessageJson).orElseThrow(InvalidRequestException::new);
        validatorRegistry.validate(DSPACE_DCP_NAMESPACE_V_1_0.toIri(CREDENTIAL_MESSAGE_TERM), expanded).orElseThrow(ValidationFailureException::new);
        var protocolRegistry = transformerRegistry.forContext(DCP_SCOPE_V_1_0);

        participantContextId = onEncoded(participantContextId).orElseThrow(InvalidRequestException::new);

        var credentialMessage = protocolRegistry.forContext(DCP_SCOPE_V_1_0).transform(expanded, CredentialMessage.class).orElseThrow(InvalidRequestException::new);


        var participantContext = participantContextService.getParticipantContext(participantContextId)
                .orElseThrow((f) -> new AuthenticationFailedException("Invalid participant"));

        // validate Issuer's SI token
        issuerTokenVerifier.verify(participantContext, authToken)
                .orElseThrow(f -> new AuthenticationFailedException("ID token verification failed: %s".formatted(f.getFailureDetail())));

        var holderPid = credentialMessage.getHolderPid();
        var issuerPid = credentialMessage.getIssuerPid();

        var writeRequests = credentialMessage.getCredentials().stream().map(c -> new CredentialWriteRequest(c.payload(), c.format())).toList();
        return credentialWriter.write(holderPid, issuerPid, writeRequests, participantContextId)
                .onSuccess(v -> monitor.debug("HolderCredentialRequest %s is now in state %s".formatted(holderPid, HolderRequestState.ISSUED)))
                .map(v -> Response.ok().build())
                .orElseThrow(exceptionMapper(CredentialMessage.class, null));
    }

}
