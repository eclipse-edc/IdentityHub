/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.store.sql.keypair;

import org.eclipse.edc.identityhub.keypair.store.KeyPairResourceStoreTestBase;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairResource;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairState;
import org.eclipse.edc.identityhub.spi.keypair.store.KeyPairResourceStore;
import org.eclipse.edc.identityhub.store.sql.keypair.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.testfixtures.PostgresqlStoreSetupExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

@ComponentTest
@ExtendWith(PostgresqlStoreSetupExtension.class)
class SqlKeyPairResourceStoreTest extends KeyPairResourceStoreTestBase {
    private static final String PARTICIPANT_ID = "test-participant";
    private static final String KEY_ID_INDEX = "keypair_resource_participant_key_id";
    private final KeyPairResourceStoreStatements statements = new PostgresDialectStatements();
    private SqlKeyPairResourceStore store;

    @BeforeEach
    void setup(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) {
        var typeManager = new JacksonTypeManager();
        store = new SqlKeyPairResourceStore(extension.getDataSourceRegistry(), extension.getDatasourceName(),
                extension.getTransactionContext(), typeManager::getMapper, queryExecutor, statements);

        var schema = TestUtils.getResourceFileContentAsString("keypairs-schema.sql");
        extension.runQuery(schema);
    }

    @AfterEach
    void tearDown(PostgresqlStoreSetupExtension extension) {
        extension.runQuery("DROP TABLE " + statements.getTableName() + " CASCADE");
    }

    @Test
    void create_whenKeyIdInUseInSameParticipantContext_shouldFail() {
        store.create(keyPair("id1", PARTICIPANT_ID, KeyPairState.ACTIVATED));

        assertThat(store.create(keyPair("id2", PARTICIPANT_ID, KeyPairState.CREATED))).isFailed()
                .detail().contains("already exists");
    }

    @Test
    void create_whenKeyIdOnlyUsedByKeyPairsOutOfUse_shouldSucceed() {
        store.create(keyPair("id1", PARTICIPANT_ID, KeyPairState.ROTATED));
        store.create(keyPair("id2", PARTICIPANT_ID, KeyPairState.REVOKED));

        assertThat(store.create(keyPair("id3", PARTICIPANT_ID, KeyPairState.ACTIVATED))).isSucceeded();
    }

    @Test
    void create_whenKeyIdInUseInOtherParticipantContext_shouldSucceed() {
        store.create(keyPair("id1", PARTICIPANT_ID, KeyPairState.ACTIVATED));

        assertThat(store.create(keyPair("id2", "other-participant", KeyPairState.ACTIVATED))).isSucceeded();
    }

    @Test
    void update_whenKeyIdInUse_shouldFail() {
        store.create(keyPair("id1", PARTICIPANT_ID, KeyPairState.ACTIVATED));
        var revoked = keyPair("id2", PARTICIPANT_ID, KeyPairState.REVOKED);
        store.create(revoked);

        revoked.activate();

        assertThatThrownBy(() -> store.update(revoked)).isInstanceOf(EdcPersistenceException.class);
    }

    @Test
    void update_whenParticipantContextChanges_shouldReturnNotFound() {
        store.create(keyPair("id1", PARTICIPANT_ID, KeyPairState.ACTIVATED));

        assertThat(store.update(keyPair("id1", "other-participant", KeyPairState.ACTIVATED))).isFailed()
                .detail().contains("does not exist");
        assertThat(store.query(QuerySpec.max())).isSucceeded()
                .satisfies(keyPairs -> assertThat(keyPairs).singleElement()
                        .extracting(KeyPairResource::getParticipantContextId).isEqualTo(PARTICIPANT_ID));
    }

    @Test
    void schema_whenKeyIdsAlreadyInUseSeveralTimes_shouldNotFail(PostgresqlStoreSetupExtension extension) {
        extension.runQuery("DROP INDEX " + KEY_ID_INDEX);
        store.create(keyPair("id1", PARTICIPANT_ID, KeyPairState.ACTIVATED));
        store.create(keyPair("id2", PARTICIPANT_ID, KeyPairState.ACTIVATED));

        extension.runQuery(TestUtils.getResourceFileContentAsString("keypairs-schema.sql"));

        assertThat(store.query(QuerySpec.max())).isSucceeded()
                .satisfies(keyPairs -> assertThat(keyPairs).hasSize(2));
    }

    @Override
    protected KeyPairResourceStore getStore() {
        return store;
    }

    private KeyPairResource keyPair(String id, String participantContextId, KeyPairState state) {
        return KeyPairResource.Builder.newPresentationSigning()
                .id(id)
                .keyId("key-1")
                .participantContextId(participantContextId)
                .privateKeyAlias("alias-" + id)
                .serializedPublicKey("public-key")
                .state(state)
                .build();
    }
}