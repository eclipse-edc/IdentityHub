/*
 *  Copyright (c) 2025 Metaform Systems Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.store.sql.credentialoffer.schema;

import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialObject;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialOffer;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialOfferStore;
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
 * SQL-based {@link CredentialOffer} store intended for use with PostgreSQL
 */
public class SqlCredentialOfferStore extends AbstractSqlStore implements CredentialOfferStore {

    public static final Duration DEFAULT_LEASE_DURATION = Duration.ofSeconds(60);

    private static final TypeReference<Collection<CredentialObject>> LIST_TYPE_REF = new TypeReference<>() {
    };
    private final CredentialOfferStoreStatements statements;
    private final String leaseHolder;
    private final Clock clock;
    private final Duration leaseDuration;
    private final StatefulEntityWriteCheck writeCheck;

    public SqlCredentialOfferStore(DataSourceRegistry dataSourceRegistry,
                                   String dataSourceName,
                                   TransactionContext transactionContext,
                                   Supplier<ObjectMapper> objectMapperSupplier,
                                   QueryExecutor queryExecutor,
                                   CredentialOfferStoreStatements statements,
                                   String leaseHolder,
                                   Clock clock) {
        this(dataSourceRegistry, dataSourceName, transactionContext, objectMapperSupplier, queryExecutor, statements, leaseHolder, clock, DEFAULT_LEASE_DURATION);
    }

    public SqlCredentialOfferStore(DataSourceRegistry dataSourceRegistry,
                                   String dataSourceName,
                                   TransactionContext transactionContext,
                                   Supplier<ObjectMapper> objectMapperSupplier,
                                   QueryExecutor queryExecutor,
                                   CredentialOfferStoreStatements statements,
                                   String leaseHolder,
                                   Clock clock,
                                   Duration leaseDuration) {
        super(dataSourceRegistry, dataSourceName, transactionContext, objectMapperSupplier, queryExecutor);
        this.statements = statements;
        this.leaseHolder = leaseHolder;
        this.clock = clock;
        this.leaseDuration = leaseDuration;
        this.writeCheck = new StatefulEntityWriteCheck(queryExecutor, statements, statements.getCredentialOffersTable(), leaseHolder, clock);
    }

    @Override
    public CredentialOffer findById(String id) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                return findByIdInternal(connection, id);
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public @NotNull List<CredentialOffer> nextNotLeased(int max, Criterion... criteria) {
        return transactionContext.execute(() -> {
            var filter = Arrays.stream(criteria).collect(toList());
            var querySpec = QuerySpec.Builder.newInstance().filter(filter).sortField("stateTimestamp").limit(max).build();
            var statement = statements.createNextNotLeaseQuery(querySpec);
            try (var connection = getConnection();
                 var stream = queryExecutor.query(connection, false, this::mapResultSet, statement.getQueryAsString(), statement.getParameters())) {
                // rows are locked until the end of the transaction, so the lease can be safely acquired on them
                return stream.toList().stream()
                        .filter(offer -> acquireLease(connection, offer.getId()))
                        .collect(toList());
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<CredentialOffer> findByIdAndLease(String id) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                // the lease is acquired before reading, so that the returned entity cannot be outdated by a concurrent save
                if (!acquireLease(connection, id)) {
                    if (findByIdInternal(connection, id) == null) {
                        return StoreResult.notFound(format("CredentialOffer %s not found", id));
                    }
                    return StoreResult.alreadyLeased(format("CredentialOffer %s is currently leased!", id));
                }
                return StoreResult.success(findByIdInternal(connection, id));
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<Void> save(CredentialOffer offer) {
        return transactionContext.execute(() -> {
            try (var conn = getConnection()) {
                var result = offer.getVersion() == 0 ? insert(conn, offer) : update(conn, offer);
                if (result.succeeded()) {
                    offer.setVersion(offer.getVersion() + 1);
                }
                return result;
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });

    }

    @Override
    public StoreResult<Void> breakLease(CredentialOffer entity) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var updated = queryExecutor.execute(connection, statements.getBreakLeaseTemplate(statements.getCredentialOffersTable()),
                        entity.getId(), leaseHolder, clock.millis());
                if (updated == 0 && findByIdInternal(connection, entity.getId()) != null) {
                    return StoreResult.alreadyLeased(format("CredentialOffer %s is leased by another holder, cannot break the lease!", entity.getId()));
                }
                return StoreResult.success();
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public Collection<CredentialOffer> query(QuerySpec querySpec) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var query = statements.createQuery(querySpec);
                return queryExecutor.query(connection, true, this::mapResultSet, query.getQueryAsString(), query.getParameters()).toList();
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<Void> deleteById(String id) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                if (findByIdInternal(connection, id) == null) {
                    return StoreResult.notFound(format("CredentialOffer '%s' not found", id));
                }
                if (!acquireLease(connection, id)) {
                    return StoreResult.alreadyLeased(format("CredentialOffer %s is currently leased!", id));
                }
                queryExecutor.execute(connection, statements.getDeleteByIdTemplate(), id);
                return StoreResult.success();
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    /**
     * Inserts an offer that was never stored. It is not inserted if an offer with the same ID exists: unlike a failed
     * statement, this does not abort the surrounding transaction.
     */
    private StoreResult<Void> insert(Connection conn, CredentialOffer offer) {
        var inserted = queryExecutor.execute(conn, statements.getInsertTemplate(),
                offer.getId(),
                offer.getState(),
                offer.getStateCount(),
                offer.getStateTimestamp(),
                offer.getCreatedAt(),
                offer.getUpdatedAt(),
                toJson(offer.getTraceContext()),
                offer.getErrorDetail(),
                offer.getParticipantContextId(),
                offer.issuer(),
                toJson(offer.getCredentialObjects()),
                offer.getVersion() + 1);
        if (inserted == 0) {
            return StoreResult.conflict(format("CredentialOffer %s already exists", offer.getId()));
        }
        return StoreResult.success();
    }

    /**
     * Updates the offer, unless someone else saved it since it was read, or unless someone else holds its lease. Saving
     * the offer breaks its lease.
     */
    private StoreResult<Void> update(Connection conn, CredentialOffer offer) {
        var updated = queryExecutor.execute(conn, statements.getUpdateTemplate(),
                offer.getState(),
                offer.getStateCount(),
                offer.getStateTimestamp(),
                offer.getUpdatedAt(),
                toJson(offer.getTraceContext()),
                offer.getErrorDetail(),
                toJson(offer.getCredentialObjects()),
                null, null, null,
                offer.getVersion() + 1,
                offer.getId(),
                offer.getVersion(),
                leaseHolder,
                clock.millis());
        if (updated == 0) {
            return writeCheck.explainNotWritten(conn, offer.getId(), offer.getVersion());
        }
        return StoreResult.success();
    }

    private boolean acquireLease(Connection connection, String id) {
        var now = clock.millis();
        var sql = statements.getAcquireLeaseTemplate(statements.getCredentialOffersTable());
        return queryExecutor.execute(connection, sql, leaseHolder, now, leaseDuration.toMillis(), id, now) > 0;
    }

    private CredentialOffer findByIdInternal(Connection connection, String id) {
        return transactionContext.execute(() -> {
            var stmt = statements.getFindByIdTemplate();
            return queryExecutor.single(connection, false, this::mapResultSet, stmt, id);
        });
    }


    private CredentialOffer mapResultSet(ResultSet resultSet) throws Exception {

        return CredentialOffer.Builder.newInstance()
                .id(resultSet.getString(statements.getIdColumn()))
                .createdAt(resultSet.getLong(statements.getCreatedAtColumn()))
                .updatedAt(resultSet.getLong(statements.getUpdatedAtColumn()))
                .state(resultSet.getInt(statements.getStateColumn()))
                .stateTimestamp(resultSet.getLong(statements.getStateTimestampColumn()))
                .stateCount(resultSet.getInt(statements.getStateCountColumn()))
                .traceContext(fromJson(resultSet.getString(statements.getTraceContextColumn()), getTypeRef()))
                .errorDetail(resultSet.getString(statements.getErrorDetailColumn()))
                .issuer(resultSet.getString(statements.getIssuerColumn()))
                .participantContextId(resultSet.getString(statements.getParticipantContextIdColumn()))
                .credentialObjects(fromJson(resultSet.getString(statements.getCredentialsColumn()), LIST_TYPE_REF))
                .version(resultSet.getLong(statements.getVersionColumn()))
                .build();

    }
}
