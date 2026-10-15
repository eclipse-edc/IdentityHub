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

import java.time.Clock;

import static java.lang.String.format;

public class BaseSqlDialectStatements implements IssuanceProcessStoreStatements {

    protected final SqlOperatorTranslator operatorTranslator;
    protected final Clock clock;

    public BaseSqlDialectStatements(SqlOperatorTranslator operatorTranslator, Clock clock) {
        this.operatorTranslator = operatorTranslator;
        this.clock = clock;
    }

    @Override
    public String getInsertTemplate() {
        var insert = executeStatement()
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
                .column(getVersionColumn())
                .insertInto(getIssuanceProcessTable());
        // a process that conflicts with an existing one, e.g. by its holderPid, is not inserted. Unlike a failed statement,
        // this does not abort the surrounding transaction. Without a conflict target, it also works on a schema that lacks
        // the unique index on the holderPid
        return insert.replaceFirst(";\\s*$", "") + " ON CONFLICT DO NOTHING;";
    }

    @Override
    public String getUpdateTemplate() {
        var update = executeStatement()
                .column(getStateColumn())
                .column(getStateCountColumn())
                .column(getStateTimestampColumn())
                .column(getUpdatedAtColumn())
                .jsonColumn(getTraceContextColumn())
                .column(getErrorDetailColumn())
                .jsonColumn(getClaimsColumn())
                .jsonColumn(getCredentialDefinitionsColumn())
                .jsonColumn(getCredentialFormatsColumn())
                .column(getLeasedByColumn())
                .column(getLeasedAtColumn())
                .column(getLeaseDurationColumn())
                .column(getVersionColumn())
                .update(getIssuanceProcessTable(), getIdColumn());
        // a process that was saved by someone else since it was read, or that is leased by someone else, is not overwritten
        return update.replaceFirst(";\\s*$", "") + " AND " + getVersionAndLeaseFilter(getIssuanceProcessTable()) + ";";
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
    public SqlQueryStatement createNextNotLeaseQuery(QuerySpec querySpec) {
        return new SqlQueryStatement(getSelectStatement(), querySpec, new IssuanceProcessMapping(this), operatorTranslator)
                .addWhereClause(getNotLeasedFilter(getIssuanceProcessTable()), clock.millis())
                .forUpdate(true);
    }


    @Override
    public String getSelectStatement() {
        return format("SELECT * FROM %s", getIssuanceProcessTable());
    }

}
