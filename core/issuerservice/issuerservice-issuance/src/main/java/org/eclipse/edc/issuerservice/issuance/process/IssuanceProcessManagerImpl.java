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
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.issuerservice.spi.credentials.CredentialStatusService;
import org.eclipse.edc.issuerservice.spi.issuance.credentialdefinition.store.CredentialDefinitionStore;
import org.eclipse.edc.issuerservice.spi.issuance.delivery.CredentialStorageClient;
import org.eclipse.edc.issuerservice.spi.issuance.generator.CredentialGenerationRequest;
import org.eclipse.edc.issuerservice.spi.issuance.generator.CredentialGeneratorRegistry;
import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialDefinition;
import org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcess;
import org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcessStates;
import org.eclipse.edc.issuerservice.spi.issuance.process.IssuanceProcessManager;
import org.eclipse.edc.issuerservice.spi.issuance.process.store.IssuanceProcessStore;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.statemachine.AbstractStateEntityManager;
import org.eclipse.edc.statemachine.Processor;
import org.eclipse.edc.statemachine.ProcessorImpl;
import org.eclipse.edc.statemachine.StateMachineManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;

import static org.eclipse.edc.spi.persistence.StateEntityStore.hasState;
import static org.eclipse.edc.spi.persistence.StateEntityStore.isNotPending;
import static org.eclipse.edc.statemachine.retry.processor.Process.result;

public class IssuanceProcessManagerImpl extends AbstractStateEntityManager<IssuanceProcess, IssuanceProcessStore> implements IssuanceProcessManager {

    private CredentialGeneratorRegistry credentialGenerator;
    private CredentialDefinitionStore credentialDefinitionStore;
    private CredentialStore credentialStore;
    private CredentialStorageClient credentialStorageClient;
    private CredentialStatusService credentialStatusService;

    private IssuanceProcessManagerImpl() {
    }

    @Override
    protected StateMachineManager.Builder configureStateMachineManager(StateMachineManager.Builder builder) {
        return builder
                .processor(processIssuanceInState(IssuanceProcessStates.APPROVED, this::processApproved));
    }

    /**
     * Process APPROVED issuance process
     */
    @WithSpan
    private boolean processApproved(IssuanceProcess process) {
        return entityRetryProcessFactory.retryProcessor(process)
                .doProcess(result("Generate Credentials", (p, result) -> generateCredential(p)))
                .doProcess(result("Add Credentials to StatusList", this::addCredentialsToStatusList))
                .doProcess(result("Deliver Credentials", this::deliverCredentials))
                .doProcess(result("Store Credentials", this::storeCredential))
                .onSuccess((t, response) -> transitionToDelivered(t))
                .onFailure((t, throwable) -> transitionToApproved(t))
                .onFinalFailure(this::transitionToError)
                .execute();
    }

    private StatusResult<Collection<VerifiableCredentialContainer>> addCredentialsToStatusList(IssuanceProcess issuanceProcess, Collection<VerifiableCredentialContainer> newCredentials) {

        var updatedCredentials = new ArrayList<VerifiableCredentialContainer>();
        for (var cred : newCredentials) {
            var result = credentialStatusService.addCredential(issuanceProcess.getParticipantContextId(), cred.credential());
            if (result.failed()) {
                return StatusResult.failure(ResponseStatus.FATAL_ERROR, "Failed to add credential to status list: %s".formatted(result.getFailureDetail()));
            }
            var updatedHolderCredential = result.getContent();
            var signedCredential = credentialGenerator.signCredential(issuanceProcess.getParticipantContextId(), updatedHolderCredential, cred.format());

            if (signedCredential.failed()) {
                return StatusResult.failure(ResponseStatus.FATAL_ERROR, "Error regenerating the user credential: %s".formatted(signedCredential.getFailureDetail()));
            }

            updatedCredentials.add(signedCredential.getContent());
        }
        return StatusResult.success(updatedCredentials);
    }


    private StatusResult<Collection<VerifiableCredentialContainer>> generateCredential(IssuanceProcess process) {
        return StatusResult.from(fetchCredentialDefinitions(process))
                .compose(credentialDefinitions -> generateCredential(process, credentialDefinitions));
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

    private StatusResult<Collection<VerifiableCredentialContainer>> storeCredential(IssuanceProcess process, Collection<VerifiableCredentialContainer> credentials) {
        for (var credential : credentials) {
            var resource = toResource(process, credential);
            var result = credentialStore.create(resource);
            if (result.failed()) {
                return StatusResult.failure(ResponseStatus.ERROR_RETRY, result.getFailureDetail());
            }
        }
        return StatusResult.success(credentials);
    }

    private VerifiableCredentialResource toResource(IssuanceProcess process, VerifiableCredentialContainer credentialContainer) {
        return VerifiableCredentialResource.Builder.newInstance()
                .issuerId(credentialContainer.credential().getIssuer().id())
                .holderId(extractHolder(credentialContainer.credential()))
                .participantContextId(process.getParticipantContextId())
                .state(VcStatus.ISSUED)
                .credential(new VerifiableCredentialContainer(null, credentialContainer.format(), credentialContainer.credential())).build();
    }

    private String extractHolder(VerifiableCredential credential) {
        return credential.getCredentialSubject().stream().findFirst().map(CredentialSubject::getId).orElse(null);
    }

    private StatusResult<Collection<VerifiableCredentialContainer>> deliverCredentials(IssuanceProcess process, Collection<VerifiableCredentialContainer> credentials) {
        var result = credentialStorageClient.deliverCredentials(process, credentials);
        if (result.succeeded()) {
            return StatusResult.success(credentials);
        } else return StatusResult.failure(ResponseStatus.ERROR_RETRY);
    }

    private void transitionToDelivered(IssuanceProcess process) {
        process.transitionToDelivered();
        update(process);
    }

    private void transitionToApproved(IssuanceProcess process) {
        process.transitionToApproved();
        update(process);
    }

    private void transitionToError(IssuanceProcess process) {
        process.transitionToError();
        update(process);
    }

    private void transitionToError(IssuanceProcess process, Throwable throwable) {
        transitionToError(process, throwable.getMessage());
    }

    private void transitionToError(IssuanceProcess process, String message) {
        process.setErrorDetail(message);
        monitor.warning(message);
        transitionToError(process);
    }

    private Processor processIssuanceInState(IssuanceProcessStates state, Function<IssuanceProcess, Boolean> function) {
        var filter = new Criterion[]{hasState(state.code()), isNotPending()};
        return createProcessor(function, filter);
    }

    private ProcessorImpl<IssuanceProcess> createProcessor(Function<IssuanceProcess, Boolean> function, Criterion[] filter) {
        return ProcessorImpl.Builder.newInstance(() -> store.nextNotLeased(batchSize, filter))
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
            return manager;
        }
    }
}
