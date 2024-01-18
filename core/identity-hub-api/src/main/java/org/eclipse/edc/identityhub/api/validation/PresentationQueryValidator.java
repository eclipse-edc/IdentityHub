/*
 *  Copyright (c) 2023 Metaform Systems, Inc.
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

package org.eclipse.edc.identityhub.api.validation;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.identitytrust.model.credentialservice.PresentationQuery;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import static org.eclipse.edc.validator.spi.ValidationResult.failure;
import static org.eclipse.edc.validator.spi.ValidationResult.success;
import static org.eclipse.edc.validator.spi.Violation.violation;

/**
 * Validates, that a JsonObject representing a {@link PresentationQuery} contains <em>either</em> a {@code scope} property,
 * <em>or</em> a {@code presentationDefinition} query.
 */
public class PresentationQueryValidator implements Validator<JsonObject> {
    @Override
    public ValidationResult validate(JsonObject input) {
        if (input == null) {
            return failure(violation("Presentation query was null", "."));
        }
        var scope = input.get(PresentationQuery.PRESENTATION_QUERY_SCOPE_PROPERTY);

        var presentationDef = input.get(PresentationQuery.PRESENTATION_QUERY_DEFINITION_PROPERTY);

        if (isNullObject(scope) && isNullObject(presentationDef)) {
            return failure(violation("Must contain either a 'scope' or a 'presentationDefinition' property.", null));
        }

        if (!isNullObject(scope) && !isNullObject(presentationDef)) {
            return failure(violation("Must contain either a 'scope' or a 'presentationDefinition', not both.", null));
        }

        return success();
    }

    /**
     * Checks if the given JsonValue object is the Null-Object. Due to JSON-LD expansion, a {@code "presentation_query": null}
     * would get expanded to an array, thus a simple equals-null check is not sufficient.
     *
     * @param value the JsonValue to check
     * @return true if the JsonValue object is either null, or its value type is NULL, false otherwise
     */
    private boolean isNullObject(JsonValue value) {
        if (value instanceof JsonArray) {
            value = ((JsonArray) value).get(0).asJsonObject().get(JsonLdKeywords.VALUE);

            return value.getValueType() == JsonValue.ValueType.NULL;
        }
        return value == null;
    }
}
