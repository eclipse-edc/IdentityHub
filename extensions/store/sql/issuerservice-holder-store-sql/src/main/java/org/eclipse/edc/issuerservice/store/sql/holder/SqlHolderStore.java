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

package org.eclipse.edc.issuerservice.store.sql.holder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.issuerservice.spi.holder.model.Holder;
import org.eclipse.edc.issuerservice.spi.holder.store.HolderStore;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.store.AbstractSqlStore;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static java.util.Optional.ofNullable;
import static org.eclipse.edc.spi.result.StoreResult.alreadyExists;
import static org.eclipse.edc.spi.result.StoreResult.success;


/**
 * SQL-based {@link Holder} store intended for use with PostgreSQL
 */
public class SqlHolderStore extends AbstractSqlStore implements HolderStore {


    private static final TypeReference<List<String>> LIST_REF = new TypeReference<>() {
    };
    private final HolderStoreStatements statements;

    public SqlHolderStore(DataSourceRegistry dataSourceRegistry,
                          String dataSourceName,
                          TransactionContext transactionContext,
                          ObjectMapper objectMapper,
                          QueryExecutor queryExecutor,
                          HolderStoreStatements statements) {
        super(dataSourceRegistry, dataSourceName, transactionContext, objectMapper, queryExecutor);
        this.statements = statements;
    }

    @Override
    public StoreResult<Holder> findById(String id) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                return ofNullable(findByIdInternal(connection, id))
                        .map(StoreResult::success)
                        .orElseGet(() -> StoreResult.notFound(notFoundErrorMessage(id)));
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<Void> create(Holder holder) {
        var id = holder.getHolderId();
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                if (findByIdInternal(connection, id) != null) {
                    return alreadyExists(alreadyExistsErrorMessage(id));
                }

                var stmt = statements.getInsertTemplate();
                queryExecutor.execute(connection, stmt,
                        holder.getHolderId(),
                        holder.getParticipantContextId(),
                        holder.getDid(),
                        holder.getHolderName(),
                        0, //participant.createdAt(),
                        0  //participant.lastModifiedAt());
                );
                return success();

            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<Collection<Holder>> query(QuerySpec querySpec) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var query = statements.createQuery(querySpec);
                return success(queryExecutor.query(connection, true, this::mapResultSet, query.getQueryAsString(), query.getParameters()).toList());
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<Void> update(Holder holder) {
        var id = holder.getHolderId();

        Objects.requireNonNull(holder);
        Objects.requireNonNull(id);
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                if (findByIdInternal(connection, id) != null) {
                    queryExecutor.execute(connection,
                            statements.getUpdateTemplate(),
                            holder.getHolderId(),
                            holder.getDid(),
                            holder.getHolderName(),
                            0, //participant.createdAt(),
                            0, //participant.lastModifiedAt());
                            holder.getHolderId()
                    );
                    return StoreResult.success();
                }
                return StoreResult.notFound(notFoundErrorMessage(id));
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<Void> deleteById(String holderId) {
        Objects.requireNonNull(holderId);
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                if (findByIdInternal(connection, holderId) != null) {
                    var stmt = statements.getDeleteByIdTemplate();
                    queryExecutor.execute(connection, stmt, holderId);
                    return success();
                }
                return StoreResult.notFound(notFoundErrorMessage(holderId));
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    private Holder findByIdInternal(Connection connection, String id) {
        return transactionContext.execute(() -> {
            var stmt = statements.getFindByIdTemplate();
            return queryExecutor.single(connection, false, this::mapResultSet, stmt, id);
        });
    }

    private Holder mapResultSet(ResultSet resultSet) throws Exception {

        var id = resultSet.getString(statements.getIdColumn());
        var did = resultSet.getString(statements.getDidColumn());
        var name = resultSet.getString(statements.getHolderNameColumn());
        var participantContextId = resultSet.getString(statements.getParticipantContextIdColumn());
        var created = resultSet.getLong(statements.getCreateTimestampColumn());
        var lastmodified = resultSet.getLong(statements.getLastModifiedTimestampColumn());
        return Holder.Builder.newInstance()
                .holderId(id)
                .did(did)
                .holderName(name)
                .participantContextId(participantContextId)
                .build();
    }
}
