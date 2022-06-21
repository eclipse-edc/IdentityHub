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

package org.eclipse.dataspaceconnector.identityhub;

import org.eclipse.dataspaceconnector.identityhub.api.IdentityHubController;
import org.eclipse.dataspaceconnector.identityhub.processor.CollectionsQueryProcessor;
import org.eclipse.dataspaceconnector.identityhub.processor.CollectionsWriteProcessor;
import org.eclipse.dataspaceconnector.identityhub.processor.FeatureDetectionReadProcessor;
import org.eclipse.dataspaceconnector.identityhub.processor.MessageProcessorRegistry;
import org.eclipse.dataspaceconnector.identityhub.store.IdentityHubInMemoryStore;
import org.eclipse.dataspaceconnector.identityhub.store.IdentityHubStore;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provider;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;

import static org.eclipse.dataspaceconnector.identityhub.models.WebNodeInterfaceMethod.COLLECTIONS_QUERY;
import static org.eclipse.dataspaceconnector.identityhub.models.WebNodeInterfaceMethod.COLLECTIONS_WRITE;
import static org.eclipse.dataspaceconnector.identityhub.models.WebNodeInterfaceMethod.FEATURE_DETECTION_READ;

/**
 * EDC extension to boot the services used by the Identity Hub
 */
public class IdentityHubExtension implements ServiceExtension {
    @Inject
    private WebService webService;

    @Inject
    private IdentityHubStore identityHubStore;

    @Inject
    private TypeManager typeManager;

    @Override
    public void initialize(ServiceExtensionContext context) {

        var methodProcessorFactory = new MessageProcessorRegistry();
        methodProcessorFactory.register(COLLECTIONS_QUERY, new CollectionsQueryProcessor(identityHubStore));
        methodProcessorFactory.register(COLLECTIONS_WRITE, new CollectionsWriteProcessor(identityHubStore, typeManager.getMapper()));
        methodProcessorFactory.register(FEATURE_DETECTION_READ, new FeatureDetectionReadProcessor());

        var identityHubController = new IdentityHubController(methodProcessorFactory);
        webService.registerResource(identityHubController);
    }

    @Provider(isDefault = true)
    public IdentityHubStore identityHubStore() {
        return new IdentityHubInMemoryStore();
    }
}
