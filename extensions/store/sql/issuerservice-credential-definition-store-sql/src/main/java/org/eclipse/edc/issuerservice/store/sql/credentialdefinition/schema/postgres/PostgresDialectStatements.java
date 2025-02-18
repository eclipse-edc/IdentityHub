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

package org.eclipse.edc.issuerservice.store.sql.credentialdefinition.schema.postgres;

import org.eclipse.edc.issuerservice.store.sql.credentialdefinition.BaseSqlDialectStatements;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.dialect.PostgresDialect;
import org.eclipse.edc.sql.translation.PostgresqlOperatorTranslator;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

import static org.eclipse.edc.sql.dialect.PostgresDialect.getSelectFromJsonArrayTemplate;

/**
 * Postgres-specific specialization for creating queries based on Postgres JSON operators
 */
public class PostgresDialectStatements extends BaseSqlDialectStatements {

    static final String RULES_ALIAS = "a_rules";
    static final String MAPPING_ALIAS = "a_mappings";

    public PostgresDialectStatements() {
        super(new PostgresqlOperatorTranslator());
    }


    @Override
    public String getFormatAsJsonOperator() {
        return PostgresDialect.getJsonCastOperator();
    }

    @Override
    public SqlQueryStatement createQuery(QuerySpec querySpec) {
        if (querySpec.containsAnyLeftOperand("rules")) {
            var select = getSelectFromJsonArrayTemplate(getSelectStatement(), getRulesColumn(), RULES_ALIAS);
            return new SqlQueryStatement(select, querySpec, new CredentialDefinitionMapping(this), operatorTranslator);
        } else if (querySpec.containsAnyLeftOperand("mappings")) {
            var select = getSelectFromJsonArrayTemplate(getSelectStatement(), getMappingsColumn(), MAPPING_ALIAS);
            return new SqlQueryStatement(select, querySpec, new CredentialDefinitionMapping(this), operatorTranslator);
        }
        return super.createQuery(querySpec);
    }
}
