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

package org.eclipse.edc.issuerservice.store.sql.credentialdefinition;

import org.eclipse.edc.issuerservice.store.sql.credentialdefinition.schema.postgres.CredentialDefinitionMapping;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.translation.PostgresqlOperatorTranslator;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

import static java.lang.String.format;
import static org.eclipse.edc.sql.statement.SqlExecuteStatement.equalTo;

public class BaseSqlDialectStatements implements CredentialDefinitionStoreStatements {

    protected final PostgresqlOperatorTranslator operatorTranslator;

    public BaseSqlDialectStatements(PostgresqlOperatorTranslator operatorTranslator) {
        this.operatorTranslator = operatorTranslator;
    }

    @Override
    public String getInsertTemplate() {
        var insert = executeStatement()
                .column(getIdColumn())
                .column(getParticipantContextIdColumn())
                .column(getCredentialTypeColumn())
                .jsonColumn(getAttestationsColumn())
                .jsonColumn(getRulesColumn())
                .jsonColumn(getMappingsColumn())
                .jsonColumn(getJsonSchemaColumn())
                .jsonColumn(getAdditionalContextColumn())
                .jsonColumn(getPrivatePropertiesColumn())
                .column(getJsonSchemaUrlColumn())
                .column(getValidityColumn())
                .column(getFormatsColumn())
                .column(getCreateTimestampColumn())
                .column(getLastModifiedTimestampColumn())
                .insertInto(getCredentialDefinitionTable());
        // a row that conflicts with an existing one is not inserted. Unlike a failed statement, this does not abort the
        // surrounding transaction
        return insert.replaceFirst(";\\s*$", "") + " ON CONFLICT DO NOTHING;";
    }

    @Override
    public String getUpdateTemplate() {
        return executeStatement()
                .column(getCredentialTypeColumn())
                .jsonColumn(getAttestationsColumn())
                .jsonColumn(getRulesColumn())
                .jsonColumn(getMappingsColumn())
                .jsonColumn(getJsonSchemaColumn())
                .jsonColumn(getAdditionalContextColumn())
                .jsonColumn(getPrivatePropertiesColumn())
                .column(getJsonSchemaUrlColumn())
                .column(getValidityColumn())
                .column(getFormatsColumn())
                .column(getLastModifiedTimestampColumn())
                .update(getCredentialDefinitionTable(), equalTo(getParticipantContextIdColumn()), equalTo(getIdColumn()));
    }

    @Override
    public String getDeleteByIdTemplate() {
        return executeStatement().delete(getCredentialDefinitionTable(), equalTo(getParticipantContextIdColumn()), equalTo(getIdColumn()));
    }

    @Override
    public String getFindByIdTemplate() {
        return format("SELECT * FROM %s WHERE %s = ? AND %s = ?", getCredentialDefinitionTable(), getParticipantContextIdColumn(), getIdColumn());

    }

    @Override
    public String getFindCredentialTypeTemplate() {
        return format("SELECT * FROM %s WHERE %s = ?", getCredentialDefinitionTable(), getCredentialTypeColumn());

    }

    @Override
    public SqlQueryStatement createQuery(QuerySpec querySpec) {
        var select = getSelectStatement();
        return new SqlQueryStatement(select, querySpec, new CredentialDefinitionMapping(this), operatorTranslator);
    }

    @Override
    public String getSelectStatement() {
        return format("SELECT * FROM %s", getCredentialDefinitionTable());
    }
}
