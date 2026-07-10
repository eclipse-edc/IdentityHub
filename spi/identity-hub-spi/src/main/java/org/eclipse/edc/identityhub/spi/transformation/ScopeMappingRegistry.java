/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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

package org.eclipse.edc.identityhub.spi.transformation;

import org.eclipse.edc.spi.query.Criterion;

import java.util.List;

/**
 * Registry of customizable, regex-based mappings from a scope string to a {@link Criterion}. This allows a dataspace to
 * express its own scope-to-criteria mappings (both the left and the right operand of the resulting {@link Criterion})
 * without having to implement a full {@link ScopeToCriterionTransformer}.
 * <p>
 * Each mapping consists of a regular expression and a {@link Criterion} <em>template</em>. When a scope matches the
 * regular expression, a concrete {@link Criterion} is produced by substituting the regex capture groups
 * ({@code $0}, {@code $1}, …) into the template's operands.
 */
public interface ScopeMappingRegistry {

    /**
     * Registers a regex mapping. When a scope matches {@code regex}, a {@link Criterion} is produced from
     * {@code criterionTemplate} by substituting capture groups ({@code $0}, {@code $1}, …) into its
     * {@code operandLeft} and {@code operandRight}.
     *
     * @param regex             The regular expression the scope string is matched against (using
     *                          {@link java.util.regex.Matcher#matches()}).
     * @param criterionTemplate The {@link Criterion} template whose operands may reference capture groups, for example
     *                          {@code verifiableCredential.credential.type contains $1}.
     */
    void addMapping(String regex, Criterion criterionTemplate);

    /**
     * Maps a scope string to the list of {@link Criterion} contributed by every mapping whose regular expression
     * matches the scope.
     *
     * @param scope The scope string to map.
     * @return All criteria contributed by matching mappings, or an empty list if no mapping matches.
     */
    List<Criterion> map(String scope);
}
