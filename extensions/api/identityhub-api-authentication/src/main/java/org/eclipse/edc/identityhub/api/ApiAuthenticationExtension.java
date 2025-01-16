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

package org.eclipse.edc.identityhub.api;

import org.eclipse.edc.identityhub.api.authentication.filter.RoleBasedAccessFeature;
import org.eclipse.edc.identityhub.api.authentication.filter.ServicePrincipalAuthenticationFilter;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.webcontext.IdentityHubApiContext;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;

import static org.eclipse.edc.identityhub.api.ApiAuthenticationExtension.NAME;

@Extension(NAME)
public class ApiAuthenticationExtension implements ServiceExtension {

    public static final String NAME = "Identity API Authentication Extension";
    @Inject
    private WebService webService;
    @Inject
    private ParticipantContextService participantContextService;
    @Inject
    private Vault vault;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var alias = IdentityHubApiContext.IDENTITY;
        webService.registerResource(alias, new RoleBasedAccessFeature());
        webService.registerResource(alias, new ServicePrincipalAuthenticationFilter(new ParticipantServicePrincipalResolver(participantContextService, vault)));
    }
}
