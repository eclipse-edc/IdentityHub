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

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.edc.api.auth.spi.ParticipantPrincipal;
import org.eclipse.edc.api.auth.spi.Scope;

import java.security.Principal;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Narrows the scopes of an authenticated {@link ParticipantPrincipal} to the scopes of one API, identified by its scope
 * namespace (e.g. {@code identity-api}). Registered on the web context of that API, it makes sure that a principal is
 * elevated on that API only by that API's own admin scope, even when one {@code AuthorizationService} accepts the admin
 * scopes of several APIs.
 * <p>
 * Runs after authentication, which sets the principal, and before the scope-based access checks.
 */
@Priority(Priorities.AUTHORIZATION - 1)
public class ApiScopeFilter implements ContainerRequestFilter {
    private static final String SCOPE_SEPARATOR = " ";
    private final String namespace;

    public ApiScopeFilter(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        var securityContext = requestContext.getSecurityContext();
        if (securityContext != null && securityContext.getUserPrincipal() instanceof ParticipantPrincipal principal) {
            var narrowed = new ParticipantPrincipal(principal.participantContextId(), narrow(principal.scope()));
            requestContext.setSecurityContext(new NarrowedSecurityContext(securityContext, narrowed));
        }
    }

    private String narrow(String scopes) {
        if (scopes == null) {
            return null;
        }
        return Arrays.stream(scopes.split(SCOPE_SEPARATOR))
                .filter(this::isInNamespace)
                .collect(Collectors.joining(SCOPE_SEPARATOR));
    }

    private boolean isInNamespace(String scope) {
        var parsed = Scope.parse(scope);
        return parsed.succeeded() && parsed.getContent().prefix().equals(namespace);
    }

    private record NarrowedSecurityContext(SecurityContext delegate, Principal principal) implements SecurityContext {

        @Override
        public Principal getUserPrincipal() {
            return principal;
        }

        @Override
        public boolean isUserInRole(String role) {
            return delegate.isUserInRole(role);
        }

        @Override
        public boolean isSecure() {
            return delegate.isSecure();
        }

        @Override
        public String getAuthenticationScheme() {
            return delegate.getAuthenticationScheme();
        }
    }
}
