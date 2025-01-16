/*
 *  Copyright (c) 2023 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.did.store.sql;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.identityhub.spi.did.model.DidResource;
import org.eclipse.edc.identityhub.spi.did.store.DidResourceStore;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.store.AbstractSqlStore;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Objects;


/**
 * SQL-based {@link DidResourceStore} intended for use with PostgreSQLs
 */
public class SqlDidResourceStore extends AbstractSqlStore implements DidResourceStore {

    private final DidResourceStatements statements;

    public SqlDidResourceStore(DataSourceRegistry dataSourceRegistry, String dataSourceName, TransactionContext transactionContext,
                               ObjectMapper objectMapper, QueryExecutor queryExecutor, DidResourceStatements statements) {
        super(dataSourceRegistry, dataSourceName, transactionContext, objectMapper, queryExecutor);
        this.statements = statements;
    }


    @Override
    public StoreResult<Void> save(DidResource resource) {
        var did = resource.getDid();
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                if (findById(did) != null) {
                    return StoreResult.alreadyExists(alreadyExistsErrorMessage(did));
                }

                var stmt = statements.getInsertTemplate();
                queryExecutor.execute(connection, stmt,
                        did,
                        resource.getState(),
                        resource.getCreateTimestamp(),
                        resource.getStateTimestamp(),
                        toJson(resource.getDocument()),
                        resource.getParticipantContextId());
                return StoreResult.success();
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<Void> update(DidResource resource) {
        var did = resource.getDid();
        Objects.requireNonNull(resource);
        Objects.requireNonNull(did);
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                if (findById(did) != null) {
                    queryExecutor.execute(connection, statements.getUpdateTemplate(),
                            did,
                            resource.getState(),
                            resource.getCreateTimestamp(),
                            resource.getStateTimestamp(),
                            toJson(resource.getDocument()),
                            resource.getParticipantContextId(),
                            did);
                    return StoreResult.success();
                }
                return StoreResult.notFound(notFoundErrorMessage(did));
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public DidResource findById(String did) {
        Objects.requireNonNull(did);
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var sql = statements.getFindByIdTemplate();
                return queryExecutor.single(connection, false, this::mapResultSet, sql, did);
            } catch (Exception exception) {
                throw new EdcPersistenceException(exception);
            }
        });
    }

    @Override
    public Collection<DidResource> query(QuerySpec query) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var sql = statements.createQuery(query);
                return queryExecutor.query(connection, true, this::mapResultSet, sql.getQueryAsString(), sql.getParameters()).toList();
            } catch (Exception exception) {
                throw new EdcPersistenceException(exception);
            }
        });
    }

    @Override
    public StoreResult<Void> deleteById(String did) {
        Objects.requireNonNull(did);
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                if (findById(did) != null) {
                    var stmt = statements.getDeleteByIdTemplate();
                    queryExecutor.execute(connection, stmt, did);
                    return StoreResult.success();
                }
                return StoreResult.notFound(notFoundErrorMessage(did));
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    private DidResource mapResultSet(ResultSet resultSet) throws Exception {
        return DidResource.Builder.newInstance()
                .did(resultSet.getString(statements.getIdColumn()))
                .createTimestamp(resultSet.getLong(statements.getCreateTimestampColumn()))
                .stateTimeStamp(resultSet.getLong(statements.getStateTimestampColumn()))
                .document(fromJson(resultSet.getString(statements.getDidDocumentColumn()), DidDocument.class))
                .state(resultSet.getInt(statements.getStateColumn()))
                .participantContextId(resultSet.getString(statements.getParticipantContextId()))
                .build();
    }
}
