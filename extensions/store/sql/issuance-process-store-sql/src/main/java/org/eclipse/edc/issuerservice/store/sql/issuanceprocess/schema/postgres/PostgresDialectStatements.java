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

package org.eclipse.edc.issuerservice.store.sql.issuanceprocess.schema.postgres;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.issuerservice.store.sql.issuanceprocess.BaseSqlDialectStatements;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.dialect.PostgresDialect;
import org.eclipse.edc.sql.translation.PostgresqlOperatorTranslator;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

import static org.eclipse.edc.issuerservice.store.sql.issuanceprocess.schema.postgres.IssuanceProcessMapping.FIELD_CREDENTIAL_DEFINITIONS;

/**
 * Postgres-specific specialization for creating queries based on Postgres JSON operators
 */
public class PostgresDialectStatements extends BaseSqlDialectStatements {

    private final ObjectMapper mapper;

    public PostgresDialectStatements(ObjectMapper mapper) {
        super(new PostgresqlOperatorTranslator());
        this.mapper = mapper;
    }


    @Override
    public String getFormatAsJsonOperator() {
        return PostgresDialect.getJsonCastOperator();
    }

    @Override
    public SqlQueryStatement createQuery(QuerySpec querySpec) {

        // if any criterion credentialDefinition array field, we need to slightly adapt the FROM clause
        // by including a JSONB containment operator
        if (querySpec.containsAnyLeftOperand(FIELD_CREDENTIAL_DEFINITIONS)) {
            var select = "SELECT * FROM %s ".formatted(getIssuanceProcessTable());

            var criteria = querySpec.getFilterExpression();
            var filteredCriteria = criteria.stream()
                    .filter(c -> c.getOperandLeft().toString().startsWith(FIELD_CREDENTIAL_DEFINITIONS))
                    .toList();

            criteria.removeAll(filteredCriteria);
            var stmt = new SqlQueryStatement(select, querySpec, new IssuanceProcessMapping(this), operatorTranslator);
            filteredCriteria.forEach(c -> {
                var rightOperand = c.getOperandRight();
                try {
                    var rightOperandJson = mapper.writeValueAsString(rightOperand);
                    stmt.addWhereClause("%s @> ?::jsonb".formatted(getCredentialDefinitionsColumn()), rightOperandJson);
                } catch (JsonProcessingException e) {
                    throw new EdcPersistenceException(e);
                }

            });
            return stmt;
        }
        return super.createQuery(querySpec);
    }
}
