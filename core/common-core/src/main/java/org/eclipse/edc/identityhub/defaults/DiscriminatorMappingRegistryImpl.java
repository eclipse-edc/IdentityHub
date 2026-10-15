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

import org.eclipse.edc.identityhub.spi.participantcontext.events.ParticipantContextDeleted;
import org.eclipse.edc.identityhub.spi.transformation.DiscriminatorMappingRegistry;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Optional.ofNullable;


/**
 * An implementation of the {@link DiscriminatorMappingRegistry} interface that maintains a registry
 * of discriminator mappings in a thread-safe manner. Mappings of a participant context are removed when that participant
 * context is deleted.
 * <p>
 * Thread-Safety: the class is designed to handle multiple threads concurrently accessing or modifying the mappings.
 */
public class DiscriminatorMappingRegistryImpl implements DiscriminatorMappingRegistry, EventSubscriber {
    // this might be accessed from multiple threads (API requests), so it needs to be thread-safe
    private final Map<String, String> mappings = new ConcurrentHashMap<>();
    private final Map<String, Map<String, String>> participantMappings = new ConcurrentHashMap<>();

    @Override
    public void addMapping(String alias, String discriminator) {
        addMapping(mappings, alias, discriminator);
    }

    @Override
    public void addMapping(String participantContextId, String alias, String discriminator) {
        addMapping(participantMappings.computeIfAbsent(participantContextId, id -> new ConcurrentHashMap<>()), alias, discriminator);
    }

    @Override
    public @NotNull String getMapping(String alias) {
        return mappings.getOrDefault(alias, alias);
    }

    @Override
    public @NotNull String getMapping(String participantContextId, String alias) {
        if (participantContextId == null) {
            return getMapping(alias);
        }
        return ofNullable(participantMappings.get(participantContextId))
                .map(participantMapping -> participantMapping.get(alias))
                .orElseGet(() -> getMapping(alias));
    }

    @Override
    public void removeMappings(String participantContextId) {
        participantMappings.remove(participantContextId);
    }

    @Override
    public <E extends Event> void on(EventEnvelope<E> event) {
        if (event.getPayload() instanceof ParticipantContextDeleted deleted) {
            removeMappings(deleted.getParticipantContextId());
        }
    }

    private void addMapping(Map<String, String> target, String alias, String discriminator) {
        if (target.containsValue(discriminator)) {
            throw new IllegalArgumentException("An alias for this discriminator already exists: '%s' -> '%s'".formatted(alias, discriminator));
        }

        target.put(alias, discriminator);
    }
}
