/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
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

package org.eclipse.edc.identityhub.api.participantcontext.v1.validation;

import org.eclipse.edc.identityhub.spi.model.participant.ParticipantManifest;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import static org.eclipse.edc.validator.spi.ValidationResult.failure;
import static org.eclipse.edc.validator.spi.ValidationResult.success;
import static org.eclipse.edc.validator.spi.Violation.violation;

public class ParticipantManifestValidator implements Validator<ParticipantManifest> {
    private final KeyDescriptorValidator keyDescriptorValidator = new KeyDescriptorValidator();

    @Override
    public ValidationResult validate(ParticipantManifest input) {
        if (input == null) {
            return failure(violation("input was null.", "."));
        }

        if (input.getKey() == null) {
            return failure(violation("key descriptor cannot be null.", "key"));
        }
        if (input.getParticipantId() == null) {
            return failure(violation("participantId cannot be null.", "participantId"));
        }

        var keyValidationResult = keyDescriptorValidator.validate(input.getKey());
        if (keyValidationResult.failed()) {
            return failure(violation("key descriptor is invalid: %s".formatted(keyValidationResult.getFailureDetail()), "key"));
        }


        return success();
    }
}
