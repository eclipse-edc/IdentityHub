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

import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.sql.translation.JsonFieldTranslator;
import org.eclipse.edc.util.reflection.PathItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps the canonical format of a {@link VerifiableCredential} onto its JSON representation
 * and generates query statements for Postgres.
 */
public class CredentialJsonMapping extends JsonFieldTranslator {

    private final Map<String, String> replacements = new HashMap<>();

    public CredentialJsonMapping(String columnName) {
        super(columnName);
        replacements.put("credentialSubject", PostgresDialectStatements.CREDENTIAL_SUBJECT_ALIAS);
    }

    @Override
    public String getLeftOperand(List<PathItem> path, Class<?> type) {
        var stmt = super.getLeftOperand(path, type);


        if (path.stream().allMatch(p -> p.toString().equals("types"))) {
            return "(%s)::jsonb".formatted(stmt).replace("->>", "->");
        }

        if (path.size() == 1) {
            return stmt;
        }
        // the WHERE clause can't handle set-returning functions such as "json_array_elements". thus, we must use an alias
        // in the FROM clause, and re-use the same alias in the WHERE clause, for example:
        // SELECT * FROM credential_resource r, json_array_elements(r.verifiable_credential -> 'credentialSubject') subj WHERE subj ->> 'test-key' = 'test-val2';
        var firstPathItem = path.get(0).toString();
        var replacement = replacements.get(firstPathItem);
        if (replacement != null) {
            var newPath = new ArrayList<PathItem>();
            path.stream().skip(1).forEach(newPath::add);
            var newMapping = new JsonFieldTranslator(replacement);
            return newMapping.getLeftOperand(newPath, type);
        }

        return stmt;
    }
}
