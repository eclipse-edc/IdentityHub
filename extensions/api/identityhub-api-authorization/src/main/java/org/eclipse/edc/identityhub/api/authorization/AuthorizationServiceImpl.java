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

import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.edc.identityhub.spi.AuthorizationService;
import org.eclipse.edc.identityhub.spi.authentication.ServicePrincipal;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantResource;
import org.eclipse.edc.spi.result.ServiceResult;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class AuthorizationServiceImpl implements AuthorizationService {
    private final Map<Class<?>, Function<String, ParticipantResource>> resourceLookupFunctions = new HashMap<>();

    @Override
    public ServiceResult<Void> isAuthorized(SecurityContext securityContext, String resourceId, Class<? extends ParticipantResource> resourceClass) {

        if (securityContext.isUserInRole(ServicePrincipal.ROLE_ADMIN)) {
            return ServiceResult.success();
        }

        var function = resourceLookupFunctions.get(resourceClass);
        var name = securityContext.getUserPrincipal().getName();
        if (function == null) {
            return ServiceResult.unauthorized("User access for '%s' to resource ID '%s' of type '%s' cannot be verified".formatted(name, resourceClass, resourceClass));
        }

        var result = function.apply(resourceId);
        if (result != null) {
            return Objects.equals(result.getParticipantId(), name)
                    ? ServiceResult.success()
                    : ServiceResult.unauthorized("User '%s' is not authorized to access resource of type %s with ID '%s'.".formatted(name, resourceClass, resourceId));
        }

        return ServiceResult.notFound("No Resource of type '%s' with ID '%s' was found.".formatted(resourceClass, resourceId));
    }

    @Override
    public void addLookupFunction(Class<?> resourceClass, Function<String, ParticipantResource> lookupFunction) {
        resourceLookupFunctions.put(resourceClass, lookupFunction);
    }
}
