/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.issuerservice.store.sql.holder;

import org.eclipse.edc.issuerservice.spi.holder.model.Holder;
import org.eclipse.edc.issuerservice.spi.holder.store.HolderStore;
import org.eclipse.edc.issuerservice.spi.holder.store.HolderStoreTestBase;
import org.eclipse.edc.issuerservice.store.sql.holder.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.testfixtures.PostgresqlStoreSetupExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

@PostgresqlIntegrationTest
@ExtendWith(PostgresqlStoreSetupExtension.class)
class SqlHolderStoreTest extends HolderStoreTestBase {

    private final HolderStoreStatements statements = new PostgresDialectStatements();
    private SqlHolderStore store;

    @BeforeEach
    void setup(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) {
        var typeManager = new JacksonTypeManager();
        store = new SqlHolderStore(extension.getDataSourceRegistry(), extension.getDatasourceName(),
                extension.getTransactionContext(), typeManager::getMapper, queryExecutor, statements);

        var schema = TestUtils.getResourceFileContentAsString("holder-schema.sql");
        extension.runQuery(schema);
    }

    @AfterEach
    void tearDown(PostgresqlStoreSetupExtension extension) {
        extension.runQuery("DROP TABLE " + statements.getHoldersTable() + " CASCADE");
    }

    @Test
    void create_whenDidExistsInSameParticipantContext_shouldFail() {
        store.create(holder("holder-1", "participant", "did:web:holder"));

        assertThat(store.create(holder("holder-2", "participant", "did:web:holder"))).isFailed();
        assertThat(store.create(holder("holder-3", "other-participant", "did:web:holder"))).isSucceeded();
    }

    private static Holder holder(String id, String participantContextId, String did) {
        return Holder.Builder.newInstance().holderId(id).participantContextId(participantContextId).did(did).build();
    }

    @Override
    protected HolderStore getStore() {
        return store;
    }
}