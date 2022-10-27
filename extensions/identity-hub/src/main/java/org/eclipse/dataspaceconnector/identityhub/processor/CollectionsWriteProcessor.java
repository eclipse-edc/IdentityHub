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

import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspaceconnector.identityhub.model.MessageResponseObject;
import org.eclipse.dataspaceconnector.identityhub.model.MessageStatus;
import org.eclipse.dataspaceconnector.identityhub.store.IdentityHubStore;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;

import java.text.ParseException;

import static org.eclipse.dataspaceconnector.identityhub.model.MessageResponseObject.MESSAGE_ID_VALUE;

/**
 * Processor of "CollectionsWrite" messages, in order to write objects into the {@link IdentityHubStore}.
 */
public class CollectionsWriteProcessor implements MessageProcessor {

    private static final String VERIFIABLE_CREDENTIALS_KEY = "vc";
    private final IdentityHubStore identityHubStore;

    private final TransactionContext transactionContext;

    public CollectionsWriteProcessor(IdentityHubStore identityHubStore, TransactionContext transactionContext) {
        this.identityHubStore = identityHubStore;
        this.transactionContext = transactionContext;
    }

    @Override
    public MessageResponseObject process(byte[] data) {
        try {
            var jwt = SignedJWT.parse(new String(data));
            if (jwt.getJWTClaimsSet().getClaim(VERIFIABLE_CREDENTIALS_KEY) == null) {
                return MessageResponseObject.Builder.newInstance().messageId(MESSAGE_ID_VALUE).status(MessageStatus.MALFORMED_MESSAGE).build();
            }
        } catch (ParseException e) {
            return MessageResponseObject.Builder.newInstance().messageId(MESSAGE_ID_VALUE).status(MessageStatus.MALFORMED_MESSAGE).build();
        }

        transactionContext.execute(() -> identityHubStore.add(data));
        return MessageResponseObject.Builder.newInstance().messageId(MESSAGE_ID_VALUE).status(MessageStatus.OK).build();
    }
}
