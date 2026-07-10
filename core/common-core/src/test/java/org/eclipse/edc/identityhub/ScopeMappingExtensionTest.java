/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub;

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.identityhub.defaults.ScopeMappingRegistryImpl;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.junit.extensions.TestExtensionContext;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.identityhub.ScopeMappingExtension.CONFIG_PREFIX;

@ExtendWith(DependencyInjectionExtension.class)
class ScopeMappingExtensionTest {

    @Test
    void createScopeMappingRegistry(ScopeMappingExtension extension) {
        assertThat(extension.createScopeMappingRegistry())
                .isInstanceOf(ScopeMappingRegistryImpl.class);
    }

    @Test
    void createScopeMappingRegistry_withSingleConfig(TestExtensionContext context, ObjectFactory factory) {
        context.setConfig(ConfigFactory.fromMap(Map.of(
                CONFIG_PREFIX + ".membership.pattern", "org\\.eclipse\\.custom\\.vc\\.type:(.+):(read|\\*|all)",
                CONFIG_PREFIX + ".membership.leftoperand", "verifiableCredential.credential.type",
                CONFIG_PREFIX + ".membership.operator", "contains",
                CONFIG_PREFIX + ".membership.rightoperand", "$1")));

        var extension = factory.constructInstance(ScopeMappingExtension.class);
        var registry = extension.createScopeMappingRegistry();

        assertThat(registry).isInstanceOf(ScopeMappingRegistryImpl.class);
        assertThat(registry.map("org.eclipse.custom.vc.type:MembershipCredential:read"))
                .singleElement()
                .satisfies(c -> {
                    assertThat(c.getOperandLeft()).isEqualTo("verifiableCredential.credential.type");
                    assertThat(c.getOperator()).isEqualTo("contains");
                    assertThat(c.getOperandRight()).isEqualTo("MembershipCredential");
                });
    }

    @Test
    void createScopeMappingRegistry_withIncompleteConfig(TestExtensionContext context, ObjectFactory factory) {
        context.setConfig(ConfigFactory.fromMap(Map.of(CONFIG_PREFIX + ".membership.pattern", "vc:(.+):read")));

        assertThatThrownBy(() -> factory.constructInstance(ScopeMappingExtension.class))
                .isInstanceOf(EdcException.class);
    }
}
