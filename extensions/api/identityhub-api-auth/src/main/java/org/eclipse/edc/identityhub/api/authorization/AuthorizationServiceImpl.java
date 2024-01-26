/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
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

package org.eclipse.edc.identityhub.api.authorization;

import org.eclipse.edc.identityhub.spi.AuthorizationService;
import org.eclipse.edc.identityhub.spi.model.ParticipantResource;
import org.eclipse.edc.spi.result.ServiceResult;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import static java.util.Optional.ofNullable;

public class AuthorizationServiceImpl implements AuthorizationService {
    private final Map<Class<?>, Function<String, ParticipantResource>> authorizationCheckFunctions = new HashMap<>();

    @Override
    public ServiceResult<Void> isAuthorized(Principal user, String resourceId, Class<?> resourceClass) {
        var function = authorizationCheckFunctions.get(resourceClass);
        if (function == null) {
            return ServiceResult.success();
        }

        var isAuthorized = ofNullable(function.apply(resourceId))
                .map(pr -> Objects.equals(pr.getParticipantId(), user.getName()))
                .orElse(false);

        return isAuthorized ? ServiceResult.success() : ServiceResult.unauthorized("User '%s' is not authorized to access resource of type %s with ID '%s'.".formatted(user.getName(), resourceClass, resourceId));

    }

    @Override
    public void addLoookupFunction(Class<?> resourceClass, Function<String, ParticipantResource> lookupFunction) {
        authorizationCheckFunctions.put(resourceClass, lookupFunction);
    }
}
