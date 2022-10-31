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

package org.eclipse.dataspaceconnector.identityhub.processor;

import org.eclipse.dataspaceconnector.identityhub.spi.model.WebNodeInterfaceMethod;
import org.eclipse.dataspaceconnector.identityhub.spi.processor.MessageProcessor;
import org.eclipse.dataspaceconnector.identityhub.spi.processor.MessageProcessorRegistry;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry used to provide the right MessageProcessor according to the message method.
 */
public class MessageProcessorRegistryImpl implements MessageProcessorRegistry {

    private static final InterfaceNotImplementedProcessor DEFAULT_PROCESSOR = new InterfaceNotImplementedProcessor();

    private final Map<WebNodeInterfaceMethod, MessageProcessor> messageProcessorsByMethod;

    public MessageProcessorRegistryImpl() {
        messageProcessorsByMethod = new HashMap<>();
    }

    @Override
    public void register(WebNodeInterfaceMethod method, MessageProcessor messageProcessor) {
        messageProcessorsByMethod.put(method, messageProcessor);
    }

    @Override
    public MessageProcessor resolve(WebNodeInterfaceMethod method) {
        return messageProcessorsByMethod.getOrDefault(method, DEFAULT_PROCESSOR);
    }
}
