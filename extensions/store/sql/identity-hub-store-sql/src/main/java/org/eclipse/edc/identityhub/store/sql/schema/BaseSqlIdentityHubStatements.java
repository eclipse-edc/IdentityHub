/*
 *  Copyright (c) 2020 - 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.store.sql.schema;

import static java.lang.String.format;

/**
 * Provide an agnostic SQL implementation of {@link IdentityHubStatements} which is not tied to
 * a particular SQL storage.
 */
public class BaseSqlIdentityHubStatements implements IdentityHubStatements {

    @Override
    public String getInsertTemplate() {
        return format("INSERT INTO %s (%s, %s, %s, %s) VALUES (?,?,?,?)",
                getTable(), getIdColumn(), getPayloadColumn(), getPayloadFormatColumn(), getCreatedAtColumn());
    }

    @Override
    public String getFindAllTemplate() {
        return format("SELECT * FROM %s", getTable());
    }
}
