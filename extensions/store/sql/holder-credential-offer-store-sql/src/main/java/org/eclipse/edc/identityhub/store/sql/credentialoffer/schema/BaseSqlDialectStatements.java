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
import org.eclipse.edc.sql.lease.spi.LeaseStatements;
import org.eclipse.edc.sql.translation.PostgresqlOperatorTranslator;
import org.eclipse.edc.sql.translation.SqlOperatorTranslator;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

import java.time.Clock;

import static java.lang.String.format;

public class BaseSqlDialectStatements implements CredentialOfferStoreStatements {

    protected final SqlOperatorTranslator operatorTranslator;
    protected final LeaseStatements leaseStatements;
    protected final Clock clock;

    public BaseSqlDialectStatements(SqlOperatorTranslator operatorTranslator, LeaseStatements leaseStatements, Clock clock) {
        this.operatorTranslator = operatorTranslator;
        this.leaseStatements = leaseStatements;
        this.clock = clock;
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
                .column(getParticipantContextIdColumn())
                .column(getIssuerColumn())
                .jsonColumn(getCredentialsColumn())
                .insertInto(getCredentialOffersTable());
    }

    @Override
    public String getUpdateTemplate() {
        return executeStatement()
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
                .update(getCredentialOffersTable(), getIdColumn());
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
        var queryTemplate = "%s LEFT JOIN %s l ON %s.%s = l.%s".formatted(getSelectStatement(), leaseStatements.getLeaseTableName(), getCredentialOffersTable(), getIdColumn(), leaseStatements.getResourceIdColumn());
        return new SqlQueryStatement(queryTemplate, querySpec, new CredentialOfferMapping(this), operatorTranslator)
                .addWhereClause(getNotLeasedFilter(), clock.millis(), getCredentialOffersTable());
    }

    @Override
    public String getSelectStatement() {
        return format("SELECT * FROM %s", getCredentialOffersTable());
    }

    private String getNotLeasedFilter() {
        return format("(l.%s IS NULL OR (? > (%s + %s) AND ? = l.%s))",
                leaseStatements.getResourceIdColumn(), leaseStatements.getLeasedAtColumn(), leaseStatements.getLeaseDurationColumn(), leaseStatements.getResourceKindColumn());
    }

}
