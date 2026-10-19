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

package org.eclipse.edc.identityhub.did.store.sql;

import org.eclipse.edc.identityhub.did.store.sql.schema.postgres.DidResourceMapping;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.translation.PostgresqlOperatorTranslator;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

import static java.lang.String.format;
import static org.eclipse.edc.sql.statement.SqlExecuteStatement.equalTo;

public class BaseSqlDialectStatements implements DidResourceStatements {
    @Override
    public String getInsertTemplate() {
        var insert = executeStatement()
                .column(getIdColumn())
                .column(getStateColumn())
                .column(getCreateTimestampColumn())
                .column(getStateTimestampColumn())
                .jsonColumn(getDidDocumentColumn())
                .column(getParticipantContextId())
                .insertInto(getDidResourceTableName());
        // a row that conflicts with an existing one is not inserted. Unlike a failed statement, this does not abort the
        // surrounding transaction
        return insert.replaceFirst(";\\s*$", "") + " ON CONFLICT DO NOTHING;";
    }

    @Override
    public String getUpdateTemplate() {
        // the owner of a DID never changes, so a DID of another participant context is not updated
        return executeStatement()
                .column(getStateColumn())
                .column(getCreateTimestampColumn())
                .column(getStateTimestampColumn())
                .jsonColumn(getDidDocumentColumn())
                .update(getDidResourceTableName(), equalTo(getParticipantContextId()), equalTo(getIdColumn()));
    }

    @Override
    public String getDeleteByIdTemplate() {
        return executeStatement().delete(getDidResourceTableName(), getIdColumn());
    }

    @Override
    public String getFindByIdTemplate() {
        return format("SELECT * FROM %s WHERE %s = ?", getDidResourceTableName(), getIdColumn());

    }

    @Override
    public SqlQueryStatement createQuery(QuerySpec querySpec) {
        var select = getSelectStatement();
        return new SqlQueryStatement(select, querySpec, new DidResourceMapping(this), new PostgresqlOperatorTranslator());
    }

    @Override
    public String getSelectStatement() {
        return format("SELECT * FROM %s", getDidResourceTableName());
    }
}
