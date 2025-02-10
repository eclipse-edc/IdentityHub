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

/**
 * A registry of factories that validate and create rule instances.
 */
public interface CredentialRuleFactoryRegistry {

    /**
     * Registers a factory for the type.
     */
    void registerFactory(String type, CredentialRuleFactory factory);

    /**
     * Resolves a factory for the rule type or returns null.
     */
    CredentialRuleFactory resolveFactory(String type);

}
