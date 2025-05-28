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

package org.eclipse.edc.issuerservice.store.sql.credentialdefinition;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.issuerservice.spi.issuance.credentialdefinition.store.CredentialDefinitionStore;
import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialDefinition;
import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialRuleDefinition;
import org.eclipse.edc.issuerservice.spi.issuance.model.MappingDefinition;
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
import java.time.Clock;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static java.util.Optional.ofNullable;
import static org.eclipse.edc.spi.result.StoreResult.alreadyExists;
import static org.eclipse.edc.spi.result.StoreResult.success;


/**
 * SQL-based {@link CredentialDefinition} store intended for use with PostgreSQL
 */
public class SqlCredentialDefinitionStore extends AbstractSqlStore implements CredentialDefinitionStore {

    private static final TypeReference<List<String>> ATTESTATIONS_LIST_REF = new TypeReference<>() {
    };

    private static final TypeReference<List<CredentialRuleDefinition>> RULES_LIST_REF = new TypeReference<>() {
    };

    private static final TypeReference<List<MappingDefinition>> MAPPINGS_LIST_REF = new TypeReference<>() {
    };

    private static final TypeReference<List<CredentialFormat>> FORMATS_LIST_REF = new TypeReference<>() {
    };

    private final CredentialDefinitionStoreStatements statements;
    private final Clock clock;

    public SqlCredentialDefinitionStore(DataSourceRegistry dataSourceRegistry,
                                        String dataSourceName,
                                        TransactionContext transactionContext,
                                        ObjectMapper objectMapper,
                                        QueryExecutor queryExecutor,
                                        CredentialDefinitionStoreStatements statements,
                                        Clock clock) {
        super(dataSourceRegistry, dataSourceName, transactionContext, objectMapper, queryExecutor);
        this.statements = statements;
        this.clock = clock;
    }

    @Override
    public StoreResult<CredentialDefinition> findById(String id) {
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
    public StoreResult<Void> create(CredentialDefinition credentialDefinition) {
        var id = credentialDefinition.getId();
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                if (findByIdInternal(connection, id) != null) {
                    return alreadyExists(alreadyExistsErrorMessage(id));
                }

                if (findByCredentialType(connection, credentialDefinition.getCredentialType()) != null) {
                    return alreadyExists(alreadyExistsForTypeErrorMessage(credentialDefinition.getCredentialType()));
                }

                var stmt = statements.getInsertTemplate();
                var timestamp = clock.millis();
                queryExecutor.execute(connection, stmt,
                        credentialDefinition.getId(),
                        credentialDefinition.getParticipantContextId(),
                        credentialDefinition.getCredentialType(),
                        toJson(credentialDefinition.getAttestations()),
                        toJson(credentialDefinition.getRules()),
                        toJson(credentialDefinition.getMappings()),
                        toJson(credentialDefinition.getJsonSchema()),
                        credentialDefinition.getJsonSchemaUrl(),
                        credentialDefinition.getValidity(),
                        credentialDefinition.getFormat(),
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
    public StoreResult<Void> update(CredentialDefinition credentialDefinition) {
        var id = credentialDefinition.getId();

        Objects.requireNonNull(credentialDefinition);
        Objects.requireNonNull(id);
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                if (findByIdInternal(connection, id) != null) {


                    var definitionByCredentialType = findByCredentialType(connection, credentialDefinition.getCredentialType());
                    if (definitionByCredentialType != null && !definitionByCredentialType.getId().equals(id)) {
                        return alreadyExists(alreadyExistsForTypeErrorMessage(credentialDefinition.getCredentialType()));
                    }
                    queryExecutor.execute(connection,
                            statements.getUpdateTemplate(),
                            credentialDefinition.getCredentialType(),
                            toJson(credentialDefinition.getAttestations()),
                            toJson(credentialDefinition.getRules()),
                            toJson(credentialDefinition.getMappings()),
                            toJson(credentialDefinition.getJsonSchema()),
                            credentialDefinition.getJsonSchemaUrl(),
                            credentialDefinition.getValidity(),
                            credentialDefinition.getFormat(),
                            clock.millis(),
                            credentialDefinition.getId()
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
    public StoreResult<Collection<CredentialDefinition>> query(QuerySpec querySpec) {
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

    private CredentialDefinition findByIdInternal(Connection connection, String id) {
        return transactionContext.execute(() -> {
            var stmt = statements.getFindByIdTemplate();
            return queryExecutor.single(connection, false, this::mapResultSet, stmt, id);
        });
    }

    private CredentialDefinition findByCredentialType(Connection connection, String credentialType) {
        return transactionContext.execute(() -> {
            var stmt = statements.getFindCredentialTypeTemplate();
            return queryExecutor.single(connection, false, this::mapResultSet, stmt, credentialType);
        });
    }

    private CredentialDefinition mapResultSet(ResultSet resultSet) throws Exception {

        return CredentialDefinition.Builder.newInstance()
                .id(resultSet.getString(statements.getIdColumn()))
                .participantContextId(resultSet.getString(statements.getParticipantContextIdColumn()))
                .credentialType(resultSet.getString(statements.getCredentialTypeColumn()))
                .attestations(fromJson(resultSet.getString(statements.getAttestationsColumn()), ATTESTATIONS_LIST_REF))
                .rules(fromJson(resultSet.getString(statements.getRulesColumn()), RULES_LIST_REF))
                .mappings(fromJson(resultSet.getString(statements.getMappingsColumn()), MAPPINGS_LIST_REF))
                .jsonSchema(resultSet.getString(statements.getJsonSchemaColumn()))
                .jsonSchemaUrl(resultSet.getString(statements.getJsonSchemaUrlColumn()))
                .validity(resultSet.getLong(statements.getValidityColumn()))
                .format(resultSet.getString(statements.getFormatsColumn()))
                .build();
    }
}
