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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.identityhub.models.MessageResponseObject;
import org.eclipse.dataspaceconnector.identityhub.models.MessageStatus;
import org.eclipse.dataspaceconnector.identityhub.store.IdentityHubStore;

import java.io.IOException;

import static org.eclipse.dataspaceconnector.identityhub.models.MessageResponseObject.MESSAGE_ID_VALUE;

/**
 * Processor of "CollectionsWrite" messages, in order to write objects into the {@link IdentityHubStore}.
 */
public class CollectionsWriteProcessor implements MessageProcessor {

    private final IdentityHubStore identityHubStore;
    private final ObjectMapper objectMapper;

    public CollectionsWriteProcessor(IdentityHubStore identityHubStore, ObjectMapper objectMapper) {
        this.identityHubStore = identityHubStore;
        this.objectMapper = objectMapper;
    }

    @Override
    public MessageResponseObject process(byte[] data) {
        Object hubObject;
        try {
            hubObject = objectMapper.readValue(data, Object.class);
        } catch (IllegalArgumentException | IOException e) {
            return MessageResponseObject.Builder.newInstance().messageId(MESSAGE_ID_VALUE).status(MessageStatus.MALFORMED_MESSAGE).build();
        }
        identityHubStore.add(hubObject);
        return MessageResponseObject.Builder.newInstance().messageId(MESSAGE_ID_VALUE).status(MessageStatus.OK).build();
    }
}
