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
import org.eclipse.edc.issuerservice.spi.issuance.rule.CredentialRule;
import org.eclipse.edc.issuerservice.spi.issuance.rule.CredentialRuleDefinitionEvaluator;
import org.eclipse.edc.issuerservice.spi.issuance.rule.CredentialRuleFactory;
import org.eclipse.edc.issuerservice.spi.issuance.rule.CredentialRuleFactoryRegistry;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CredentialRuleDefinitionEvaluatorImplTest {

    private final CredentialRuleFactoryRegistry credentialRuleFactoryRegistry = mock();

    private final CredentialRuleDefinitionEvaluator evaluator = new CredentialRuleDefinitionEvaluatorImpl(credentialRuleFactoryRegistry);

    @Test
    void evaluate() {

        var ruleDefinition = new CredentialRuleDefinition("test", Map.of());
        var factory = mock(CredentialRuleFactory.class);
        var rule = mock(CredentialRule.class);
        when(credentialRuleFactoryRegistry.resolveFactory("test")).thenReturn(factory);
        when(factory.createRule(ruleDefinition)).thenReturn(rule);
        when(rule.evaluate(any())).thenReturn(Result.success());
        var result = evaluator.evaluate(List.of(ruleDefinition), mock());

        assertThat(result).isSucceeded();
    }

    @Test
    void evaluate_shouldFail_whenRuleEvaluationFails() {

        var ruleDefinition = new CredentialRuleDefinition("test", Map.of());
        var factory = mock(CredentialRuleFactory.class);
        var rule = mock(CredentialRule.class);
        when(credentialRuleFactoryRegistry.resolveFactory("test")).thenReturn(factory);
        when(factory.createRule(ruleDefinition)).thenReturn(rule);
        when(rule.evaluate(any())).thenReturn(Result.failure("failed"));
        var result = evaluator.evaluate(List.of(ruleDefinition), mock());

        assertThat(result).isFailed().detail().contains("failed");
    }
}
