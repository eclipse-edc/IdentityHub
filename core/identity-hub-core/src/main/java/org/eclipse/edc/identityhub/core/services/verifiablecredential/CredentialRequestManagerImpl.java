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
import org.eclipse.edc.iam.identitytrust.spi.SecureTokenService;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequest;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestMessage;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestStatus.Status;
import org.eclipse.edc.identityhub.spi.credential.request.model.HolderCredentialRequest;
import org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState;
import org.eclipse.edc.identityhub.spi.credential.request.store.HolderCredentialRequestStore;
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

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState.CREATED;
import static org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState.ERROR;
import static org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState.REQUESTED;
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
    private SecureTokenService secureTokenService;
    private String ownDid;
    private TransactionContext transactionContext;

    private CredentialRequestManagerImpl() {

    }

    @Override
    public ServiceResult<String> initiateRequest(String participantContext, String issuerDid, String requestId, Map<String, String> typesAndFormats) {

        var newRequest = HolderCredentialRequest.Builder.newInstance()
                .id(requestId)
                .issuerDid(issuerDid)
                .typesAndFormats(typesAndFormats)
                .participantContext(participantContext)
                .state(CREATED.code())
                .build();

        try {
            transactionContext.execute(() -> store.save(newRequest));
        } catch (EdcPersistenceException e) {
            return ServiceResult.badRequest(e.getMessage());
        }
        return ServiceResult.success(requestId);
    }

    @Override
    protected StateMachineManager.Builder configureStateMachineManager(StateMachineManager.Builder builder) {
        return builder
                .processor(processRequestsInState(CREATED, this::processInitial))
                .processor(processRequestsInState(REQUESTING, this::processInitial))
                .processor(processRequestsInState(REQUESTED, this::processRequested));
    }

    private @NotNull Result<String> handleCredentialResponse(String issuanceProcessId, HolderCredentialRequest newRequest) {
        transitionRequested(newRequest, issuanceProcessId);
        return success(issuanceProcessId);
    }

    private Result<String> sendCredentialRequest(HolderCredentialRequest request, String endpoint) {
        var issuerDid = request.getIssuerDid();
        var requestId = request.getId();
        var typesAndFormats = request.getTypesAndFormats();

        return transactionContext.execute(() -> {
            transition(request.copy().toBuilder(), REQUESTING);
            return getAuthToken(issuerDid, ownDid)
                    .compose(token -> createCredentialsRequest(token, endpoint, requestId, typesAndFormats))
                    .compose(httpRequest -> httpClient.execute(httpRequest, this::mapResponseAsString));
        });
    }

    private void transitionRequested(HolderCredentialRequest req, String issuanceProcessId) {
        transition(req.copy().toBuilder().issuanceProcessId(issuanceProcessId), REQUESTED);
    }

    private void transitionIssued(HolderCredentialRequest request) {
        transition(request.copy().toBuilder(), HolderRequestState.ISSUED);
    }

    private void transitionError(HolderCredentialRequest request, String failureDetail) {
        transition(request.copy().toBuilder().errorDetail(failureDetail), ERROR);
    }

    private void transition(HolderCredentialRequest.Builder request, HolderRequestState newState) {
        var rq = request.state(newState.code()).build();
        transactionContext.execute(() -> store.save(rq));
    }

    /**
     * processes all credentials that are in state {@link HolderRequestState#REQUESTED} and transitions to {@link HolderRequestState#ERROR}
     * if the time limit was exceeded.
     *
     * @return true if the request was processed, false otherwise.
     */
    private Boolean processRequested(HolderCredentialRequest request) {
        var created = Instant.ofEpochMilli(request.getStateTimestamp());

        var age = Duration.between(created, Instant.now());

        var limit = Duration.ofHours(1); //todo: make configurable
        if (age.compareTo(limit) > 0) {
            var msg = "Time limit exceeded: request '%s' has been in state '%s' for '%s' time. Limit = %s"
                    .formatted(request.getRequestId(), REQUESTED, age, limit);
            monitor.warning(msg);

            transitionError(request, msg);
            return true;
        } else {
            var res = getCredentialRequestEndpoint(request)
                    .compose(endpoint -> sendCredentialsStatusRequest(request, endpoint))
                    .map(response -> handleIssuerResponse(request, response))
                    .onFailure(failure -> transitionError(request, failure.getFailureDetail()));

            return res.succeeded() ? res.getContent() : false;

        }
    }

    /**
     * Handles the response from the Issuer in reply to a DCP CredentialStatusRequest message:
     * <ul>
     *     <li>If the Issuer returns {@code REJECTED} or an unknown status value, the holder request is transitioned to {@link HolderRequestState#ERROR}.</li>
     *     <li>If the Issuer returns {@code ISSUED}, then the holder request is transitioned to {@link HolderRequestState#ISSUED}.</li>
     *     <li>If the Issuer returns {@code REJECTED}, or an unknown status field, the holder request is transitioned to {@link HolderRequestState#ERROR}</li>
     *     <li>If the Issuer returns {@code RECEIVED}, then no action is performed, and the method returns {@code false}</li>
     * </ul>
     *
     * @param request  the original holder request.
     * @param response A JSON string containing the raw response body.
     * @return true if the holder request was processed, false otherwise.
     */
    private @NotNull Boolean handleIssuerResponse(HolderCredentialRequest request, String response) {
        if (response.contains(Status.REJECTED.toString())) {
            // transition to error, credential request was rejected, but we never received that info
            transitionError(request, "The credential request has been rejected by the Issuer");
            return true;
        } else if (response.contains(Status.ISSUED.toString())) {
            //huh? did we miss a state update?
            transitionIssued(request);
            return true;
        } else if (response.contains(Status.RECEIVED.toString())) { // RECEIVED
            // no update yet, let it tick over
            return false;
        } else {
            transitionError(request, "Invalid status response received from Issuer: '%s'".formatted(response));
            return true;
        }
    }

    private Processor processRequestsInState(HolderRequestState state, Function<HolderCredentialRequest, Boolean> function) {
        var filter = new Criterion[]{ hasState(state.code()), isNotPending() };
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
        monitor.debug("Processing '%s' request '%s'".formatted(holderCredentialRequest.stateAsString(), holderCredentialRequest.getRequestId()));
        var result = getCredentialRequestEndpoint(holderCredentialRequest)
                .compose(endpoint -> sendCredentialRequest(holderCredentialRequest, endpoint))
                .compose(issuanceProcessId -> handleCredentialResponse(issuanceProcessId, holderCredentialRequest))
                .onFailure(failure -> transactionContext.execute(() -> transitionError(holderCredentialRequest, failure.getFailureDetail())));

        return result.succeeded();
    }

    /**
     * Sends a DCP CredentialStatusRequest to the Issuer
     *
     * @param request  the {@link Request} that represents the HTTP call.
     * @param endpoint the base URL of the Issuer
     * @return the entire JSON response received from the Issuer
     */
    private Result<String> sendCredentialsStatusRequest(HolderCredentialRequest request, String endpoint) {
        var issuerDid = request.getIssuerDid();
        var issuanceProcessId = request.getIssuanceProcessId();
        return getAuthToken(issuerDid, ownDid)
                .compose(token -> createCredentialsStatusRequest(token, endpoint, issuanceProcessId))
                .compose(httpRequest -> httpClient.execute(httpRequest, this::mapResponseAsString));
    }

    /**
     * Creates a {@link Request} out of the given parameters, that targets the CredentialRequestStatus API
     *
     * @param token           a Self-Issued ID token that was obtained from STS
     * @param endpoint        the base URL of the Issuer. Note that {@code /request/{requestId}} is appended automatically.
     * @param issuerProcessId the {@code issuerProcessId}. The process ID that was generated by the Issuer. NOT the holder-generated request ID!
     */
    private Result<Request> createCredentialsStatusRequest(TokenRepresentation token, String endpoint, String issuerProcessId) {
        return success(new Request.Builder()
                .url(endpoint + "/request/" + issuerProcessId)
                .header("Authorization", "Bearer" + token.getToken())
                .get()
                .build());
    }

    /**
     * send credential request message over DCP to the issuer endpoint
     *
     * @param token                    the token that should be used in the Authorization header of the DCP request
     * @param issuerRequestEndpointUrl the URL of the Issuer's Credential Request API endpoint
     * @param requestId                the request ID property that will be attached to the request
     * @param typesAndFormats          a map of credential-type-to-format entries. The credential-type is the entry's key, the format is the entry's value
     * @return a Result containing the Issuer-assigned issuance process ID
     */
    private Result<Request> createCredentialsRequest(TokenRepresentation token, String issuerRequestEndpointUrl, String requestId, Map<String, String> typesAndFormats) {
        var rqMessage = CredentialRequestMessage.Builder.newInstance();
        rqMessage.holderPid(requestId);

        typesAndFormats.forEach((type, format) -> rqMessage.credential(new CredentialRequest(type, format, null)));

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
     * @param audience the String used as {@code aud} claim
     * @param myOwnDid the String used as {@code iss} and {@code sub} claims
     * @return a JWT token that can be used to send DCP messages to the issuer
     */
    private Result<TokenRepresentation> getAuthToken(String audience, String myOwnDid) {
        var siTokenClaims = Map.of(
                ISSUED_AT, Instant.now().toString(),
                AUDIENCE, audience,
                ISSUER, myOwnDid,
                SUBJECT, myOwnDid,
                EXPIRATION_TIME, Instant.now().plus(5, ChronoUnit.MINUTES).toString());
        return secureTokenService.createToken(siTokenClaims, null);
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

        public Builder secureTokenService(SecureTokenService secureTokenService) {
            manager.secureTokenService = secureTokenService;
            return this;
        }

        public Builder ownDid(String ownDid) {
            manager.ownDid = ownDid;
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
            requireNonNull(manager.ownDid);
            requireNonNull(manager.transactionContext);
            return manager;
        }
    }
}
