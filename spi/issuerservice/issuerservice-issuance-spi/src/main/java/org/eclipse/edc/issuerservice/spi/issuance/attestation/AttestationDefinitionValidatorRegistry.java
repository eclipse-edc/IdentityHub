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

package org.eclipse.edc.issuerservice.spi.issuance.attestation;

import org.eclipse.edc.issuerservice.spi.issuance.model.AttestationDefinition;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;


/**
 * Registry for attestation definition validators.
 */
public interface AttestationDefinitionValidatorRegistry {

    /**
     * Registers a validator for a type.
     */
    void registerValidator(String type, Validator<AttestationDefinition> validator);

    /**
     * Validates the definition.
     */
    ValidationResult validateDefinition(AttestationDefinition definition);

}
