/*
 *  Copyright (c) 2025 Metaform Systems Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.store.sql.credentialoffer.schema.schema.postgres;

import org.eclipse.edc.identityhub.store.sql.credentialoffer.schema.BaseSqlDialectStatements;
import org.eclipse.edc.sql.dialect.PostgresDialect;
import org.eclipse.edc.sql.translation.PostgresqlOperatorTranslator;

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

}
