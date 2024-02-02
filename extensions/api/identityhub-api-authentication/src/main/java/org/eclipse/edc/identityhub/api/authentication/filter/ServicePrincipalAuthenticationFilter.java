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
import org.eclipse.edc.identityhub.spi.authentication.ServicePrincipal;
import org.eclipse.edc.identityhub.spi.authentication.ServicePrincipalResolver;

import java.security.Principal;

/**
 * This filter takes the x-api-key header and attempts to resolve a {@link ServicePrincipal} from it using the {@link ServicePrincipalResolver}.
 * If there is 1 (and exactly 1!) {@code x-api-key} header, and a user can be resolved, the request is propagated through the filter chain.
 * <p>
 * This filter sets the {@link SecurityContext} of the request using the resolved user. Downstream filters can then get the security context:
 * <pre>
 *     var securityContext = containerRequestContext.getSecurityContext();
 *     var principal = securityContext.getPrincipal();
 *
 *     if(principal instanceof User user){
 *         // perform more auth stuff with user.
 *     }
 * </pre>
 * <p>
 * If no or more than 1 {@code x-api-key} headers were specified, or if a valid user could not be resolved, the request is aborted with HTTP 401.
 */
@PreMatching
@Priority(Priorities.AUTHENTICATION)
public class ServicePrincipalAuthenticationFilter implements ContainerRequestFilter {
    private static final String API_KEY_HEADER = "x-api-key";
    private final ServicePrincipalResolver servicePrincipalResolver;

    public ServicePrincipalAuthenticationFilter(ServicePrincipalResolver servicePrincipalResolver) {
        this.servicePrincipalResolver = servicePrincipalResolver;
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext) {
        var apiKeyHeader = containerRequestContext.getHeaders().get(API_KEY_HEADER);

        // reject 0 or >1 api key headers
        if (apiKeyHeader == null || apiKeyHeader.size() != 1) {
            containerRequestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
        } else {
            var apiKey = apiKeyHeader.get(0);

            var servicePrincipal = servicePrincipalResolver.findByCredential(apiKey);
            containerRequestContext.setSecurityContext(new SecurityContext() {
                @Override
                public Principal getUserPrincipal() {
                    return servicePrincipal;
                }

                @Override
                public boolean isUserInRole(String s) {
                    return servicePrincipal.getRoles().contains(s);
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
