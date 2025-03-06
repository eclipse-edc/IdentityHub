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
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Violation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class AttestationDefinitionServiceImplTest {

    private final AttestationDefinitionStore attestationDefinitionStore = mock();
    private final AttestationDefinitionValidatorRegistry definitionValidatorRegistry = mock();

    private final AttestationDefinitionService attestationDefinitionService = new AttestationDefinitionServiceImpl(new NoopTransactionContext(),
            attestationDefinitionStore,
            definitionValidatorRegistry);

    @Test
    void createAttestation() {
        when(attestationDefinitionStore.create(any())).thenReturn(StoreResult.success());
        when(definitionValidatorRegistry.validateDefinition(any())).thenReturn(ValidationResult.success());

        assertThat(attestationDefinitionService.createAttestation(createAttestationDefinition("id", "type", Map.of())))
                .isSucceeded();
        verify(attestationDefinitionStore).create(any());
        verifyNoMoreInteractions(attestationDefinitionStore);
    }

    @Test
    void createAttestation_shouldFail_whenValidationFails() {
        when(attestationDefinitionStore.create(any())).thenReturn(StoreResult.success());
        when(definitionValidatorRegistry.validateDefinition(any())).thenReturn(ValidationResult.failure(Violation.violation("failure", "bar")));

        assertThat(attestationDefinitionService.createAttestation(createAttestationDefinition("id", "type", Map.of())))
                .isFailed()
                .detail().contains("failure");
    }

    @Test
    void deleteAttestation() {
        when(attestationDefinitionStore.deleteById(anyString())).thenReturn(StoreResult.success());

        assertThat(attestationDefinitionService.deleteAttestation("id"))
                .isSucceeded();
        verify(attestationDefinitionStore).deleteById(any());
        verifyNoMoreInteractions(attestationDefinitionStore);
    }

    @Test
    void getAttestationById() {

        when(attestationDefinitionStore.resolveDefinition(anyString())).thenReturn(createAttestationDefinition("1", "type", Map.of()));

        assertThat(attestationDefinitionService.getAttestationById("participant-id"))
                .isSucceeded();
    }

    @Test
    void getAttestationById_whenNotFound() {

        when(attestationDefinitionStore.resolveDefinition(anyString())).thenReturn(null);

        assertThat(attestationDefinitionService.getAttestationById("participant-id"))
                .isFailed();
    }

    @Test
    void queryAttestations() {
        when(attestationDefinitionStore.query(any()))
                .thenReturn(StoreResult.success(List.of(createAttestationDefinition("id", "type", Map.of()))));

        assertThat(attestationDefinitionService.queryAttestations(QuerySpec.max()))
                .isSucceeded();

        verify(attestationDefinitionStore).query(any());
        verifyNoMoreInteractions(attestationDefinitionStore);
    }

    private AttestationDefinition createAttestationDefinition(String id, String type, Map<String, Object> configuration) {
        return AttestationDefinition.Builder.newInstance()
                .id(id)
                .attestationType(type)
                .participantContextId(UUID.randomUUID().toString())
                .configuration(configuration).build();
    }
}