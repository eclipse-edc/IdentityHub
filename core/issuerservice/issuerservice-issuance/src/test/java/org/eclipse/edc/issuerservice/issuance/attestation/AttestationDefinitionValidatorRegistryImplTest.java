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

import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionValidatorRegistry;
import org.eclipse.edc.issuerservice.spi.issuance.model.AttestationDefinition;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

class AttestationDefinitionValidatorRegistryImplTest {

    protected AttestationDefinitionValidatorRegistry registry = new AttestationDefinitionValidatorRegistryImpl();

    @Test
    void validateDefinition() {
        registry.registerValidator("test", def -> ValidationResult.success());
        var result = registry.validateDefinition(createAttestationDefinition("id", "test", Map.of("key", "value")));
        assertThat(result).isSucceeded();
    }


    @Test
    void validateDefinition_shouldFails_whenTypeNotFound() {
        var result = registry.validateDefinition(createAttestationDefinition("id", "test", Map.of("key", "value")));
        assertThat(result).isFailed().detail().contains("Unknown attestation type: test");
    }

    private AttestationDefinition createAttestationDefinition(String id, String type, Map<String, Object> configuration) {
        return AttestationDefinition.Builder.newInstance()
                .id(id)
                .attestationType(type)
                .participantContextId(UUID.randomUUID().toString())
                .configuration(configuration).build();
    }
}