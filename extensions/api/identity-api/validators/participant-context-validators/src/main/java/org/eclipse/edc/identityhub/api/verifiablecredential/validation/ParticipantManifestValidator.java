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

package org.eclipse.edc.identityhub.api.verifiablecredential.validation;

import org.eclipse.edc.identityhub.api.keypair.validation.KeyDescriptorValidator;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.util.string.StringUtils;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import static org.eclipse.edc.validator.spi.ValidationResult.failure;
import static org.eclipse.edc.validator.spi.ValidationResult.success;
import static org.eclipse.edc.validator.spi.Violation.violation;

public class ParticipantManifestValidator implements Validator<ParticipantManifest> {
    private final KeyDescriptorValidator keyDescriptorValidator;

    public ParticipantManifestValidator(Monitor monitor) {
        this.keyDescriptorValidator = new KeyDescriptorValidator(monitor);
    }

    @Override
    public ValidationResult validate(ParticipantManifest input) {
        if (input == null) {
            return failure(violation("input was null.", "."));
        }

        if (input.getKey() == null) {
            return failure(violation("key descriptor cannot be null.", "key"));
        }
        if (StringUtils.isNullOrBlank(input.getParticipantId())) {
            return failure(violation("participantId cannot be null or empty.", "participantId"));
        }
        if (StringUtils.isNullOrBlank(input.getDid())) {
            return failure(violation("DID cannot be null or empty.", "did"));
        }

        var keyValidationResult = keyDescriptorValidator.validate(input.getKey());
        if (keyValidationResult.failed()) {
            return failure(violation("key descriptor is invalid: %s".formatted(keyValidationResult.getFailureDetail()), "key"));
        }


        return success();
    }
}
