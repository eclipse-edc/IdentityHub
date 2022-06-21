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

import org.eclipse.dataspaceconnector.identityhub.models.MessageResponseObject;
import org.eclipse.dataspaceconnector.identityhub.models.MessageStatus;
import org.eclipse.dataspaceconnector.identityhub.store.IdentityHubStore;

import java.util.Collection;

import static org.eclipse.dataspaceconnector.identityhub.models.MessageResponseObject.MESSAGE_ID_VALUE;

/**
 * Processor of "CollectionsQuery" messages, returning the list of objects available in the {@link IdentityHubStore}
 */
public class CollectionsQueryProcessor implements MessageProcessor {

    private final IdentityHubStore identityHubStore;

    public CollectionsQueryProcessor(IdentityHubStore identityHubStore) {
        this.identityHubStore = identityHubStore;
    }

    @Override
    public MessageResponseObject process(byte[] data) {
        Collection<?> entries = identityHubStore.getAll();
        return MessageResponseObject.Builder.newInstance()
                .messageId(MESSAGE_ID_VALUE)
                .status(MessageStatus.OK)
                .entries(entries)
                .build();
    }
}
