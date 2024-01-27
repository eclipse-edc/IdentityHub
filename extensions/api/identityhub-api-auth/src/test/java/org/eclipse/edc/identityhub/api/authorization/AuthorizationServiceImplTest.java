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

import org.eclipse.edc.identityhub.spi.model.ParticipantResource;
import org.junit.jupiter.api.Test;

import java.security.Principal;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthorizationServiceImplTest {

    private final AuthorizationServiceImpl authorizationService = new AuthorizationServiceImpl();

    @Test
    void isAuthorized_whenAuthorized() {
        authorizationService.addLoookupFunction(String.class, s -> new ParticipantResource() {
            @Override
            public String getParticipantId() {
                return "test-id";
            }
        });

        var principal = mock(Principal.class);
        when(principal.getName()).thenReturn("test-id");
        assertThat(authorizationService.isAuthorized(principal, "test-resource-id", String.class))
                .isSucceeded();
    }

    @Test
    void isAuthorized_whenNoLookupFunction() {
        var principal = mock(Principal.class);
        when(principal.getName()).thenReturn("test-id");
        assertThat(authorizationService.isAuthorized(principal, "test-resource-id", String.class))
                .isFailed();
    }

    @Test
    void isAuthorized_whenNotAuthorized() {
        authorizationService.addLoookupFunction(String.class, s -> new ParticipantResource() {
            @Override
            public String getParticipantId() {
                return "another-test-id";
            }
        });
        var principal = mock(Principal.class);
        when(principal.getName()).thenReturn("test-id");
        assertThat(authorizationService.isAuthorized(principal, "test-resource-id", String.class))
                .isFailed();
    }
}