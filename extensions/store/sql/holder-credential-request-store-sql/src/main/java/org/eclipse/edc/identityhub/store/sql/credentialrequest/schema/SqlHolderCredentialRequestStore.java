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

import org.eclipse.edc.identityhub.spi.credential.request.model.HolderCredentialRequest;
import org.eclipse.edc.identityhub.spi.credential.request.model.RequestedCredential;
import org.eclipse.edc.identityhub.spi.credential.request.store.HolderCredentialRequestStore;
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
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;


/**
 * SQL-based {@link HolderCredentialRequest} store intended for use with PostgreSQL
 */
public class SqlHolderCredentialRequestStore extends AbstractSqlStore implements HolderCredentialRequestStore {

    public static final Duration DEFAULT_LEASE_DURATION = Duration.ofSeconds(60);

    private static final TypeReference<List<RequestedCredential>> LIST_REF = new TypeReference<>() {
    };
    private final HolderCredentialRequestStoreStatements statements;
    private final String leaseHolder;
    private final Clock clock;
    private final Duration leaseDuration;
    private final StatefulEntityWriteCheck writeCheck;

    public SqlHolderCredentialRequestStore(DataSourceRegistry dataSourceRegistry,
                                           String dataSourceName,
                                           TransactionContext transactionContext,
                                           Supplier<ObjectMapper> objectMapperSupplier,
                                           QueryExecutor queryExecutor,
                                           HolderCredentialRequestStoreStatements statements,
                                           String leaseHolder,
                                           Clock clock) {
        this(dataSourceRegistry, dataSourceName, transactionContext, objectMapperSupplier, queryExecutor, statements, leaseHolder, clock, DEFAULT_LEASE_DURATION);
    }

    public SqlHolderCredentialRequestStore(DataSourceRegistry dataSourceRegistry,
                                           String dataSourceName,
                                           TransactionContext transactionContext,
                                           Supplier<ObjectMapper> objectMapperSupplier,
                                           QueryExecutor queryExecutor,
                                           HolderCredentialRequestStoreStatements statements,
                                           String leaseHolder,
                                           Clock clock,
                                           Duration leaseDuration) {
        super(dataSourceRegistry, dataSourceName, transactionContext, objectMapperSupplier, queryExecutor);
        this.statements = statements;
        this.leaseHolder = leaseHolder;
        this.clock = clock;
        this.leaseDuration = leaseDuration;
        this.writeCheck = new StatefulEntityWriteCheck(queryExecutor, statements, statements.getHolderCredentialRequestTable(), leaseHolder, clock);
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
            var statement = statements.createNextNotLeaseQuery(querySpec);
            try (var connection = getConnection();
                 var stream = queryExecutor.query(connection, false, this::mapResultSet, statement.getQueryAsString(), statement.getParameters())) {
                // rows are locked until the end of the transaction, so the lease can be safely acquired on them
                return stream.toList().stream()
                        .filter(request -> acquireLease(connection, request.getId()))
                        .collect(toList());
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<HolderCredentialRequest> findByIdAndLease(String id) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                // the lease is acquired before reading, so that the returned entity cannot be outdated by a concurrent save
                if (!acquireLease(connection, id)) {
                    if (findByIdInternal(connection, id) == null) {
                        return StoreResult.notFound(format("HolderCredentialRequest %s not found", id));
                    }
                    return StoreResult.alreadyLeased(format("HolderCredentialRequest %s is currently leased!", id));
                }
                return StoreResult.success(findByIdInternal(connection, id));
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<Void> save(HolderCredentialRequest request) {
        return transactionContext.execute(() -> {
            try (var conn = getConnection()) {
                var result = request.getVersion() == 0 ? insert(conn, request) : update(conn, request);
                if (result.succeeded()) {
                    request.setVersion(request.getVersion() + 1);
                }
                return result;
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<Void> breakLease(HolderCredentialRequest entity) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var updated = queryExecutor.execute(connection, statements.getBreakLeaseTemplate(statements.getHolderCredentialRequestTable()),
                        entity.getId(), leaseHolder, clock.millis());
                if (updated == 0 && findByIdInternal(connection, entity.getId()) != null) {
                    return StoreResult.alreadyLeased(format("HolderCredentialRequest %s is leased by another holder, cannot break the lease!", entity.getId()));
                }
                return StoreResult.success();
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
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

    /**
     * Inserts a request that was never stored. It is not inserted if a request with the same ID exists: unlike a failed
     * statement, this does not abort the surrounding transaction.
     */
    private StoreResult<Void> insert(Connection conn, HolderCredentialRequest process) {
        var insertTpStatement = statements.getInsertTemplate();
        var inserted = queryExecutor.execute(conn, insertTpStatement, process.getId(),
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
                toJson(process.getIdsAndFormats()),
                process.getVersion() + 1);
        if (inserted == 0) {
            return StoreResult.conflict("A HolderCredentialRequest with ID '%s' already exists.".formatted(process.getId()));
        }
        return StoreResult.success();
    }

    /**
     * Updates the request, unless someone else saved it since it was read, e.g. another runtime that leased it after the
     * lease of this one expired, or unless someone else holds its lease. Saving the request breaks its lease.
     */
    private StoreResult<Void> update(Connection conn, HolderCredentialRequest process) {
        var updateStmt = statements.getUpdateTemplate();
        var updated = queryExecutor.execute(conn, updateStmt,
                process.getState(),
                process.getStateCount(),
                process.getStateTimestamp(),
                process.getUpdatedAt(),
                toJson(process.getTraceContext()),
                process.getErrorDetail(),
                process.getIssuerDid(),
                process.getIssuerPid(),
                toJson(process.getIdsAndFormats()),
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

    @Override
    public StoreResult<Void> deleteById(String id) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                if (findByIdInternal(connection, id) == null) {
                    return StoreResult.notFound(format("HolderCredentialRequest '%s' not found", id));
                }
                if (!acquireLease(connection, id)) {
                    return StoreResult.alreadyLeased(format("HolderCredentialRequest %s is currently leased!", id));
                }
                queryExecutor.execute(connection, statements.getDeleteByIdTemplate(), id);
                return StoreResult.success();
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    private boolean acquireLease(Connection connection, String id) {
        var now = clock.millis();
        var sql = statements.getAcquireLeaseTemplate(statements.getHolderCredentialRequestTable());
        return queryExecutor.execute(connection, sql, leaseHolder, now, leaseDuration.toMillis(), id, now) > 0;
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
                .participantContextId(resultSet.getString(statements.getParticipantContextIdColumn()))
                .version(resultSet.getLong(statements.getVersionColumn()))
                .build();
    }
}
