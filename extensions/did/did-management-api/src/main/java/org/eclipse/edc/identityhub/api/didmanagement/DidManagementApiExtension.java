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
import org.eclipse.edc.identityhub.api.didmanagement.v1.DidManagementApiController;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebServer;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.WebServiceConfiguration;
import org.eclipse.edc.web.spi.configuration.WebServiceConfigurer;
import org.eclipse.edc.web.spi.configuration.WebServiceSettings;

import static org.eclipse.edc.identityhub.api.didmanagement.DidManagementApiExtension.NAME;

@Extension(value = NAME)
public class DidManagementApiExtension implements ServiceExtension {

    public static final String NAME = "DID Management API Extension";
    private static final String MGMT_CONTEXT_ALIAS = "management";
    private static final String DEFAULT_DID_PATH = "/api/management";
    private static final int DEFAULT_DID_PORT = 8182;
    public static final WebServiceSettings SETTINGS = WebServiceSettings.Builder.newInstance()
            .apiConfigKey("web.http." + MGMT_CONTEXT_ALIAS)
            .contextAlias(MGMT_CONTEXT_ALIAS)
            .defaultPath(DEFAULT_DID_PATH)
            .defaultPort(DEFAULT_DID_PORT)
            .useDefaultContext(false)
            .name("IdentityHub Management API")
            .build();
    @Inject
    private WebService webService;
    @Inject
    private DidDocumentService didDocumentService;
    @Inject
    private WebServiceConfigurer configurer;
    @Inject
    private WebServer webServer;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var controller = new DidManagementApiController(didDocumentService);
        webService.registerResource(createManagementApiConfiguration(context).getContextAlias(), controller);
    }

    //todo: move to separate extension
    @Provider
    public WebServiceConfiguration createManagementApiConfiguration(ServiceExtensionContext context) {
        return configurer.configure(context, webServer, SETTINGS);
    }
}
