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

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.edc.api.auth.spi.ParticipantPrincipal;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.security.Principal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApiScopeFilterTest {

    private final ContainerRequestContext requestContext = mock();
    private final SecurityContext securityContext = mock();
    private final ApiScopeFilter filter = new ApiScopeFilter("identity-api");

    @Test
    void filter_keepsOnlyScopesOfNamespace() {
        var principal = new ParticipantPrincipal("participant", "identity-api:write issuer-admin-api:admin identity-api:dids:read management-api:admin");
        when(requestContext.getSecurityContext()).thenReturn(securityContext);
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(securityContext.isSecure()).thenReturn(true);

        filter.filter(requestContext);

        var captor = ArgumentCaptor.forClass(SecurityContext.class);
        verify(requestContext).setSecurityContext(captor.capture());
        var narrowed = captor.getValue();
        assertThat(narrowed.getUserPrincipal()).isEqualTo(new ParticipantPrincipal("participant", "identity-api:write identity-api:dids:read"));
        assertThat(narrowed.isSecure()).isTrue();
    }

    @Test
    void filter_whenNoScopeOfNamespace_removesAllScopes() {
        when(requestContext.getSecurityContext()).thenReturn(securityContext);
        when(securityContext.getUserPrincipal()).thenReturn(new ParticipantPrincipal("participant", "issuer-admin-api:admin"));

        filter.filter(requestContext);

        var captor = ArgumentCaptor.forClass(SecurityContext.class);
        verify(requestContext).setSecurityContext(captor.capture());
        assertThat(captor.getValue().getUserPrincipal()).isEqualTo(new ParticipantPrincipal("participant", ""));
    }

    @Test
    void filter_dropsMalformedScopes() {
        when(requestContext.getSecurityContext()).thenReturn(securityContext);
        when(securityContext.getUserPrincipal()).thenReturn(new ParticipantPrincipal("participant", "identity-api identity-api:foo identity-api:read"));

        filter.filter(requestContext);

        var captor = ArgumentCaptor.forClass(SecurityContext.class);
        verify(requestContext).setSecurityContext(captor.capture());
        assertThat(captor.getValue().getUserPrincipal()).isEqualTo(new ParticipantPrincipal("participant", "identity-api:read"));
    }

    @Test
    void filter_whenNoScopes_keepsNull() {
        when(requestContext.getSecurityContext()).thenReturn(securityContext);
        when(securityContext.getUserPrincipal()).thenReturn(new ParticipantPrincipal("participant", null));

        filter.filter(requestContext);

        var captor = ArgumentCaptor.forClass(SecurityContext.class);
        verify(requestContext).setSecurityContext(captor.capture());
        assertThat(captor.getValue().getUserPrincipal()).isEqualTo(new ParticipantPrincipal("participant", null));
    }

    @Test
    void filter_whenNotParticipantPrincipal_leavesSecurityContext() {
        when(requestContext.getSecurityContext()).thenReturn(securityContext);
        when(securityContext.getUserPrincipal()).thenReturn(mock(Principal.class));

        filter.filter(requestContext);

        verify(requestContext, never()).setSecurityContext(any());
    }

    @Test
    void filter_whenNoSecurityContext_doesNothing() {
        filter.filter(requestContext);

        verify(requestContext, never()).setSecurityContext(any());
    }
}
