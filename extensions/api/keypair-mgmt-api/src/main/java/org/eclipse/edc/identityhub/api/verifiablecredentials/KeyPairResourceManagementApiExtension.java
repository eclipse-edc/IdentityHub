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

package org.eclipse.edc.identityhub.api.verifiablecredentials;

import org.eclipse.edc.identityhub.api.configuration.ManagementApiConfiguration;
import org.eclipse.edc.identityhub.api.verifiablecredentials.v1.KeyPairResourceApiController;
import org.eclipse.edc.identityhub.spi.KeyPairService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;

import static org.eclipse.edc.identityhub.api.verifiablecredentials.KeyPairResourceManagementApiExtension.NAME;

@Extension(NAME)
public class KeyPairResourceManagementApiExtension implements ServiceExtension {
    public static final String NAME = "KeyPairResource Management API Extension";

    @Inject
    private ManagementApiConfiguration apiConfiguration;
    @Inject
    private WebService webService;
    @Inject
    private KeyPairService keyPairService;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var controller = new KeyPairResourceApiController(keyPairService);
        webService.registerResource(apiConfiguration.getContextAlias(), controller);
    }
}
