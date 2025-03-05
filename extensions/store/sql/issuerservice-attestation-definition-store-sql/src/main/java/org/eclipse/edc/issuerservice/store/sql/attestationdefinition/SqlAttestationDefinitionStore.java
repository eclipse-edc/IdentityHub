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

package org.eclipse.edc.issuerservice.store.sql.attestationdefinition;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionStore;
import org.eclipse.edc.issuerservice.spi.issuance.model.AttestationDefinition;
import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialDefinition;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.store.AbstractSqlStore;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import static org.eclipse.edc.spi.result.StoreResult.alreadyExists;
import static org.eclipse.edc.spi.result.StoreResult.success;


/**
 * SQL-based {@link CredentialDefinition} store intended for use with PostgreSQL
 */
public class SqlAttestationDefinitionStore extends AbstractSqlStore implements AttestationDefinitionStore {

    private static final TypeReference<Map<String, Object>> CONFIG_REF = new TypeReference<>() {
    };

    private final AttestationDefinitionStoreStatements statements;
    private final Clock clock;

    public SqlAttestationDefinitionStore(DataSourceRegistry dataSourceRegistry,
                                         String dataSourceName,
                                         TransactionContext transactionContext,
                                         ObjectMapper objectMapper,
                                         QueryExecutor queryExecutor,
                                         AttestationDefinitionStoreStatements statements,
                                         Clock clock) {
        super(dataSourceRegistry, dataSourceName, transactionContext, objectMapper, queryExecutor);
        this.statements = statements;
        this.clock = clock;
    }

    @Override
    public @Nullable AttestationDefinition resolveDefinition(String id) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                return findByIdInternal(connection, id);
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<Void> create(AttestationDefinition attestationDefinition) {
        var id = attestationDefinition.getId();
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                if (findByIdInternal(connection, id) != null) {
                    return alreadyExists(alreadyExistsErrorMessage(id));
                }

                var stmt = statements.getInsertTemplate();
                var timestamp = clock.millis();
                queryExecutor.execute(connection, stmt,
                        id,
                        attestationDefinition.getParticipantContextId(),
                        attestationDefinition.getAttestationType(),
                        toJson(attestationDefinition.getConfiguration()),
                        timestamp,
                        timestamp
                );
                return success();

            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<Void> update(AttestationDefinition attestationDefinition) {
        var id = attestationDefinition.getId();

        Objects.requireNonNull(attestationDefinition);
        Objects.requireNonNull(id);
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                if (findByIdInternal(connection, id) != null) {

                    queryExecutor.execute(connection,
                            statements.getUpdateTemplate(),
                            attestationDefinition.getAttestationType(),
                            toJson(attestationDefinition.getConfiguration()),
                            clock.millis(),
                            id
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
    public StoreResult<Collection<AttestationDefinition>> query(QuerySpec querySpec) {
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

    private AttestationDefinition findByIdInternal(Connection connection, String id) {
        return transactionContext.execute(() -> {
            var stmt = statements.getFindByIdTemplate();
            return queryExecutor.single(connection, false, this::mapResultSet, stmt, id);
        });
    }

    private AttestationDefinition mapResultSet(ResultSet resultSet) throws Exception {

        var id = resultSet.getString(statements.getIdColumn());
        var participantContextId = resultSet.getString(statements.getParticipantIdColumn());
        var type = resultSet.getString(statements.getAttestationTypeColumn());
        var config = resultSet.getString(statements.getConfigurationColumn());
        return AttestationDefinition.Builder.newInstance()
                .id(id)
                .participantContextId(participantContextId)
                .attestationType(type)
                .configuration(fromJson(config, CONFIG_REF))
                .build();
    }
}
