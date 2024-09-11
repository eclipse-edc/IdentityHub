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

import org.eclipse.edc.identityhub.store.sql.keypair.schema.postgres.KeyPairResourceMapping;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.translation.PostgresqlOperatorTranslator;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

import static java.lang.String.format;

public class BaseSqlDialectStatements implements KeyPairResourceStoreStatements {
    @Override
    public String getInsertTemplate() {
        return executeStatement()
                .column(getIdColumn())
                .column(getParticipantIdColumn())
                .column(getTimestampColumn())
                .column(getKeyIdColumn())
                .column(getGroupNameColumn())
                .column(getIsDefaultKeyPairColumn())
                .column(getUseDurationColumn())
                .column(getRotationDurationColumn())
                .column(getSerializedPublicKeyColumn())
                .column(getPrivateKeyAliasColumn())
                .column(getStateColumn())
                .column(getKeyContextColumn())
                .insertInto(getTableName());
    }

    @Override
    public String getUpdateTemplate() {
        return executeStatement()
                .column(getIdColumn())
                .column(getParticipantIdColumn())
                .column(getTimestampColumn())
                .column(getKeyIdColumn())
                .column(getGroupNameColumn())
                .column(getIsDefaultKeyPairColumn())
                .column(getUseDurationColumn())
                .column(getRotationDurationColumn())
                .column(getSerializedPublicKeyColumn())
                .column(getPrivateKeyAliasColumn())
                .column(getStateColumn())
                .column(getKeyContextColumn())
                .update(getTableName(), getIdColumn());
    }

    @Override
    public String getDeleteByIdTemplate() {
        return executeStatement().delete(getTableName(), getIdColumn());
    }

    @Override
    public String getFindByIdTemplate() {
        return format("SELECT * FROM %s WHERE %s = ?", getTableName(), getIdColumn());

    }

    @Override
    public SqlQueryStatement createQuery(QuerySpec querySpec) {
        var select = getSelectStatement();
        return new SqlQueryStatement(select, querySpec, new KeyPairResourceMapping(this), new PostgresqlOperatorTranslator());
    }

    @Override
    public String getSelectStatement() {
        return format("SELECT * FROM %s", getTableName());
    }
}
