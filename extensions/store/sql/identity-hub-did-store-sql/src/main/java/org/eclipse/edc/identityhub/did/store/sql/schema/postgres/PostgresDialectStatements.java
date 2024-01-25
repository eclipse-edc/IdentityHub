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

package org.eclipse.edc.identityhub.did.store.sql.schema.postgres;

import org.eclipse.edc.identityhub.did.store.sql.BaseSqlDialectStatements;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.dialect.PostgresDialect;
import org.eclipse.edc.sql.translation.PostgresqlOperatorTranslator;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

import static java.lang.String.format;
import static org.eclipse.edc.sql.dialect.PostgresDialect.getSelectFromJsonArrayTemplate;

/**
 * Postgres-specific specialization for creating queries based on Postgres JSON operators
 */
public class PostgresDialectStatements extends BaseSqlDialectStatements {
    @Override
    public String getFormatAsJsonOperator() {
        return PostgresDialect.getJsonCastOperator();
    }

    @Override
    public SqlQueryStatement createQuery(QuerySpec querySpec) {
        if (querySpec.containsAnyLeftOperand("document.service")) {
            var select = getSelectFromJsonArrayTemplate(getSelectStatement(), "%s -> '%s'".formatted(getDidDocumentColumn(), "service"), DidDocumentMapping.FIELD_SERVICE);
            return new SqlQueryStatement(select, querySpec, new DidResourceMapping(this), new PostgresqlOperatorTranslator());
        } else if (querySpec.containsAnyLeftOperand("document.verificationMethod")) {
            var select = getSelectFromJsonArrayTemplate(getSelectStatement(), "%s -> '%s'".formatted(getDidDocumentColumn(), "verificationMethod"), DidDocumentMapping.FIELD_VERIFICATION_METHOD);
            return new SqlQueryStatement(select, querySpec, new DidResourceMapping(this), new PostgresqlOperatorTranslator());
        } else if (querySpec.containsAnyLeftOperand("document.authentication")) {
            var select = getSelectFromJsonArrayTextTemplate(getSelectStatement(), "%s -> '%s'".formatted(getDidDocumentColumn(), "authentication"), DidDocumentMapping.FIELD_AUTHENTICATION);
            return new SqlQueryStatement(select, querySpec, new DidResourceMapping(this), new PostgresqlOperatorTranslator());
        }
        return super.createQuery(querySpec);
    }

    private String getSelectFromJsonArrayTextTemplate(String selectStatement, String jsonPath, String aliasName) {
        return format("%s, json_array_elements_text(%s) as %s", selectStatement, jsonPath, aliasName);
    }
}
