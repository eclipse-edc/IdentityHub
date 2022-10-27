/*
 *  Copyright (c) 2020 - 2022 Amadeus
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

package org.eclipse.dataspaceconnector.identityhub.store.sql;

import org.eclipse.dataspaceconnector.identityhub.store.spi.IdentityHubRecord;
import org.eclipse.dataspaceconnector.identityhub.store.spi.IdentityHubStore;
import org.eclipse.dataspaceconnector.identityhub.store.sql.schema.IdentityHubStatements;
import org.eclipse.dataspaceconnector.spi.persistence.EdcPersistenceException;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.datasource.DataSourceRegistry;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.stream.Stream;
import javax.sql.DataSource;

import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.sql.SqlQueryExecutor.executeQuery;

/**
 * SQL implementation for {@link org.eclipse.dataspaceconnector.identityhub.store.spi.IdentityHubStore}.
 */
public class SqlIdentityHubStore implements IdentityHubStore {

    private final DataSourceRegistry dataSourceRegistry;
    private final String dataSourceName;
    private final TransactionContext transactionContext;
    private final IdentityHubStatements statements;

    public SqlIdentityHubStore(DataSourceRegistry dataSourceRegistry,
                               String dataSourceName,
                               TransactionContext transactionContext,
                               IdentityHubStatements identityHubStatements) {
        this.dataSourceRegistry = Objects.requireNonNull(dataSourceRegistry);
        this.dataSourceName = Objects.requireNonNull(dataSourceName);
        this.transactionContext = Objects.requireNonNull(transactionContext);
        this.statements = Objects.requireNonNull(identityHubStatements);
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
                executeQuery(connection, statements.getInsertTemplate(), record.getId(), payload, record.getCreatedAt());
            } catch (Exception e) {
                throw new EdcPersistenceException(e.getMessage(), e);
            }
        });
    }

    private IdentityHubRecord parse(ResultSet resultSet) throws SQLException {
        return IdentityHubRecord.Builder.newInstance()
                .id(resultSet.getString(statements.getIdColumn()))
                .payload(resultSet.getString(statements.getPayloadColumn()).getBytes())
                .createdAt(resultSet.getLong(statements.getCreatedAtColumn()))
                .build();
    }

    private DataSource getDataSource() {
        return Objects.requireNonNull(dataSourceRegistry.resolve(dataSourceName), format("DataSource %s could not be resolved", dataSourceName));
    }

    private Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }
}
