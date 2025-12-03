/*
 *  Copyright (c) 2025 Metaform Systems Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.issuerservice.api;

import org.eclipse.edc.api.authorization.filter.RoleBasedAccessFeature;
import org.eclipse.edc.api.authorization.filter.ScopeBasedAccessFeature;
import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.identityhub.spi.webcontext.IdentityHubApiContext;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(DependencyInjectionExtension.class)
class Oauth2AuthorizationExtensionTest {

    @Test
    void verifyRegistrations(ServiceExtensionContext context, ObjectFactory factory) {
        WebService webService = mock();
        context.registerService(WebService.class, webService);

        var ext = factory.constructInstance(Oauth2AuthorizationExtension.class);
        ext.initialize(context);

        verify(webService).registerResource(eq(IdentityHubApiContext.IDENTITY), isA(RoleBasedAccessFeature.class));
        verify(webService).registerResource(eq(IdentityHubApiContext.IDENTITY), isA(ScopeBasedAccessFeature.class));
    }
}