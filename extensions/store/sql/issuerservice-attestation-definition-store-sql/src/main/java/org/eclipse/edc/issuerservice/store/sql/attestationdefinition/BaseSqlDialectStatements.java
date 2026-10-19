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

package org.eclipse.edc.issuerservice.store.sql.attestationdefinition;

import org.eclipse.edc.issuerservice.store.sql.attestationdefinition.schema.postgres.AttestationDefinitionMapping;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.translation.PostgresqlOperatorTranslator;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

import static java.lang.String.format;
import static org.eclipse.edc.sql.statement.SqlExecuteStatement.equalTo;

public class BaseSqlDialectStatements implements AttestationDefinitionStoreStatements {
    @Override
    public String getInsertTemplate() {
        var insert = executeStatement()
                .column(getIdColumn())
                .column(getParticipantContextIdColumn())
                .column(getAttestationTypeColumn())
                .jsonColumn(getConfigurationColumn())
                .jsonColumn(getPrivatePropertiesColumn())
                .column(getCreateTimestampColumn())
                .column(getLastModifiedTimestampColumn())
                .insertInto(getAttestationDefinitionTable());
        // a row that conflicts with an existing one is not inserted. Unlike a failed statement, this does not abort the
        // surrounding transaction
        return insert.replaceFirst(";\\s*$", "") + " ON CONFLICT DO NOTHING;";
    }

    @Override
    public String getUpdateTemplate() {
        return executeStatement()
                .column(getAttestationTypeColumn())
                .jsonColumn(getConfigurationColumn())
                .jsonColumn(getPrivatePropertiesColumn())
                .column(getLastModifiedTimestampColumn())
                .update(getAttestationDefinitionTable(), equalTo(getParticipantContextIdColumn()), equalTo(getIdColumn()));
    }

    @Override
    public String getDeleteByIdTemplate() {
        return executeStatement().delete(getAttestationDefinitionTable(), equalTo(getParticipantContextIdColumn()), equalTo(getIdColumn()));
    }

    @Override
    public String getFindByIdTemplate() {
        return format("SELECT * FROM %s WHERE %s = ? AND %s = ?", getAttestationDefinitionTable(), getParticipantContextIdColumn(), getIdColumn());

    }

    @Override
    public String getFindCredentialTypeTemplate() {
        return format("SELECT * FROM %s WHERE %s = ?", getAttestationDefinitionTable(), getAttestationTypeColumn());

    }

    @Override
    public SqlQueryStatement createQuery(QuerySpec querySpec) {
        var select = getSelectStatement();
        return new SqlQueryStatement(select, querySpec, new AttestationDefinitionMapping(this), new PostgresqlOperatorTranslator());
    }

    @Override
    public String getSelectStatement() {
        return format("SELECT * FROM %s", getAttestationDefinitionTable());
    }
}
