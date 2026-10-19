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

package org.eclipse.edc.issuerservice.store.sql.attestationdefinition;

import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionStore;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.store.AttestationDefinitionStoreTestBase;
import org.eclipse.edc.issuerservice.spi.issuance.model.AttestationDefinition;
import org.eclipse.edc.issuerservice.store.sql.attestationdefinition.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.testfixtures.PostgresqlStoreSetupExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

@PostgresqlIntegrationTest
@ExtendWith(PostgresqlStoreSetupExtension.class)
class SqlAttestationDefinitionStoreTest extends AttestationDefinitionStoreTestBase {

    private final AttestationDefinitionStoreStatements statements = new PostgresDialectStatements();
    private SqlAttestationDefinitionStore store;

    @BeforeEach
    void setup(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) {
        var typeManager = new JacksonTypeManager();
        store = new SqlAttestationDefinitionStore(extension.getDataSourceRegistry(), extension.getDatasourceName(),
                extension.getTransactionContext(), typeManager::getMapper, queryExecutor, statements);

        var schema = TestUtils.getResourceFileContentAsString("attestation-definition-schema.sql");
        extension.runQuery(schema);
    }

    @AfterEach
    void tearDown(PostgresqlStoreSetupExtension extension) {
        extension.runQuery("DROP TABLE " + statements.getAttestationDefinitionTable() + " CASCADE");
    }

    @Test
    void schema_migratesTableWithIdsUniqueAcrossParticipantContexts(PostgresqlStoreSetupExtension extension) {
        extension.runQuery("DROP TABLE " + statements.getAttestationDefinitionTable() + " CASCADE");
        extension.runQuery("""
                CREATE TABLE attestation_definitions
                (
                    id                     VARCHAR NOT NULL,
                    participant_context_id VARCHAR NOT NULL,
                    attestation_type       VARCHAR NOT NULL,
                    configuration          JSON DEFAULT '{}',
                    private_properties     JSON DEFAULT '{}',
                    created_date           BIGINT  NOT NULL,
                    last_modified_date     BIGINT  NOT NULL,
                    PRIMARY KEY (id)
                );
                CREATE UNIQUE INDEX attestation_definition_ix ON attestation_definitions (id);
                """);
        store.create(attestationDefinition("participant"));

        // the schema is applied on every start, so it must be repeatable
        var schema = TestUtils.getResourceFileContentAsString("attestation-definition-schema.sql");
        extension.runQuery(schema);
        extension.runQuery(schema);

        assertThat(store.create(attestationDefinition("other-participant"))).isSucceeded();
        assertThat(store.create(attestationDefinition("participant"))).isFailed();
    }

    private static AttestationDefinition attestationDefinition(String participantContextId) {
        return AttestationDefinition.Builder.newInstance().id("attestation-id").attestationType("type")
                .configuration(Map.of()).participantContextId(participantContextId).build();
    }

    @Override
    protected AttestationDefinitionStore getStore() {
        return store;
    }
}