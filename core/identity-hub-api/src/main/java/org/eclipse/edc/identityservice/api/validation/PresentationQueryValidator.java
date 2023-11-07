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

package org.eclipse.edc.identityservice.api.validation;

import jakarta.json.JsonObject;
import org.eclipse.edc.identityhub.spi.model.PresentationQuery;
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
        var scope = input.get(PresentationQuery.PRESENTATION_QUERY_SCOPE_PROPERTY);

        var presentationDef = input.get(PresentationQuery.PRESENTATION_QUERY_DEFINITION_PROPERTY);

        if (scope == null && presentationDef == null) {
            return failure(violation("Must contain either a 'scope' or a 'presentationDefinition' property.", null));
        }

        if (scope != null && presentationDef != null) {
            return failure(violation("Must contain either a 'scope' or a 'presentationDefinition', not both.", null));
        }

        return success();
    }
}
