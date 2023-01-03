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

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.models.PartitionKey;
import dev.failsafe.RetryPolicy;
import org.eclipse.edc.azure.cosmos.CosmosDbApiImpl;
import org.eclipse.edc.azure.testfixtures.CosmosTestClient;
import org.eclipse.edc.azure.testfixtures.annotations.AzureCosmosDbIntegrationTest;
import org.eclipse.edc.identityhub.store.cosmos.model.IdentityHubRecordDocument;
import org.eclipse.edc.identityhub.store.spi.IdentityHubRecord;
import org.eclipse.edc.identityhub.store.spi.IdentityHubStore;
import org.eclipse.edc.identityhub.store.spi.IdentityHubStoreTestBase;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@AzureCosmosDbIntegrationTest
class CosmosIdentityHubStoreIntegrationTest extends IdentityHubStoreTestBase {
    private static final String TEST_ID = UUID.randomUUID().toString();
    private static final String DATABASE_NAME = "connector-itest-" + TEST_ID;
    private static final String CONTAINER_NAME = "CosmosIdentityHubRecordIndexTest-" + TEST_ID;
    private static final String TEST_PARTITION_KEY = "test-partitionkey";
    private static CosmosContainer container;
    private static CosmosDatabase database;
    private CosmosIdentityHubStore store;

    @BeforeAll
    static void prepareCosmosClient() {
        var client = CosmosTestClient.createClient();

        var response = client.createDatabaseIfNotExists(DATABASE_NAME);
        database = client.getDatabase(response.getProperties().getId());
        var containerIfNotExists = database.createContainerIfNotExists(CONTAINER_NAME, "/partitionKey");
        container = database.getContainer(containerIfNotExists.getProperties().getId());
    }

    @AfterAll
    static void deleteDatabase() {
        if (database != null) {
            var delete = database.delete();
            assertThat(delete.getStatusCode()).isGreaterThanOrEqualTo(200).isLessThan(300);
        }
    }

    @BeforeEach
    void setUp() {
        assertThat(database).describedAs("CosmosDB database is null - did something go wrong during initialization?").isNotNull();

        var typeManager = new TypeManager();
        typeManager.registerTypes(IdentityHubRecord.class, IdentityHubRecordDocument.class);
        var api = new CosmosDbApiImpl(container, true);
        store = new CosmosIdentityHubStore(api, TEST_PARTITION_KEY, typeManager.getMapper(), RetryPolicy.ofDefaults());
    }

    @AfterEach
    void tearDown() {
        // Delete items one by one as deleteAllItemsByPartitionKey is disabled by default on new Cosmos DB accounts.
        var partitionKey = new PartitionKey(TEST_PARTITION_KEY);
        container.readAllItems(partitionKey, CosmosDbEntity.class)
                .stream().parallel()
                .forEach(i -> container.deleteItem(i.id, partitionKey, null));
    }

    @Override
    protected IdentityHubStore getStore() {
        return store;
    }

    static class CosmosDbEntity {
        String id;

        public void setId(String id) {
            this.id = id;
        }
    }
}
