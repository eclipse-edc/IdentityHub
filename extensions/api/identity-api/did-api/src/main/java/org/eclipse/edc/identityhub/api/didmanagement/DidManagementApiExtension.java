/*
 *  Copyright (c) 2023 Metaform Systems, Inc.
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

package org.eclipse.edc.identityhub.api.didmanagement;

import org.eclipse.edc.identityhub.api.didmanagement.v1.unstable.DidManagementApiController;
import org.eclipse.edc.identityhub.api.didmanagement.v1.unstable.GetAllDidsApiController;
import org.eclipse.edc.identityhub.spi.authorization.AuthorizationService;
import org.eclipse.edc.identityhub.spi.did.DidDocumentService;
import org.eclipse.edc.identityhub.spi.did.model.DidResource;
import org.eclipse.edc.identityhub.spi.webcontext.IdentityHubApiContext;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;

import static org.eclipse.edc.identityhub.api.didmanagement.DidManagementApiExtension.NAME;

@Extension(value = NAME)
public class DidManagementApiExtension implements ServiceExtension {

    public static final String NAME = "DID management Identity API Extension";

    @Inject
    private WebService webService;
    @Inject
    private DidDocumentService didDocumentService;
    @Inject
    private AuthorizationService authorizationService;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        authorizationService.addLookupFunction(DidResource.class, s -> didDocumentService.findById(s));
        var controller = new DidManagementApiController(didDocumentService, authorizationService);
        var getAllController = new GetAllDidsApiController(didDocumentService);
        webService.registerResource(IdentityHubApiContext.IDENTITY, controller);
        webService.registerResource(IdentityHubApiContext.IDENTITY, getAllController);
    }

}
