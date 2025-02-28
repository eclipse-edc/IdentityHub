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

package org.eclipse.edc.identityhub.protocols.dcp.issuer.api.v1alpha.credentialrequeststatus;

import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import org.eclipse.edc.identityhub.protocols.dcp.spi.DcpHolderTokenVerifier;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestStatus;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcess;
import org.eclipse.edc.issuerservice.spi.issuance.process.IssuanceProcessService;
import org.eclipse.edc.issuerservice.spi.participant.model.Participant;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.exception.AuthenticationFailedException;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextId.onEncoded;
import static org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcessResource.queryByIssuerContextId;
import static org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcessStates.from;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v1alpha/issuers/{issuerContextId}/requests")
public class CredentialRequestStatusApiController implements CredentialRequestStatusApi {

    private final TypeTransformerRegistry dcpRegistry;
    private final ParticipantContextService participantContextService;
    private final DcpHolderTokenVerifier tokenValidator;
    private final IssuanceProcessService issuanceProcessService;

    public CredentialRequestStatusApiController(ParticipantContextService participantContextService, DcpHolderTokenVerifier tokenValidator, IssuanceProcessService issuanceProcessService, TypeTransformerRegistry dcpRegistry) {
        this.dcpRegistry = dcpRegistry;
        this.participantContextService = participantContextService;
        this.tokenValidator = tokenValidator;
        this.issuanceProcessService = issuanceProcessService;
    }

    @GET
    @Path("/{credentialRequestId}")
    @Override
    public JsonObject credentialStatus(@PathParam("issuerContextId") String issuerContextId, @PathParam("credentialRequestId") String credentialRequestId, @HeaderParam(AUTHORIZATION) String token) {
        if (token == null) {
            throw new AuthenticationFailedException("Authorization header missing");
        }

        var decodedIssuerContextId = onEncoded(issuerContextId).orElseThrow(InvalidRequestException::new);

        var participantContext = participantContextService.getParticipantContext(decodedIssuerContextId)
                .orElseThrow((f) -> new AuthenticationFailedException("Invalid issuer"));

        var tokenRepresentation = TokenRepresentation.Builder.newInstance().token(token).build();

        var requestContext = tokenValidator.verify(participantContext, tokenRepresentation)
                .orElseThrow((f) -> new AuthenticationFailedException("ID token verification failed: %s".formatted(f.getFailureDetail())));

        var status = fetchByParticipant(decodedIssuerContextId, requestContext.participant(), credentialRequestId)
                .map(this::toCredentialStatus)
                .orElseThrow((f) -> new AuthenticationFailedException("Invalid credential request %s: %s".formatted(credentialRequestId, f.getFailureDetail())));

        return dcpRegistry.transform(status, JsonObject.class)
                .orElseThrow(f -> new EdcException("Error creating response body: " + f.getFailureDetail()));

    }

    private Result<IssuanceProcess> fetchByParticipant(String issuerContextId, Participant participant, String credentialRequestId) {
        var query = queryByIssuerContextId(issuerContextId)
                .filter(Criterion.criterion("participantId", "=", participant.participantId()))
                .filter(Criterion.criterion("id", "=", credentialRequestId))
                .build();
        var result = issuanceProcessService.search(query);

        if (result.failed()) {
            return Result.failure(result.getFailureDetail());
        }
        var processes = result.getContent();

        return processes.stream().findFirst().map(Result::success)
                .orElseGet(() -> Result.failure("Credential request %s not found".formatted(credentialRequestId)));
    }

    private CredentialRequestStatus toCredentialStatus(IssuanceProcess process) {
        return CredentialRequestStatus.Builder.newInstance()
                .status(toStatus(process))
                .issuerPid(process.getId())
                .holderPid(process.getHolderPid())
                .build();
    }

    private CredentialRequestStatus.Status toStatus(IssuanceProcess process) {
        var state = from(process.getState());
        return switch (state) {
            case SUBMITTED, APPROVED -> CredentialRequestStatus.Status.RECEIVED;
            case DELIVERED -> CredentialRequestStatus.Status.ISSUED;
            case ERRORED -> CredentialRequestStatus.Status.REJECTED;
        };
    }
}
