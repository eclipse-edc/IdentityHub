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

package org.eclipse.edc.identityhub.api.participantcontext;

import org.eclipse.edc.identithub.did.spi.DidDocumentService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebServer;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.WebServiceConfigurer;
import org.eclipse.edc.web.spi.configuration.WebServiceSettings;

import static org.eclipse.edc.identityhub.api.participantcontext.ParticipantContextManagementApiExtension.NAME;

@Extension(value = NAME)
public class ParticipantContextManagementApiExtension implements ServiceExtension {

    public static final String NAME = "ParticipantContext Management API Extension";
    private static final String MGMT_CONTEXT_ALIAS = "management";
    private static final String DEFAULT_DID_PATH = "/api/management";
    private static final int DEFAULT_DID_PORT = 8182;
    public static final WebServiceSettings SETTINGS = WebServiceSettings.Builder.newInstance()
            .apiConfigKey("web.http." + MGMT_CONTEXT_ALIAS)
            .contextAlias(MGMT_CONTEXT_ALIAS)
            .defaultPath(DEFAULT_DID_PATH)
            .defaultPort(DEFAULT_DID_PORT)
            .useDefaultContext(false)
            .name(NAME)
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
        var webServiceConfiguration = configurer.configure(context, webServer, SETTINGS);

//        webService.registerResource(webServiceConfiguration.getContextAlias(), controller);
    }

}
