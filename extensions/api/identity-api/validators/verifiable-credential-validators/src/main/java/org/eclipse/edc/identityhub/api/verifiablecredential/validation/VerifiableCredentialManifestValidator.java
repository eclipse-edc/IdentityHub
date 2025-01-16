/*
 *  Copyright (c) 2024 Amadeus IT Group
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus IT Group - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.api.verifiablecredential.validation;

import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialManifest;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import static org.eclipse.edc.validator.spi.ValidationResult.failure;
import static org.eclipse.edc.validator.spi.ValidationResult.success;
import static org.eclipse.edc.validator.spi.Violation.violation;

public class VerifiableCredentialManifestValidator implements Validator<VerifiableCredentialManifest> {
    @Override
    public ValidationResult validate(VerifiableCredentialManifest input) {
        if (input == null) {
            return failure(violation("Input was null", "."));
        }

        if (input.getParticipantContextId() == null) {
            return failure(violation("participantContextId id was null", "participantContextId"));
        }

        var container = input.getVerifiableCredentialContainer();
        if (container == null) {
            return failure(violation("VerifiableCredentialContainer was null", "credential"));
        }

        var credential = container.credential();
        if (credential == null) {
            return failure(violation("VerifiableCredential was null", "credential.credential"));
        }

        return success();
    }
}
