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

package org.eclipse.edc.issuerservice.issuance.attestation;

import org.eclipse.edc.issuerservice.spi.holder.model.Holder;
import org.eclipse.edc.issuerservice.spi.holder.store.HolderStore;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionService;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionStore;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionValidatorRegistry;
import org.eclipse.edc.issuerservice.spi.issuance.model.AttestationDefinition;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.edc.validator.spi.ValidationResult;

import java.util.Collection;
import java.util.Objects;

import static org.eclipse.edc.spi.result.ServiceResult.from;
import static org.eclipse.edc.spi.result.ServiceResult.fromFailure;

public class AttestationDefinitionServiceImpl implements AttestationDefinitionService {

    private final TransactionContext transactionContext;
    private final AttestationDefinitionStore attestationDefinitionStore;
    private final HolderStore holderStore;
    private final AttestationDefinitionValidatorRegistry definitionValidatorRegistry;

    public AttestationDefinitionServiceImpl(TransactionContext transactionContext,
                                            AttestationDefinitionStore attestationDefinitionStore,
                                            HolderStore holderStore,
                                            AttestationDefinitionValidatorRegistry definitionValidatorRegistry) {
        this.transactionContext = transactionContext;
        this.attestationDefinitionStore = attestationDefinitionStore;
        this.holderStore = holderStore;
        this.definitionValidatorRegistry = definitionValidatorRegistry;
    }

    @Override
    public ServiceResult<Void> createAttestation(AttestationDefinition attestationDefinition) {
        return transactionContext.execute(() -> validateAttestationDefinition(attestationDefinition)
                .compose(v -> from(attestationDefinitionStore.create(attestationDefinition))));
    }

    @Override
    public ServiceResult<Void> deleteAttestation(String attestationId) {
        return transactionContext.execute(() -> from(attestationDefinitionStore.deleteById(attestationId)));
    }

    @Override
    public ServiceResult<Boolean> linkAttestation(String attestationId, String participantId) {

        return transactionContext.execute(() -> {
            if (attestationDefinitionStore.resolveDefinition(attestationId) == null) {
                return ServiceResult.notFound("No attestation with id %s was found".formatted(attestationId));
            }

            var participantResult = holderStore.findById(participantId);
            if (participantResult.failed()) {
                return fromFailure(participantResult);
            }

            var participant = participantResult.getContent();
            if (participant.attestations().contains(attestationId)) { // no need to update, already linked
                return ServiceResult.success(false);
            }

            participant.addAttestation(attestationId);
            return from(holderStore.update(participant)).map(v -> true);

        });
    }

    @Override
    public ServiceResult<Boolean> unlinkAttestation(String attestationId, String participantId) {
        return transactionContext.execute(() -> {
            var participantResult = holderStore.findById(participantId);
            if (participantResult.failed()) {
                return fromFailure(participantResult);
            }

            var participant = participantResult.getContent();
            if (participant.removeAttestation(attestationId)) {
                return from(holderStore.update(participant)).map(v -> true);
            }
            return ServiceResult.success(false);
        });
    }

    @Override
    public ServiceResult<Collection<AttestationDefinition>> getAttestationsForParticipant(String participantId) {

        return transactionContext.execute(() -> ServiceResult.from(holderStore.findById(participantId)
                .map(Holder::attestations)
                .map(this::findForIds)));
    }

    @Override
    public ServiceResult<Collection<AttestationDefinition>> queryAttestations(QuerySpec querySpec) {
        return transactionContext.execute(() -> from(attestationDefinitionStore.query(querySpec)));
    }


    private ServiceResult<Void> validateAttestationDefinition(AttestationDefinition attestationDefinition) {
        return definitionValidatorRegistry.validateDefinition(attestationDefinition)
                .flatMap(this::toServiceResult);
    }

    private ServiceResult<Void> toServiceResult(ValidationResult validationResult) {
        return validationResult.failed() ? ServiceResult.badRequest(validationResult.getFailureDetail()) : ServiceResult.success();
    }

    private Collection<AttestationDefinition> findForIds(Collection<String> ids) {
        return ids.stream().map(attestationDefinitionStore::resolveDefinition).filter(Objects::nonNull).toList();
    }
}
