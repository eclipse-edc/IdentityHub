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

package org.eclipse.edc.identityhub.protocols.dcp.validation;

import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;
import org.eclipse.edc.validator.spi.Violation;

import java.util.Optional;

public class MandatoryId implements Validator<JsonObject> {
    private final JsonLdPath path;

    public MandatoryId(JsonLdPath path) {
        this.path = path;
    }

    @Override
    public ValidationResult validate(JsonObject input) {
        return Optional.ofNullable(input.getJsonArray(this.path.last()))
                .filter((it) -> !it.isEmpty())
                .map((it) -> it.getJsonObject(0))
                .map((it) -> it.getJsonString(JsonLdKeywords.ID))
                .map(it -> ValidationResult.success())
                .orElseGet(() -> ValidationResult.failure(Violation.violation(String.format("mandatory @id '%s' is missing", this.path), this.path.toString())));
    }
}