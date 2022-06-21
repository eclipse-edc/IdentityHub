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

import org.eclipse.dataspaceconnector.identityhub.models.WebNodeInterfaceMethod;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry used to provide the right MessageProcessor according to the message method.
 */
public class MessageProcessorRegistry {

    private static final InterfaceNotImplementedProcessor DEFAULT_PROCESSOR = new InterfaceNotImplementedProcessor();

    private final Map<WebNodeInterfaceMethod, MessageProcessor> messageProcessorsByMethod;

    public MessageProcessorRegistry() {
        this.messageProcessorsByMethod = new HashMap<>();
    }

    public void register(WebNodeInterfaceMethod method, MessageProcessor messageProcessor) {
        messageProcessorsByMethod.put(method, messageProcessor);
    }

    public MessageProcessor resolve(WebNodeInterfaceMethod method) {
        return messageProcessorsByMethod.getOrDefault(method, DEFAULT_PROCESSOR);
    }
}
