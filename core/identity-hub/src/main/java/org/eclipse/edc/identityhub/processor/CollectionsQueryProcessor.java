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

import org.eclipse.edc.identityhub.spi.model.MessageRequestObject;
import org.eclipse.edc.identityhub.spi.model.MessageResponseObject;
import org.eclipse.edc.identityhub.spi.model.MessageStatus;
import org.eclipse.edc.identityhub.spi.processor.MessageProcessor;
import org.eclipse.edc.identityhub.store.spi.IdentityHubRecord;
import org.eclipse.edc.identityhub.store.spi.IdentityHubStore;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.stream.Collectors;

import static org.eclipse.edc.identityhub.spi.model.MessageResponseObject.MESSAGE_ID_VALUE;

/**
 * Processor of "CollectionsQuery" messages, returning the list of objects available in the {@link IdentityHubStore}
 */
public class CollectionsQueryProcessor implements MessageProcessor {

    private final IdentityHubStore identityHubStore;

    private final TransactionContext transactionContext;

    public CollectionsQueryProcessor(IdentityHubStore identityHubStore, TransactionContext transactionContext) {
        this.identityHubStore = identityHubStore;
        this.transactionContext = transactionContext;
    }

    @Override
    public MessageResponseObject process(MessageRequestObject requestObject) {
        try (var stream = transactionContext.execute(identityHubStore::getAll)) {
            var entries = stream.map(IdentityHubRecord::getPayload).collect(Collectors.toList());
            return MessageResponseObject.Builder.newInstance()
                    .messageId(MESSAGE_ID_VALUE)
                    .status(MessageStatus.OK)
                    .entries(entries)
                    .build();
        }
    }
}
