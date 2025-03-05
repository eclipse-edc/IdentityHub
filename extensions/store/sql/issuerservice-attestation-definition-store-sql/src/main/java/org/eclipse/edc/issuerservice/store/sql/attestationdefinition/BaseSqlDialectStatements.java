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

public class BaseSqlDialectStatements implements AttestationDefinitionStoreStatements {
    @Override
    public String getInsertTemplate() {
        return executeStatement()
                .column(getIdColumn())
                .column(getParticipantIdColumn())
                .column(getAttestationTypeColumn())
                .jsonColumn(getConfigurationColumn())
                .column(getCreateTimestampColumn())
                .column(getLastModifiedTimestampColumn())
                .insertInto(getAttestationDefinitionTable());
    }

    @Override
    public String getUpdateTemplate() {
        return executeStatement()
                .column(getAttestationTypeColumn())
                .jsonColumn(getConfigurationColumn())
                .column(getLastModifiedTimestampColumn())
                .update(getAttestationDefinitionTable(), getIdColumn());
    }

    @Override
    public String getDeleteByIdTemplate() {
        return executeStatement().delete(getAttestationDefinitionTable(), getIdColumn());
    }

    @Override
    public String getFindByIdTemplate() {
        return format("SELECT * FROM %s WHERE %s = ?", getAttestationDefinitionTable(), getIdColumn());

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
