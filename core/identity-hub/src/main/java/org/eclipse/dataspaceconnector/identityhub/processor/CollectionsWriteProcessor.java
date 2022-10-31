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
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspaceconnector.identityhub.spi.credentials.model.VerifiableCredential;
import org.eclipse.dataspaceconnector.identityhub.spi.model.MessageRequestObject;
import org.eclipse.dataspaceconnector.identityhub.spi.model.MessageResponseObject;
import org.eclipse.dataspaceconnector.identityhub.spi.model.MessageStatus;
import org.eclipse.dataspaceconnector.identityhub.spi.processor.MessageProcessor;
import org.eclipse.dataspaceconnector.identityhub.store.spi.IdentityHubRecord;
import org.eclipse.dataspaceconnector.identityhub.store.spi.IdentityHubStore;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;

import java.util.Optional;

import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.identityhub.spi.model.MessageResponseObject.MESSAGE_ID_VALUE;

/**
 * Processor of "CollectionsWrite" messages, in order to write objects into the {@link IdentityHubStore}.
 */
public class CollectionsWriteProcessor implements MessageProcessor {
    private static final String VERIFIABLE_CREDENTIALS_KEY = "vc";

    private final IdentityHubStore identityHubStore;
    private final ObjectMapper mapper;
    private final Monitor monitor;
    private final TransactionContext transactionContext;

    public CollectionsWriteProcessor(IdentityHubStore identityHubStore, ObjectMapper mapper, Monitor monitor, TransactionContext transactionContext) {
        this.identityHubStore = identityHubStore;
        this.mapper = mapper;
        this.monitor = monitor;
        this.transactionContext = transactionContext;
    }

    @Override
    public MessageResponseObject process(MessageRequestObject requestObject) {
        var record = createRecord(requestObject);
        if (record.failed()) {
            monitor.warning(format("Failed to create record %s", record.getFailureDetail()));
            return MessageResponseObject.Builder.newInstance().messageId(MESSAGE_ID_VALUE).status(MessageStatus.MALFORMED_MESSAGE).build();
        }

        try {
            transactionContext.execute(() -> identityHubStore.add(record.getContent()));
        } catch (Exception e) {
            monitor.warning("Failed to add Verifiable Credential to Identity Hub", e);
            return MessageResponseObject.Builder.newInstance().messageId(MESSAGE_ID_VALUE).status(MessageStatus.UNHANDLED_ERROR).build();
        }

        return MessageResponseObject.Builder.newInstance().messageId(MESSAGE_ID_VALUE).status(MessageStatus.OK).build();
    }

    private Result<IdentityHubRecord> createRecord(MessageRequestObject requestObject) {
        var parsing = canParseData(requestObject.getData());
        if (parsing.failed()) {
            return Result.failure(parsing.getFailureMessages());
        }
        var descriptor = requestObject.getDescriptor();
        if (descriptor.getRecordId() == null) {
            return Result.failure("Missing mandatory `recordId` in descriptor");
        }
        if (descriptor.getDateCreated() == 0) {
            return Result.failure("Missing mandatory `dateCreated` in descriptor");
        }
        return Result.success(IdentityHubRecord.Builder.newInstance()
                .id(requestObject.getDescriptor().getRecordId())
                .payload(requestObject.getData())
                .createdAt(requestObject.getDescriptor().getDateCreated())
                .build());
    }

    private Result<Void> canParseData(byte[] data) {
        try {
            var jwt = SignedJWT.parse(new String(data));
            var vcClaim = Optional.ofNullable(jwt.getJWTClaimsSet().getClaim(VERIFIABLE_CREDENTIALS_KEY))
                    .orElseThrow(() -> new EdcException("Missing `vc` claim in signed JWT"));
            mapper.readValue(vcClaim.toString(), VerifiableCredential.class);
        } catch (Exception e) {
            return Result.failure("Failed to parse Verifiable Credential: " + e.getMessage());
        }
        return Result.success();
    }
}
