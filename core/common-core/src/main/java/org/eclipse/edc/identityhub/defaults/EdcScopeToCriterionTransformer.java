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
import org.eclipse.edc.spi.monitor.Monitor;
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
    public static final String CONTEXT_OPERAND = "verifiableCredential.credential.context";
    public static final String ALIAS_LITERAL = "org.eclipse.edc.vc.type";
    public static final String LIKE_OPERATOR = "like";
    public static final String CONTAINS_OPERATOR = "contains";
    private static final String SCOPE_SEPARATOR = ":";
    private final List<String> allowedOperations = List.of("read", "*", "all");
    private final Monitor monitor;

    public EdcScopeToCriterionTransformer(Monitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public Result<Criterion> transform(String scope) {
        monitor.warning("the transform() method is deprecated. Please use transformScope() instead.");
        return transformScope(scope).map(list -> list.get(0));
    }

    @Override
    public Result<List<Criterion>> transformScope(String scope) {
        var tokens = tokenize(scope);
        if (tokens.failed()) {
            return failure("Scope string cannot be converted: %s".formatted(tokens.getFailureDetail()));
        }
        var discriminator = tokens.getContent()[1];

        return convertDiscriminator(discriminator);
    }

    protected Result<String[]> tokenize(String scope) {
        if (scope == null) return failure("Scope was null");

        var firstSeparatorIndex = scope.indexOf(SCOPE_SEPARATOR);
        var lastSeparatorIndex = scope.lastIndexOf(SCOPE_SEPARATOR);

        if (firstSeparatorIndex == -1 || lastSeparatorIndex == -1 || firstSeparatorIndex == lastSeparatorIndex) {
            return failure("Scope string has invalid format.");
        }

        var tokens = new String[3];
        tokens[0] = scope.substring(0, firstSeparatorIndex);
        tokens[1] = scope.substring(firstSeparatorIndex + 1, lastSeparatorIndex);
        tokens[2] = scope.substring(lastSeparatorIndex + 1);

        if (!ALIAS_LITERAL.equalsIgnoreCase(tokens[0])) {
            return failure("Scope alias MUST be %s but was %s".formatted(ALIAS_LITERAL, tokens[0]));
        }
        if (!allowedOperations.contains(tokens[2])) {
            return failure("Invalid scope operation: " + tokens[2]);
        }

        return success(tokens);
    }

    private Result<List<Criterion>> convertDiscriminator(String discriminator) {
        if (discriminator == null) {
            return failure("discriminator was null");
        }

        var lastHashIndex = discriminator.lastIndexOf("#");

        if (lastHashIndex == -1) {
            // No hash found, treat entire string as type
            var typeCriterion = new Criterion(TYPE_OPERAND, CONTAINS_OPERATOR, discriminator);
            return success(List.of(typeCriterion));
        }

        var contextPart = discriminator.substring(0, lastHashIndex);
        var typePart = discriminator.substring(lastHashIndex + 1);

        var contextCriterion = new Criterion(CONTEXT_OPERAND, CONTAINS_OPERATOR, contextPart);
        var typeCriterion = new Criterion(TYPE_OPERAND, CONTAINS_OPERATOR, typePart);

        return success(List.of(contextCriterion, typeCriterion));
    }
}
