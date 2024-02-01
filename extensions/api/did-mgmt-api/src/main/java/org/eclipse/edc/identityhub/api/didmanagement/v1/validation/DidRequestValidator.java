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

package org.eclipse.edc.identityhub.api.didmanagement.v1.validation;

import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import java.net.URI;

import static org.eclipse.edc.validator.spi.ValidationResult.failure;
import static org.eclipse.edc.validator.spi.Violation.violation;

/**
 * Validates that a {@link DidDocument} is valid by checking all mandatory properties.
 */
public class DidRequestValidator implements Validator<DidDocument> {

    @Override
    public ValidationResult validate(DidDocument input) {
        if (input == null) {
            return failure(violation("input was null", "."));
        }

        if (input.getId() == null) {
            return failure(violation("ID was null", "id"));
        }

        if (!isValidUri(input.getId())) {
            return failure(violation("ID is not a valid URI", "id"));
        }

        return ValidationResult.success();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private boolean isValidUri(String supposedUri) {
        try {
            URI.create(supposedUri);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
