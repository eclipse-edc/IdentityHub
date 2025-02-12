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

package org.eclipse.edc.issuerservice.issuance.rules.expression;

import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialRuleDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExpressionCredentialRuleDefinitionValidatorTest {

    private ExpressionCredentialRuleDefinitionValidator validator;

    @Test
    void verify_valid_configuration() {
        var config = new CredentialRuleDefinition("expression", Map.of("claim", "onboarding.active", "operator", "eq", "value", true));
        assertThat(validator.validate(config).succeeded()).isTrue();
    }

    @Test
    void verify_invalid_no_claim() {
        var config = new CredentialRuleDefinition("expression", Map.of("operator", "eq", "value", true));
        var result = validator.validate(config);
        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureDetail().toLowerCase()).contains("claim");
    }

    @Test
    void verify_invalid_no_operator() {
        var config = new CredentialRuleDefinition("expression", Map.of("claim", "eq", "value", true));
        var result = validator.validate(config);
        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureDetail().toLowerCase()).contains("operator");
    }

    @Test
    void verify_invalid_no_value() {
        var config = new CredentialRuleDefinition("expression", Map.of("claim", "onboarding.active", "operator", "eq"));
        var result = validator.validate(config);
        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureDetail().toLowerCase()).contains("value");
    }

    @BeforeEach
    void setUp() {
        validator = new ExpressionCredentialRuleDefinitionValidator();
    }
}