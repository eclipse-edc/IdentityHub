/*
 *  Copyright (c) 2023 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.store.cosmos;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.failsafe.RetryPolicy;
import org.eclipse.edc.azure.cosmos.CosmosDbApi;
import org.eclipse.edc.identityhub.store.cosmos.model.IdentityHubRecordDocument;
import org.eclipse.edc.identityhub.store.spi.IdentityHubRecord;
import org.eclipse.edc.identityhub.store.spi.IdentityHubStore;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.stream.Stream;

import static dev.failsafe.Failsafe.with;

/**
 * CosmosDB implementation for {@link org.eclipse.edc.identityhub.store.spi.IdentityHubStore}.
 */
public class CosmosIdentityHubStore implements IdentityHubStore {
    private final CosmosDbApi cosmosDbApi;
    private final String partitionKey;
    private final ObjectMapper objectMapper;
    private final RetryPolicy<Object> retryPolicy;


    public CosmosIdentityHubStore(CosmosDbApi participantDb, String partitionKey, ObjectMapper objectMapper, RetryPolicy<Object> retryPolicy) {
        this.cosmosDbApi = Objects.requireNonNull(participantDb);
        this.partitionKey = partitionKey;
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.retryPolicy = Objects.requireNonNull(retryPolicy);
    }

    @Override
    public @NotNull Stream<IdentityHubRecord> getAll() {
        return with(retryPolicy).get(() -> cosmosDbApi.queryAllItems())
                .stream()
                .map(this::convertObject)
                .map(IdentityHubRecordDocument::getWrappedInstance);
    }

    @Override
    public void add(IdentityHubRecord record) {
        var document = new IdentityHubRecordDocument(record, partitionKey);
        cosmosDbApi.saveItem(document);
    }

    private IdentityHubRecordDocument convertObject(Object databaseDocument) {
        try {
            return objectMapper.readValue(objectMapper.writeValueAsString(databaseDocument), IdentityHubRecordDocument.class);
        } catch (JsonProcessingException e) {
            throw new EdcPersistenceException(e);
        }
    }
}
