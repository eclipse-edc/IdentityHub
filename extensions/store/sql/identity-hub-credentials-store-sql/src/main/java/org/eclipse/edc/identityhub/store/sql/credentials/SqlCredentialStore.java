/*
 *  Copyright (c) 2023 Metaform Systems, Inc.
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

package org.eclipse.edc.identityhub.store.sql.credentials;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.policy.model.Policy;
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
import java.util.Collection;
import java.util.Objects;

import static java.util.Optional.ofNullable;
import static org.eclipse.edc.spi.result.StoreResult.alreadyExists;
import static org.eclipse.edc.spi.result.StoreResult.success;


/**
 * SQL-based {@link VerifiableCredentialResource} store intended for use with PostgreSQL
 */
public class SqlCredentialStore extends AbstractSqlStore implements CredentialStore {

    private final CredentialStoreStatements statements;

    public SqlCredentialStore(DataSourceRegistry dataSourceRegistry, String dataSourceName, TransactionContext transactionContext,
                              ObjectMapper objectMapper, QueryExecutor queryExecutor, CredentialStoreStatements statements) {
        super(dataSourceRegistry, dataSourceName, transactionContext, objectMapper, queryExecutor);
        this.statements = statements;
    }

    @Override
    public StoreResult<Void> create(VerifiableCredentialResource credentialResource) {
        var id = credentialResource.getId();
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                if (findByIdInternal(connection, id) != null) {
                    return alreadyExists(alreadyExistsErrorMessage(id));
                }

                var stmt = statements.getInsertTemplate();
                queryExecutor.execute(connection, stmt, credentialResource.getId(),
                        credentialResource.getTimestamp(),
                        credentialResource.getIssuerId(),
                        credentialResource.getHolderId(),
                        credentialResource.getState(),
                        toJson(credentialResource.getMetadata()),
                        toJson(credentialResource.getIssuancePolicy()),
                        toJson(credentialResource.getReissuancePolicy()),
                        credentialResource.getVerifiableCredential().format().ordinal(),
                        credentialResource.getVerifiableCredential().rawVc(),
                        toJson(credentialResource.getVerifiableCredential().credential()),
                        credentialResource.getParticipantContextId());
                return success();

            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<Collection<VerifiableCredentialResource>> query(QuerySpec querySpec) {
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
    public StoreResult<Void> update(VerifiableCredentialResource credentialResource) {
        var id = credentialResource.getId();

        Objects.requireNonNull(credentialResource);
        Objects.requireNonNull(id);
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                if (findByIdInternal(connection, id) != null) {
                    queryExecutor.execute(connection, statements.getUpdateTemplate(),
                            id,
                            credentialResource.getTimestamp(),
                            credentialResource.getIssuerId(),
                            credentialResource.getHolderId(),
                            credentialResource.getState(),
                            toJson(credentialResource.getMetadata()),
                            toJson(credentialResource.getIssuancePolicy()),
                            toJson(credentialResource.getReissuancePolicy()),
                            credentialResource.getVerifiableCredential().format().ordinal(),
                            credentialResource.getVerifiableCredential().rawVc(),
                            toJson(credentialResource.getVerifiableCredential().credential()),
                            credentialResource.getParticipantContextId(),
                            id);
                    return StoreResult.success();
                }
                return StoreResult.notFound(notFoundErrorMessage(id));
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

    @Override
    public StoreResult<VerifiableCredentialResource> findById(String credentialId) {
        Objects.requireNonNull(credentialId);
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                return ofNullable(findByIdInternal(connection, credentialId))
                        .map(StoreResult::success)
                        .orElseGet(() -> StoreResult.notFound(notFoundErrorMessage(credentialId)));
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    private VerifiableCredentialResource findByIdInternal(Connection connection, String id) {
        return transactionContext.execute(() -> {
            var stmt = statements.getFindByIdTemplate();
            return queryExecutor.single(connection, false, this::mapResultSet, stmt, id);
        });
    }

    private VerifiableCredentialResource mapResultSet(ResultSet resultSet) throws Exception {

        var rawVc = resultSet.getString(statements.getRawVcColumn());
        var formatInt = resultSet.getInt(statements.getVcFormatColumn());
        var format = CredentialFormat.values()[formatInt];
        var vcJson = fromJson(resultSet.getString(statements.getVerifiableCredentialColumn()), VerifiableCredential.class);
        var vcc = new VerifiableCredentialContainer(rawVc, format, vcJson);

        return VerifiableCredentialResource.Builder.newInstance()
                .id(resultSet.getString(statements.getIdColumn()))
                .timestamp(resultSet.getLong(statements.getCreateTimestampColumn()))
                .issuerId(resultSet.getString(statements.getIssuerIdColumn()))
                .holderId(resultSet.getString(statements.getHolderIdColumn()))
                .state(VcStatus.from(resultSet.getInt(statements.getVcStateColumn())))
                .metadata(fromJson(resultSet.getString(statements.getMetadataColumn()), getTypeRef()))
                .issuancePolicy(fromJson(resultSet.getString(statements.getIssuancePolicyColumn()), Policy.class))
                .reissuancePolicy(fromJson(resultSet.getString(statements.getReissuancePolicyColumn()), Policy.class))
                .credential(vcc)
                .participantContextId(resultSet.getString(statements.getParticipantContextIdColumn()))
                .build();
    }
}
