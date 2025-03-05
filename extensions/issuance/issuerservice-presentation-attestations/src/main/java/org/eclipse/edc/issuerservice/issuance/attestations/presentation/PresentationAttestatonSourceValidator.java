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

package org.eclipse.edc.issuerservice.issuance.attestations.presentation;


import org.eclipse.edc.issuerservice.spi.issuance.model.AttestationDefinition;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import static java.lang.String.format;
import static org.eclipse.edc.validator.spi.ValidationResult.failure;
import static org.eclipse.edc.validator.spi.ValidationResult.success;
import static org.eclipse.edc.validator.spi.Violation.violation;

/**
 * Validates presentation attestation definitions.
 */
public class PresentationAttestatonSourceValidator implements Validator<AttestationDefinition> {
    private static final String ATTESTATION_TYPE = "presentation";
    private static final String CREDENTIAL_TYPE = "credentialType";
    private static final String OUTPUT_CLAIM = "outputClaim";

    @Override
    public ValidationResult validate(AttestationDefinition definition) {
        if (!ATTESTATION_TYPE.equals(definition.getAttestationType())) {
            return failure(violation("Expecting attestation type: " + ATTESTATION_TYPE, ATTESTATION_TYPE));
        }
        if (!definition.getConfiguration().containsKey(CREDENTIAL_TYPE)) {
            return failure(violation(format("No %s specified", CREDENTIAL_TYPE), CREDENTIAL_TYPE));
        }
        if (!definition.getConfiguration().containsKey(OUTPUT_CLAIM)) {
            return failure(violation(format("No %s specified", OUTPUT_CLAIM), OUTPUT_CLAIM));
        }
        return success();
    }
}
