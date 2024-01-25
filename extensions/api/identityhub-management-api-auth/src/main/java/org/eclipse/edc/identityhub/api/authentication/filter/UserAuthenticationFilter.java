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

package org.eclipse.edc.identityhub.api.authentication.filter;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.edc.identityhub.api.authentication.spi.UserService;

import java.security.Principal;

@PreMatching
@Priority(Priorities.AUTHENTICATION)
public class UserAuthenticationFilter implements ContainerRequestFilter {
    private static final String API_KEY_HEADER = "x-api-key";
    private final UserService userService;

    public UserAuthenticationFilter(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext) {
        var apiKeyHeader = containerRequestContext.getHeaders().get(API_KEY_HEADER);

        // reject 0 or >1 api key headers
        if (apiKeyHeader == null || apiKeyHeader.size() != 1) {
            containerRequestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
        } else {
            var apiKey = apiKeyHeader.get(0);

            var user = userService.findByCredential(apiKey);
            containerRequestContext.setSecurityContext(new SecurityContext() {
                @Override
                public Principal getUserPrincipal() {
                    return user;
                }

                @Override
                public boolean isUserInRole(String s) {
                    return user.getRoles().contains(s);
                }

                @Override
                public boolean isSecure() {
                    return containerRequestContext.getUriInfo().getBaseUri().toString().startsWith("https");
                }

                @Override
                public String getAuthenticationScheme() {
                    return null;
                }
            });
        }
    }
}
