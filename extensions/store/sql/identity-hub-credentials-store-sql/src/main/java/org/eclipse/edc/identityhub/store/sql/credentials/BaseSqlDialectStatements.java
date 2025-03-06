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

import org.eclipse.edc.identityhub.store.sql.credentials.schema.postgres.VerifiableCredentialResourceMapping;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.translation.PostgresqlOperatorTranslator;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

import static java.lang.String.format;

public class BaseSqlDialectStatements implements CredentialStoreStatements {
    @Override
    public String getInsertTemplate() {
        return executeStatement()
                .column(getIdColumn())
                .column(getCreateTimestampColumn())
                .column(getIssuerIdColumn())
                .column(getHolderIdColumn())
                .column(getVcStateColumn())
                .jsonColumn(getMetadataColumn())
                .jsonColumn(getIssuancePolicyColumn())
                .jsonColumn(getReissuancePolicyColumn())
                .column(getVcFormatColumn())
                .column(getRawVcColumn())
                .jsonColumn(getVerifiableCredentialColumn())
                .column(getParticipantContextIdColumn())
                .insertInto(getCredentialResourceTable());
    }

    @Override
    public String getUpdateTemplate() {
        return executeStatement()
                .column(getIdColumn())
                .column(getCreateTimestampColumn())
                .column(getIssuerIdColumn())
                .column(getHolderIdColumn())
                .column(getVcStateColumn())
                .jsonColumn(getMetadataColumn())
                .jsonColumn(getIssuancePolicyColumn())
                .jsonColumn(getReissuancePolicyColumn())
                .column(getVcFormatColumn())
                .column(getRawVcColumn())
                .jsonColumn(getVerifiableCredentialColumn())
                .column(getParticipantContextIdColumn())
                .update(getCredentialResourceTable(), getIdColumn());
    }

    @Override
    public String getDeleteByIdTemplate() {
        return executeStatement().delete(getCredentialResourceTable(), getIdColumn());
    }

    @Override
    public String getFindByIdTemplate() {
        return format("SELECT * FROM %s WHERE %s = ?", getCredentialResourceTable(), getIdColumn());

    }

    @Override
    public SqlQueryStatement createQuery(QuerySpec querySpec) {
        var select = getSelectStatement();
        return new SqlQueryStatement(select, querySpec, new VerifiableCredentialResourceMapping(this), new PostgresqlOperatorTranslator());
    }

    @Override
    public String getSelectStatement() {
        return format("SELECT * FROM %s", getCredentialResourceTable());
    }
}
