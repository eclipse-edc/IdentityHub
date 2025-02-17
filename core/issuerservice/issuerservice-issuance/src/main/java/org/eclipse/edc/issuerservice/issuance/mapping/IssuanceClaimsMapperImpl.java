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

package org.eclipse.edc.issuerservice.issuance.mapping;

import org.eclipse.edc.issuerservice.spi.issuance.mapping.IssuanceClaimsMapper;
import org.eclipse.edc.issuerservice.spi.issuance.model.MappingDefinition;
import org.eclipse.edc.spi.result.Result;

import java.util.HashMap;
import java.util.Map;

import static org.eclipse.edc.issuerservice.issuance.common.JsonNavigator.navigateProperty;

public class IssuanceClaimsMapperImpl implements IssuanceClaimsMapper {

    @Override
    public Result<Map<String, Object>> apply(MappingDefinition mappingDefinition, Map<String, Object> inputClaims) {
        return applyMapping(mappingDefinition, inputClaims);
    }

    private Result<Map<String, Object>> applyMapping(MappingDefinition mappingDefinition, Map<String, Object> claims) {
        var mappedClaims = new HashMap<String, Object>();

        var input = mappingDefinition.input().split("\\.");
        var output = mappingDefinition.output().split("\\.");

        var result = navigateProperty(input, claims, mappingDefinition.required());

        if (result.failed()) {
            return result.mapFailure();
        }
        var value = result.getContent();

        if (value != null) {
            writeProperty(output, value, mappedClaims);
        }

        return Result.success(mappedClaims);
    }


    @SuppressWarnings("unchecked")
    private void writeProperty(String[] path, Object value, Map<String, Object> claims) {
        var current = claims;
        for (var i = 0; i < path.length - 1; i++) {
            var segment = path[i];
            if (!current.containsKey(segment)) {
                current.put(segment, new HashMap<String, Object>());
            }
            current = (Map<String, Object>) current.get(segment);
        }
        current.put(path[path.length - 1], value);
    }

}
