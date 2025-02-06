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

package org.eclipse.edc.issuerservice.credentialdefinition;

import org.eclipse.edc.identityhub.spi.issuance.credentials.attestation.AttestationDefinitionStore;
import org.eclipse.edc.identityhub.spi.issuance.credentials.model.AttestationDefinition;
import org.eclipse.edc.identityhub.spi.issuance.credentials.model.CredentialDefinition;
import org.eclipse.edc.issuerservice.spi.credentialdefinition.CredentialDefinitionService;
import org.eclipse.edc.issuerservice.spi.credentialdefinition.store.CredentialDefinitionStore;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.Collection;
import java.util.stream.Collectors;

import static org.eclipse.edc.spi.result.ServiceResult.from;

public class CredentialDefinitionServiceImpl implements CredentialDefinitionService {
    private final TransactionContext transactionContext;
    private final CredentialDefinitionStore credentialDefinitionStore;
    private final AttestationDefinitionStore attestationDefinitionStore;

    public CredentialDefinitionServiceImpl(TransactionContext transactionContext, CredentialDefinitionStore credentialDefinitionStore, AttestationDefinitionStore attestationDefinitionStore) {
        this.transactionContext = transactionContext;
        this.credentialDefinitionStore = credentialDefinitionStore;
        this.attestationDefinitionStore = attestationDefinitionStore;
    }

    @Override
    public ServiceResult<Void> createCredentialDefinition(CredentialDefinition credentialDefinition) {
        return transactionContext.execute(() -> internalCreate(credentialDefinition));

    }

    @Override
    public ServiceResult<Void> deleteCredentialDefinition(String credentialDefinitionId) {
        return transactionContext.execute(() -> from(credentialDefinitionStore.deleteById(credentialDefinitionId)));
    }

    @Override
    public ServiceResult<Void> updateCredentialDefinition(CredentialDefinition credentialDefinition) {
        return transactionContext.execute(() -> internalUpdate(credentialDefinition));
    }

    @Override
    public ServiceResult<Collection<CredentialDefinition>> queryCredentialDefinitions(QuerySpec querySpec) {
        return transactionContext.execute(() -> from(credentialDefinitionStore.query(querySpec)));

    }

    @Override
    public ServiceResult<CredentialDefinition> findCredentialDefinitionById(String credentialDefinitionId) {
        return transactionContext.execute(() -> from(credentialDefinitionStore.findById(credentialDefinitionId)));

    }

    private ServiceResult<Void> internalCreate(CredentialDefinition credentialDefinition) {
        return validateAttestations(credentialDefinition)
                .compose(u -> from(credentialDefinitionStore.create(credentialDefinition)));
    }

    private ServiceResult<Void> internalUpdate(CredentialDefinition credentialDefinition) {
        return validateAttestations(credentialDefinition)
                .compose(u -> from(credentialDefinitionStore.update(credentialDefinition)));
    }

    private ServiceResult<Void> validateAttestations(CredentialDefinition credentialDefinition) {
        var query = QuerySpec.Builder.newInstance()
                .filter(Criterion.criterion("id", "in", credentialDefinition.getAttestations()))
                .build();
        return from(attestationDefinitionStore.query(query))
                .compose(attestationDefinitions -> checkAttestations(credentialDefinition, attestationDefinitions));
    }

    private ServiceResult<Void> checkAttestations(CredentialDefinition credentialDefinition, Collection<AttestationDefinition> attestationDefinitions) {
        if (attestationDefinitions.size() != credentialDefinition.getAttestations().size()) {

            var attestationsIds = attestationDefinitions.stream().map(AttestationDefinition::id).collect(Collectors.toSet());

            var missingAttestations = credentialDefinition.getAttestations().stream()
                    .filter(attestationId -> !attestationsIds.contains(attestationId))
                    .collect(Collectors.toSet());

            return ServiceResult.badRequest("Attestation definitions [%s] not found".formatted(String.join(",", missingAttestations)));
        }
        return ServiceResult.success();
    }
}
