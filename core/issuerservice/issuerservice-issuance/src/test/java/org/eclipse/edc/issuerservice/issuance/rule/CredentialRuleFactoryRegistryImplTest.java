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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

class CredentialRuleFactoryRegistryImplTest {

    @Test
    void verify_registry() {
        var registry = new CredentialRuleFactoryRegistryImpl();
        registry.registerFactory("test", mock(CredentialRuleFactory.class));

        assertThat(registry.resolveFactory("test")).isNotNull();
    }

    // B4.7: resolving an unregistered rule type must produce a graceful failure, not an NPE
    // NOTE: this documents intended behavior - currently resolveFactory() throws an NPE from requireNonNull(),
    //  the graceful contract being: return null, per the @Nullable annotation on resolveFactory()
    @DisplayName("B4.7: resolving an unregistered rule type fails gracefully instead of throwing an NPE")
    @Test
    void resolveFactory_whenUnknownType_shouldFailGracefully() {
        var registry = new CredentialRuleFactoryRegistryImpl();

        assertThatCode(() -> registry.resolveFactory("unknown-type")).doesNotThrowAnyException();
        assertThat(registry.resolveFactory("unknown-type")).isNull();
    }
}