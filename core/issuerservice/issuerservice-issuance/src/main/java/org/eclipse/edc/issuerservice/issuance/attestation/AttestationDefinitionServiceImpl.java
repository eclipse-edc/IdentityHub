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

import static java.util.Optional.ofNullable;
import static org.eclipse.edc.spi.result.ServiceResult.from;

public class AttestationDefinitionServiceImpl implements AttestationDefinitionService {

    private final TransactionContext transactionContext;
    private final AttestationDefinitionStore attestationDefinitionStore;
    private final AttestationDefinitionValidatorRegistry definitionValidatorRegistry;

    public AttestationDefinitionServiceImpl(TransactionContext transactionContext,
                                            AttestationDefinitionStore attestationDefinitionStore,
                                            AttestationDefinitionValidatorRegistry definitionValidatorRegistry) {
        this.transactionContext = transactionContext;
        this.attestationDefinitionStore = attestationDefinitionStore;
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
    public ServiceResult<AttestationDefinition> getAttestationById(String attestationId) {
        return transactionContext.execute(() -> ofNullable(attestationDefinitionStore.resolveDefinition(attestationId))
                .map(ServiceResult::success)
                .orElseGet(() -> ServiceResult.notFound("No attestation with id %s was found".formatted(attestationId))));
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
