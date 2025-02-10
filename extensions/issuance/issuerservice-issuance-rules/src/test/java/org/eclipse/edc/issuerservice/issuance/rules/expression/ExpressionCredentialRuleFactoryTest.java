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

class ExpressionCredentialRuleFactoryTest {

    private ExpressionCredentialRuleFactory factory;

    @Test
    void verify_rule_created() {
        var config = new CredentialRuleDefinition("expression", Map.of("claim", "onboarding.active", "operator", "eq", "value", true));
        var rule = factory.createRule(config);
        assertThat(rule.evaluate(() -> Map.of("onboarding", Map.of("active", true))).succeeded()).isTrue();
    }

    @BeforeEach
    void setUp() {
        factory = new ExpressionCredentialRuleFactory();
    }
}