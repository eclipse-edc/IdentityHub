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

package org.eclipse.dataspaceconnector.identityhub.api;

import org.eclipse.dataspaceconnector.identityhub.api.controller.IdentityHubController;
import org.eclipse.dataspaceconnector.identityhub.selfdescription.SelfDescriptionLoader;
import org.eclipse.dataspaceconnector.identityhub.spi.processor.MessageProcessorRegistry;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.EdcSetting;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Extension;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Inject;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;

import java.util.Optional;

/**
 * EDC extension for Identity Hub API
 */
@Extension(value = IdentityHubApiExtension.NAME)
public class IdentityHubApiExtension implements ServiceExtension {

    public static final String NAME = "Identity Hub API";
    @EdcSetting
    private static final String SELF_DESCRIPTION_DOCUMENT_PATH_SETTING = "edc.self.description.document.path";
    private static final String DEFAULT_SELF_DESCRIPTION_FILE_NAME = "default-self-description.json";
    @Inject
    private WebService webService;

    @Inject
    private TransactionContext transactionContext;
    @Inject
    private MessageProcessorRegistry messageProcessorRegistry;

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
        webService.registerResource(identityHubController);
    }

}
