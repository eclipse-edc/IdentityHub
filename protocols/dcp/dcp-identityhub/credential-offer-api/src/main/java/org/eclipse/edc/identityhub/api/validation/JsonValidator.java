/*
 *  Copyright (c) 2025 Metaform Systems Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.api.validation;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.validator.spi.Validator;

import static java.util.Optional.ofNullable;

public abstract class JsonValidator implements Validator<JsonObject> {
    /**
     * Checks if the given JsonValue object is the Null-Object. Due to JSON-LD expansion, a {@code "key": null}
     * would get expanded to an array, thus a simple equals-null check is not sufficient.
     *
     * @param value the JsonValue to check
     * @return true if the JsonValue object is either null, or its value type is NULL, false otherwise
     */
    protected boolean isNullObject(JsonValue value) {
        if (value instanceof JsonArray jarray) {
            if (jarray.isEmpty()) {
                return false; // empty arrays are OK
            }
            value = jarray.get(0).asJsonObject().get(JsonLdKeywords.VALUE);
            return ofNullable(value).map(jv -> jv.getValueType() == JsonValue.ValueType.NULL).orElse(false);
        }
        return value == null;
    }
}
