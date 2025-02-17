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

import org.eclipse.edc.issuerservice.spi.issuance.IssuanceContext;
import org.eclipse.edc.issuerservice.spi.issuance.rule.CredentialRule;
import org.eclipse.edc.spi.result.Result;

import static org.eclipse.edc.issuerservice.issuance.common.JsonNavigator.navigateProperty;
import static org.eclipse.edc.spi.result.Result.success;

/**
 * Supports expression-based rules. Equality operators are supported for all value types by converting those types to a string and performing
 * the comparison. Other comparison operators are supported only for int and long types.
 */
public class ExpressionCredentialRule implements CredentialRule {
    private final String[] path;
    private final Operator operator;
    private Object value;

    public ExpressionCredentialRule(String path, Operator operator, Object value) {
        this.path = path.split("\\.");
        this.operator = operator;
        this.value = value;
        if ((Operator.GT == operator ||
                Operator.GEQ == operator ||
                Operator.LT == operator ||
                Operator.LEQ == operator)) {
            this.value = Long.valueOf(value.toString());
        }
    }

    @Override
    public Result<Void> evaluate(IssuanceContext context) {
        var result = navigateProperty(path, context.getClaims(), true);
        if (result.failed()) {
            return result.mapFailure();
        }
        switch (operator) {
            case EQ -> {
                // convert to strings for the comparison
                return result.getContent().toString().equals(value.toString()) ? success() : Result.failure("Values not equal");
            }
            case NEQ -> {
                return !result.getContent().equals(value) ? success() : Result.failure("Values are equal");
            }
            case GT -> {
                if (comparableLong(result)) {
                    return ((Long) result.getContent()) > (Long) value ? success() : Result.failure("Value is not greater than");
                } else if (comparableInt(result)) {
                    return ((Integer) result.getContent()).longValue() > (Long) value ? success() : Result.failure("Value is not greater than");
                }
                return Result.failure("Value is not greater than");
            }
            case GEQ -> {
                if (comparableLong(result)) {
                    return ((Long) result.getContent()) >= (Long) value ? success() : Result.failure("Value is not greater than or equal to");
                } else if (comparableInt(result)) {
                    return ((Integer) result.getContent()).longValue() >= (Long) value ? success() : Result.failure("Value is not greater than or equal to");
                }
                return Result.failure("Value is not greater than or equal to");
            }
            case LT -> {
                if (comparableLong(result)) {
                    return ((Long) result.getContent()) < (Long) value ? success() : Result.failure("Value is not less than");
                } else if (comparableInt(result)) {
                    return ((Integer) result.getContent()).longValue() < (Long) value ? success() : Result.failure("Value is not less than");
                }
                return Result.failure("Value is not less than");
            }
            case LEQ -> {
                if (comparableLong(result)) {
                    return ((Long) result.getContent()) <= (Long) value ? success() : Result.failure("Value is not less than or equal to");
                } else if (comparableInt(result)) {
                    return ((Integer) result.getContent()).longValue() <= (Long) value ? success() : Result.failure("Value is not less than or equal to");
                }
                return Result.failure("Value is not less than or equal to");
            }
            default -> throw new IllegalStateException("Unexpected value: " + operator);
        }
    }

    private boolean comparableLong(Result<Object> result) {
        return result.getContent() instanceof Long;
    }

    private boolean comparableInt(Result<Object> result) {
        return result.getContent() instanceof Integer;
    }

    public enum Operator {
        EQ, NEQ, GT, GEQ, LT, LEQ
    }

}
