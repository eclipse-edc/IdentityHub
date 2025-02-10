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

import org.eclipse.edc.issuerservice.issuance.rules.expression.ExpressionCredentialRule.Operator;
import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialRuleDefinition;
import org.eclipse.edc.issuerservice.spi.issuance.rule.CredentialRuleFactory;

/**
 * Creates {@link ExpressionCredentialRule}s.
 */
public class ExpressionCredentialRuleFactory implements CredentialRuleFactory {

    @Override
    public ExpressionCredentialRule createRule(CredentialRuleDefinition definition) {
        var configuration = definition.configuration();
        var claim = (String) configuration.get("claim");
        var operator = configuration.get("operator");
        var value = configuration.get("value");
        return new ExpressionCredentialRule(claim, Operator.valueOf(operator.toString().toUpperCase()), value);
    }
}
