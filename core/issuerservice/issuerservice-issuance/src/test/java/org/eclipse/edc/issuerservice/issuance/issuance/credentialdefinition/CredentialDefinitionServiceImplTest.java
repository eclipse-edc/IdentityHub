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

package org.eclipse.edc.issuerservice.issuance.issuance.credentialdefinition;

import org.eclipse.edc.issuerservice.issuance.credentialdefinition.CredentialDefinitionServiceImpl;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionStore;
import org.eclipse.edc.issuerservice.spi.issuance.credentialdefinition.CredentialDefinitionService;
import org.eclipse.edc.issuerservice.spi.issuance.credentialdefinition.store.CredentialDefinitionStore;
import org.eclipse.edc.issuerservice.spi.issuance.model.AttestationDefinition;
import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialDefinition;
import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialRuleDefinition;
import org.eclipse.edc.issuerservice.spi.issuance.rule.CredentialRuleDefinitionValidatorRegistry;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Violation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat.VC1_0_JWT;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class CredentialDefinitionServiceImplTest {


    private final CredentialDefinitionStore credentialDefinitionStore = mock();
    private final AttestationDefinitionStore attestationDefinitionStore = mock();
    private final CredentialRuleDefinitionValidatorRegistry ruleDefinitionValidatorRegistry = mock();
    private final CredentialDefinitionService credentialDefinitionService = new CredentialDefinitionServiceImpl(new NoopTransactionContext(), credentialDefinitionStore, attestationDefinitionStore, ruleDefinitionValidatorRegistry);

    @Test
    void createCredentialDefinition() {

        var definition = credentialDefinition();

        var attestations = definition.getAttestations().stream().map(id -> createAttestationDefinition(id, "type", Map.of()))
                .toList();

        when(attestationDefinitionStore.query(any())).thenReturn(StoreResult.success(attestations));
        when(credentialDefinitionStore.create(definition)).thenReturn(StoreResult.success());
        when(ruleDefinitionValidatorRegistry.validateDefinition(any())).thenReturn(ValidationResult.success());

        assertThat(credentialDefinitionService.createCredentialDefinition(definition)).isSucceeded();

        verify(credentialDefinitionStore).create(definition);
    }

    @Test
    void createCredentialDefinition_whenStoreFails() {

        var definition = credentialDefinition();
        var attestations = definition.getAttestations().stream().map(id -> createAttestationDefinition(id, "type", Map.of()))
                .toList();

        when(attestationDefinitionStore.query(any())).thenReturn(StoreResult.success(attestations));
        when(credentialDefinitionStore.create(definition)).thenReturn(StoreResult.alreadyExists("already exists"));
        when(ruleDefinitionValidatorRegistry.validateDefinition(any())).thenReturn(ValidationResult.success());
        assertThat(credentialDefinitionService.createCredentialDefinition(definition)).isFailed();

    }

    @Test
    void createCredentialDefinition_whenAttestationsValidationFails() {

        var definition = credentialDefinition();
        var attestations = definition.getAttestations().stream().map(id -> createAttestationDefinition(id, "type", Map.of()))
                .toList();

        when(attestationDefinitionStore.query(any())).thenReturn(StoreResult.success(attestations));
        when(credentialDefinitionStore.create(definition)).thenReturn(StoreResult.success());
        when(ruleDefinitionValidatorRegistry.validateDefinition(any())).thenReturn(ValidationResult.failure(Violation.violation("violation", "path")));

        assertThat(credentialDefinitionService.createCredentialDefinition(definition)).isFailed()
                .detail().contains("violation");


        verifyNoInteractions(credentialDefinitionStore);
    }

    @Test
    void createCredentialDefinition_whenRulesValidationFails() {

        var definition = credentialDefinition();

        when(attestationDefinitionStore.query(any())).thenReturn(StoreResult.success(List.of()));
        when(credentialDefinitionStore.create(definition)).thenReturn(StoreResult.success());

        assertThat(credentialDefinitionService.createCredentialDefinition(definition)).isFailed();

        verifyNoInteractions(credentialDefinitionStore);
    }


    @Test
    void updateCredentialDefinition() {

        var definition = credentialDefinition();

        var attestations = definition.getAttestations().stream().map(id -> createAttestationDefinition(id, "type", Map.of()))
                .toList();

        when(attestationDefinitionStore.query(any())).thenReturn(StoreResult.success(attestations));
        when(credentialDefinitionStore.update(definition)).thenReturn(StoreResult.success());
        when(ruleDefinitionValidatorRegistry.validateDefinition(any())).thenReturn(ValidationResult.success());

        assertThat(credentialDefinitionService.updateCredentialDefinition(definition)).isSucceeded();

        verify(credentialDefinitionStore).update(definition);
    }

    @Test
    void updateCredentialDefinition_whenAttestationsValidationFails() {

        var definition = credentialDefinition();

        when(attestationDefinitionStore.query(any())).thenReturn(StoreResult.success(List.of()));
        when(credentialDefinitionStore.update(definition)).thenReturn(StoreResult.success());

        assertThat(credentialDefinitionService.updateCredentialDefinition(definition)).isFailed();

        verifyNoInteractions(credentialDefinitionStore);
    }

    @Test
    void updateCredentialDefinition_whenRulesValidationFails() {

        var definition = credentialDefinition();

        var attestations = definition.getAttestations().stream().map(id -> createAttestationDefinition(id, "type", Map.of()))
                .toList();

        when(attestationDefinitionStore.query(any())).thenReturn(StoreResult.success(attestations));
        when(credentialDefinitionStore.update(definition)).thenReturn(StoreResult.success());
        when(ruleDefinitionValidatorRegistry.validateDefinition(any())).thenReturn(ValidationResult.failure(Violation.violation("violation", "path")));

        assertThat(credentialDefinitionService.updateCredentialDefinition(definition)).isFailed()
                .detail().contains("violation");

        verifyNoInteractions(credentialDefinitionStore);
    }

    @Test
    void updateCredentialDefinition_whenStoreFails() {

        var definition = credentialDefinition();
        var attestations = definition.getAttestations().stream().map(id -> createAttestationDefinition(id, "type", Map.of()))
                .toList();

        when(attestationDefinitionStore.query(any())).thenReturn(StoreResult.success(attestations));
        when(credentialDefinitionStore.update(definition)).thenReturn(StoreResult.alreadyExists("already exists"));
        when(ruleDefinitionValidatorRegistry.validateDefinition(any())).thenReturn(ValidationResult.success());

        assertThat(credentialDefinitionService.updateCredentialDefinition(definition)).isFailed();

    }

    @Test
    void findCredentialDefinitionById() {

        var definition = credentialDefinition();

        when(credentialDefinitionStore.findById(definition.getId())).thenReturn(StoreResult.success(definition));

        assertThat(credentialDefinitionService.findCredentialDefinitionById(definition.getId()))
                .isSucceeded()
                .usingRecursiveComparison().isEqualTo(definition);

    }


    @Test
    void queryCredentialDefinitions() {

        var definition = credentialDefinition();

        when(credentialDefinitionStore.query(any())).thenReturn(StoreResult.success(List.of(definition)));

        assertThat(credentialDefinitionService.queryCredentialDefinitions(any()))
                .isSucceeded()
                .satisfies(credentialDefinitions ->
                        assertThat(credentialDefinitions).hasSize(1).contains(definition));

    }

    @Test
    void queryCredentialDefinitions_whenStoreFails() {


        when(credentialDefinitionStore.query(any())).thenReturn(StoreResult.generalError("error"));

        assertThat(credentialDefinitionService.queryCredentialDefinitions(any()))
                .isFailed();

    }

    @Test
    void deleteCredentialDefinition() {

        var id = UUID.randomUUID().toString();

        when(credentialDefinitionStore.deleteById(id)).thenReturn(StoreResult.success());

        assertThat(credentialDefinitionService.deleteCredentialDefinition(id))
                .isSucceeded();
    }

    @Test
    void deleteCredentialDefinition_whenStoreFails() {

        var id = UUID.randomUUID().toString();

        when(credentialDefinitionStore.deleteById(id)).thenReturn(StoreResult.notFound("not found"));

        assertThat(credentialDefinitionService.deleteCredentialDefinition(id))
                .isFailed();
    }

    private CredentialDefinition credentialDefinition() {
        return credentialDefinition(UUID.randomUUID().toString(), "Membership");
    }

    private AttestationDefinition createAttestationDefinition(String id, String type, Map<String, Object> configuration) {
        return AttestationDefinition.Builder.newInstance()
                .id(id)
                .attestationType(type)
                .participantContextId(UUID.randomUUID().toString())
                .configuration(configuration).build();
    }

    private CredentialDefinition credentialDefinition(String id, String type) {
        return CredentialDefinition.Builder.newInstance().id(id).jsonSchema("")
                .jsonSchemaUrl("http://example.com/schema").validity(1000)
                .attestation("test-attestation")
                .participantContextId(UUID.randomUUID().toString())
                .rule(new CredentialRuleDefinition("test-rule", Map.of()))
                .credentialType(type)
                .formatFrom(VC1_0_JWT)
                .build();
    }
}
