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

import org.eclipse.edc.issuerservice.issuance.database.DatabaseAttestationSourceValidator;
import org.eclipse.edc.issuerservice.spi.issuance.model.AttestationDefinition;
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
class DatabaseAttestationSourceValidatorPostgresTest {

    private final String tableName = "membership_attestation";
    private String dataSourceName;
    private DatabaseAttestationSourceValidator validator;

    @BeforeEach
    void setup(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) {

        try (Connection connection = extension.getConnection()) {
            var sql = TestUtils.getResourceFileContentAsString("test-attestation-table.sql");
            queryExecutor.execute(connection, sql);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        dataSourceName = extension.getDatasourceName();
        validator = new DatabaseAttestationSourceValidator(extension.getDataSourceRegistry(),
                extension.getTransactionContext(),
                queryExecutor);
    }

    @AfterEach
    void tearDown(PostgresqlStoreSetupExtension extension) {
        extension.runQuery("DROP TABLE %s".formatted(tableName));
    }

    @Test
    void validate() {
        var definition = createDefinition(Map.of("dataSourceName", dataSourceName, "tableName", tableName));

        assertThat(validator.validate(definition)).isSucceeded();
    }

    @Test
    void validate_whenTableNotExists() {
        var definition = createDefinition(Map.of("dataSourceName", dataSourceName, "tableName", "not_exist"));

        assertThat(validator.validate(definition)).isFailed().detail().contains("not_exist");
    }

    @Test
    void validate_whenIdColumnNotExists() {
        var definition = createDefinition(Map.of("dataSourceName", dataSourceName,
                "tableName", tableName,
                "idColumn", "not_a_column"));

        assertThat(validator.validate(definition)).isFailed().detail().contains("not_a_column");
    }

    @Test
    void validate_whenDataSourceNotRegistered() {
        var definition = createDefinition(Map.of("dataSourceName", "unregistered", "tableName", tableName));

        assertThat(validator.validate(definition)).isFailed().detail().contains("unregistered");
    }

    private AttestationDefinition createDefinition(Map<String, Object> configuration) {
        return AttestationDefinition.Builder.newInstance().id("att1")
                .attestationType("database")
                .participantContextId("participantContextId")
                .configuration(configuration)
                .build();
    }
}
