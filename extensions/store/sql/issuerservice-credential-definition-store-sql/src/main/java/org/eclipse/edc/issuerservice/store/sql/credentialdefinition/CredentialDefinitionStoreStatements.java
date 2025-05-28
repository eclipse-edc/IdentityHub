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

package org.eclipse.edc.issuerservice.store.sql.credentialdefinition;

import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialDefinition;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.statement.SqlStatements;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

/**
 * Defines SQL-statements and column names for use with a SQL-based {@link CredentialDefinition}
 */
public interface CredentialDefinitionStoreStatements extends SqlStatements {

    default String getCredentialDefinitionTable() {
        return "credential_definitions";
    }

    default String getIdColumn() {
        return "id";
    }

    default String getCredentialTypeColumn() {
        return "credential_type";
    }

    default String getParticipantContextIdColumn() {
        return "participant_context_id";
    }

    default String getAttestationsColumn() {
        return "attestations";
    }

    default String getRulesColumn() {
        return "rules";
    }

    default String getMappingsColumn() {
        return "mappings";
    }

    default String getJsonSchemaColumn() {
        return "json_schema";
    }

    default String getJsonSchemaUrlColumn() {
        return "json_schema_url";
    }

    default String getValidityColumn() {
        return "validity";
    }

    default String getFormatsColumn() {
        return "format";
    }

    default String getCreateTimestampColumn() {
        return "created_date";
    }

    default String getLastModifiedTimestampColumn() {
        return "last_modified_date";
    }


    String getInsertTemplate();

    String getUpdateTemplate();

    String getDeleteByIdTemplate();

    String getFindByIdTemplate();

    String getFindCredentialTypeTemplate();

    SqlQueryStatement createQuery(QuerySpec query);

    String getSelectStatement();
}
