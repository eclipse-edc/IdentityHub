/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.defaults;

import org.eclipse.edc.identityhub.spi.transformation.ScopeToCriterionTransformer;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.result.Result;

import java.util.List;

import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;

/**
 * Implementation of the {@link ScopeToCriterionTransformer} interface that converts a scope string to a {@link Criterion} object.
 * This is a default/example implementation, that assumes scope strings adhere to the following format:
 * <pre>
 *  org.eclipse.edc.vc.type:SomeCredential:[read|all|*]
 * </pre>
 * This scope string will get translated into a {@link Criterion} like:
 * <pre>
 *     verifiableCredential.credential.types like SomeCredential
 * </pre>
 *
 * <em>This MUST be adapted to the needs and requirements of the dataspace!</em>
 * <em>Do NOT use this in production code!</em>
 */
public class EdcScopeToCriterionTransformer implements ScopeToCriterionTransformer {
    public static final String TYPE_OPERAND = "verifiableCredential.credential.type";
    public static final String ALIAS_LITERAL = "org.eclipse.edc.vc.type";
    public static final String LIKE_OPERATOR = "like";
    public static final String CONTAINS_OPERATOR = "contains";
    private static final String SCOPE_SEPARATOR = ":";
    private final List<String> allowedOperations = List.of("read", "*", "all");

    @Override
    public Result<Criterion> transform(String scope) {
        var tokens = tokenize(scope);
        if (tokens.failed()) {
            return failure("Scope string cannot be converted: %s".formatted(tokens.getFailureDetail()));
        }
        var credentialType = tokens.getContent()[1];
        return success(new Criterion(TYPE_OPERAND, CONTAINS_OPERATOR, credentialType));
    }

    protected Result<String[]> tokenize(String scope) {
        if (scope == null) return failure("Scope was null");

        var tokens = scope.split(SCOPE_SEPARATOR);
        if (tokens.length != 3) {
            return failure("Scope string has invalid format.");
        }
        if (!ALIAS_LITERAL.equalsIgnoreCase(tokens[0])) {
            return failure("Scope alias MUST be %s but was %s".formatted(ALIAS_LITERAL, tokens[0]));
        }
        if (!allowedOperations.contains(tokens[2])) {
            return failure("Invalid scope operation: " + tokens[2]);
        }

        return success(tokens);
    }
}
