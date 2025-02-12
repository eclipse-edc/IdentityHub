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

import org.eclipse.edc.issuerservice.spi.issuance.IssuanceContext;
import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialRuleDefinition;
import org.eclipse.edc.issuerservice.spi.issuance.rule.CredentialRuleDefinitionEvaluator;
import org.eclipse.edc.issuerservice.spi.issuance.rule.CredentialRuleFactoryRegistry;
import org.eclipse.edc.spi.result.Result;

import java.util.Collection;

public class CredentialRuleDefinitionEvaluatorImpl implements CredentialRuleDefinitionEvaluator {

    private final CredentialRuleFactoryRegistry credentialRuleFactoryRegistry;

    public CredentialRuleDefinitionEvaluatorImpl(CredentialRuleFactoryRegistry credentialRuleFactoryRegistry) {
        this.credentialRuleFactoryRegistry = credentialRuleFactoryRegistry;
    }

    @Override
    public Result<Void> evaluate(Collection<CredentialRuleDefinition> definitions, IssuanceContext context) {
        for (var definition : definitions) {
            var factory = credentialRuleFactoryRegistry.resolveFactory(definition.type());
            var rule = factory.createRule(definition);
            var result = rule.evaluate(context);
            if (result.failed()) {
                return result;
            }
        }
        return Result.success();
    }
}
