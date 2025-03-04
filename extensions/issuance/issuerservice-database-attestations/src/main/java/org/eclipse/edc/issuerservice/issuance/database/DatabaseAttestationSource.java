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

package org.eclipse.edc.issuerservice.issuance.database;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationContext;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationSource;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.store.AbstractSqlStore;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Resolves an attestation from a Postgres database. The resulting input claims map is resolved by executing a {@code SELECT} statement
 * against the database. Each column name in the result set is a key in the claims map.
 */
public class DatabaseAttestationSource extends AbstractSqlStore implements AttestationSource {
    public static final String DATASOURCE_NAME = "dataSourceName";
    public static final String TABLE_NAME = "tableName";
    public static final String REQUIRED = "required";
    public static final String ID_COLUMN = "idColumn";
    private final boolean required;
    private final TransactionContext transactionContext;
    private final QueryExecutor queryExecutor;
    private final String tableName;
    private final String idColumn;

    /**
     * Instantiate a new {@link DatabaseAttestationSource}
     *
     * @param dataSourceName     The name of the datasource. Must be configured using a {@link org.eclipse.edc.spi.system.configuration.Config}
     * @param required           Whether this attestation is mandatory.
     * @param objectMapper       Currently not needed. Simply pass {@code new ObjectMapper()}
     * @param tableName          The name of the table that contains the attestations.
     * @param dataSourceRegistry The datasource registry, that contains configuration for the data source
     * @param queryExecutor      A {@link QueryExecutor}
     * @param transactionContext A {@link TransactionContext}
     * @param idColumn           The column name of the column that contains the participant context ID.
     */
    public DatabaseAttestationSource(String dataSourceName, boolean required, ObjectMapper objectMapper, String tableName, DataSourceRegistry dataSourceRegistry, QueryExecutor queryExecutor, TransactionContext transactionContext, String idColumn) {
        super(dataSourceRegistry, dataSourceName, transactionContext, objectMapper, queryExecutor);
        this.required = required;
        this.transactionContext = transactionContext;
        this.queryExecutor = queryExecutor;
        this.tableName = tableName;
        this.idColumn = idColumn;
    }

    @Override
    public Result<Map<String, Object>> execute(AttestationContext context) {
        var participantId = context.participantId();
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {

                var query = "SELECT * FROM %s where %s = ?".formatted(tableName, idColumn);
                return Result.success(queryExecutor.single(connection, true, this::mapGenericResult, query, participantId));

            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public boolean isRequired() {
        return required;
    }

    private Map<String, Object> mapGenericResult(ResultSet resultSet) {
        try {
            var map = new HashMap<String, Object>();
            var metaData = resultSet.getMetaData();
            var cols = metaData.getColumnCount();
            for (int i = 1; i <= cols; i++) {
                var columnName = metaData.getColumnName(i);
                var value = resultSet.getString(columnName);
                map.put(columnName, value);
            }

            return map;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
