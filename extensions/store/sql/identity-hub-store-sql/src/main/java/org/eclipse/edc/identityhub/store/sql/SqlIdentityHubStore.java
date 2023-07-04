/*
 *  Copyright (c) 2020 - 2023 Amadeus
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

package org.eclipse.edc.identityhub.store.sql;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.identityhub.store.spi.IdentityHubRecord;
import org.eclipse.edc.identityhub.store.spi.IdentityHubStore;
import org.eclipse.edc.identityhub.store.sql.schema.IdentityHubStatements;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.sql.store.AbstractSqlStore;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.stream.Stream;

import static org.eclipse.edc.sql.SqlQueryExecutor.executeQuery;

/**
 * SQL implementation for {@link IdentityHubStore}.
 */
public class SqlIdentityHubStore extends AbstractSqlStore implements IdentityHubStore {
    private final IdentityHubStatements statements;

    public SqlIdentityHubStore(DataSourceRegistry dataSourceRegistry,
                               String dataSourceName,
                               TransactionContext transactionContext,
                               IdentityHubStatements identityHubStatements, ObjectMapper mapper) {
        super(dataSourceRegistry, dataSourceName, transactionContext, mapper);
        statements = Objects.requireNonNull(identityHubStatements);
    }

    @Override
    public @NotNull Stream<IdentityHubRecord> getAll() {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                return executeQuery(connection, true, this::parse, statements.getFindAllTemplate());
            } catch (Exception e) {
                throw new EdcPersistenceException(e.getMessage(), e);
            }
        });
    }

    @Override
    public void add(IdentityHubRecord record) {
        transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var payload = new String(record.getPayload());
                executeQuery(connection, statements.getInsertTemplate(), record.getId(), payload, record.getPayloadFormat(), record.getCreatedAt());
            } catch (Exception e) {
                throw new EdcPersistenceException(e.getMessage(), e);
            }
        });
    }

    IdentityHubRecord parse(ResultSet resultSet) throws SQLException {
        return IdentityHubRecord.Builder.newInstance()
                .id(resultSet.getString(statements.getIdColumn()))
                .payloadFormat(resultSet.getString(statements.getPayloadFormatColumn()))
                .payload(resultSet.getString(statements.getPayloadColumn()).getBytes())
                .createdAt(resultSet.getLong(statements.getCreatedAtColumn()))
                .build();
    }
}
