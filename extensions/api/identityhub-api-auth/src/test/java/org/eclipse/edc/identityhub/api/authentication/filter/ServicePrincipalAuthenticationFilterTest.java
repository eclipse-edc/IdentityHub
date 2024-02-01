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

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.edc.identityhub.spi.authentication.ServicePrincipal;
import org.eclipse.edc.identityhub.spi.authentication.ServicePrincipalResolver;
import org.eclipse.edc.web.spi.exception.AuthenticationFailedException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServicePrincipalAuthenticationFilterTest {

    private final ServicePrincipalResolver servicePrincipalResolver = mock();
    private final ServicePrincipalAuthenticationFilter filter = new ServicePrincipalAuthenticationFilter(servicePrincipalResolver);

    @Test
    void filter_success() {
        var request = mock(ContainerRequestContext.class);

        when(request.getHeaders()).thenReturn(headers(Map.of("x-api-key", "test-token")));
        when(servicePrincipalResolver.findByCredential(eq("test-token"))).thenReturn(mock(ServicePrincipal.class));

        filter.filter(request);

        verify(request).setSecurityContext(argThat(sc -> sc.getUserPrincipal() instanceof ServicePrincipal));
    }

    @Test
    void filter_noApiKeyHeader() {
        var request = mock(ContainerRequestContext.class);
        when(request.getHeaders()).thenReturn(headers(Map.of()));

        filter.filter(request);

        verify(request).abortWith(argThat(response -> response.getStatus() == 401));
    }

    @Test
    void filter_tooManyApiKeyHeader() {
        var request = mock(ContainerRequestContext.class);

        var headers = new MultivaluedHashMap<String, String>();
        headers.put("x-api-key", List.of("key1", "key2"));
        when(request.getHeaders()).thenReturn(headers);
        when(servicePrincipalResolver.findByCredential(eq("test-token"))).thenReturn(mock(ServicePrincipal.class));

        filter.filter(request);
        verify(request).abortWith(argThat(response -> response.getStatus() == 401));
    }

    @Test
    void filter_userNotResolved() {
        var request = mock(ContainerRequestContext.class);

        when(request.getHeaders()).thenReturn(headers(Map.of("x-api-key", "test-token")));
        when(servicePrincipalResolver.findByCredential(eq("test-token"))).thenThrow(new AuthenticationFailedException("test-message"));

        assertThatThrownBy(() -> filter.filter(request)).isInstanceOf(AuthenticationFailedException.class);
    }

    private MultivaluedMap<String, String> headers(Map<String, String> headers) {
        return new MultivaluedHashMap<>(headers);
    }
}