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

package org.eclipse.edc.identityhub.spi.processor;

import org.eclipse.edc.identityhub.spi.model.WebNodeInterfaceMethod;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;

@ExtensionPoint
public interface MessageProcessorRegistry {

    void register(WebNodeInterfaceMethod method, MessageProcessor messageProcessor);

    MessageProcessor resolve(WebNodeInterfaceMethod method);

}
