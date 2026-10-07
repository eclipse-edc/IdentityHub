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

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.identityhub.core.CredentialRequestConfiguration;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestMessage;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestSpecifier;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestStatus;
import org.eclipse.edc.identityhub.spi.authentication.ParticipantSecureTokenService;
import org.eclipse.edc.identityhub.spi.credential.request.model.HolderCredentialRequest;
import org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState;
import org.eclipse.edc.identityhub.spi.credential.request.model.RequestedCredential;
import org.eclipse.edc.identityhub.spi.credential.request.store.HolderCredentialRequestStore;
import org.eclipse.edc.identityhub.spi.participantcontext.IdentityHubParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.IdentityHubParticipantContext;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.CredentialRequestManager;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.statemachine.AbstractStateEntityManager;
import org.eclipse.edc.statemachine.Processor;
import org.eclipse.edc.statemachine.ProcessorImpl;
import org.eclipse.edc.statemachine.StateMachineManager;
import org.eclipse.edc.statemachine.retry.processor.Process;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.StringReader;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.DcpConstants.DCP_SCOPE_V_1_0;
import static org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState.CREATED;
import static org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState.ERROR;
import static org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState.REQUESTED;
import static org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState.REQUESTING;
import static org.eclipse.edc.identityhub.store.QueryPages.forEachPage;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.AUDIENCE;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.EXPIRATION_TIME;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUED_AT;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUER;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.SUBJECT;
import static org.eclipse.edc.spi.persistence.StateEntityStore.hasState;
import static org.eclipse.edc.spi.persistence.StateEntityStore.isNotPending;
import static org.eclipse.edc.spi.response.ResponseStatus.ERROR_RETRY;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;
import static org.eclipse.edc.spi.result.Result.failure;

public class CredentialRequestManagerImpl extends AbstractStateEntityManager<HolderCredentialRequest, HolderCredentialRequestStore>
        implements CredentialRequestManager {
    private static final int HTTP_CONFLICT = 409;
    private static final String UNKNOWN_ISSUER_PID = "";
    private static final int STATUS_POLL_PAGE_SIZE = 100;
    private ScheduledExecutorService statusPollScheduler;
    private DidResolverRegistry didResolverRegistry;
    private TypeTransformerRegistry dcpTypeTransformerRegistry;
    private JsonLd jsonLd;
    private EdcHttpClient httpClient;
    private ParticipantSecureTokenService secureTokenService;
    private TransactionContext transactionContext;
    private IdentityHubParticipantContextService participantContextService;
    private CredentialRequestConfiguration configuration;

    private CredentialRequestManagerImpl() {

    }

    @WithSpan(value = "credential-request.initiate", kind = SpanKind.INTERNAL)
    @Override
    public ServiceResult<String> initiateRequest(String participantContextId, String issuerDid, String holderPid, List<RequestedCredential> requestedCredentials) {

        var traceContext = telemetry.getCurrentTraceContext();

        var newRequest = HolderCredentialRequest.Builder.newInstance()
                .id(holderPid)
                .issuerDid(issuerDid)
                .requestedCredentials(requestedCredentials)
                .participantContextId(participantContextId)
                .state(CREATED.code())
                .traceContext(traceContext)
                .build();

        return transactionContext.execute(() -> {
            if (findById(holderPid) != null) {
                return ServiceResult.conflict("Holder Credential Request with holderPid '%s' already exists".formatted(holderPid));
            }
            try {
                return ServiceResult.from(updateRequest(newRequest)).map(u -> holderPid);
            } catch (EdcPersistenceException e) {
                return ServiceResult.badRequest(e.getMessage());
            }
        });
    }

    @Override
    public @Nullable HolderCredentialRequest findById(String holderPid) {
        return transactionContext.execute(() -> store.findById(holderPid));
    }

    /**
     * Polls the Issuer's Credential Request Status API for requests that were accepted but not yet fulfilled. An
     * issuance can still fail on the Issuer side after it acknowledged the request, and the Holder would otherwise never
     * learn about it. A request that the Issuer reports as {@code REJECTED} is transitioned to
     * {@link HolderRequestState#ERROR}; anything else leaves it untouched so that it is polled again later.
     */
    @Override
    public void start() {
        super.start();
        statusPollScheduler = Executors.newSingleThreadScheduledExecutor();
        statusPollScheduler.scheduleWithFixedDelay(this::pollPendingRequests, configuration.statusPollInterval(), configuration.statusPollInterval(), MILLISECONDS);
    }

    @Override
    public void stop() {
        if (statusPollScheduler != null) {
            statusPollScheduler.shutdownNow();
        }
        super.stop();
    }

    @Override
    protected StateMachineManager.Builder configureStateMachineManager(StateMachineManager.Builder builder) {
        return builder
                .processor(processRequestsInState(CREATED, this::processCreated))
                .processor(processRequestsInState(REQUESTING, this::processRequesting));
    }

    /**
     * Records that the Issuer accepted a credential request by transitioning it to
     * {@link HolderRequestState#REQUESTED} and storing the Issuer-assigned issuance process ID.
     * <p>
     * Issuers might not disclose that ID when acknowledging a request, e.g. when a request for that ID already exists,
     * in which case {@link #UNKNOWN_ISSUER_PID} is passed here. An ID obtained from the earlier attempt is then retained, since
     * overwriting it would lose the only correlation the Holder has until the credentials are delivered.
     *
     * @param issuerPid  the issuance process ID as reported by the Issuer, or {@link #UNKNOWN_ISSUER_PID} if it did not
     *                   report one
     * @param newRequest the request that was sent to the Issuer
     */
    private void handleCredentialResponse(String issuerPid, HolderCredentialRequest newRequest) {
        var effectiveIssuerPid = UNKNOWN_ISSUER_PID.equals(issuerPid) && newRequest.getIssuerPid() != null
                ? newRequest.getIssuerPid()
                : issuerPid;
        transitionRequested(newRequest, effectiveIssuerPid);
    }

    /**
     * Sends a {@code CredentialRequestMessage} for the given request to the Issuer's Credential Request API, authenticated
     * with a freshly created Self-Issued ID token.
     * <p>
     * The request was persisted in {@link HolderRequestState#REQUESTING} before, so that an interruption cannot lose the
     * fact that the Issuer may already have received the message. Recovery, like a retry, re-enters this method with the
     * same {@code holderPid}, which lets the Issuer recognize the duplicate - see {@link #mapResponseAsIssuerPid(Response)}
     * for how that answer is interpreted.
     * <p>
     * No transaction is open while the message is sent, so that waiting for the Issuer does not hold a database
     * connection, nor any locks. The lease on the request keeps other runtimes from processing it in the meantime.
     *
     * @param request  the request to send, in state {@link HolderRequestState#REQUESTING}
     * @param endpoint the base URL of the Issuer's Issuer Service, as resolved from its DID document
     * @return a StatusResult containing the Issuer-assigned issuance process ID, or {@link #UNKNOWN_ISSUER_PID} if the
     *         Issuer accepted the request without reporting one. This can happen if an issuance request already exists on
     *         the issuer side and HTTP 409 is returned. Fails if the token, the message or the HTTP exchange failed, with
     *         {@link ResponseStatus#ERROR_RETRY} if another attempt may succeed.
     */
    private StatusResult<String> sendCredentialRequest(HolderCredentialRequest request, String endpoint) {
        var issuerDid = request.getIssuerDid();
        var holderPid = request.getId();
        var requestedCredentials = request.getIdsAndFormats();

        // failing to mint the token or to assemble the message is a configuration problem, which another attempt
        // would run into just the same
        var httpRequest = getAuthToken(request.getParticipantContextId(), issuerDid)
                .compose(token -> createCredentialsRequest(token, endpoint, holderPid, requestedCredentials));
        if (httpRequest.failed()) {
            return StatusResult.failure(FATAL_ERROR, httpRequest.getFailureDetail());
        }
        return executeCredentialRequest(httpRequest.getContent());
    }

    /**
     * Performs the HTTP exchange with the Issuer's Credential Request API. An Issuer that cannot be reached at all is
     * reported as retryable, because that is precisely the temporary outage another attempt is meant to ride out.
     */
    private StatusResult<String> executeCredentialRequest(Request httpRequest) {
        try (var response = httpClient.execute(httpRequest)) {
            return mapResponseAsIssuerPid(response);
        } catch (IOException e) {
            return StatusResult.failure(ERROR_RETRY, "Error sending DCP Credential Request: %s".formatted(e.getMessage()));
        }
    }

    private void transitionRequested(HolderCredentialRequest req, String issuerPid) {
        req.transitionRequested(issuerPid);
        updateRequest(req);
    }

    private void transitionRetry(HolderCredentialRequest request) {
        request.transitionRetry();
        updateRequest(request);
    }

    private void transitionError(HolderCredentialRequest request, String failureDetail) {
        request.transitionError(failureDetail);
        updateRequest(request);
        monitor.warning("A Holder Credential Request has been transitioned to '%s': %s".formatted(ERROR, failureDetail));
    }

    private StoreResult<?> updateRequest(HolderCredentialRequest request) {
        return transactionContext.execute(() -> update(request));
    }

    private Processor processRequestsInState(HolderRequestState state, Function<HolderCredentialRequest, CompletableFuture<StatusResult<Void>>> function) {
        var filter = new Criterion[]{ hasState(state.code()), isNotPending() };
        return createProcessor(function, filter);
    }

    private ProcessorImpl<HolderCredentialRequest> createProcessor(Function<HolderCredentialRequest, CompletableFuture<StatusResult<Void>>> function, Criterion[] filter) {
        return ProcessorImpl.Builder.newInstance(() -> store.nextNotLeased(batchSize, filter), entityRetryProcessConfiguration, clock, monitor)
                .process(telemetry.contextPropagationMiddleware(function))
                .onNotProcessed(this::breakLease)
                .build();
    }

    /**
     * Processes a request in {@link HolderRequestState#CREATED} state by transitioning it to
     * {@link HolderRequestState#REQUESTING}. The message is only sent once that is persisted, see
     * {@link #processRequesting(HolderCredentialRequest)}.
     *
     * @return a CompletableFuture containing the result of processing the request.
     */
    private CompletableFuture<StatusResult<Void>> processCreated(HolderCredentialRequest holderCredentialRequest) {
        holderCredentialRequest.transitionRequesting();
        updateRequest(holderCredentialRequest);
        return CompletableFuture.completedFuture(StatusResult.success());
    }

    /**
     * Processes a request in {@link HolderRequestState#REQUESTING} state by sending it to the Issuer. Credential requests
     * that were interrupted before receiving the Issuer's response are in this state as well.
     * <p>
     * A failure that may resolve itself - an Issuer that is unreachable or answering 5xx - leaves the request in its
     * current state, so that the state machine attempts it again with a growing delay. Only a failure the Issuer would
     * repeat, or one that outlives the retry limit, moves the request to {@link HolderRequestState#ERROR}.
     *
     * @return a CompletableFuture containing the result of processing the request.
     */
    private CompletableFuture<StatusResult<Void>> processRequesting(HolderCredentialRequest holderCredentialRequest) {
        monitor.debug("Processing '%s' request '%s'".formatted(holderCredentialRequest.stateAsString(), holderCredentialRequest.getHolderPid()));

        return telemetry.contextPropagationMiddleware(() -> entityRetryProcessFactory.<HolderCredentialRequest, Void>retryProcessor(holderCredentialRequest)
                .doProcess(Process.result("Resolve the Issuer's CredentialRequest endpoint", (request, unused) -> getCredentialRequestEndpoint(request)))
                .doProcess(Process.result("Send the DCP CredentialRequestMessage", this::sendCredentialRequest))
                .onSuccess((request, issuerPid) -> transactionContext.execute(() -> handleCredentialResponse(issuerPid, request)))
                .onFailure((request, throwable) -> transactionContext.execute(() -> transitionRetry(request)))
                .onFinalFailure((request, throwable) -> transactionContext.execute(() -> transitionError(request, throwable.getMessage())))
                .execute(), holderCredentialRequest).get();
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

        var jsonObj = dcpTypeTransformerRegistry.transform(rqMessage.build(), JsonObject.class)
                .compose(json -> jsonLd.compact(json, DCP_SCOPE_V_1_0));

        return jsonObj.map(JsonObject::toString)
                .map(json -> new Request.Builder()
                        .url(issuerRequestEndpointUrl + "/credentials")
                        .post(RequestBody.create(json, MediaType.parse("application/json")))
                        .header("Authorization", "Bearer " + token.getToken())
                        .build());
    }

    /**
     * Asks the Issuer about every request that was accepted but not yet fulfilled. An issuance can still fail on the
     * Issuer side after it acknowledged the request, and the Holder would otherwise wait for credentials that will never
     * arrive.
     * <p>
     * This deliberately runs outside the state machine: leasing a request for the duration of the status call would
     * block the Issuer from delivering the credentials for that very request. Only a rejection acquires the request.
     */
    private void pollPendingRequests() {
        try {
            var query = QuerySpec.Builder.newInstance()
                    .filter(Criterion.criterion("state", "=", REQUESTED.code()))
                    .build();
            forEachPage(query, STATUS_POLL_PAGE_SIZE, page -> transactionContext.execute(() -> store.query(page)), HolderCredentialRequest::getId,
                    requests -> requests.stream()
                            .filter(request -> request.getIssuerPid() != null && !request.getIssuerPid().isBlank())
                            .forEach(this::pollStatus));
        } catch (Exception e) {
            monitor.debug("Error while polling the Issuer for credential request states: %s".formatted(e.getMessage()));
        }
    }

    private void pollStatus(HolderCredentialRequest request) {
        var endpoint = getCredentialRequestEndpoint(request);
        if (endpoint.failed()) {
            monitor.debug("Could not read the status of credential request '%s': %s".formatted(request.getId(), endpoint.getFailureDetail()));
            return;
        }
        getAuthToken(request.getParticipantContextId(), request.getIssuerDid())
                .map(token -> createStatusRequest(request, endpoint.getContent(), token))
                .compose(httpRequest -> httpClient.execute(httpRequest, this::mapResponseAsStatus))
                .onSuccess(status -> handleStatusResponse(status, request))
                .onFailure(f -> monitor.debug("Could not read the status of credential request '%s': %s".formatted(request.getId(), f.getFailureDetail())));
    }

    /**
     * Acts on the status the Issuer reports. Only a rejection is terminal and is recorded on the request; any other
     * status leaves it in {@link HolderRequestState#REQUESTED} to be polled again later.
     */
    private void handleStatusResponse(CredentialRequestStatus status, HolderCredentialRequest request) {
        if (status.getStatus() != CredentialRequestStatus.Status.REJECTED) {
            return;
        }
        transactionContext.execute(() -> {
            // the request is only acquired now: it may be held by an incoming credential delivery, in which case this
            // round is skipped and the rejection is picked up later
            var leased = store.findByIdAndLease(request.getId());
            if (leased.failed()) {
                monitor.debug("Could not acquire credential request '%s' to record the Issuer's rejection: %s"
                        .formatted(request.getId(), leased.getFailureDetail()));
                return null;
            }
            var current = leased.getContent();
            if (current.stateAsEnum() != HolderRequestState.REQUESTED) {
                // the credentials arrived in the meantime, so the rejection is stale
                store.breakLease(current);
                return null;
            }
            transitionError(current, "The Issuer rejected the credential request '%s'".formatted(request.getIssuerPid()));
            return null;
        });
    }

    private @NotNull Request createStatusRequest(HolderCredentialRequest request, String endpoint, TokenRepresentation token) {
        return new Request.Builder()
                .url(endpoint + "/requests/" + request.getIssuerPid())
                .get()
                .header("Authorization", "Bearer " + token.getToken())
                .build();
    }

    private Result<CredentialRequestStatus> mapResponseAsStatus(Response response) {
        try (var body = response.body()) {
            if (!response.isSuccessful()) {
                return failure("Error fetching the credential request status: code: '%s', message: '%s'".formatted(response.code(), response.message()));
            }
            try (var reader = Json.createReader(new StringReader(body.string()))) {
                return jsonLd.expand(reader.readObject())
                        .compose(expanded -> dcpTypeTransformerRegistry.transform(expanded, CredentialRequestStatus.class));
            }
        } catch (IOException e) {
            return failure("Error fetching the credential request status: %s".formatted(e.getMessage()));
        }
    }

    /**
     * Maps a {@link Response} to a result containing the Issuer-assigned issuance process ID. The Issuer conveys it in
     * the {@code Location} header, which points at the request-status resource, i.e. its last path segment is the ID.
     * Falls back to the response body for Issuers that return the ID there.
     */
    private StatusResult<String> mapResponseAsIssuerPid(Response response) {
        try (var body = response.body()) {
            if (response.code() == HTTP_CONFLICT) {
                // The Issuer already tracks an issuance process for this holderPid, which happens when a request is re-sent
                // after having been interrupted, e.g. by a restart. It was accepted earlier, so this is not a failure. The
                // Issuer-assigned ID is not disclosed here, it gets recorded once the credentials are delivered.
                monitor.debug("Issuer reports an already existing issuance process, treating the re-sent request as accepted");
                return StatusResult.success(UNKNOWN_ISSUER_PID);
            }
            if (response.isSuccessful()) {
                var location = response.header("Location");
                if (location != null && !location.isBlank()) {
                    var segments = location.split("/");
                    return StatusResult.success(segments[segments.length - 1]);
                }
                return StatusResult.success(body.string());
            } else {
                // an Issuer that refuses the request as it stands would refuse the identical retry just the same, whereas
                // a server-side error says nothing about the request and may well be gone by the next attempt
                var status = response.code() >= 400 && response.code() < 500 ? FATAL_ERROR : ERROR_RETRY;
                return StatusResult.failure(status, "Error sending DCP Credential Request: code: '%s', message: '%s', body: '%s'"
                        .formatted(response.code(), response.message(), body.string()));
            }
        } catch (IOException e) {
            return StatusResult.failure(ERROR_RETRY, "Error sending DCP Credential Request: code: '%s', message: '%s'"
                    .formatted(response.code(), response.message()));
        }
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
                .map(participantContext -> Map.of(
                        ISSUED_AT, Instant.now().toString(),
                        AUDIENCE, audience,
                        ISSUER, participantContext.getDid(),
                        SUBJECT, participantContext.getDid(),
                        EXPIRATION_TIME, Instant.now().plus(5, ChronoUnit.MINUTES).toString())
                )
                .compose(siTokenClaims -> secureTokenService
                        .createToken(participantContextId, siTokenClaims, configuration.bearerAccessScope()));
    }

    private Result<IdentityHubParticipantContext> getParticipantContext(String participantContextId) {
        var result = participantContextService.getParticipantContext(participantContextId);
        if (result.failed()) {
            return failure("Invalid participant");
        }
        return Result.success(result.getContent());

    }

    /**
     * Extracts the {@code CredentialRequest} service endpoint from the DID document.
     *
     * @param request The request whose Issuer's DID document is resolved
     * @return A result containing the service endpoint. An unresolvable DID and a document without such an endpoint are
     *         both terminal: the resolver does not tell a DID that does not exist from one that is momentarily
     *         unavailable, and a request whose Issuer cannot be found is not going to succeed by being repeated.
     */
    private StatusResult<String> getCredentialRequestEndpoint(HolderCredentialRequest request) {
        var didDocument = didResolverRegistry.resolve(request.getIssuerDid());
        if (didDocument.failed()) {
            return StatusResult.failure(FATAL_ERROR, didDocument.getFailureDetail());
        }
        var service = didDocument.getContent().getService().stream()
                .filter(s -> s.getType().equalsIgnoreCase(ISSUER_SERVICE_ENDPOINT_TYPE))
                .findAny();
        return service.map(s -> StatusResult.success(s.getServiceEndpoint()))
                .orElseGet(() -> StatusResult.failure(FATAL_ERROR,
                        "The Issuer's DID Document does not contain any '%s' endpoint".formatted(ISSUER_SERVICE_ENDPOINT_TYPE)));
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

        public Builder jsonLd(JsonLd jsonLd) {
            manager.jsonLd = jsonLd;
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

        public Builder participantContextService(IdentityHubParticipantContextService participantContextService) {
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

        public Builder configuration(CredentialRequestConfiguration configuration) {
            manager.configuration = configuration;
            return this;
        }

        @Override
        public CredentialRequestManagerImpl build() {
            super.build();
            requireNonNull(manager.didResolverRegistry);
            requireNonNull(manager.dcpTypeTransformerRegistry);
            requireNonNull(manager.jsonLd);
            requireNonNull(manager.httpClient);
            requireNonNull(manager.secureTokenService);
            requireNonNull(manager.transactionContext);
            requireNonNull(manager.participantContextService);
            return manager;
        }
    }
}
