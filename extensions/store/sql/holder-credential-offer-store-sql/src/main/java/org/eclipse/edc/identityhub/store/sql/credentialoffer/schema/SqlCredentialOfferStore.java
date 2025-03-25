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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.identityhub.spi.credential.request.model.HolderCredentialRequest;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialObject;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialOffer;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialOfferStore;
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
public class SqlCredentialOfferStore extends AbstractSqlStore implements CredentialOfferStore {

    private static final TypeReference<Collection<CredentialObject>> LIST_TYPE_REF = new TypeReference<>() {
    };
    private final String leaseHolderName;
    private final SqlLeaseContextBuilder leaseContext;
    private final CredentialOfferStoreStatements statements;
    private final Clock clock;

    public SqlCredentialOfferStore(DataSourceRegistry dataSourceRegistry,
                                   String dataSourceName,
                                   TransactionContext transactionContext,
                                   ObjectMapper objectMapper,
                                   QueryExecutor queryExecutor,
                                   CredentialOfferStoreStatements statements,
                                   String leaseHolderName,
                                   Clock clock) {
        super(dataSourceRegistry, dataSourceName, transactionContext, objectMapper, queryExecutor);
        this.statements = statements;
        this.leaseHolderName = leaseHolderName;
        this.clock = clock;
        leaseContext = SqlLeaseContextBuilder.with(transactionContext, leaseHolderName, statements, clock, queryExecutor);
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
    public StoreResult<CredentialOffer> findByIdAndLease(String id) {
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
    public void save(CredentialOffer offer) {
        try (var conn = getConnection()) {
            var existing = findByIdInternal(conn, offer.getId());
            if (existing != null) {
                leaseContext.by(leaseHolderName).withConnection(conn).breakLease(offer.getId());
                update(conn, offer);
            } else {
                insert(conn, offer);
            }
        } catch (SQLException e) {
            throw new EdcPersistenceException(e);
        }
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
                if (findByIdInternal(connection, id) != null) {
                    var stmt = statements.getDeleteByIdTemplate();

                    queryExecutor.execute(connection, stmt, id);
                    leaseContext.withConnection(connection).breakLease(id);
                    return StoreResult.success();
                }
                return StoreResult.notFound(format("CredentialOffer '%s' not found", id));
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    private void insert(Connection conn, CredentialOffer offer) {
        var insertTpStatement = statements.getInsertTemplate();
        queryExecutor.execute(conn, insertTpStatement,
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
                toJson(offer.getCredentialObjects()));
    }

    private void update(Connection conn, CredentialOffer offer) {
        var updateStmt = statements.getUpdateTemplate();
        queryExecutor.execute(conn, updateStmt,
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
                offer.getId());

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
                .participantContextId(resultSet.getString(statements.getParticipantIdColumn()))
                .credentialObjects(fromJson(resultSet.getString(statements.getCredentialsColumn()), LIST_TYPE_REF))
                .build();

    }
}
