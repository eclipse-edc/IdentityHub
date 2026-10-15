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

import org.eclipse.edc.identityhub.defaults.DiscriminatorMappingRegistryImpl;
import org.eclipse.edc.identityhub.spi.participantcontext.events.ParticipantContextDeleted;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.identityhub.DiscriminatorMappingExtension.CONFIG_PREFIX;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class DiscriminatorMappingExtensionTest {

    private final EventRouter eventRouter = mock();

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(EventRouter.class, eventRouter);
    }

    @Test
    void createDiscriminatorMappingRegistry(DiscriminatorMappingExtension extension, ServiceExtensionContext context) {
        var registry = extension.createDiscriminatorMappingRegistry(context);

        assertThat(registry).isInstanceOf(DiscriminatorMappingRegistryImpl.class);
        verify(eventRouter).registerSync(ParticipantContextDeleted.class, (DiscriminatorMappingRegistryImpl) registry);
    }

    @Test
    void createDiscriminatorMappingRegistry_withSingleConfig(DiscriminatorMappingExtension extension, ServiceExtensionContext context) {
        when(context.getConfig(eq(CONFIG_PREFIX)))
                .thenReturn(ConfigFactory.fromMap(Map.of("foo.alias", "bar", "foo.value", "long-value")));
        var discriminatorMappingRegistry = extension.createDiscriminatorMappingRegistry(context);
        assertThat(discriminatorMappingRegistry)
                .isInstanceOf(DiscriminatorMappingRegistryImpl.class);

        assertThat(discriminatorMappingRegistry.getMapping("bar")).isEqualTo("long-value");
    }

    @Test
    void createDiscriminatorMappingRegistry_withIncompleteConfig(DiscriminatorMappingExtension extension, ServiceExtensionContext context) {
        when(context.getConfig(eq(CONFIG_PREFIX)))
                .thenReturn(ConfigFactory.fromMap(Map.of("foo.alias", "bar")));
        assertThatThrownBy(() -> extension.createDiscriminatorMappingRegistry(context))
                .isInstanceOf(EdcException.class)
                .hasMessage("No setting found for key foo.value");
    }

    @Test
    void createDiscriminatorMappingRegistry_withInvalidSuffix(DiscriminatorMappingExtension extension, ServiceExtensionContext context) {
        when(context.getConfig(eq(CONFIG_PREFIX)))
                .thenReturn(ConfigFactory.fromMap(Map.of("foo.name", "bar", // should be .alias
                        "foo.value", "long-value")));
        assertThatThrownBy(() -> extension.createDiscriminatorMappingRegistry(context))
                .isInstanceOf(EdcException.class)
                .hasMessage("No setting found for key foo.alias");

    }
}