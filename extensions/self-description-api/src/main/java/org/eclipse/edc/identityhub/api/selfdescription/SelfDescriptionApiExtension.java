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

package org.eclipse.edc.identityhub.api.selfdescription;

import org.eclipse.edc.identityhub.api.IdentityHubApiConfiguration;
import org.eclipse.edc.identityhub.api.selfdescription.controller.SelfDescriptionController;
import org.eclipse.edc.identityhub.api.selfdescription.loader.SelfDescriptionLoader;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;

import java.util.Optional;

/**
 * Temporary extension for serving a static self-description document.
 */
@Extension(value = SelfDescriptionApiExtension.NAME)
public class SelfDescriptionApiExtension implements ServiceExtension {

    public static final String NAME = "Self Description API";
    @Setting
    private static final String SELF_DESCRIPTION_DOCUMENT_PATH_SETTING = "edc.self.description.document.path";
    private static final String DEFAULT_SELF_DESCRIPTION_FILE_NAME = "default-self-description.json";

    @Inject
    private WebService webService;

    @Inject
    private IdentityHubApiConfiguration configuration;

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
        var controller = new SelfDescriptionController(selfDescription);
        webService.registerResource(configuration.getContextAlias(), controller);
    }
}
