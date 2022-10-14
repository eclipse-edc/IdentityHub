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

import org.eclipse.dataspaceconnector.identityhub.store.spi.IdentityHubStore;
import org.eclipse.dataspaceconnector.identityhub.store.sql.schema.IdentityHubStatements;
import org.eclipse.dataspaceconnector.spi.persistence.EdcPersistenceException;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.datasource.DataSourceRegistry;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.sql.SqlQueryExecutor.executeQuery;

/**
 * SQL implementation for {@link org.eclipse.dataspaceconnector.identityhub.store.spi.IdentityHubStore}.
 */
public class SqlIdentityHubStore implements IdentityHubStore {

    private final DataSourceRegistry dataSourceRegistry;
    private final String dataSourceName;
    private final TransactionContext transactionContext;
    private final IdentityHubStatements identityHubStatements;

    public SqlIdentityHubStore(DataSourceRegistry dataSourceRegistry,
                               String dataSourceName,
                               TransactionContext transactionContext,
                               IdentityHubStatements identityHubStatements) {
        this.dataSourceRegistry = Objects.requireNonNull(dataSourceRegistry);
        this.dataSourceName = Objects.requireNonNull(dataSourceName);
        this.transactionContext = Objects.requireNonNull(transactionContext);
        this.identityHubStatements = Objects.requireNonNull(identityHubStatements);
    }

    @Override
    public Collection<byte[]> getAll() {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                try (var stream = executeQuery(connection, true, this::parse, identityHubStatements.getSelectAllItemsTemplate())) {
                    return stream.collect(Collectors.toList());
                }
            } catch (EdcPersistenceException e) {
                throw e;
            } catch (Exception e) {
                throw new EdcPersistenceException(e.getMessage(), e);
            }
        });
    }

    @Override
    public void add(byte[] hubObject) {
        transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                executeQuery(connection, identityHubStatements.getInsertItemTemplate(), new String(hubObject));
            } catch (EdcPersistenceException e) {
                throw e;
            } catch (Exception e) {
                throw new EdcPersistenceException(e.getMessage(), e);
            }
        });
    }

    private byte[] parse(ResultSet resultSet) throws SQLException {
        return resultSet.getString(identityHubStatements.getItemColumn()).getBytes();
    }

    private DataSource getDataSource() {
        return Objects.requireNonNull(dataSourceRegistry.resolve(dataSourceName), format("DataSource %s could not be resolved", dataSourceName));
    }

    private Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }
}
