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
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CredentialRuleFactoryRegistryImplTest {

    @Test
    void verify_registry() {
        var registry = new CredentialRuleFactoryRegistryImpl();
        registry.registerFactory("test", mock(CredentialRuleFactory.class));

        assertThat(registry.resolveFactory("test")).isNotNull();
    }
}