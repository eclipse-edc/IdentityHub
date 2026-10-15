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

package org.eclipse.edc.identityhub.store.sql.credentialrequest.schema;

import org.eclipse.edc.identityhub.store.sql.credentialrequest.schema.schema.postgres.HolderCredentialRequestMapping;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.translation.PostgresqlOperatorTranslator;
import org.eclipse.edc.sql.translation.SqlOperatorTranslator;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

import java.time.Clock;

import static java.lang.String.format;

public class BaseSqlDialectStatements implements HolderCredentialRequestStoreStatements {

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
                .column(getParticipantContextIdColumn())
                .column(getIssuerDidColumn())
                .column(getissuerPidColumn())
                .jsonColumn(getCredentialFormatsColumn())
                .column(getVersionColumn())
                .insertInto(getHolderCredentialRequestTable());
        // a row that conflicts with an existing one is not inserted. Unlike a failed statement, this does not abort the
        // surrounding transaction
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
                .column(getIssuerDidColumn())
                .column(getissuerPidColumn())
                .jsonColumn(getCredentialFormatsColumn())
                .column(getLeasedByColumn())
                .column(getLeasedAtColumn())
                .column(getLeaseDurationColumn())
                .column(getVersionColumn())
                .update(getHolderCredentialRequestTable(), getIdColumn());
        // a request that was saved by someone else since it was read, or that is leased by someone else, is not overwritten
        return update.replaceFirst(";\\s*$", "") + " AND " + getVersionAndLeaseFilter(getHolderCredentialRequestTable()) + ";";
    }

    @Override
    public String getDeleteByIdTemplate() {
        return executeStatement().delete(getHolderCredentialRequestTable(), getIdColumn());
    }

    @Override
    public String getFindByIdTemplate() {
        return format("SELECT * FROM %s WHERE %s = ?", getHolderCredentialRequestTable(), getIdColumn());

    }


    @Override
    public SqlQueryStatement createQuery(QuerySpec querySpec) {
        var select = getSelectStatement();
        return new SqlQueryStatement(select, querySpec, new HolderCredentialRequestMapping(this), new PostgresqlOperatorTranslator());
    }

    @Override
    public SqlQueryStatement createNextNotLeaseQuery(QuerySpec querySpec) {
        return new SqlQueryStatement(getSelectStatement(), querySpec, new HolderCredentialRequestMapping(this), operatorTranslator)
                .addWhereClause(getNotLeasedFilter(getHolderCredentialRequestTable()), clock.millis())
                .forUpdate(true);
    }

    @Override
    public String getSelectStatement() {
        return format("SELECT * FROM %s", getHolderCredentialRequestTable());
    }

}
