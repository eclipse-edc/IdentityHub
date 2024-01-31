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

package org.eclipse.edc.identityhub.spi;

import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.edc.identityhub.spi.model.ParticipantResource;
import org.eclipse.edc.spi.result.ServiceResult;

import java.security.Principal;
import java.util.function.Function;

/**
 * This service takes a {@link Principal}, that is typically obtained from the {@link jakarta.ws.rs.core.SecurityContext} of an incoming
 * HTTP request, and checks whether this principal is authorized to access a particular resource, identified by ID and by object class.
 */
public interface AuthorizationService {
    /**
     * Checks whether the principal is authorized to access a particular resource.
     *
     * @param user          The {@link Principal}, typically obtained via {@link SecurityContext#getUserPrincipal()}.
     * @param resourceId    The database ID of the resource. The resource must be of type {@link ParticipantResource}.
     * @param resourceClass The concrete type of the resource.
     * @return success if authorized, {@link ServiceResult#unauthorized(String)} if not authorized
     */
    ServiceResult<Void> isAuthorized(Principal user, String resourceId, Class<?> resourceClass);

    /**
     * Register a function, that can lookup a particular resource type by ID. Typically, every resource that should be protected with
     * authorization, registers a lookup function for the type of resource.
     */
    void addLoookupFunction(Class<?> resourceClass, Function<String, ParticipantResource> checkFunction);

    /**
     * Determines whether the given {@link Principal} has elevated user permissions (e.g. "admin" role), etc. It is up to the implementation to
     * determine what constitutes "elevated privileges".
     *
     * @param userPrincipal The principal that was authenticated during request ingress.
     * @return true if has elevated privileges, false otherwise.
     */
    boolean hasElevatedPrivilege(Principal userPrincipal);
}
