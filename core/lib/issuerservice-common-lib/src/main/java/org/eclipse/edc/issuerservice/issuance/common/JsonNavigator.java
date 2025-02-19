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

package org.eclipse.edc.issuerservice.issuance.common;

import org.eclipse.edc.spi.result.Result;

import java.util.Map;

import static java.lang.String.format;
import static java.lang.String.join;
import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;

/**
 * Navigates Json types and resolves property values.
 */
public class JsonNavigator {

    private JsonNavigator() {
    }

    public static Result<Object> navigateProperty(String[] path, Map<String, Object> input, boolean required) {
        Object result = input;
        for (var property : path) {
            if (!(result instanceof Map)) {
                return failure(format("Unexpected type at segment %s for path %s", property, join(".", path)));
            }
            //noinspection rawtypes
            result = ((Map) result).get(property);
            if (result == null) {
                break;
            }
        }
        return result == null && required ? failure(format("Property not found for path %s", join(".", path))) : success(result);
    }
}
