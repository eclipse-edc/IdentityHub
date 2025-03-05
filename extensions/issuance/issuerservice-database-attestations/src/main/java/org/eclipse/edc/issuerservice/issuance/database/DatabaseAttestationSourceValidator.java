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

package org.eclipse.edc.issuerservice.issuance.database;


import org.eclipse.edc.issuerservice.spi.issuance.model.AttestationDefinition;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import static java.lang.String.format;
import static org.eclipse.edc.issuerservice.issuance.database.DatabaseAttestationSource.DATASOURCE_NAME;
import static org.eclipse.edc.issuerservice.issuance.database.DatabaseAttestationSource.TABLE_NAME;
import static org.eclipse.edc.validator.spi.ValidationResult.failure;
import static org.eclipse.edc.validator.spi.ValidationResult.success;
import static org.eclipse.edc.validator.spi.Violation.violation;

/**
 * Validates presentation attestation definitions.
 */
public class DatabaseAttestationSourceValidator implements Validator<AttestationDefinition> {
    private static final String ATTESTATION_TYPE = "database";

    @Override
    public ValidationResult validate(AttestationDefinition definition) {
        if (!ATTESTATION_TYPE.equals(definition.getAttestationType())) {
            return failure(violation("Expecting attestation type: " + ATTESTATION_TYPE, ATTESTATION_TYPE));
        }
        var config = definition.getConfiguration();
        if (!config.containsKey(DATASOURCE_NAME)) {
            return failure(violation(format("No %s specified", DATASOURCE_NAME), DATASOURCE_NAME));
        }
        if (!config.containsKey(TABLE_NAME)) {
            return failure(violation(format("No %s specified", TABLE_NAME), TABLE_NAME));
        }

        return success();
    }
}
