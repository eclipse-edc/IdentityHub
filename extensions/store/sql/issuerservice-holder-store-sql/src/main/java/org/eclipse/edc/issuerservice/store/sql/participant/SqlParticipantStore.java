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

package org.eclipse.edc.issuerservice.store.sql.participant;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.issuerservice.spi.participant.model.Participant;
import org.eclipse.edc.issuerservice.spi.participant.store.ParticipantStore;
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
 * SQL-based {@link Participant} store intended for use with PostgreSQL
 */
public class SqlParticipantStore extends AbstractSqlStore implements ParticipantStore {


    private static final TypeReference<List<String>> LIST_REF = new TypeReference<>() {
    };
    private final ParticipantStoreStatements statements;

    public SqlParticipantStore(DataSourceRegistry dataSourceRegistry,
                               String dataSourceName,
                               TransactionContext transactionContext,
                               ObjectMapper objectMapper,
                               QueryExecutor queryExecutor,
                               ParticipantStoreStatements statements) {
        super(dataSourceRegistry, dataSourceName, transactionContext, objectMapper, queryExecutor);
        this.statements = statements;
    }

    @Override
    public StoreResult<Participant> findById(String id) {
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
    public StoreResult<Void> create(Participant participant) {
        var id = participant.participantId();
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                if (findByIdInternal(connection, id) != null) {
                    return alreadyExists(alreadyExistsErrorMessage(id));
                }

                var stmt = statements.getInsertTemplate();
                queryExecutor.execute(connection, stmt,
                        participant.participantId(),
                        participant.did(),
                        participant.participantName(),
                        0, //participant.createdAt(),
                        0, //participant.lastModifiedAt());
                        toJson(participant.attestations())
                );
                return success();

            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<Collection<Participant>> query(QuerySpec querySpec) {
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
    public StoreResult<Void> update(Participant participant) {
        var id = participant.participantId();

        Objects.requireNonNull(participant);
        Objects.requireNonNull(id);
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                if (findByIdInternal(connection, id) != null) {
                    queryExecutor.execute(connection,
                            statements.getUpdateTemplate(),
                            participant.participantId(),
                            participant.did(),
                            participant.participantName(),
                            0, //participant.createdAt(),
                            0, //participant.lastModifiedAt());
                            toJson(participant.attestations()),
                            participant.participantId()
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
    public StoreResult<Void> deleteById(String id) {
        Objects.requireNonNull(id);
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                if (findByIdInternal(connection, id) != null) {
                    var stmt = statements.getDeleteByIdTemplate();
                    queryExecutor.execute(connection, stmt, id);
                    return success();
                }
                return StoreResult.notFound(notFoundErrorMessage(id));
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    private Participant findByIdInternal(Connection connection, String id) {
        return transactionContext.execute(() -> {
            var stmt = statements.getFindByIdTemplate();
            return queryExecutor.single(connection, false, this::mapResultSet, stmt, id);
        });
    }

    private Participant mapResultSet(ResultSet resultSet) throws Exception {

        var id = resultSet.getString(statements.getIdColumn());
        var did = resultSet.getString(statements.getDidColumn());
        var name = resultSet.getString(statements.getParticipantNameColumn());
        var created = resultSet.getLong(statements.getCreateTimestampColumn());
        var lastmodified = resultSet.getLong(statements.getLastModifiedTimestampColumn());
        var attestations = fromJson(resultSet.getString(statements.getAttestationsColumn()), LIST_REF);
        return new Participant(id, did, name, attestations);
    }
}
