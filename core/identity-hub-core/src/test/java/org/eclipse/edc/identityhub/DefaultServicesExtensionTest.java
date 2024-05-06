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

package org.eclipse.edc.identityhub;

import org.eclipse.edc.identityhub.accesstoken.rules.ClaimIsPresentRule;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(DependencyInjectionExtension.class)
class DefaultServicesExtensionTest {
    private final TokenValidationRulesRegistry registry = mock();

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(TokenValidationRulesRegistry.class, registry);
    }

    @Test
    void initialize_verifyTokenRules(DefaultServicesExtension extension, ServiceExtensionContext context) {
        extension.initialize(context);
        verify(registry).addRule(eq("iatp-si"), isA(ClaimIsPresentRule.class));
        verify(registry).addRule(eq("iatp-access-token"), isA(ClaimIsPresentRule.class));
        verifyNoMoreInteractions(registry);
    }
}