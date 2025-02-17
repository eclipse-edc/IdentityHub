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

package org.eclipse.edc.issuerservice.spi.issuance.mapping;

import org.eclipse.edc.issuerservice.spi.issuance.model.MappingDefinition;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.result.Result;

import java.util.List;
import java.util.Map;

/**
 * Maps input claims to output claims using a mapping definition.
 */
@ExtensionPoint
public interface IssuanceClaimsMapper {

    /**
     * Applies the mapping definition to the input claims.
     *
     * @param mappingDefinition the mapping definition
     * @param inputClaims       the input claims
     * @return the output claims
     */
    Result<Map<String, Object>> apply(MappingDefinition mappingDefinition, Map<String, Object> inputClaims);

    /**
     * Applies the mapping definitions to the input claims.
     *
     * @param mappingDefinitions the mapping definitions
     * @param inputClaims        the input claims
     * @return the output claims
     */
    default Result<Map<String, Object>> apply(List<MappingDefinition> mappingDefinitions, Map<String, Object> inputClaims) {
        var outputClaims = inputClaims;
        for (var mappingDefinition : mappingDefinitions) {
            var result = apply(mappingDefinition, outputClaims);
            if (result.failed()) {
                return Result.failure("Failed to apply mapping definition");
            }
            outputClaims = result.getContent();
        }
        return Result.success(outputClaims);
    }
}
