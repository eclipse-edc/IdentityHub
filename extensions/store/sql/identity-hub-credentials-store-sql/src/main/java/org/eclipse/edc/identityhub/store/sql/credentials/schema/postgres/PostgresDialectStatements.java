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

package org.eclipse.edc.identityhub.store.sql.credentials.schema.postgres;

import org.eclipse.edc.identityhub.store.sql.credentials.BaseSqlDialectStatements;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.dialect.PostgresDialect;
import org.eclipse.edc.sql.translation.PostgresqlOperatorTranslator;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

import static org.eclipse.edc.sql.dialect.PostgresDialect.getSelectFromJsonArrayTemplate;

/**
 * Postgres-specific specialization for creating queries based on Postgres JSON operators
 */
public class PostgresDialectStatements extends BaseSqlDialectStatements {

    public static final String CREDENTIAL_SUBJECT_ALIAS = "crs";

    @Override
    public String getFormatAsJsonOperator() {
        return PostgresDialect.getJsonCastOperator();
    }

    @Override
    public SqlQueryStatement createQuery(QuerySpec querySpec) {

        //-- verifiableCredential.credential.credentialSubject.degreeType
        //-> verifiable_credential -> credentialSubject ->> degreeType
        if (querySpec.containsAnyLeftOperand("verifiableCredential.credential.credentialSubject")) {
            var select = getSelectStatement();
            var stmt = getSelectFromJsonArrayTemplate(select, "%s -> '%s'".formatted(getVerifiableCredentialColumn(), "credentialSubject"), CREDENTIAL_SUBJECT_ALIAS);

            return new SqlQueryStatement(stmt, querySpec, new VerifiableCredentialResourceMapping(this), new PostgresqlOperatorTranslator());
        }

        return super.createQuery(querySpec);
    }
}
