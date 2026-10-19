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

package org.eclipse.edc.identityhub.protocols.dcp.issuer.api.v1.credentialrequeststatus;

import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import org.eclipse.edc.identityhub.protocols.dcp.spi.DcpHolderTokenVerifier;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestStatus;
import org.eclipse.edc.identityhub.spi.participantcontext.IdentityHubParticipantContextService;
import org.eclipse.edc.issuerservice.spi.holder.model.Holder;
import org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcess;
import org.eclipse.edc.issuerservice.spi.issuance.process.IssuanceProcessService;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.exception.AuthenticationFailedException;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.identityhub.verification.AuthenticationFailures.idTokenVerificationFailed;
import static org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcessStates.from;
import static org.eclipse.edc.participantcontext.spi.types.ParticipantResource.queryByParticipantContextId;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v1/participants/{participantContextId}/requests")
public class CredentialRequestStatusApiController implements CredentialRequestStatusApi {

    private final TypeTransformerRegistry dcpRegistry;
    private final IdentityHubParticipantContextService participantContextService;
    private final DcpHolderTokenVerifier tokenValidator;
    private final IssuanceProcessService issuanceProcessService;
    private final Monitor monitor;

    public CredentialRequestStatusApiController(IdentityHubParticipantContextService participantContextService, DcpHolderTokenVerifier tokenValidator, IssuanceProcessService issuanceProcessService, TypeTransformerRegistry dcpRegistry,
                                                Monitor monitor) {
        this.dcpRegistry = dcpRegistry;
        this.participantContextService = participantContextService;
        this.tokenValidator = tokenValidator;
        this.issuanceProcessService = issuanceProcessService;
        this.monitor = monitor;
    }

    @GET
    @Path("/{credentialRequestId}")
    @Override
    public JsonObject credentialStatus(@PathParam("participantContextId") String participantContextId, @PathParam("credentialRequestId") String credentialRequestId, @HeaderParam(AUTHORIZATION) String authHeader) {
        if (authHeader == null) {
            throw new AuthenticationFailedException("Authorization header missing");
        }
        if (!authHeader.startsWith("Bearer ")) {
            throw new AuthenticationFailedException("Invalid authorization header, must start with 'Bearer'");
        }
        var authToken = authHeader.replace("Bearer ", "").trim();


        var participantContext = participantContextService.getParticipantContext(participantContextId)
                .orElseThrow(idTokenVerificationFailed(monitor, "Participant context '%s' could not be resolved".formatted(participantContextId)));

        var tokenRepresentation = TokenRepresentation.Builder.newInstance().token(authToken).build();

        var requestContext = tokenValidator.verify(participantContext, tokenRepresentation)
                .orElseThrow(idTokenVerificationFailed(monitor, "ID token verification failed"));

        var status = fetchByParticipant(participantContextId, requestContext.holder(), credentialRequestId)
                .map(this::toCredentialStatus)
                .orElseThrow(f -> {
                    monitor.debug("Credential request '%s' could not be found: %s".formatted(credentialRequestId, f.getFailureDetail()));
                    return new AuthenticationFailedException("Invalid credential request");
                });

        return dcpRegistry.transform(status, JsonObject.class)
                .orElseThrow(f -> new EdcException("Error creating response body: " + f.getFailureDetail()));

    }

    private Result<IssuanceProcess> fetchByParticipant(String participantContextId, Holder holder, String credentialRequestId) {
        var query = queryByParticipantContextId(participantContextId)
                .filter(Criterion.criterion("holderId", "=", holder.getHolderId()))
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
            // the credentials are not delivered to the Holder until DELIVERED, so until then the request is merely received
            case SUBMITTED, APPROVED, DELIVERING -> CredentialRequestStatus.Status.RECEIVED;
            case DELIVERED -> CredentialRequestStatus.Status.ISSUED;
            // the issuance has failed for good, even while the Holder is still being notified of it
            case REJECTING, ERRORED -> CredentialRequestStatus.Status.REJECTED;
        };
    }
}
