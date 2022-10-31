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

import org.eclipse.dataspaceconnector.identityhub.spi.model.MessageRequestObject;
import org.eclipse.dataspaceconnector.identityhub.spi.model.MessageResponseObject;
import org.eclipse.dataspaceconnector.identityhub.spi.model.MessageStatus;
import org.eclipse.dataspaceconnector.identityhub.spi.processor.MessageProcessor;

import java.util.List;

import static org.eclipse.dataspaceconnector.identityhub.spi.model.MessageResponseObject.MESSAGE_ID_VALUE;

/**
 * Default message processor when a non-supported interface is provided
 */
public class InterfaceNotImplementedProcessor implements MessageProcessor {

    @Override
    public MessageResponseObject process(MessageRequestObject requestObject) {
        return MessageResponseObject.Builder.newInstance()
                .messageId(MESSAGE_ID_VALUE)
                .status(MessageStatus.INTERFACE_NOT_IMPLEMENTED)
                .entries(List.of())
                .build();
    }
}
