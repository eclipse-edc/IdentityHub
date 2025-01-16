/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.spi.transformation;

import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.result.Result;

/**
 * Converts a scope string to a {@link Criterion} object. Implementations must be able to parse the shape of the
 * scope string and convert it into a {@link Criterion}.
 * <p>
 * The shape of the scope string is specific to the dataspace.
 */
@FunctionalInterface
public interface ScopeToCriterionTransformer {
    /**
     * Converts a scope string to a {@link Criterion} object. If the scope string is invalid, a failure result is returned.
     * This can happen, for example if the shape of the string is not correct, or if a wrong operator is used in a specific
     * context.
     *
     * @param scope The scope string to convert.
     * @return A {@link Result} with the converted {@link Criterion}.
     */
    Result<Criterion> transform(String scope);
}
