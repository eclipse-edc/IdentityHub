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

import org.eclipse.edc.issuerservice.store.sql.issuanceprocess.schema.postgres.IssuanceProcessMapping;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.translation.PostgresqlOperatorTranslator;
import org.eclipse.edc.sql.translation.SqlOperatorTranslator;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

import static java.lang.String.format;

public class BaseSqlDialectStatements implements IssuanceProcessStoreStatements {

    protected final SqlOperatorTranslator operatorTranslator;

    public BaseSqlDialectStatements(SqlOperatorTranslator operatorTranslator) {
        this.operatorTranslator = operatorTranslator;
    }

    @Override
    public String getInsertTemplate() {
        return executeStatement()
                .column(getIdColumn())
                .column(getStateColumn())
                .column(getStateCountColumn())
                .column(getStateTimestampColumn())
                .column(getCreatedAtColumn())
                .column(getUpdatedAtColumn())
                .jsonColumn(getTraceContextColumn())
                .column(getErrorDetailColumn())
                .column(getHolderIdColumn())
                .column(getParticipantContextIdColumn())
                .column(getHolderPidColumn())
                .jsonColumn(getClaimsColumn())
                .jsonColumn(getCredentialDefinitionsColumn())
                .jsonColumn(getCredentialFormatsColumn())
                .insertInto(getIssuanceProcessTable());
    }

    @Override
    public String getUpdateTemplate() {
        return executeStatement()
                .column(getStateColumn())
                .column(getStateCountColumn())
                .column(getStateTimestampColumn())
                .column(getUpdatedAtColumn())
                .jsonColumn(getTraceContextColumn())
                .column(getErrorDetailColumn())
                .jsonColumn(getClaimsColumn())
                .jsonColumn(getCredentialDefinitionsColumn())
                .jsonColumn(getCredentialFormatsColumn())
                .update(getIssuanceProcessTable(), getIdColumn());
    }

    @Override
    public String getDeleteByIdTemplate() {
        return executeStatement().delete(getIssuanceProcessTable(), getIdColumn());
    }

    @Override
    public String getFindByIdTemplate() {
        return format("SELECT * FROM %s WHERE %s = ?", getIssuanceProcessTable(), getIdColumn());

    }


    @Override
    public SqlQueryStatement createQuery(QuerySpec querySpec) {
        var select = getSelectStatement();
        return new SqlQueryStatement(select, querySpec, new IssuanceProcessMapping(this), new PostgresqlOperatorTranslator());
    }

    @Override
    public String getSelectStatement() {
        return format("SELECT * FROM %s", getIssuanceProcessTable());
    }

    @Override
    public String getDeleteLeaseTemplate() {
        return executeStatement().delete(getLeaseTableName(), getLeaseIdColumn());
    }

    @Override
    public String getInsertLeaseTemplate() {
        return executeStatement()
                .column(getLeaseIdColumn())
                .column(getLeasedByColumn())
                .column(getLeasedAtColumn())
                .column(getLeaseDurationColumn())
                .insertInto(getLeaseTableName());
    }

    @Override
    public String getUpdateLeaseTemplate() {
        return executeStatement()
                .column(getLeaseIdColumn())
                .update(getIssuanceProcessTable(), getIdColumn());
    }

    @Override
    public String getFindLeaseByEntityTemplate() {
        return format("SELECT * FROM %s  WHERE %s = (SELECT lease_id FROM %s WHERE %s=? )",
                getLeaseTableName(), getLeaseIdColumn(), getIssuanceProcessTable(), getIdColumn());
    }
}
