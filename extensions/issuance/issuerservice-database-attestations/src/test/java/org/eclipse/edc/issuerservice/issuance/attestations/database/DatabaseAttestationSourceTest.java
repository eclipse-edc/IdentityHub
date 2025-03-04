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

import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

@PostgresqlIntegrationTest
@ExtendWith(PostgresqlStoreSetupExtension.class)
class DatabaseAttestationSourceTest {

    private final String tableName = "membership_attestation";
    private DatabaseAttestationSource attestationSource;

    @BeforeEach
    void setup(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) {

        try (Connection connection = extension.getConnection()) {
            var sql = TestUtils.getResourceFileContentAsString("test-attestation-table.sql");
            queryExecutor.execute(connection, sql);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        attestationSource = new DatabaseAttestationSource(extension.getDatasourceName(),
                true,
                new ObjectMapper(),
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
}