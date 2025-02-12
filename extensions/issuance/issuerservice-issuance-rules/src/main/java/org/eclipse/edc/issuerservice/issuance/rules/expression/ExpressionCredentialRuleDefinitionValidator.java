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
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import java.util.Arrays;

import static org.eclipse.edc.validator.spi.ValidationResult.failure;
import static org.eclipse.edc.validator.spi.ValidationResult.success;
import static org.eclipse.edc.validator.spi.Violation.violation;

/**
 * Validates expression rules.
 */
public class ExpressionCredentialRuleDefinitionValidator implements Validator<CredentialRuleDefinition> {
    private static final String CLAIM = "claim";
    private static final String OPERATOR = "operator";
    private static final String VALUE = "value";

    @Override
    public ValidationResult validate(CredentialRuleDefinition definition) {
        var configuration = definition.configuration();
        var claim = configuration.get(CLAIM);
        if (claim == null) {
            return failure(violation("Claim is required", CLAIM));
        } else if (!(claim instanceof String)) {
            return failure(violation("Claim must be a string", CLAIM, claim.toString()));
        }

        var operator = configuration.get(OPERATOR);
        if (operator == null) {
            return failure(violation("Operator is required", OPERATOR));
        } else if (!(operator instanceof String)) {
            return failure(violation("Operator must be a string", OPERATOR));
        }

        try {
            ExpressionCredentialRule.Operator.valueOf(operator.toString().toUpperCase());
        } catch (IllegalArgumentException e) {
            return failure(violation("Operator must be one of " + Arrays.toString(ExpressionCredentialRule.Operator.values()), OPERATOR));
        }

        var value = configuration.get(VALUE);
        if (value == null) {
            return failure(violation("Value is required", VALUE));
        }
        return success();

    }
}
