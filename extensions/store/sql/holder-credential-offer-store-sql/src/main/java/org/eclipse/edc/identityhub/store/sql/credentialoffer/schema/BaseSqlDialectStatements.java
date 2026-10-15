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

import org.eclipse.edc.identityhub.store.sql.credentialoffer.schema.schema.postgres.CredentialOfferMapping;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.translation.PostgresqlOperatorTranslator;
import org.eclipse.edc.sql.translation.SqlOperatorTranslator;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

import java.time.Clock;

import static java.lang.String.format;

public class BaseSqlDialectStatements implements CredentialOfferStoreStatements {

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
                .column(getIssuerColumn())
                .jsonColumn(getCredentialsColumn())
                .column(getVersionColumn())
                .insertInto(getCredentialOffersTable());
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
                .jsonColumn(getCredentialsColumn())
                .column(getLeasedByColumn())
                .column(getLeasedAtColumn())
                .column(getLeaseDurationColumn())
                .column(getVersionColumn())
                .update(getCredentialOffersTable(), getIdColumn());
        // an offer that was saved by someone else since it was read, or that is leased by someone else, is not overwritten
        return update.replaceFirst(";\\s*$", "") + " AND " + getVersionAndLeaseFilter(getCredentialOffersTable()) + ";";
    }

    @Override
    public String getDeleteByIdTemplate() {
        return executeStatement().delete(getCredentialOffersTable(), getIdColumn());
    }

    @Override
    public String getFindByIdTemplate() {
        return format("SELECT * FROM %s WHERE %s = ?", getCredentialOffersTable(), getIdColumn());

    }


    @Override
    public SqlQueryStatement createQuery(QuerySpec querySpec) {
        var select = getSelectStatement();
        return new SqlQueryStatement(select, querySpec, new CredentialOfferMapping(this), new PostgresqlOperatorTranslator());
    }


    @Override
    public SqlQueryStatement createNextNotLeaseQuery(QuerySpec querySpec) {
        return new SqlQueryStatement(getSelectStatement(), querySpec, new CredentialOfferMapping(this), operatorTranslator)
                .addWhereClause(getNotLeasedFilter(getCredentialOffersTable()), clock.millis())
                .forUpdate(true);
    }

    @Override
    public String getSelectStatement() {
        return format("SELECT * FROM %s", getCredentialOffersTable());
    }

}
