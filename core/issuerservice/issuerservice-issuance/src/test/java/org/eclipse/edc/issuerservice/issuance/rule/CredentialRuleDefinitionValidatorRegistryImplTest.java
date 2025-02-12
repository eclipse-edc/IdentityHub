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

package org.eclipse.edc.issuerservice.issuance.rule;

import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialRuleDefinition;
import org.eclipse.edc.issuerservice.spi.issuance.rule.CredentialRuleDefinitionValidatorRegistry;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

class CredentialRuleDefinitionValidatorRegistryImplTest {

    protected CredentialRuleDefinitionValidatorRegistry registry = new CredentialRuleDefinitionValidatorRegistryImpl();

    @Test
    void validateDefinition() {
        registry.registerValidator("test", def -> ValidationResult.success());
        var result = registry.validateDefinition(new CredentialRuleDefinition("test", Map.of("key", "value")));
        assertThat(result).isSucceeded();
    }


    @Test
    void validateDefinition_shouldFails_whenTypeNotFound() {
        var result = registry.validateDefinition(new CredentialRuleDefinition("test", Map.of("key", "value")));
        assertThat(result).isFailed().detail().contains("Unknown rule type: test");
    }
}