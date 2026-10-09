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

package org.eclipse.edc.issuerservice.store.sql.issuanceprocess;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcess;
import org.eclipse.edc.issuerservice.spi.issuance.process.store.IssuanceProcessStore;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.lease.spi.SqlLeaseContextBuilder;
import org.eclipse.edc.sql.store.AbstractSqlStore;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.NotNull;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;


/**
 * SQL-based {@link IssuanceProcess} store intended for use with PostgreSQL
 */
public class SqlIssuanceProcessStore extends AbstractSqlStore implements IssuanceProcessStore {

    private static final TypeReference<List<String>> ATTESTATIONS_LIST_REF = new TypeReference<>() {
    };

    private static final TypeReference<Map<String, CredentialFormat>> CREDENTIAL_FORMATS_REF = new TypeReference<>() {
    };
    private final SqlLeaseContextBuilder leaseContext;

    private final IssuanceProcessStoreStatements statements;

    public SqlIssuanceProcessStore(DataSourceRegistry dataSourceRegistry,
                                   String dataSourceName,
                                   TransactionContext transactionContext,
                                   Supplier<ObjectMapper> objectMapperSupplier,
                                   QueryExecutor queryExecutor,
                                   IssuanceProcessStoreStatements statements,
                                   SqlLeaseContextBuilder leaseContext) {
        super(dataSourceRegistry, dataSourceName, transactionContext, objectMapperSupplier, queryExecutor);
        this.statements = statements;
        this.leaseContext = leaseContext;
    }

    @Override
    public IssuanceProcess findById(String id) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                return findByIdInternal(connection, id);
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public @NotNull List<IssuanceProcess> nextNotLeased(int max, Criterion... criteria) {
        return transactionContext.execute(() -> {
            var filter = Arrays.stream(criteria).collect(toList());
            var querySpec = QuerySpec.Builder.newInstance().filter(filter).sortField("stateTimestamp").limit(max).build();
            var statement = statements.createNextNotLeaseQuery(querySpec);
            try (var connection = getConnection()) {
                List<String> leasedIds;
                try (var stream = queryExecutor.query(connection, false, this::mapResultSet, statement.getQueryAsString(), statement.getParameters())) {
                    leasedIds = stream.map(IssuanceProcess::getId)
                            .filter(id -> leaseContext.withConnection(connection).acquireLease(id).succeeded())
                            .toList();
                }
                return readLeased(connection, leasedIds, filter);
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<IssuanceProcess> findByIdAndLease(String id) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                if (findByIdInternal(connection, id) == null) {
                    return StoreResult.notFound(format("IssuanceProcess %s not found", id));
                }

                var leaseResult = leaseContext.withConnection(connection).acquireLease(id);
                if (leaseResult.failed()) {
                    return leaseResult.mapFailure();
                }
                // acquiring the lease waits for another transaction that holds a lease on the entity, which may have changed
                // it in the meantime. Every statement reads what was committed when it starts, so the entity is read again.
                var entity = findByIdInternal(connection, id);
                if (entity == null) {
                    leaseContext.withConnection(connection).breakLease(id);
                    return StoreResult.notFound(format("IssuanceProcess %s not found", id));
                }
                return StoreResult.success(entity);
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<Void> save(IssuanceProcess issuanceProcess) {
        return transactionContext.execute(() -> {
            try (var conn = getConnection()) {
                var existing = findByIdInternal(conn, issuanceProcess.getId());
                if (existing != null) {
                    var result = leaseContext.withConnection(conn).breakLease(issuanceProcess.getId());
                    if (result.failed()) {
                        return result;
                    }
                    update(conn, issuanceProcess);
                    return StoreResult.success();
                }
                return insert(conn, issuanceProcess);
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<Void> breakLease(IssuanceProcess entity) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                return leaseContext.withConnection(connection).breakLease(entity.getId());
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public Stream<IssuanceProcess> query(QuerySpec querySpec) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var query = statements.createQuery(querySpec);
                return queryExecutor.query(connection, true, this::mapResultSet, query.getQueryAsString(), query.getParameters());
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    private StoreResult<Void> insert(Connection conn, IssuanceProcess process) {
        var insertTpStatement = statements.getInsertTemplate();
        var inserted = queryExecutor.execute(conn, insertTpStatement, process.getId(),
                process.getState(),
                process.getStateCount(),
                process.getStateTimestamp(),
                process.getCreatedAt(),
                process.getUpdatedAt(),
                toJson(process.getTraceContext()),
                process.getErrorDetail(),
                process.getHolderId(),
                process.getParticipantContextId(),
                process.getHolderPid(),
                toJson(process.getClaims()),
                toJson(process.getCredentialDefinitions()),
                toJson(process.getCredentialFormats())
        );
        return inserted > 0
                ? StoreResult.success()
                : StoreResult.alreadyExists(holderPidConflictMessage(process));
    }

    private void update(Connection conn, IssuanceProcess process) {
        var updateStmt = statements.getUpdateTemplate();
        queryExecutor.execute(conn, updateStmt,
                process.getState(),
                process.getStateCount(),
                process.getStateTimestamp(),
                process.getUpdatedAt(),
                toJson(process.getTraceContext()),
                process.getErrorDetail(),
                toJson(process.getClaims()),
                toJson(process.getCredentialDefinitions()),
                toJson(process.getCredentialFormats()),
                process.getId());

    }

    /**
     * Reads entities again after their leases were acquired: acquiring a lease waits for another transaction that holds a
     * lease on the same entity, which may have changed it in the meantime. The leases of entities that no longer match the
     * criteria are broken again.
     */
    private List<IssuanceProcess> readLeased(Connection connection, List<String> leasedIds, List<Criterion> criteria) {
        if (leasedIds.isEmpty()) {
            return List.of();
        }
        var filter = new ArrayList<>(criteria);
        filter.add(new Criterion("id", "in", leasedIds));
        var querySpec = QuerySpec.Builder.newInstance().filter(filter).sortField("stateTimestamp").limit(leasedIds.size()).build();
        var statement = statements.createQuery(querySpec);
        try (var stream = queryExecutor.query(connection, false, this::mapResultSet, statement.getQueryAsString(), statement.getParameters())) {
            var entities = stream.toList();
            leasedIds.stream()
                    .filter(id -> entities.stream().noneMatch(entity -> entity.getId().equals(id)))
                    .forEach(id -> leaseContext.withConnection(connection).breakLease(id));
            return entities;
        }
    }

    private IssuanceProcess findByIdInternal(Connection connection, String id) {
        return transactionContext.execute(() -> {
            var stmt = statements.getFindByIdTemplate();
            return queryExecutor.single(connection, false, this::mapResultSet, stmt, id);
        });
    }


    private IssuanceProcess mapResultSet(ResultSet resultSet) throws Exception {
        return IssuanceProcess.Builder.newInstance()
                .id(resultSet.getString(statements.getIdColumn()))
                .createdAt(resultSet.getLong(statements.getCreatedAtColumn()))
                .updatedAt(resultSet.getLong(statements.getUpdatedAtColumn()))
                .state(resultSet.getInt(statements.getStateColumn()))
                .stateTimestamp(resultSet.getLong(statements.getStateTimestampColumn()))
                .stateCount(resultSet.getInt(statements.getStateCountColumn()))
                .traceContext(fromJson(resultSet.getString(statements.getTraceContextColumn()), getTypeRef()))
                .errorDetail(resultSet.getString(statements.getErrorDetailColumn()))
                .holderId(resultSet.getString(statements.getHolderIdColumn()))
                .participantContextId(resultSet.getString(statements.getParticipantContextIdColumn()))
                .holderPid(resultSet.getString(statements.getHolderPidColumn()))
                .claims(fromJson(resultSet.getString(statements.getClaimsColumn()), getTypeRef()))
                .credentialDefinitions(fromJson(resultSet.getString(statements.getCredentialDefinitionsColumn()), ATTESTATIONS_LIST_REF))
                .credentialFormats(fromJson(resultSet.getString(statements.getCredentialFormatsColumn()), CREDENTIAL_FORMATS_REF))
                .build();
    }
}
