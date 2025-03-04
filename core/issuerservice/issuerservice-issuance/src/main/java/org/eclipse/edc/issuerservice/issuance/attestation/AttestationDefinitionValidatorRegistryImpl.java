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

package org.eclipse.edc.issuerservice.issuance.attestation;

import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionValidatorRegistry;
import org.eclipse.edc.issuerservice.spi.issuance.model.AttestationDefinition;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;
import org.eclipse.edc.validator.spi.Violation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class AttestationDefinitionValidatorRegistryImpl implements AttestationDefinitionValidatorRegistry {

    private final Map<String, Validator<AttestationDefinition>> validators = new HashMap<>();

    @Override
    public void registerValidator(String type, Validator<AttestationDefinition> validator) {
        validators.put(type, validator);
    }

    @Override
    public ValidationResult validateDefinition(AttestationDefinition definition) {
        return Optional.ofNullable(validators.get(definition.getAttestationType()))
                .map(validator -> validator.validate(definition))
                .orElseGet(() -> ValidationResult.failure(Violation.violation("Unknown attestation type: " + definition.getAttestationType(), null)));
    }
}
