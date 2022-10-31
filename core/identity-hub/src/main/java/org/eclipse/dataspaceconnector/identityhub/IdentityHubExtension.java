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

import org.eclipse.dataspaceconnector.identityhub.processor.CollectionsQueryProcessor;
import org.eclipse.dataspaceconnector.identityhub.processor.CollectionsWriteProcessor;
import org.eclipse.dataspaceconnector.identityhub.processor.FeatureDetectionReadProcessor;
import org.eclipse.dataspaceconnector.identityhub.processor.MessageProcessorRegistryImpl;
import org.eclipse.dataspaceconnector.identityhub.spi.processor.MessageProcessorRegistry;
import org.eclipse.dataspaceconnector.identityhub.store.InMemoryIdentityHubStore;
import org.eclipse.dataspaceconnector.identityhub.store.spi.IdentityHubStore;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Inject;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Provider;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;

import static org.eclipse.dataspaceconnector.identityhub.spi.model.WebNodeInterfaceMethod.COLLECTIONS_QUERY;
import static org.eclipse.dataspaceconnector.identityhub.spi.model.WebNodeInterfaceMethod.COLLECTIONS_WRITE;
import static org.eclipse.dataspaceconnector.identityhub.spi.model.WebNodeInterfaceMethod.FEATURE_DETECTION_READ;

/**
 * EDC extension to boot the services used by the Identity Hub
 */

public class IdentityHubExtension implements ServiceExtension {


    @Inject
    private IdentityHubStore identityHubStore;

    @Inject
    private TransactionContext transactionContext;


    @Provider(isDefault = true)
    public MessageProcessorRegistry messageProcessorRegistry(ServiceExtensionContext context) {
        var mapper = context.getTypeManager().getMapper();
        var methodProcessorFactory = new MessageProcessorRegistryImpl();

        methodProcessorFactory.register(COLLECTIONS_QUERY, new CollectionsQueryProcessor(identityHubStore, transactionContext));
        methodProcessorFactory.register(COLLECTIONS_WRITE, new CollectionsWriteProcessor(identityHubStore, mapper, context.getMonitor(), transactionContext));
        methodProcessorFactory.register(FEATURE_DETECTION_READ, new FeatureDetectionReadProcessor());

        return methodProcessorFactory;
    }

    @Provider(isDefault = true)
    public IdentityHubStore identityHubStore() {
        return new InMemoryIdentityHubStore();
    }
}
