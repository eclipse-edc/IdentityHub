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
import org.eclipse.edc.identityhub.spi.participantcontext.model.AbstractParticipantResource;
import org.junit.jupiter.api.Test;

import java.security.Principal;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class AuthorizationServiceImplTest {

    private final AuthorizationServiceImpl authorizationService = new AuthorizationServiceImpl();

    @Test
    void isAuthorized_whenAuthorized() {
        authorizationService.addLookupFunction(TestResource.class, s -> new AbstractParticipantResource() {
            @Override
            public String getParticipantContextId() {
                return "test-id";
            }
        });

        var principal = mock(Principal.class);
        when(principal.getName()).thenReturn("test-id");
        var securityContext = mock(SecurityContext.class);
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        assertThat(authorizationService.isAuthorized(securityContext, "test-resource-id", TestResource.class))
                .isSucceeded();
    }

    @Test
    void isAuthorized_whenNoLookupFunction() {
        var principal = mock(Principal.class);
        when(principal.getName()).thenReturn("test-id");
        var securityContext = mock(SecurityContext.class);
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        assertThat(authorizationService.isAuthorized(securityContext, "test-resource-id", TestResource.class))
                .isFailed();
    }

    @Test
    void isAuthorized_whenNotAuthorized() {
        authorizationService.addLookupFunction(TestResource.class, s -> new AbstractParticipantResource() {
            @Override
            public String getParticipantContextId() {
                return "another-test-id";
            }
        });
        var principal = mock(Principal.class);
        when(principal.getName()).thenReturn("test-id");
        var securityContext = mock(SecurityContext.class);
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        assertThat(authorizationService.isAuthorized(securityContext, "test-resource-id", TestResource.class))
                .isFailed();
    }

    @Test
    void isAuthorized_whenSuperUser() {

        var securityContext = mock(SecurityContext.class);
        when(securityContext.isUserInRole(eq("admin"))).thenReturn(true);

        assertThat(authorizationService.isAuthorized(securityContext, "test-resource-id", TestResource.class))
                .isSucceeded();

        verify(securityContext).isUserInRole(eq("admin"));
        verifyNoMoreInteractions(securityContext);
    }

    private static class TestResource extends AbstractParticipantResource {

    }
}