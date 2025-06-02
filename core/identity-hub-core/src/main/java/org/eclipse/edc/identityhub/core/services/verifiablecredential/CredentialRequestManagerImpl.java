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

package org.eclipse.edc.identityhub.core.services.verifiablecredential;

import jakarta.json.JsonObject;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestMessage;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestSpecifier;
import org.eclipse.edc.identityhub.spi.authentication.ParticipantSecureTokenService;
import org.eclipse.edc.identityhub.spi.credential.request.model.HolderCredentialRequest;
import org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState;
import org.eclipse.edc.identityhub.spi.credential.request.model.RequestedCredential;
import org.eclipse.edc.identityhub.spi.credential.request.store.HolderCredentialRequestStore;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.CredentialRequestManager;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.statemachine.AbstractStateEntityManager;
import org.eclipse.edc.statemachine.Processor;
import org.eclipse.edc.statemachine.ProcessorImpl;
import org.eclipse.edc.statemachine.StateMachineManager;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState.CREATED;
import static org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState.ERROR;
import static org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState.REQUESTING;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.AUDIENCE;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.EXPIRATION_TIME;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUED_AT;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUER;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.SUBJECT;
import static org.eclipse.edc.spi.persistence.StateEntityStore.hasState;
import static org.eclipse.edc.spi.persistence.StateEntityStore.isNotPending;
import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;

public class CredentialRequestManagerImpl extends AbstractStateEntityManager<HolderCredentialRequest, HolderCredentialRequestStore>
        implements CredentialRequestManager {
    private DidResolverRegistry didResolverRegistry;
    private TypeTransformerRegistry dcpTypeTransformerRegistry;
    private EdcHttpClient httpClient;
    private ParticipantSecureTokenService secureTokenService;
    private TransactionContext transactionContext;
    private ParticipantContextService participantContextService;

    private CredentialRequestManagerImpl() {

    }

    @Override
    public ServiceResult<String> initiateRequest(String participantContextId, String issuerDid, String holderPid, List<RequestedCredential> requestedCredentials) {

        var newRequest = HolderCredentialRequest.Builder.newInstance()
                .id(holderPid)
                .issuerDid(issuerDid)
                .requestedCredentials(requestedCredentials)
                .participantContextId(participantContextId)
                .state(CREATED.code())
                .build();

        try {
            updateRequest(newRequest);
        } catch (EdcPersistenceException e) {
            return ServiceResult.badRequest(e.getMessage());
        }
        return ServiceResult.success(holderPid);
    }

    @Override
    public @Nullable HolderCredentialRequest findById(String holderPid) {
        return transactionContext.execute(() -> store.findById(holderPid));
    }

    @Override
    protected StateMachineManager.Builder configureStateMachineManager(StateMachineManager.Builder builder) {
        return builder
                .processor(processRequestsInState(CREATED, this::processInitial))
                .processor(processRequestsInState(REQUESTING, this::processInitial));
    }

    private @NotNull Result<String> handleCredentialResponse(String issuerPid, HolderCredentialRequest newRequest) {
        transitionRequested(newRequest, issuerPid);
        return success(issuerPid);
    }

    private Result<String> sendCredentialRequest(HolderCredentialRequest request, String endpoint) {
        var issuerDid = request.getIssuerDid();
        var holderPid = request.getId();
        var requestedCredentials = request.getIdsAndFormats();

        return transactionContext.execute(() -> {
            request.transitionRequesting();
            updateRequest(request);
            return getAuthToken(request.getParticipantContextId(), issuerDid)
                    .compose(token -> createCredentialsRequest(token, endpoint, holderPid, requestedCredentials))
                    .compose(httpRequest -> httpClient.execute(httpRequest, this::mapResponseAsString));
        });
    }

    private void transitionRequested(HolderCredentialRequest req, String issuerPid) {
        req.transitionRequested(issuerPid);
        updateRequest(req);
    }

    private void transitionError(HolderCredentialRequest request, String failureDetail) {
        request.transitionError(failureDetail);
        updateRequest(request);
        monitor.warning("A Holder Credential Request has been transitioned to '%s': %s".formatted(ERROR, failureDetail));
    }

    private void updateRequest(HolderCredentialRequest request) {
        transactionContext.execute(() -> update(request));
    }

    private Processor processRequestsInState(HolderRequestState state, Function<HolderCredentialRequest, Boolean> function) {
        var filter = new Criterion[]{hasState(state.code()), isNotPending()};
        return createProcessor(function, filter);
    }

    private ProcessorImpl<HolderCredentialRequest> createProcessor(Function<HolderCredentialRequest, Boolean> function, Criterion[] filter) {
        return ProcessorImpl.Builder.newInstance(() -> store.nextNotLeased(batchSize, filter))
                .process(telemetry.contextPropagationMiddleware(function))
                //.guard(pendingGuard, this::setPending) //todo: needed?
                .onNotProcessed(this::breakLease)
                .build();
    }

    /**
     * processes all requests that are in {@link HolderRequestState#CREATED} or {@link HolderRequestState#REQUESTING} state. Credential requests that were
     * interrupted before receiving the Issuer's response are in this state.
     *
     * @return true if the request was processed, false otherwise.
     */
    private Boolean processInitial(HolderCredentialRequest holderCredentialRequest) {
        monitor.debug("Processing '%s' request '%s'".formatted(holderCredentialRequest.stateAsString(), holderCredentialRequest.getHolderPid()));
        var result = getCredentialRequestEndpoint(holderCredentialRequest)
                .compose(endpoint -> sendCredentialRequest(holderCredentialRequest, endpoint))
                .compose(issuerPid -> handleCredentialResponse(issuerPid, holderCredentialRequest))
                .onFailure(failure -> transactionContext.execute(() -> transitionError(holderCredentialRequest, failure.getFailureDetail())));

        return result.succeeded();
    }

    /**
     * send credential request message over DCP to the issuer endpoint
     *
     * @param token                    the token that should be used in the Authorization header of the DCP request
     * @param issuerRequestEndpointUrl the URL of the Issuer's Credential Request API endpoint
     * @param holderPid                the request ID property that will be attached to the request
     * @param idsAndFormats            a map of credential-object-id-to-format entries. The credential-type is the entry's key, the format is the entry's value
     * @return a Result containing the Issuer-assigned issuance process ID
     */
    private Result<Request> createCredentialsRequest(TokenRepresentation token, String issuerRequestEndpointUrl, String holderPid, List<RequestedCredential> idsAndFormats) {
        var rqMessage = CredentialRequestMessage.Builder.newInstance();
        rqMessage.holderPid(holderPid);

        idsAndFormats.forEach((rq) -> rqMessage.credential(new CredentialRequestSpecifier(rq.id())));

        var jsonObj = dcpTypeTransformerRegistry.transform(rqMessage.build(), JsonObject.class);

        return jsonObj.map(JsonObject::toString)
                .map(json -> new Request.Builder()
                        .url(issuerRequestEndpointUrl + "/credentials")
                        .post(RequestBody.create(json, MediaType.parse("application/json")))
                        .header("Authorization", "Bearer " + token.getToken())
                        .build());

    }

    /**
     * maps a {@link Response} to a result containing the response body
     */
    private Result<String> mapResponseAsString(Response response) {
        if (response.isSuccessful()) {
            if (response.body() != null) {
                try {
                    return success(response.body().string());
                } catch (IOException e) {
                    return failure(e.getMessage());
                }
            }
        }
        return failure("Error sending DCP Credential Request: code: '%s', message: '%s'".formatted(response.code(), response.message()));
    }

    /**
     * Fetches the authentication token from the SecureTokenService.
     *
     * @param participantContextId The ID of the participant context on behalf of which the token is generated
     * @param audience             the String used as {@code aud} claim
     * @return a JWT token that can be used to send DCP messages to the issuer
     */
    private Result<TokenRepresentation> getAuthToken(String participantContextId, String audience) {
        return getParticipantContext(participantContextId)
                .compose(participantContext -> {
                    var siTokenClaims = Map.of(
                            ISSUED_AT, Instant.now().toString(),
                            AUDIENCE, audience,
                            ISSUER, participantContext.getDid(),
                            SUBJECT, participantContext.getDid(),
                            EXPIRATION_TIME, Instant.now().plus(5, ChronoUnit.MINUTES).toString());
                    return secureTokenService.createToken(participantContextId, siTokenClaims, null);
                });
    }

    private Result<ParticipantContext> getParticipantContext(String participantContextId) {
        var result = participantContextService.getParticipantContext(participantContextId);
        if (result.failed()) {
            return failure("Invalid participant");
        }
        return Result.success(result.getContent());

    }

    /**
     * Extracts the {@code CredentialRequest} service endpoint from the DID document
     *
     * @param request The Issuer's DID document
     * @return A result containing the service entry
     */
    private Result<String> getCredentialRequestEndpoint(HolderCredentialRequest request) {
        return didResolverRegistry.resolve(request.getIssuerDid())
                .compose(didDocument -> {
                    var service = didDocument.getService().stream().filter(s -> s.getType().equalsIgnoreCase(ISSUER_SERVICE_ENDPOINT_TYPE)).findAny();
                    return service.map(s -> success((s.getServiceEndpoint())))
                            .orElseGet(() -> failure("The Issuer's DID Document does not contain any '%s' endpoint".formatted(ISSUER_SERVICE_ENDPOINT_TYPE)));
                });
    }

    public static class Builder
            extends AbstractStateEntityManager.Builder<HolderCredentialRequest, HolderCredentialRequestStore, CredentialRequestManagerImpl, Builder> {

        protected Builder(CredentialRequestManagerImpl service) {
            super(service);
        }

        public static Builder newInstance() {
            return new Builder(new CredentialRequestManagerImpl());
        }

        public Builder didResolverRegistry(DidResolverRegistry didResolverRegistry) {
            manager.didResolverRegistry = didResolverRegistry;
            return this;
        }

        public Builder typeTransformerRegistry(TypeTransformerRegistry typeTransformerRegistry) {
            manager.dcpTypeTransformerRegistry = typeTransformerRegistry;
            return this;
        }

        public Builder httpClient(EdcHttpClient httpClient) {
            manager.httpClient = httpClient;
            return this;
        }

        public Builder secureTokenService(ParticipantSecureTokenService secureTokenService) {
            manager.secureTokenService = secureTokenService;
            return this;
        }

        public Builder participantContextService(ParticipantContextService participantContextService) {
            manager.participantContextService = participantContextService;
            return this;
        }

        public Builder transactionContext(TransactionContext transactionContext) {
            manager.transactionContext = transactionContext;
            return this;
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public Builder monitor(Monitor monitor) {
            manager.monitor = monitor;
            return this;
        }

        @Override
        public Builder store(HolderCredentialRequestStore store) {
            manager.store = store;
            return this;
        }

        @Override
        public CredentialRequestManagerImpl build() {
            super.build();
            requireNonNull(manager.didResolverRegistry);
            requireNonNull(manager.dcpTypeTransformerRegistry);
            requireNonNull(manager.httpClient);
            requireNonNull(manager.secureTokenService);
            requireNonNull(manager.transactionContext);
            requireNonNull(manager.participantContextService);
            return manager;
        }
    }
}
