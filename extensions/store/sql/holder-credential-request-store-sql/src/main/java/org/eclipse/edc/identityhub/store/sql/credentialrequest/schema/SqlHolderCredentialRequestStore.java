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

package org.eclipse.edc.identityhub.store.sql.credentialrequest.schema;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.identityhub.spi.credential.request.model.HolderCredentialRequest;
import org.eclipse.edc.identityhub.spi.credential.request.model.RequestedCredential;
import org.eclipse.edc.identityhub.spi.credential.request.store.HolderCredentialRequestStore;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.lease.SqlLeaseContextBuilder;
import org.eclipse.edc.sql.store.AbstractSqlStore;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;


/**
 * SQL-based {@link HolderCredentialRequest} store intended for use with PostgreSQL
 */
public class SqlHolderCredentialRequestStore extends AbstractSqlStore implements HolderCredentialRequestStore {

    private static final TypeReference<List<RequestedCredential>> LIST_REF = new TypeReference<>() {
    };
    private final String leaseHolderName;
    private final SqlLeaseContextBuilder leaseContext;
    private final HolderCredentialRequestStoreStatements statements;
    private final Clock clock;

    public SqlHolderCredentialRequestStore(DataSourceRegistry dataSourceRegistry,
                                           String dataSourceName,
                                           TransactionContext transactionContext,
                                           ObjectMapper objectMapper,
                                           QueryExecutor queryExecutor,
                                           HolderCredentialRequestStoreStatements statements,
                                           String leaseHolderName,
                                           Clock clock) {
        super(dataSourceRegistry, dataSourceName, transactionContext, objectMapper, queryExecutor);
        this.statements = statements;
        this.leaseHolderName = leaseHolderName;
        this.clock = clock;
        leaseContext = SqlLeaseContextBuilder.with(transactionContext, leaseHolderName, statements, clock, queryExecutor);
    }

    @Override
    public HolderCredentialRequest findById(String id) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                return findByIdInternal(connection, id);
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public @NotNull List<HolderCredentialRequest> nextNotLeased(int max, Criterion... criteria) {
        return transactionContext.execute(() -> {
            var filter = Arrays.stream(criteria).collect(toList());
            var querySpec = QuerySpec.Builder.newInstance().filter(filter).sortField("stateTimestamp").limit(max).build();
            var statement = statements.createQuery(querySpec)
                    .addWhereClause(statements.getNotLeasedFilter(), clock.millis());

            try (
                    var connection = getConnection();
                    var stream = queryExecutor.query(connection, true, this::mapResultSet, statement.getQueryAsString(), statement.getParameters())
            ) {
                var issuanceProcesses = stream.collect(Collectors.toList());
                issuanceProcesses.forEach(issuanceProcess -> leaseContext.withConnection(connection).acquireLease(issuanceProcess.getId()));
                return issuanceProcesses;
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<HolderCredentialRequest> findByIdAndLease(String id) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var entity = findByIdInternal(connection, id);
                if (entity == null) {
                    return StoreResult.notFound(format("HolderCredentialRequest %s not found", id));
                }

                leaseContext.withConnection(connection).acquireLease(entity.getId());
                return StoreResult.success(entity);
            } catch (IllegalStateException e) {
                return StoreResult.alreadyLeased(format("HolderCredentialRequest %s is already leased", id));
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public void save(HolderCredentialRequest issuanceProcess) {
        try (var conn = getConnection()) {
            var existing = findByIdInternal(conn, issuanceProcess.getId());
            if (existing != null) {
                leaseContext.by(leaseHolderName).withConnection(conn).breakLease(issuanceProcess.getId());
                update(conn, issuanceProcess);
            } else {
                insert(conn, issuanceProcess);
            }
        } catch (SQLException e) {
            throw new EdcPersistenceException(e);
        }
    }

    @Override
    public Collection<HolderCredentialRequest> query(QuerySpec querySpec) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var query = statements.createQuery(querySpec);
                return queryExecutor.query(connection, true, this::mapResultSet, query.getQueryAsString(), query.getParameters()).toList();
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    private void insert(Connection conn, HolderCredentialRequest process) {
        var insertTpStatement = statements.getInsertTemplate();
        queryExecutor.execute(conn, insertTpStatement, process.getId(),
                process.getState(),
                process.getStateCount(),
                process.getStateTimestamp(),
                process.getCreatedAt(),
                process.getUpdatedAt(),
                toJson(process.getTraceContext()),
                process.getErrorDetail(),
                process.getParticipantContextId(),
                process.getIssuerDid(),
                process.getIssuerPid(),
                toJson(process.getIdsAndFormats()));
    }

    private void update(Connection conn, HolderCredentialRequest process) {
        var updateStmt = statements.getUpdateTemplate();
        queryExecutor.execute(conn, updateStmt,
                process.getState(),
                process.getStateCount(),
                process.getStateTimestamp(),
                process.getUpdatedAt(),
                toJson(process.getTraceContext()),
                process.getErrorDetail(),
                process.getIssuerDid(),
                process.getIssuerPid(),
                toJson(process.getIdsAndFormats()),
                process.getId());

    }

    private HolderCredentialRequest findByIdInternal(Connection connection, String id) {
        return transactionContext.execute(() -> {
            var stmt = statements.getFindByIdTemplate();
            return queryExecutor.single(connection, false, this::mapResultSet, stmt, id);
        });
    }


    private HolderCredentialRequest mapResultSet(ResultSet resultSet) throws Exception {
        return HolderCredentialRequest.Builder.newInstance()
                .id(resultSet.getString(statements.getIdColumn()))
                .createdAt(resultSet.getLong(statements.getCreatedAtColumn()))
                .updatedAt(resultSet.getLong(statements.getUpdatedAtColumn()))
                .state(resultSet.getInt(statements.getStateColumn()))
                .stateTimestamp(resultSet.getLong(statements.getStateTimestampColumn()))
                .stateCount(resultSet.getInt(statements.getStateCountColumn()))
                .traceContext(fromJson(resultSet.getString(statements.getTraceContextColumn()), getTypeRef()))
                .errorDetail(resultSet.getString(statements.getErrorDetailColumn()))
                .requestedCredentials(fromJson(resultSet.getString(statements.getCredentialFormatsColumn()), LIST_REF))
                .issuerDid(resultSet.getString(statements.getIssuerDidColumn()))
                .issuerPid(resultSet.getString(statements.getissuerPidColumn()))
                .participantContextId(resultSet.getString(statements.getParticipantIdColumn()))
                .build();
    }
}
