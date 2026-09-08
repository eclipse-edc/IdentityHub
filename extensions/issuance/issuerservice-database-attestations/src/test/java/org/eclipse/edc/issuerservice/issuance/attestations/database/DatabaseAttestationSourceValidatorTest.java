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
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class DatabaseAttestationSourceValidatorTest {

    private final DataSourceRegistry dataSourceRegistry = mock();
    private final QueryExecutor queryExecutor = mock();
    private final DataSource dataSource = mock();
    private final DatabaseAttestationSourceValidator validator =
            new DatabaseAttestationSourceValidator(dataSourceRegistry, new NoopTransactionContext(), queryExecutor);

    @BeforeEach
    void setUp() throws SQLException {
        when(dataSource.getConnection()).thenReturn(mock(Connection.class));
    }

    @Test
    void validate_success() {
        when(dataSourceRegistry.resolve("barbaz")).thenReturn(dataSource);

        var definition = createDefinition(Map.of("jdbcUrl", "jdbc:postgresql://localhost:5432/postgres",
                "tableName", "membership_attestations",
                "dataSourceName", "barbaz"));

        assertThat(validator.validate(definition)).isSucceeded();
    }

    @Test
    void validate_wrongAttestationType_shouldFail() {
        var definition = AttestationDefinition.Builder.newInstance().id("att1")
                .attestationType("presentation")
                .participantContextId("participantContextId")
                .configuration(Map.of("tableName", "membership_attestations", "dataSourceName", "barbaz"))
                .build();

        assertThat(validator.validate(definition)).isFailed().detail().contains("database");
    }

    @Test
    void validate_missingDataSourceName_shouldFail() {
        var definition = createDefinition(Map.of("jdbcUrl", "jdbc:postgresql://localhost:5432/postgres",
                "tableName", "membership_attestations"));

        assertThat(validator.validate(definition)).isFailed().detail().contains("dataSourceName");
    }

    @Test
    void validate_missingTableName_shouldFail() {
        var definition = createDefinition(Map.of("jdbcUrl", "jdbc:postgresql://localhost:5432/postgres",
                "dataSourceName", "foobar"));

        assertThat(validator.validate(definition)).isFailed().detail().contains("tableName");
    }

    @Test
    void validate_nullTableName_shouldFail() {
        var configuration = new HashMap<String, Object>();
        configuration.put("dataSourceName", "foobar");
        configuration.put("tableName", null);

        assertThat(validator.validate(createDefinition(configuration))).isFailed().detail().contains("tableName");
    }

    @Test
    void validate_dataSourceNotRegistered_shouldFail() {
        when(dataSourceRegistry.resolve(anyString())).thenReturn(null);

        var definition = createDefinition(Map.of("tableName", "membership_attestations", "dataSourceName", "nonexistent"));

        assertThat(validator.validate(definition)).isFailed().detail().contains("nonexistent");
    }

    @Test
    void validate_tableNotExists_shouldFail() {
        when(dataSourceRegistry.resolve("barbaz")).thenReturn(dataSource);
        when(queryExecutor.single(any(), anyBoolean(), any(), anyString()))
                .thenThrow(new EdcPersistenceException("relation \"not_exist\" does not exist"));

        var definition = createDefinition(Map.of("tableName", "not_exist", "dataSourceName", "barbaz"));

        assertThat(validator.validate(definition)).isFailed().detail().contains("not_exist");
    }

    @Test
    void validate_connectionFails_shouldFail() throws SQLException {
        when(dataSourceRegistry.resolve("barbaz")).thenReturn(dataSource);
        when(dataSource.getConnection()).thenThrow(new SQLException("connection refused"));

        var definition = createDefinition(Map.of("tableName", "membership_attestations", "dataSourceName", "barbaz"));

        assertThat(validator.validate(definition)).isFailed().detail().contains("connection refused");
    }

    @Test
    void validate_customIdColumn_isUsedInProbeQuery() {
        when(dataSourceRegistry.resolve("barbaz")).thenReturn(dataSource);

        var definition = createDefinition(Map.of("tableName", "membership_attestations",
                "dataSourceName", "barbaz",
                "idColumn", "custom_id"));

        assertThat(validator.validate(definition)).isSucceeded();

        var queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(queryExecutor).single(any(), anyBoolean(), any(), queryCaptor.capture());
        assertThat(queryCaptor.getValue()).contains("custom_id").contains("membership_attestations");
    }

    @Test
    void validate_defaultIdColumn_isUsedInProbeQuery() {
        when(dataSourceRegistry.resolve("barbaz")).thenReturn(dataSource);

        var definition = createDefinition(Map.of("tableName", "membership_attestations", "dataSourceName", "barbaz"));

        assertThat(validator.validate(definition)).isSucceeded();

        var queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(queryExecutor).single(any(), anyBoolean(), any(), queryCaptor.capture());
        assertThat(queryCaptor.getValue()).contains("holder_id");
    }

    private AttestationDefinition createDefinition(Map<String, Object> configuration) {
        return AttestationDefinition.Builder.newInstance().id("att1")
                .attestationType("database")
                .participantContextId("participantContextId")
                .configuration(configuration)
                .build();
    }
}
