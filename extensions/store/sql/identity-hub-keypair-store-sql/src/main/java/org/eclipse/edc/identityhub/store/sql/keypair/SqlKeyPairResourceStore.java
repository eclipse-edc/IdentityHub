/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.store.sql.keypair;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairResource;
import org.eclipse.edc.identityhub.spi.store.KeyPairResourceStore;
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
import java.util.Collection;
import java.util.Objects;

import static org.eclipse.edc.spi.result.StoreResult.alreadyExists;
import static org.eclipse.edc.spi.result.StoreResult.notFound;
import static org.eclipse.edc.spi.result.StoreResult.success;

public class SqlKeyPairResourceStore extends AbstractSqlStore implements KeyPairResourceStore {

    private final KeyPairResourceStoreStatements statements;

    public SqlKeyPairResourceStore(DataSourceRegistry dataSourceRegistry, String dataSourceName, TransactionContext transactionContext, ObjectMapper objectMapper, QueryExecutor queryExecutor, KeyPairResourceStoreStatements statements) {
        super(dataSourceRegistry, dataSourceName, transactionContext, objectMapper, queryExecutor);
        this.statements = statements;
    }

    @Override
    public StoreResult<Void> create(KeyPairResource keyPairResource) {
        Objects.requireNonNull(keyPairResource);
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                if (findByIdInternal(connection, keyPairResource.getId()) != null) {
                    return alreadyExists("A KeyPairResource with ID '%s' already exists.".formatted(keyPairResource.getId()));
                }
                var stmt = statements.getInsertTemplate();
                queryExecutor.execute(connection, stmt, keyPairResource.getId(),
                        keyPairResource.getParticipantId(),
                        keyPairResource.getTimestamp(),
                        keyPairResource.getKeyId(),
                        keyPairResource.getGroupName(),
                        keyPairResource.isDefaultPair(),
                        keyPairResource.getUseDuration(),
                        keyPairResource.getRotationDuration(),
                        keyPairResource.getSerializedPublicKey(),
                        keyPairResource.getPrivateKeyAlias(),
                        keyPairResource.getState(),
                        keyPairResource.getKeyContext());

                return success();
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<Collection<KeyPairResource>> query(QuerySpec querySpec) {
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
    public StoreResult<Void> update(KeyPairResource keyPairResource) {
        Objects.requireNonNull(keyPairResource);
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {

                var id = keyPairResource.getId();
                if (findByIdInternal(connection, id) == null) {
                    return notFound("A KeyPairResource with ID '%s' does not exist.".formatted(id));
                }

                var updateStmt = statements.getUpdateTemplate();
                queryExecutor.execute(connection, updateStmt, id,
                        keyPairResource.getParticipantId(),
                        keyPairResource.getTimestamp(),
                        keyPairResource.getKeyId(),
                        keyPairResource.getGroupName(),
                        keyPairResource.isDefaultPair(),
                        keyPairResource.getUseDuration(),
                        keyPairResource.getRotationDuration(),
                        keyPairResource.getSerializedPublicKey(),
                        keyPairResource.getPrivateKeyAlias(),
                        keyPairResource.getState(),
                        keyPairResource.getKeyContext(),
                        id);

                return success();
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
                    return notFound("A KeyPairResource with ID '%s' does not exist.".formatted(id));
                }

                var deleteStmt = statements.getDeleteByIdTemplate();
                queryExecutor.execute(connection, deleteStmt, id);

                return success();
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    private @Nullable KeyPairResource findByIdInternal(Connection connection, String id) {
        return transactionContext.execute(() -> {
            var stmt = statements.getFindByIdTemplate();
            return queryExecutor.single(connection, false, this::mapResultSet, stmt, id);
        });
    }

    private KeyPairResource mapResultSet(ResultSet resultSet) throws Exception {

        return KeyPairResource.Builder.newInstance()
                .id(resultSet.getString(statements.getIdColumn()))
                .participantId(resultSet.getString(statements.getParticipantIdColumn()))
                .timestamp(resultSet.getLong(statements.getTimestampColumn()))
                .keyId(resultSet.getString(statements.getKeyIdColumn()))
                .groupName(resultSet.getString(statements.getGroupNameColumn()))
                .isDefaultPair(resultSet.getBoolean(statements.getIsDefaultKeyPairColumn()))
                .useDuration(resultSet.getLong(statements.getUseDurationColumn()))
                .rotationDuration(resultSet.getLong(statements.getRotationDurationColumn()))
                .serializedPublicKey(resultSet.getString(statements.getSerializedPublicKeyColumn()))
                .privateKeyAlias(resultSet.getString(statements.getPrivateKeyAliasColumn()))
                .state(resultSet.getInt(statements.getStateColumn()))
                .keyContext(resultSet.getString(statements.getKeyContextColumn()))
                .build();
    }

}
