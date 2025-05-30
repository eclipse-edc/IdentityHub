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

package org.eclipse.edc.identityhub.store.sql.credentialrequest.schema.schema.postgres;

import org.eclipse.edc.identityhub.store.sql.credentialrequest.schema.BaseSqlDialectStatements;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.dialect.PostgresDialect;
import org.eclipse.edc.sql.translation.PostgresqlOperatorTranslator;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

import static org.eclipse.edc.identityhub.store.sql.credentialrequest.schema.schema.postgres.HolderCredentialRequestMapping.FIELD_IDS_AND_FORMATS;
import static org.eclipse.edc.identityhub.store.sql.credentialrequest.schema.schema.postgres.HolderCredentialRequestMapping.FORMATS_ALIAS;
import static org.eclipse.edc.sql.dialect.PostgresDialect.getSelectFromJsonArrayTemplate;

/**
 * Postgres-specific specialization for creating queries based on Postgres JSON operators
 */
public class PostgresDialectStatements extends BaseSqlDialectStatements {


    public PostgresDialectStatements() {
        super(new PostgresqlOperatorTranslator());
    }


    @Override
    public String getFormatAsJsonOperator() {
        return PostgresDialect.getJsonCastOperator();
    }

    @Override
    public SqlQueryStatement createQuery(QuerySpec querySpec) {
        if (querySpec.containsAnyLeftOperand(FIELD_IDS_AND_FORMATS)) {

            var sql = getSelectFromJsonArrayTemplate(getSelectStatement(), getCredentialFormatsColumn(), FORMATS_ALIAS);
            return new SqlQueryStatement(sql, querySpec, new HolderCredentialRequestMapping(this), new PostgresqlOperatorTranslator());
        }
        return super.createQuery(querySpec);
    }
}