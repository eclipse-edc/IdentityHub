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

package org.eclipse.edc.issuerservice.spi.issuance.rule;

import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialRuleDefinition;

/**
 * Creates rule credential instances for a rule type.
 */
public interface CredentialRuleFactory {

    /**
     * Creates a rule instance for the given definition.
     */
    CredentialRule createRule(CredentialRuleDefinition definition);
}
