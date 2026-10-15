/*
 *  Copyright (c) 2026 Metaform Systems Inc.
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

package org.eclipse.edc.issuerservice.issuance.database;

import java.util.regex.Pattern;

/**
 * Checks table and column names before they are put into a SQL statement. Identifiers cannot be bound as statement
 * parameters, so only plain, unquoted identifiers are accepted: a letter or underscore followed by up to 62 letters,
 * digits or underscores. Table names may additionally be qualified with a schema name ({@code schema.table}).
 */
public final class SqlIdentifiers {
    private static final String IDENTIFIER = "[A-Za-z_][A-Za-z0-9_]{0,62}";
    private static final Pattern COLUMN_NAME = Pattern.compile(IDENTIFIER);
    private static final Pattern TABLE_NAME = Pattern.compile("(" + IDENTIFIER + "\\.)?" + IDENTIFIER);

    private SqlIdentifiers() {
    }

    public static boolean isValidTableName(String tableName) {
        return tableName != null && TABLE_NAME.matcher(tableName).matches();
    }

    public static boolean isValidColumnName(String columnName) {
        return columnName != null && COLUMN_NAME.matcher(columnName).matches();
    }
}
