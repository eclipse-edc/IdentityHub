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

package org.eclipse.edc.issuerservice.issuance.rule;

import org.eclipse.edc.issuerservice.spi.issuance.rule.CredentialRuleFactory;
import org.eclipse.edc.issuerservice.spi.issuance.rule.CredentialRuleFactoryRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public class CredentialRuleFactoryRegistryImpl implements CredentialRuleFactoryRegistry {
    private final Map<String, CredentialRuleFactory> factories = new HashMap<>();

    @Override
    public void registerFactory(String type, CredentialRuleFactory factory) {
        factories.put(type, factory);
    }

    @Override
    public @Nullable CredentialRuleFactory resolveFactory(String type) {
        return requireNonNull(factories.get(type), "Unknown rule type: " + type);
    }
}
