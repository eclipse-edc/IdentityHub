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

/**
 * Provides the mapping with columns, statements with the underlying SQL storage system.
 */
public interface IdentityHubStatements {

    default String getTable() {
        return "edc_identityhub";
    }

    default String getIdColumn() {
        return "id";
    }

    default String getPayloadColumn() {
        return "payload";
    }

    default String getCreatedAtColumn() {
        return "created_at";
    }

    String getInsertTemplate();

    String getFindAllTemplate();

}
