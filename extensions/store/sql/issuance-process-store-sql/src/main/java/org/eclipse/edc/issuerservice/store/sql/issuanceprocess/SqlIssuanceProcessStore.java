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
import org.eclipse.edc.sql.lease.StatefulEntityWriteCheck;
import org.eclipse.edc.sql.store.AbstractSqlStore;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.NotNull;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
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

    public static final Duration DEFAULT_LEASE_DURATION = Duration.ofSeconds(60);

    private static final TypeReference<List<String>> ATTESTATIONS_LIST_REF = new TypeReference<>() {
    };

    private static final TypeReference<Map<String, CredentialFormat>> CREDENTIAL_FORMATS_REF = new TypeReference<>() {
    };

    private final IssuanceProcessStoreStatements statements;
    private final String leaseHolder;
    private final Clock clock;
    private final Duration leaseDuration;
    private final StatefulEntityWriteCheck writeCheck;

    public SqlIssuanceProcessStore(DataSourceRegistry dataSourceRegistry,
                                   String dataSourceName,
                                   TransactionContext transactionContext,
                                   Supplier<ObjectMapper> objectMapperSupplier,
                                   QueryExecutor queryExecutor,
                                   IssuanceProcessStoreStatements statements,
                                   String leaseHolder,
                                   Clock clock) {
        this(dataSourceRegistry, dataSourceName, transactionContext, objectMapperSupplier, queryExecutor, statements, leaseHolder, clock, DEFAULT_LEASE_DURATION);
    }

    public SqlIssuanceProcessStore(DataSourceRegistry dataSourceRegistry,
                                   String dataSourceName,
                                   TransactionContext transactionContext,
                                   Supplier<ObjectMapper> objectMapperSupplier,
                                   QueryExecutor queryExecutor,
                                   IssuanceProcessStoreStatements statements,
                                   String leaseHolder,
                                   Clock clock,
                                   Duration leaseDuration) {
        super(dataSourceRegistry, dataSourceName, transactionContext, objectMapperSupplier, queryExecutor);
        this.statements = statements;
        this.leaseHolder = leaseHolder;
        this.clock = clock;
        this.leaseDuration = leaseDuration;
        this.writeCheck = new StatefulEntityWriteCheck(queryExecutor, statements, statements.getIssuanceProcessTable(), leaseHolder, clock);
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
            try (var connection = getConnection();
                 var stream = queryExecutor.query(connection, false, this::mapResultSet, statement.getQueryAsString(), statement.getParameters())) {
                // rows are locked until the end of the transaction, so the lease can be safely acquired on them
                return stream.toList().stream()
                        .filter(process -> acquireLease(connection, process.getId()))
                        .collect(toList());
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<IssuanceProcess> findByIdAndLease(String id) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                // the lease is acquired before reading, so that the returned entity cannot be outdated by a concurrent save
                if (!acquireLease(connection, id)) {
                    if (findByIdInternal(connection, id) == null) {
                        return StoreResult.notFound(format("IssuanceProcess %s not found", id));
                    }
                    return StoreResult.alreadyLeased(format("IssuanceProcess %s is currently leased!", id));
                }
                return StoreResult.success(findByIdInternal(connection, id));
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<Void> save(IssuanceProcess issuanceProcess) {
        return transactionContext.execute(() -> {
            try (var conn = getConnection()) {
                var result = issuanceProcess.getVersion() == 0 ? insert(conn, issuanceProcess) : update(conn, issuanceProcess);
                if (result.succeeded()) {
                    issuanceProcess.setVersion(issuanceProcess.getVersion() + 1);
                }
                return result;
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<Void> breakLease(IssuanceProcess entity) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var updated = queryExecutor.execute(connection, statements.getBreakLeaseTemplate(statements.getIssuanceProcessTable()),
                        entity.getId(), leaseHolder, clock.millis());
                if (updated == 0 && findByIdInternal(connection, entity.getId()) != null) {
                    return StoreResult.alreadyLeased(format("IssuanceProcess %s is leased by another holder, cannot break the lease!", entity.getId()));
                }
                return StoreResult.success();
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

    /**
     * Inserts a process that was never stored. It is not inserted if a process with the same ID or holderPid exists: unlike
     * a failed statement, this does not abort the surrounding transaction.
     */
    private StoreResult<Void> insert(Connection conn, IssuanceProcess process) {
        var inserted = queryExecutor.execute(conn, statements.getInsertTemplate(), process.getId(),
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
                toJson(process.getCredentialFormats()),
                process.getVersion() + 1
        );
        if (inserted == 0) {
            if (findByIdInternal(conn, process.getId()) != null) {
                return StoreResult.conflict(format("IssuanceProcess %s already exists", process.getId()));
            }
            return StoreResult.alreadyExists(holderPidConflictMessage(process));
        }
        return StoreResult.success();
    }

    /**
     * Updates the process, unless someone else saved it since it was read, e.g. another runtime that leased it after the
     * lease of this one expired, or unless someone else holds its lease. Saving the process breaks its lease.
     */
    private StoreResult<Void> update(Connection conn, IssuanceProcess process) {
        var updated = queryExecutor.execute(conn, statements.getUpdateTemplate(),
                process.getState(),
                process.getStateCount(),
                process.getStateTimestamp(),
                process.getUpdatedAt(),
                toJson(process.getTraceContext()),
                process.getErrorDetail(),
                toJson(process.getClaims()),
                toJson(process.getCredentialDefinitions()),
                toJson(process.getCredentialFormats()),
                null, null, null,
                process.getVersion() + 1,
                process.getId(),
                process.getVersion(),
                leaseHolder,
                clock.millis());
        if (updated == 0) {
            return writeCheck.explainNotWritten(conn, process.getId(), process.getVersion());
        }
        return StoreResult.success();
    }

    private boolean acquireLease(Connection connection, String id) {
        var now = clock.millis();
        var sql = statements.getAcquireLeaseTemplate(statements.getIssuanceProcessTable());
        return queryExecutor.execute(connection, sql, leaseHolder, now, leaseDuration.toMillis(), id, now) > 0;
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
                .version(resultSet.getLong(statements.getVersionColumn()))
                .build();
    }
}
