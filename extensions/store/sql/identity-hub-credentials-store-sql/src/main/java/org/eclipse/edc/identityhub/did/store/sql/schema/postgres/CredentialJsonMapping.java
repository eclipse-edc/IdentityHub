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

import org.eclipse.edc.spi.types.PathItem;
import org.eclipse.edc.sql.translation.JsonFieldMapping;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.edc.identityhub.did.store.sql.schema.postgres.PostgresDialectStatements.CREDENTIAL_SUBJECT_ALIAS;

public class CredentialJsonMapping extends JsonFieldMapping {

    public CredentialJsonMapping(String columnName) {
        super(columnName);
        add("credentialSubject", CREDENTIAL_SUBJECT_ALIAS);

    }

    @Override
    public String getStatement(String canonicalPropertyName, Class<?> type) {
        return super.getStatement(canonicalPropertyName, type);
    }

    @Override
    public String getStatement(List<PathItem> path, Class<?> type) {
        var replacement = fieldMap.get(path.get(0).toString());

        // the WHERE clause can't handle set-returning functions such as "json_array_elements". thus, we must use an alias
        // in the FROM clause, and re-use the same alias in the WHERE clause, for example:
        //   SELECT * FROM credential_resource r, json_array_elements(r.verifiable_credential -> 'credentialSubject') subj WHERE subj ->> 'test-key' = 'test-val2';
        if (replacement != null) {
            List<PathItem> newPath = new ArrayList<>();
            path.stream().skip(1).forEach(newPath::add);
            var newMapping = new JsonFieldMapping(replacement.toString());
            return newMapping.getStatement(newPath, type);
        }
        var stmt = super.getStatement(path, type);

        if (path.stream().allMatch(p -> p.toString().equals("types"))) {
            stmt = "(%s)::jsonb".formatted(stmt).replace("->>", "->");
        }
        return stmt;
    }
}
