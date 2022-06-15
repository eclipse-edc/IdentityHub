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

import org.eclipse.dataspaceconnector.identityhub.api.featuredetection.FeatureDetectionController;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

/**
 * EDC extension to boot the services used by the Identity Hub
 */
public class IdentityHubExtension implements ServiceExtension {
    @Inject
    private WebService webService;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var controller = new FeatureDetectionController();
        webService.registerResource(controller);
    }
}
