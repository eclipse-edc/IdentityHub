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

import org.eclipse.dataspaceconnector.identityhub.models.FeatureDetection;
import org.eclipse.dataspaceconnector.identityhub.models.MessageResponseObject;
import org.eclipse.dataspaceconnector.identityhub.models.MessageStatus;
import org.eclipse.dataspaceconnector.identityhub.models.WebNodeInterfaces;

import java.util.List;

import static org.eclipse.dataspaceconnector.identityhub.models.MessageResponseObject.MESSAGE_ID_VALUE;
import static org.eclipse.dataspaceconnector.identityhub.models.WebNodeInterfaceMethod.COLLECTIONS_QUERY;
import static org.eclipse.dataspaceconnector.identityhub.models.WebNodeInterfaceMethod.COLLECTIONS_WRITE;

/**
 * Processor of "FeatureDetectionRead" messages
 */
public class FeatureDetectionReadProcessor implements MessageProcessor {

    @Override
    public MessageResponseObject process(byte[] data) {
        return MessageResponseObject.Builder.newInstance()
                .messageId(MESSAGE_ID_VALUE)
                .status(MessageStatus.OK)
                .entries(List.of(
                        FeatureDetection.Builder.newInstance().interfaces(
                                WebNodeInterfaces.Builder.newInstance()
                                        .supportedCollection(COLLECTIONS_QUERY.getName())
                                    .supportedCollection(COLLECTIONS_WRITE.getName())
                                    .build()
                        ).build()
                ))
                .build();
    }
}
