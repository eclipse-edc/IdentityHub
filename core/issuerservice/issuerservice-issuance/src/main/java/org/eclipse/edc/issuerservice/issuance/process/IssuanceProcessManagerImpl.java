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

package org.eclipse.edc.issuerservice.issuance.process;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialUsage;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.issuerservice.spi.credentials.CredentialStatusService;
import org.eclipse.edc.issuerservice.spi.issuance.credentialdefinition.store.CredentialDefinitionStore;
import org.eclipse.edc.issuerservice.spi.issuance.delivery.CredentialStorageClient;
import org.eclipse.edc.issuerservice.spi.issuance.events.IssuanceObservable;
import org.eclipse.edc.issuerservice.spi.issuance.generator.CredentialGenerationRequest;
import org.eclipse.edc.issuerservice.spi.issuance.generator.CredentialGeneratorRegistry;
import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialDefinition;
import org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcess;
import org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcessStates;
import org.eclipse.edc.issuerservice.spi.issuance.process.IssuanceProcessManager;
import org.eclipse.edc.issuerservice.spi.issuance.process.store.IssuanceProcessStore;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.statemachine.AbstractStateEntityManager;
import org.eclipse.edc.statemachine.Processor;
import org.eclipse.edc.statemachine.ProcessorImpl;
import org.eclipse.edc.statemachine.StateMachineManager;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource.METADATA_ISSUANCE_PROCESS_ID;
import static org.eclipse.edc.participantcontext.spi.types.ParticipantResource.filterByParticipantContextId;
import static org.eclipse.edc.spi.persistence.StateEntityStore.hasState;
import static org.eclipse.edc.spi.persistence.StateEntityStore.isNotPending;
import static org.eclipse.edc.statemachine.retry.processor.Process.result;

public class IssuanceProcessManagerImpl extends AbstractStateEntityManager<IssuanceProcess, IssuanceProcessStore> implements IssuanceProcessManager {

    private IssuanceObservable observable;
    private Vault vault;
    private CredentialGeneratorRegistry credentialGenerator;
    private CredentialDefinitionStore credentialDefinitionStore;
    private CredentialStore credentialStore;
    private CredentialStorageClient credentialStorageClient;
    private CredentialStatusService credentialStatusService;
    private TransactionContext transactionContext;

    private IssuanceProcessManagerImpl() {
    }

    @Override
    protected StateMachineManager.Builder configureStateMachineManager(StateMachineManager.Builder builder) {
        return builder
                .processor(processIssuanceInState(IssuanceProcessStates.APPROVED, this::processApproved))
                .processor(processIssuanceInState(IssuanceProcessStates.DELIVERING, this::processDelivering));
    }

    /**
     * Process APPROVED issuance process: generates the credentials and records them, before any of them is delivered, so
     * that the Issuer keeps track of every credential a Holder may receive, even if the outcome of a delivery is unknown.
     */
    @WithSpan(value = "issuance.approved")
    private CompletableFuture<StatusResult<Void>> processApproved(IssuanceProcess process) {
        observable.invokeForEach(l -> l.approved(process));
        return entityRetryProcessFactory.retryProcessor(process)
                .doProcess(result("Record Credentials", (p, result) -> recordCredentialsOnce(p)))
                .onSuccess((t, unused) -> transitionToDelivering(t))
                .onFailure((t, throwable) -> transitionToApproved(t))
                .onFinalFailure(this::transitionToError)
                .execute();
    }

    /**
     * Process DELIVERING issuance process: delivers the recorded credentials to the Holder. Every attempt delivers the same
     * credentials.
     */
    @WithSpan(value = "issuance.delivering")
    private CompletableFuture<StatusResult<Void>> processDelivering(IssuanceProcess process) {
        return entityRetryProcessFactory.retryProcessor(process)
                .doProcess(result("Sign Credentials", (p, result) -> signRecordedCredentials(p)))
                .doProcess(result("Deliver Credentials", this::deliverCredentials))
                .onSuccess((t, credentials) -> {
                    transitionToDelivered(t);
                    observable.invokeForEach(l -> l.delivered(process, credentials));
                })
                .onFailure((t, throwable) -> transitionToDelivering(t))
                .onFinalFailure(this::transitionToError)
                .execute();
    }

    /**
     * Generates and records the credentials of the process, unless they are recorded already, e.g. by an attempt that was
     * interrupted before the process was saved, or by another runtime that held the process before.
     */
    private StatusResult<Void> recordCredentialsOnce(IssuanceProcess process) {
        var recordedCredentials = findRecordedCredentials(process);
        if (recordedCredentials.failed()) {
            return StatusResult.failure(ResponseStatus.ERROR_RETRY, "Failed to look up the credentials recorded for issuance process '%s': %s"
                    .formatted(process.getId(), recordedCredentials.getFailureDetail()));
        }
        if (!recordedCredentials.getContent().isEmpty()) {
            return StatusResult.success();
        }
        return generateCredential(process)
                .compose(credentials -> addCredentialsToStatusList(process, credentials))
                .compose(credentials -> writeResources(process, credentials.stream().map(credential -> toResource(process, credential)).toList(), credentialStore::create));
    }

    /**
     * Prepares the recorded credentials of the process for delivery. The Issuer does not keep signed credentials, so they
     * are signed for every delivery attempt. Their validity starts when they are signed and keeps its original length, so
     * that the Holder gets all of it, however long the delivery takes. The records are updated accordingly before the
     * credentials are delivered.
     */
    private StatusResult<Collection<VerifiableCredentialContainer>> signRecordedCredentials(IssuanceProcess process) {
        var recordedCredentials = findRecordedCredentials(process);
        if (recordedCredentials.failed()) {
            return StatusResult.failure(ResponseStatus.ERROR_RETRY, "Failed to look up the credentials recorded for issuance process '%s': %s"
                    .formatted(process.getId(), recordedCredentials.getFailureDetail()));
        }
        if (recordedCredentials.getContent().isEmpty()) {
            return StatusResult.failure(ResponseStatus.FATAL_ERROR, "No credentials are recorded for issuance process '%s'".formatted(process.getId()));
        }

        var records = recordedCredentials.getContent().stream().map(this::validFromNow).toList();
        var updateResult = writeResources(process, records, credentialStore::update);
        if (updateResult.failed()) {
            return updateResult.mapFailure();
        }

        var signedCredentials = new ArrayList<VerifiableCredentialContainer>();
        for (var record : records) {
            var container = record.getVerifiableCredential();
            var signedCredential = credentialGenerator.signCredential(process.getParticipantContextId(), container.credential(), container.format());
            if (signedCredential.failed()) {
                return StatusResult.failure(ResponseStatus.FATAL_ERROR, "Error signing the recorded credential '%s': %s"
                        .formatted(record.getId(), signedCredential.getFailureDetail()));
            }
            signedCredentials.add(signedCredential.getContent());
        }
        return StatusResult.success(signedCredentials);
    }

    private StoreResult<Collection<VerifiableCredentialResource>> findRecordedCredentials(IssuanceProcess process) {
        var query = QuerySpec.Builder.newInstance()
                .filter(filterByParticipantContextId(process.getParticipantContextId()))
                .filter(Criterion.criterion("usage", "=", CredentialUsage.IssuanceTracking.toString()))
                .filter(Criterion.criterion("metadata." + METADATA_ISSUANCE_PROCESS_ID, "=", process.getId()))
                .build();
        return credentialStore.query(query);
    }

    /**
     * Moves the validity of a recorded credential, so that it starts now and keeps its original length.
     */
    private VerifiableCredentialResource validFromNow(VerifiableCredentialResource resource) {
        var container = resource.getVerifiableCredential();
        var credential = container.credential();
        var validity = credential.getExpirationDate() == null ? null : Duration.between(credential.getIssuanceDate(), credential.getExpirationDate());
        var now = clock.instant();
        var validFromNow = credential.toBuilder()
                .issuanceDate(now)
                .expirationDate(validity == null ? null : now.plus(validity))
                .build();
        return resource.toBuilder()
                .credential(new VerifiableCredentialContainer(null, container.format(), validFromNow))
                .build();
    }

    /**
     * Writes all given resources, or none of them: they are written in one transaction, which is rolled back if one of them
     * cannot be written. The credentials are not delivered unless their resources are written, so a failure is retried.
     */
    private StatusResult<Void> writeResources(IssuanceProcess process, Collection<VerifiableCredentialResource> resources,
                                              Function<VerifiableCredentialResource, StoreResult<Void>> write) {
        try {
            transactionContext.execute(() -> {
                for (var res : resources) {
                    var result = write.apply(res);
                    if (result.failed()) {
                        // the transaction only rolls back the resources written before if an exception is thrown
                        throw new EdcException(result.getFailureDetail());
                    }
                }
            });
            return StatusResult.success();
        } catch (EdcException e) {
            return StatusResult.failure(ResponseStatus.ERROR_RETRY, "Failed to write the credential resources of issuance process '%s': %s"
                    .formatted(process.getId(), e.getMessage()));
        }
    }

    /**
     * Adds a status list entry to each credential. The credentials are not signed again here, but each time they are
     * delivered.
     */
    @WithSpan(value = "issuance.add-credentials-to-status-list")
    private StatusResult<Collection<VerifiableCredentialContainer>> addCredentialsToStatusList(IssuanceProcess issuanceProcess, Collection<VerifiableCredentialContainer> newCredentials) {

        var updatedCredentials = new ArrayList<VerifiableCredentialContainer>();
        for (var cred : newCredentials) {
            var result = credentialStatusService.addCredential(issuanceProcess.getParticipantContextId(), cred.credential());
            if (result.failed()) {
                return StatusResult.failure(ResponseStatus.FATAL_ERROR, "Failed to add credential to status list: %s".formatted(result.getFailureDetail()));
            }
            updatedCredentials.add(new VerifiableCredentialContainer(null, cred.format(), result.getContent()));
        }
        return StatusResult.success(updatedCredentials);
    }


    private StatusResult<Collection<VerifiableCredentialContainer>> generateCredential(IssuanceProcess process) {
        return StatusResult.from(fetchCredentialDefinitions(process))
                .compose(credentialDefinitions -> generateCredential(process, credentialDefinitions))
                .onSuccess(creds -> observable.invokeForEach(l -> l.generated(process, creds)));
    }

    private StatusResult<Collection<VerifiableCredentialContainer>> generateCredential(IssuanceProcess process, Collection<CredentialDefinition> credentialDefinitions) {
        var requests = credentialDefinitions.stream()
                .map(credentialDefinition -> new CredentialGenerationRequest(credentialDefinition, process.getCredentialFormats().get(credentialDefinition.getId())))
                .toList();

        var result = credentialGenerator.generateCredentials(process.getParticipantContextId(), process.getHolderId(), requests, process.getClaims());
        if (result.succeeded()) {
            return StatusResult.success(result.getContent());
        } else {
            return StatusResult.failure(ResponseStatus.ERROR_RETRY, result.getFailureDetail());
        }
    }


    private StoreResult<Collection<CredentialDefinition>> fetchCredentialDefinitions(IssuanceProcess process) {
        var query = QuerySpec.Builder.newInstance()
                .filter(Criterion.criterion("id", "in", process.getCredentialDefinitions()))
                .build();
        return credentialDefinitionStore.query(query);
    }

    private VerifiableCredentialResource toResource(IssuanceProcess process, VerifiableCredentialContainer credentialContainer) {
        return VerifiableCredentialResource.Builder.newIssuanceTracker()
                .issuerId(credentialContainer.credential().getIssuer().id())
                .holderId(extractHolder(credentialContainer.credential()))
                .participantContextId(process.getParticipantContextId())
                .state(VcStatus.ISSUED)
                .metadata(METADATA_ISSUANCE_PROCESS_ID, process.getId())
                .credential(new VerifiableCredentialContainer(null, credentialContainer.format(), credentialContainer.credential())).build();
    }

    private String extractHolder(VerifiableCredential credential) {
        return credential.getCredentialSubject().stream().findFirst().map(CredentialSubject::getId).orElse(null);
    }

    @WithSpan(value = "issuance.deliver-credential")
    private StatusResult<Collection<VerifiableCredentialContainer>> deliverCredentials(IssuanceProcess process, Collection<VerifiableCredentialContainer> credentials) {
        var result = credentialStorageClient.deliverCredentials(process, credentials);
        if (result.succeeded()) {
            return StatusResult.success(credentials);
        } else return StatusResult.failure(ResponseStatus.ERROR_RETRY);
    }

    private void transitionToDelivered(IssuanceProcess process) {
        process.transitionToDelivered();
        update(process)
                .onSuccess(v -> discardHolderAccessToken(process))
                // the process is delivered again, e.g. by the runtime that holds its lease, which needs the access token for that
                .onFailure(f -> monitor.warning("Issuance process '%s' was delivered, but could not be saved as such: %s"
                        .formatted(process.getId(), f.getFailureDetail())));
    }

    private void transitionToApproved(IssuanceProcess process) {
        process.transitionToApproved();
        update(process);
    }

    private void transitionToDelivering(IssuanceProcess process) {
        process.transitionToDelivering();
        update(process);
    }

    private void transitionToError(IssuanceProcess process) {
        process.transitionToError();
        update(process);
        notifyHolderOfRejection(process);
        discardHolderAccessToken(process);
    }

    /**
     * Tells the Holder that the issuance it was told had been accepted has failed, so it stops waiting for credentials
     * that are never coming. Best effort: the failure is already recorded and is served by the Credential Request Status
     * API, so a Holder that never receives this still learns about it by polling.
     */
    private void notifyHolderOfRejection(IssuanceProcess process) {
        try {
            credentialStorageClient.deliverRejection(process, process.getErrorDetail())
                    .onFailure(f -> monitor.debug("Could not notify the Holder that issuance process '%s' was rejected: %s"
                            .formatted(process.getId(), f.getFailureDetail())));
        } catch (Exception e) {
            monitor.debug("Could not notify the Holder that issuance process '%s' was rejected: %s".formatted(process.getId(), e.getMessage()));
        }
    }

    /**
     * Removes the Holder's access token from the vault. The process is in a terminal state, so the token will not be
     * used again and must not be kept around.
     */
    private void discardHolderAccessToken(IssuanceProcess process) {
        // housekeeping must never keep the process from reaching its terminal state, so nothing here is allowed to escape
        try {
            vault.deleteSecret(process.getId())
                    .onFailure(f -> monitor.debug("Could not remove the access token for issuance process '%s': %s"
                            .formatted(process.getId(), f.getFailureDetail())));
        } catch (Exception e) {
            monitor.debug("Could not remove the access token for issuance process '%s': %s".formatted(process.getId(), e.getMessage()));
        }
    }

    private void transitionToError(IssuanceProcess process, Throwable throwable) {
        if (process.getState() == IssuanceProcessStates.DELIVERING.code()) {
            reportUnconfirmedDelivery(process, throwable.getMessage());
        }
        transitionToError(process, throwable.getMessage());
        observable.invokeForEach(l -> l.errored(process, throwable));
    }

    /**
     * A process that fails while delivering leaves its credentials recorded as ISSUED, although it is unknown whether the
     * Holder has received them, e.g. if a delivery timed out after the Holder had stored them. They are not revoked, because
     * that would leave the Holder to deal with a failure of the Issuer. Instead, their records need manual reconciliation.
     */
    private void reportUnconfirmedDelivery(IssuanceProcess process, String reason) {
        // nothing here may keep the process from reaching its terminal state
        String recordIds;
        try {
            recordIds = findRecordedCredentials(process)
                    .map(records -> records.stream().map(VerifiableCredentialResource::getId).collect(Collectors.joining(", ")))
                    .orElse(failure -> "unknown: %s".formatted(failure.getFailureDetail()));
        } catch (Exception e) {
            recordIds = "unknown: %s".formatted(e.getMessage());
        }
        monitor.severe(("Issuance process '%s' failed to deliver its credentials to Holder '%s' (holderPid '%s'): %s. The credentials " +
                "are recorded as ISSUED (%s), but may or may not have reached the Holder. Manual reconciliation is needed.")
                .formatted(process.getId(), process.getHolderId(), process.getHolderPid(), reason, recordIds));
    }

    private void transitionToError(IssuanceProcess process, String message) {
        process.setErrorDetail(message);
        monitor.warning(message);
        transitionToError(process);
    }

    private Processor processIssuanceInState(IssuanceProcessStates state, Function<IssuanceProcess, CompletableFuture<StatusResult<Void>>> function) {
        var filter = new Criterion[]{ hasState(state.code()), isNotPending() };
        return createProcessor(function, filter);
    }

    private ProcessorImpl<IssuanceProcess> createProcessor(Function<IssuanceProcess, CompletableFuture<StatusResult<Void>>> function, Criterion[] filter) {
        return ProcessorImpl.Builder.newInstance(() -> store.nextNotLeased(batchSize, filter), entityRetryProcessConfiguration, clock, monitor)
                .process(telemetry.contextPropagationMiddleware(function))
                .onNotProcessed(this::breakLease)
                .build();
    }

    public static class Builder
            extends AbstractStateEntityManager.Builder<IssuanceProcess, IssuanceProcessStore, IssuanceProcessManagerImpl, Builder> {

        private Builder() {
            super(new IssuanceProcessManagerImpl());
        }

        public static Builder newInstance() {
            return new Builder();
        }


        public Builder credentialGeneratorRegistry(CredentialGeneratorRegistry credentialGenerator) {
            manager.credentialGenerator = credentialGenerator;
            return this;
        }

        public Builder credentialDefinitionStore(CredentialDefinitionStore credentialDefinitionStore) {
            manager.credentialDefinitionStore = credentialDefinitionStore;
            return this;
        }

        public Builder credentialStore(CredentialStore credentialStore) {
            manager.credentialStore = credentialStore;
            return this;
        }

        public Builder credentialStorageClient(CredentialStorageClient credentialStorageClient) {
            manager.credentialStorageClient = credentialStorageClient;
            return this;
        }

        public Builder credentialStatusService(CredentialStatusService credentialStatusService) {
            manager.credentialStatusService = credentialStatusService;
            return this;
        }

        public Builder vault(Vault vault) {
            manager.vault = vault;
            return this;
        }

        public Builder transactionContext(TransactionContext transactionContext) {
            manager.transactionContext = transactionContext;
            return this;
        }

        public Builder observable(IssuanceObservable observable) {
            manager.observable = observable;
            return this;
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public IssuanceProcessManagerImpl build() {
            super.build();
            Objects.requireNonNull(this.manager.credentialGenerator, "Credential generator");
            Objects.requireNonNull(this.manager.credentialDefinitionStore, "Credential definition store");
            Objects.requireNonNull(this.manager.credentialStore, "Credential store");
            Objects.requireNonNull(this.manager.credentialStorageClient, "Credential service client");
            Objects.requireNonNull(this.manager.credentialStatusService, "Credential status service");
            Objects.requireNonNull(this.manager.observable, "IssuanceObservable");
            Objects.requireNonNull(this.manager.transactionContext, "TransactionContext");
            return manager;
        }
    }
}
