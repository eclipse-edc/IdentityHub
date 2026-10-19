/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.iam.decentralizedclaims.sts.store.schema;

import org.eclipse.edc.iam.decentralizedclaims.sts.store.schema.postgres.StsClientMapping;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.translation.SqlOperatorTranslator;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

import static java.lang.String.format;
import static org.eclipse.edc.sql.statement.SqlExecuteStatement.equalTo;

public class BaseSqlDialectStatements implements StsClientStatements {

    protected final SqlOperatorTranslator operatorTranslator;

    public BaseSqlDialectStatements(SqlOperatorTranslator operatorTranslator) {
        this.operatorTranslator = operatorTranslator;
    }

    @Override
    public String getDeleteByIdTemplate() {
        return executeStatement().delete(getStsClientTable(), getIdColumn());
    }

    @Override
    public String getFindByTemplate() {
        return format("SELECT * FROM %s WHERE %s = ?", getStsClientTable(), getIdColumn());
    }

    @Override
    public String getFindByClientIdTemplate() {
        return format("SELECT * FROM %s WHERE %s = ?", getStsClientTable(), getClientIdColumn());
    }

    @Override
    public String getInsertTemplate() {
        var insert = executeStatement()
                .column(getIdColumn())
                .column(getNameColumn())
                .column(getClientIdColumn())
                .column(getDidColumn())
                .column(getSecretAliasColumn())
                .column(getCreatedAtColumn())
                .column(getParticipantContextIdColumn())
                .insertInto(getStsClientTable());
        // a row that conflicts with an existing one is not inserted. Unlike a failed statement, this does not abort the
        // surrounding transaction
        return insert.replaceFirst(";\\s*$", "") + " ON CONFLICT DO NOTHING;";
    }


    @Override
    public String getUpdateTemplate() {
        // the owner of an STS account never changes, so an account of another participant context is not updated
        return executeStatement()
                .column(getNameColumn())
                .column(getClientIdColumn())
                .column(getDidColumn())
                .column(getSecretAliasColumn())
                .column(getCreatedAtColumn())
                .update(getStsClientTable(), equalTo(getParticipantContextIdColumn()), equalTo(getIdColumn()));

    }

    @Override
    public SqlQueryStatement createQuery(QuerySpec querySpec) {
        var select = format("SELECT * FROM %s", getStsClientTable());
        return new SqlQueryStatement(select, querySpec, new StsClientMapping(this), operatorTranslator);
    }

}
