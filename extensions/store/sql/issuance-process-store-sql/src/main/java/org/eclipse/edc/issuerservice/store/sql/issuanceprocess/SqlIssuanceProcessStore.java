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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcess;
import org.eclipse.edc.issuerservice.spi.issuance.process.store.IssuanceProcessStore;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
    private final String leaseHolderName;
    private final SqlLeaseContextBuilder leaseContext;

    private final IssuanceProcessStoreStatements statements;
    private final Clock clock;

    public SqlIssuanceProcessStore(DataSourceRegistry dataSourceRegistry,
                                   String dataSourceName,
                                   TransactionContext transactionContext,
                                   ObjectMapper objectMapper,
                                   QueryExecutor queryExecutor,
                                   IssuanceProcessStoreStatements statements,
                                   String leaseHolderName,
                                   Clock clock) {
        super(dataSourceRegistry, dataSourceName, transactionContext, objectMapper, queryExecutor);
        this.statements = statements;
        this.leaseHolderName = leaseHolderName;
        this.clock = clock;
        leaseContext = SqlLeaseContextBuilder.with(transactionContext, leaseHolderName, statements, clock, queryExecutor);
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
    public StoreResult<IssuanceProcess> findByIdAndLease(String id) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var entity = findByIdInternal(connection, id);
                if (entity == null) {
                    return StoreResult.notFound(format("IssuanceProcess %s not found", id));
                }

                leaseContext.withConnection(connection).acquireLease(entity.getId());
                return StoreResult.success(entity);
            } catch (IllegalStateException e) {
                return StoreResult.alreadyLeased(format("IssuanceProcess %s is already leased", id));
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public void save(IssuanceProcess issuanceProcess) {
        transactionContext.execute(() -> {
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

    private void insert(Connection conn, IssuanceProcess process) {
        var insertTpStatement = statements.getInsertTemplate();
        queryExecutor.execute(conn, insertTpStatement, process.getId(),
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
