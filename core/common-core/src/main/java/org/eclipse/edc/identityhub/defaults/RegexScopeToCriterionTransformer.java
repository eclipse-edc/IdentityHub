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

package org.eclipse.edc.identityhub.defaults;

import org.eclipse.edc.identityhub.spi.transformation.ScopeMappingRegistry;
import org.eclipse.edc.identityhub.spi.transformation.ScopeToCriterionTransformer;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.result.Result;

import java.util.List;

import static org.eclipse.edc.spi.result.Result.success;

/**
 * A {@link ScopeToCriterionTransformer} that first consults a customizable {@link ScopeMappingRegistry} of regex-based
 * mappings. If at least one mapping matches the scope, the accumulated {@link Criterion} list is returned. Otherwise, the
 * scope is delegated to a fallback transformer (typically the {@link EdcScopeToCriterionTransformer}).
 */
public class RegexScopeToCriterionTransformer implements ScopeToCriterionTransformer {

    private final ScopeMappingRegistry scopeMappingRegistry;
    private final ScopeToCriterionTransformer fallback;

    public RegexScopeToCriterionTransformer(ScopeMappingRegistry scopeMappingRegistry, ScopeToCriterionTransformer fallback) {
        this.scopeMappingRegistry = scopeMappingRegistry;
        this.fallback = fallback;
    }

    @Override
    public Result<List<Criterion>> transformScope(String scope) {
        var criteria = scopeMappingRegistry.map(scope);
        if (!criteria.isEmpty()) {
            return success(criteria);
        }
        return fallback.transformScope(scope);
    }
}
