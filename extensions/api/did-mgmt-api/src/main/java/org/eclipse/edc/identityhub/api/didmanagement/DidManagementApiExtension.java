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

import org.eclipse.edc.identithub.did.spi.DidDocumentService;
import org.eclipse.edc.identithub.did.spi.model.DidResource;
import org.eclipse.edc.identityhub.api.configuration.ManagementApiConfiguration;
import org.eclipse.edc.identityhub.api.didmanagement.v1.DidManagementApiController;
import org.eclipse.edc.identityhub.spi.AuthorizationService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;

import static org.eclipse.edc.identityhub.api.didmanagement.DidManagementApiExtension.NAME;

@Extension(value = NAME)
public class DidManagementApiExtension implements ServiceExtension {

    public static final String NAME = "DID Management API Extension";

    @Inject
    private WebService webService;
    @Inject
    private DidDocumentService didDocumentService;
    @Inject
    private ManagementApiConfiguration webServiceConfiguration;
    @Inject
    private AuthorizationService authorizationService;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        authorizationService.addLoookupFunction(DidResource.class, s -> didDocumentService.findById(s));
        var controller = new DidManagementApiController(didDocumentService, authorizationService);
        webService.registerResource(webServiceConfiguration.getContextAlias(), controller);

    }


}
