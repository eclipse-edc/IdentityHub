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

package org.eclipse.edc.identityhub;

import org.eclipse.edc.identityhub.processor.CollectionsQueryProcessor;
import org.eclipse.edc.identityhub.processor.CollectionsWriteProcessor;
import org.eclipse.edc.identityhub.processor.FeatureDetectionReadProcessor;
import org.eclipse.edc.identityhub.processor.MessageProcessorRegistryImpl;
import org.eclipse.edc.identityhub.processor.data.DataValidatorRegistryImpl;
import org.eclipse.edc.identityhub.processor.data.JwtVerifiableCredentialValidator;
import org.eclipse.edc.identityhub.spi.processor.MessageProcessorRegistry;
import org.eclipse.edc.identityhub.spi.processor.data.DataValidatorRegistry;
import org.eclipse.edc.identityhub.store.InMemoryIdentityHubStore;
import org.eclipse.edc.identityhub.store.spi.IdentityHubStore;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;

import static org.eclipse.edc.identityhub.spi.model.WebNodeInterfaceMethod.COLLECTIONS_QUERY;
import static org.eclipse.edc.identityhub.spi.model.WebNodeInterfaceMethod.COLLECTIONS_WRITE;
import static org.eclipse.edc.identityhub.spi.model.WebNodeInterfaceMethod.FEATURE_DETECTION_READ;

/**
 * EDC extension to boot the services used by the Identity Hub
 */

public class IdentityHubExtension implements ServiceExtension {


    @Inject
    private IdentityHubStore identityHubStore;

    @Inject
    private TransactionContext transactionContext;

    @Inject
    private DataValidatorRegistry dataValidatorRegistry;


    @Provider(isDefault = true)
    public MessageProcessorRegistry messageProcessorRegistry(ServiceExtensionContext context) {
        var mapper = context.getTypeManager().getMapper();
        var methodProcessorFactory = new MessageProcessorRegistryImpl();


        dataValidatorRegistry.register(new JwtVerifiableCredentialValidator(mapper));

        methodProcessorFactory.register(COLLECTIONS_QUERY, new CollectionsQueryProcessor(identityHubStore, transactionContext));
        methodProcessorFactory.register(COLLECTIONS_WRITE, new CollectionsWriteProcessor(identityHubStore, context.getMonitor(), transactionContext, dataValidatorRegistry));
        methodProcessorFactory.register(FEATURE_DETECTION_READ, new FeatureDetectionReadProcessor());

        return methodProcessorFactory;
    }

    @Provider(isDefault = true)
    public IdentityHubStore identityHubStore() {
        return new InMemoryIdentityHubStore();
    }


    @Provider(isDefault = true)
    public DataValidatorRegistry dataValidatorRegistry() {
        return new DataValidatorRegistryImpl();
    }
}
