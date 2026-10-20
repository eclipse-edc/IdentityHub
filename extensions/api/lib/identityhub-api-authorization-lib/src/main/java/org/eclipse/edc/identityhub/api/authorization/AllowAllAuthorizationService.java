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

package org.eclipse.edc.identityhub.api.authorization;

import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.edc.api.auth.spi.AuthorizationService;
import org.eclipse.edc.participantcontext.spi.types.ParticipantResource;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.ServiceResult;

import java.util.function.BiFunction;

/**
 * Fallback {@link AuthorizationService} for runtimes that contain no authorization module. It does not check resource
 * ownership, so it logs a warning when it is created.
 */
public class AllowAllAuthorizationService implements AuthorizationService {

    /**
     * Creates the fallback service and warns that the given API does not check resource ownership.
     *
     * @param monitor the monitor to log the warning to
     * @param apiName the name of the API that falls back to this service, used in the warning
     */
    public AllowAllAuthorizationService(Monitor monitor, String apiName) {
        monitor.warning(("No AuthorizationService is registered, so the %s does not check resource ownership: any " +
                "authenticated participant context can access the resources of all others. Add the " +
                "'identityhub-api-authorization' module or an OAuth2 authorization module to the runtime.").formatted(apiName));
    }

    @Override
    public ServiceResult<Void> authorize(SecurityContext securityContext, String resourceOwnerId, String resourceId, Class<? extends ParticipantResource> resourceClass) {
        return ServiceResult.success();
    }

    @Override
    public void addLookupFunction(Class<?> resourceClass, BiFunction<String, String, ParticipantResource> checkFunction) {
    }
}
