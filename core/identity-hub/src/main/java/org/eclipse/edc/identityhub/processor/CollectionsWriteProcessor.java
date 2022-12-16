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
import org.eclipse.edc.identityhub.spi.processor.data.DataValidatorRegistry;
import org.eclipse.edc.identityhub.store.spi.IdentityHubRecord;
import org.eclipse.edc.identityhub.store.spi.IdentityHubStore;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transaction.spi.TransactionContext;

import static java.lang.String.format;

/**
 * Processor of "CollectionsWrite" messages, in order to write objects into the {@link IdentityHubStore}.
 */
public class CollectionsWriteProcessor implements MessageProcessor {

    private final IdentityHubStore identityHubStore;
    private final Monitor monitor;
    private final TransactionContext transactionContext;

    private final DataValidatorRegistry validatorRegistry;

    public CollectionsWriteProcessor(IdentityHubStore identityHubStore, Monitor monitor, TransactionContext transactionContext, DataValidatorRegistry validatorRegistry) {
        this.identityHubStore = identityHubStore;
        this.monitor = monitor;
        this.transactionContext = transactionContext;
        this.validatorRegistry = validatorRegistry;
    }

    @Override
    public MessageResponseObject process(MessageRequestObject requestObject) {
        var record = createRecord(requestObject);
        if (record.failed()) {
            monitor.warning(format("Failed to create record %s", record.getFailureDetail()));
            return MessageResponseObject.Builder.newInstance().status(MessageStatus.MALFORMED_MESSAGE).build();
        }

        try {
            transactionContext.execute(() -> identityHubStore.add(record.getContent()));
        } catch (Exception e) {
            monitor.warning("Failed to add Verifiable Credential to Identity Hub", e);
            return MessageResponseObject.Builder.newInstance().status(MessageStatus.UNHANDLED_ERROR).build();
        }

        return MessageResponseObject.Builder.newInstance().status(MessageStatus.OK).build();
    }

    private Result<IdentityHubRecord> createRecord(MessageRequestObject requestObject) {
        var descriptor = requestObject.getDescriptor();
        if (descriptor.getRecordId() == null) {
            return Result.failure("Missing mandatory `recordId` in descriptor");
        }
        if (descriptor.getDateCreated() == 0) {
            return Result.failure("Missing mandatory `dateCreated` in descriptor");
        }
        if (descriptor.getDataFormat() == null) {
            return Result.failure("Missing mandatory `dataFormat` in descriptor");
        }

        var validator = validatorRegistry.resolve(descriptor.getDataFormat());

        if (validator == null) {
            return Result.failure(format("No registered validator for `dataFormat` %s", descriptor.getDataFormat()));
        }
        var parsing = validator.validate(requestObject.getData());

        if (parsing.failed()) {
            return Result.failure(parsing.getFailureMessages());
        }
        return Result.success(IdentityHubRecord.Builder.newInstance()
                .id(descriptor.getRecordId())
                .payload(requestObject.getData())
                .payloadFormat(descriptor.getDataFormat())
                .createdAt(descriptor.getDateCreated())
                .build());
    }
}
