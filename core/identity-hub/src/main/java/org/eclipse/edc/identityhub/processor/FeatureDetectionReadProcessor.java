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

package org.eclipse.edc.identityhub.processor;

import org.eclipse.edc.identityhub.spi.model.FeatureDetection;
import org.eclipse.edc.identityhub.spi.model.MessageRequestObject;
import org.eclipse.edc.identityhub.spi.model.MessageResponseObject;
import org.eclipse.edc.identityhub.spi.model.MessageStatus;
import org.eclipse.edc.identityhub.spi.model.WebNodeInterfaces;
import org.eclipse.edc.identityhub.spi.processor.MessageProcessor;

import java.util.List;

import static org.eclipse.edc.identityhub.spi.model.WebNodeInterfaceMethod.COLLECTIONS_QUERY;
import static org.eclipse.edc.identityhub.spi.model.WebNodeInterfaceMethod.COLLECTIONS_WRITE;

/**
 * Processor of "FeatureDetectionRead" messages
 */
public class FeatureDetectionReadProcessor implements MessageProcessor {

    @Override
    public MessageResponseObject process(MessageRequestObject requestObject) {
        return MessageResponseObject.Builder.newInstance()
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
