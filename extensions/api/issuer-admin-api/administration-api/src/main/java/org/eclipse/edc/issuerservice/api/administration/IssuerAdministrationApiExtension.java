/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.issuerservice.api.administration;

import org.eclipse.edc.identityhub.spi.webcontext.IdentityHubApiContext;
import org.eclipse.edc.issuerservice.api.administration.v1.unstable.IssuerAdministrationApiController;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;

import static org.eclipse.edc.issuerservice.api.administration.IssuerAdministrationApiExtension.NAME;

@Extension(value = NAME)
public class IssuerAdministrationApiExtension implements ServiceExtension {

    public static final String NAME = "Issuer Service Administration API Extension";
    @Inject
    private WebService webService;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var controller = new IssuerAdministrationApiController();
        webService.registerResource(IdentityHubApiContext.ISSUERADMIN, controller);
    }
}
