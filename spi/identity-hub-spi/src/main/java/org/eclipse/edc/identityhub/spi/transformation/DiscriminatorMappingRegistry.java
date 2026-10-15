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

package org.eclipse.edc.identityhub.spi.transformation;

import org.jetbrains.annotations.NotNull;

/**
 * Maps aliases ("short-hands") to fully qualified credential discriminators, which are then used to resolve scopes in
 * presentation queries.
 * <p>
 * Mappings can either apply to the entire runtime (typically sourced from configuration), or to a single participant
 * context. A participant context's own mappings take precedence over runtime-wide mappings, but are never visible to
 * other participant contexts.
 */
public interface DiscriminatorMappingRegistry {
    /**
     * Registers a mapping between a specified alias and a discriminator value, that applies to all participant contexts of the runtime.
     *
     * @param alias         The alias ("short-hand") for the fully qualified credential type, for example, {@code MyMembershipCredential}
     * @param discriminator The full discriminator. In most cases this will be the fully-qualified credential type, such as {@code https://example.com/membership-credentials/v1#MembershipCredential}
     */
    void addMapping(String alias, String discriminator);

    /**
     * Registers a mapping between a specified alias and a discriminator value, that only applies to the given participant context.
     *
     * @param participantContextId The ID of the participant context that owns the mapping
     * @param alias                The alias ("short-hand") for the fully qualified credential type, for example, {@code MyMembershipCredential}
     * @param discriminator        The full discriminator. In most cases this will be the fully-qualified credential type, such as {@code https://example.com/membership-credentials/v1#MembershipCredential}
     */
    void addMapping(String participantContextId, String alias, String discriminator);

    /**
     * Resolves an alias using only the runtime-wide mappings.
     *
     * @param alias the alias
     * @return the discriminator, or the alias itself if no mapping exists
     */
    @NotNull String getMapping(String alias);

    /**
     * Resolves an alias for a particular participant context. Mappings of that participant context take precedence over
     * runtime-wide mappings, mappings of other participant contexts are never considered.
     *
     * @param participantContextId the ID of the participant context for which the alias is resolved
     * @param alias                the alias
     * @return the discriminator, or the alias itself if no mapping exists
     */
    @NotNull String getMapping(String participantContextId, String alias);

    /**
     * Removes all mappings that are owned by the given participant context. Runtime-wide mappings are not affected.
     *
     * @param participantContextId the ID of the participant context
     */
    void removeMappings(String participantContextId);
}
