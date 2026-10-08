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

package org.eclipse.edc.identityhub.api.configuration;

import jakarta.ws.rs.ext.ContextResolver;
import org.eclipse.edc.spi.types.TypeManager;
import tools.jackson.databind.json.JsonMapper;

/**
 * Supplies the {@link JsonMapper} configured by the {@link TypeManager} to Jersey's Jackson 3 message body providers.
 * <p>
 * Those providers resolve the mapper through a {@code ContextResolver<JsonMapper>}, while the EDC runtime only registers
 * a {@code ContextResolver<ObjectMapper>} (the super type). Jersey matches context resolvers by their exact type
 * parameter, so the provider never finds the EDC mapper and falls back to a stock {@link JsonMapper}. That stock mapper
 * lacks the EDC configuration (notably {@code USE_GETTERS_AS_SETTERS}), which silently drops request-body properties
 * deserialized through their getter, such as {@link org.eclipse.edc.spi.query.QuerySpec#getFilterExpression()}.
 */
public class JsonMapperProvider implements ContextResolver<JsonMapper> {

    private final TypeManager typeManager;

    public JsonMapperProvider(TypeManager typeManager) {
        this.typeManager = typeManager;
    }

    @Override
    public JsonMapper getContext(Class<?> type) {
        return (JsonMapper) typeManager.getMapper();
    }
}
