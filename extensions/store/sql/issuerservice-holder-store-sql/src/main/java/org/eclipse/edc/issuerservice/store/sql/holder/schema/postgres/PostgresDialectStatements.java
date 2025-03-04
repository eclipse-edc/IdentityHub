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

package org.eclipse.edc.issuerservice.store.sql.holder.schema.postgres;

import org.eclipse.edc.issuerservice.store.sql.holder.BaseSqlDialectStatements;
import org.eclipse.edc.sql.dialect.PostgresDialect;

/**
 * Postgres-specific specialization for creating queries based on Postgres JSON operators
 */
public class PostgresDialectStatements extends BaseSqlDialectStatements {

    @Override
    public String getFormatAsJsonOperator() {
        return PostgresDialect.getJsonCastOperator();
    }
}
