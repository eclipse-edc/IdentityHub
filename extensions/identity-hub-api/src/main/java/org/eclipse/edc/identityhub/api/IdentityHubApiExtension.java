/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.api;

import org.eclipse.edc.identityhub.api.controller.IdentityHubController;
import org.eclipse.edc.identityhub.selfdescription.SelfDescriptionLoader;
import org.eclipse.edc.identityhub.spi.processor.MessageProcessorRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebServer;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.WebServiceConfigurer;
import org.eclipse.edc.web.spi.configuration.WebServiceSettings;

import java.util.Optional;

/**
 * EDC extension for Identity Hub API
 */
@Extension(value = IdentityHubApiExtension.NAME)
public class IdentityHubApiExtension implements ServiceExtension {

    public static final String NAME = "Identity Hub API";
    @Setting
    private static final String SELF_DESCRIPTION_DOCUMENT_PATH_SETTING = "edc.self.description.document.path";
    private static final String DEFAULT_SELF_DESCRIPTION_FILE_NAME = "default-self-description.json";
    private static final String IDENTITY_CONTEXT_ALIAS = "identity";
    private static final String DEFAULT_IDENTITY_API_CONTEXT_PATH = "/api/v1/identity";
    private static final int DEFAULT_IDENTITY_API_PORT = 8188;
    public static final WebServiceSettings SETTINGS = WebServiceSettings.Builder.newInstance()
            .apiConfigKey("web.http." + IDENTITY_CONTEXT_ALIAS)
            .contextAlias(IDENTITY_CONTEXT_ALIAS)
            .defaultPath(DEFAULT_IDENTITY_API_CONTEXT_PATH)
            .defaultPort(DEFAULT_IDENTITY_API_PORT)
            .useDefaultContext(true)
            .name("Identity API")
            .build();
    @Inject
    private WebService webService;
    @Inject
    private MessageProcessorRegistry messageProcessorRegistry;
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
        var mapper = context.getTypeManager().getMapper();
        var loader = new SelfDescriptionLoader(mapper);
        var selfDescription = Optional.ofNullable(context.getSetting(SELF_DESCRIPTION_DOCUMENT_PATH_SETTING, null))
                .map(loader::fromFile)
                .orElse(loader.fromClasspath(DEFAULT_SELF_DESCRIPTION_FILE_NAME));
        var identityHubController = new IdentityHubController(messageProcessorRegistry, selfDescription);

        var webServiceConfig = configurer.configure(context, webServer, SETTINGS);

        context.registerService(IdentityHubApiConfiguration.class, new IdentityHubApiConfiguration(webServiceConfig));
        webService.registerResource(webServiceConfig.getContextAlias(), identityHubController);
    }

}
