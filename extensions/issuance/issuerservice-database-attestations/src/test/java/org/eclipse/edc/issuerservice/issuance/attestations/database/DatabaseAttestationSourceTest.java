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

package org.eclipse.edc.issuerservice.issuance.attestations.database;

import org.assertj.core.api.Assertions;
import org.eclipse.edc.issuerservice.issuance.database.DatabaseAttestationSource;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.testfixtures.PostgresqlStoreSetupExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.ObjectMapper;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

@PostgresqlIntegrationTest
@ExtendWith(PostgresqlStoreSetupExtension.class)
class DatabaseAttestationSourceTest {

    private final String tableName = "membership_attestation";
    private DatabaseAttestationSource attestationSource;
    private PostgresqlStoreSetupExtension extension;
    private QueryExecutor queryExecutor;

    @BeforeEach
    void setup(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) {
        this.extension = extension;
        this.queryExecutor = queryExecutor;

        try (Connection connection = extension.getConnection()) {
            var sql = TestUtils.getResourceFileContentAsString("test-attestation-table.sql");
            queryExecutor.execute(connection, sql);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        attestationSource = new DatabaseAttestationSource(extension.getDatasourceName(),
                true,
                ObjectMapper::new,
                tableName,
                extension.getDataSourceRegistry(),
                queryExecutor,
                extension.getTransactionContext(),
                "holder_id");
    }

    @AfterEach
    void tearDown(PostgresqlStoreSetupExtension extension) {
        extension.runQuery("DROP TABLE %s".formatted(tableName));
    }

    @Test
    void createAttestationSource() {
        var map = attestationSource.execute(new TestAttestationContext("holder-1", Map.of()));

        assertThat(map).isSucceeded()
                .satisfies(m -> Assertions.assertThat(m).containsEntry("holder_id", "holder-1")
                        .containsEntry("membership_type", "0")
                        .hasEntrySatisfying("membership_start_date", o -> Assertions.assertThat(o).isNotNull())
                        .hasEntrySatisfying("id", o -> Assertions.assertThat(o).isNotNull()));
    }

    @Test
    void createAttestationSource_whenIdNotFound() {
        var map = attestationSource.execute(new TestAttestationContext("holder-notexist", Map.of()));

        assertThat(map).isSucceeded().isNull();
    }

    @Test
    void createAttestationSource_withSchemaQualifiedTableName() {
        var source = createSource("public." + tableName, "holder_id");

        var map = source.execute(new TestAttestationContext("holder-1", Map.of()));

        assertThat(map).isSucceeded()
                .satisfies(m -> Assertions.assertThat(m).containsEntry("holder_id", "holder-1"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"membership_attestation; DROP TABLE membership_attestation; --",
            "membership_attestation WHERE 1=1 OR holder_id",
            "(SELECT * FROM membership_attestation) AS t"})
    void execute_invalidTableName_shouldFail(String invalidTableName) {
        var source = createSource(invalidTableName, "holder_id");

        var result = source.execute(new TestAttestationContext("holder-1", Map.of()));

        assertThat(result).isFailed().detail().contains("table name");
        assertThat(attestationSource.execute(new TestAttestationContext("holder-1", Map.of()))).isSucceeded().isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"holder_id OR 1=1 OR holder_id", "holder_id = holder_id; DROP TABLE membership_attestation; --"})
    void execute_invalidIdColumn_shouldFail(String invalidIdColumn) {
        var source = createSource(tableName, invalidIdColumn);

        var result = source.execute(new TestAttestationContext("holder-notexist", Map.of()));

        assertThat(result).isFailed().detail().contains("ID column name");
        assertThat(attestationSource.execute(new TestAttestationContext("holder-1", Map.of()))).isSucceeded().isNotNull();
    }

    private DatabaseAttestationSource createSource(String tableName, String idColumn) {
        return new DatabaseAttestationSource(extension.getDatasourceName(),
                true,
                ObjectMapper::new,
                tableName,
                extension.getDataSourceRegistry(),
                queryExecutor,
                extension.getTransactionContext(),
                idColumn);
    }
}
