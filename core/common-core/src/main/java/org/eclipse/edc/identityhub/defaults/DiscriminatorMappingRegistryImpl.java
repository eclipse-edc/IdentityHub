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

package org.eclipse.edc.identityhub.defaults;

import org.eclipse.edc.identityhub.spi.transformation.DiscriminatorMappingRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * An implementation of the {@link DiscriminatorMappingRegistry} interface that maintains a registry
 * of discriminator mappings in a thread-safe manner.
 * <p>
 * Thread-Safety: the class is designed to handle multiple threads concurrently accessing or modifying the mappings.
 */
public class DiscriminatorMappingRegistryImpl implements DiscriminatorMappingRegistry {
    // this might be accessed from multiple threads (API requests), so it needs to be thread-safe
    private final Map<String, String> mappings = new ConcurrentHashMap<>();

    @Override
    public void addMapping(String alias, String discriminator) {
        mappings.put(alias, discriminator);
    }

    @Override
    public @NotNull String getMapping(String alias) {
        return mappings.getOrDefault(alias, alias);
    }
}
