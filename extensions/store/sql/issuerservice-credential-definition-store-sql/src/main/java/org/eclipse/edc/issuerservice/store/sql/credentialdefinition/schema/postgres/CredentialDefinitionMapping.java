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

package org.eclipse.edc.issuerservice.store.sql.credentialdefinition.schema.postgres;

import org.eclipse.edc.issuerservice.store.sql.credentialdefinition.CredentialDefinitionStoreStatements;
import org.eclipse.edc.sql.translation.JsonArrayTranslator;
import org.eclipse.edc.sql.translation.JsonFieldTranslator;
import org.eclipse.edc.sql.translation.TranslationMapping;

import static org.eclipse.edc.issuerservice.store.sql.credentialdefinition.schema.postgres.PostgresDialectStatements.MAPPING_ALIAS;
import static org.eclipse.edc.issuerservice.store.sql.credentialdefinition.schema.postgres.PostgresDialectStatements.RULES_ALIAS;


/**
 * Provides a mapping from the canonical format to SQL column names for a {@code CredentialDefinition}
 */
public class CredentialDefinitionMapping extends TranslationMapping {

    public static final String FIELD_ID = "id";
    public static final String FIELD_PARTICIPANT_CONTEXT_ID = "participantContextId";
    public static final String FIELD_CREDENTIAL_TYPE = "credentialType";
    public static final String FIELD_CREATE_TIMESTAMP = "createdAt";
    public static final String FIELD_LASTMODIFIED_TIMESTAMP = "lastModified";
    public static final String FIELD_JSON_SCHEMA = "jsonSchema";
    public static final String FIELD_JSON_SCHEMA_URL = "jsonSchemaUrl";
    public static final String FIELD_VALIDITY = "validity";
    public static final String FIELD_FORMAT = "format";
    public static final String FIELD_ATTESTATIONS = "attestations";
    public static final String FIELD_RULES = "rules";
    public static final String FIELD_MAPPINGS = "mappings";


    public CredentialDefinitionMapping(CredentialDefinitionStoreStatements statements) {
        add(FIELD_ID, statements.getIdColumn());
        add(FIELD_PARTICIPANT_CONTEXT_ID, statements.getParticipantContextIdColumn());
        add(FIELD_CREDENTIAL_TYPE, statements.getCredentialTypeColumn());
        add(FIELD_CREATE_TIMESTAMP, statements.getCreateTimestampColumn());
        add(FIELD_LASTMODIFIED_TIMESTAMP, statements.getLastModifiedTimestampColumn());
        add(FIELD_JSON_SCHEMA, new JsonFieldTranslator(statements.getJsonSchemaColumn()));
        add(FIELD_JSON_SCHEMA_URL, statements.getJsonSchemaUrlColumn());
        add(FIELD_VALIDITY, statements.getValidityColumn());
        add(FIELD_FORMAT, statements.getFormatsColumn());
        add(FIELD_ATTESTATIONS, new JsonArrayTranslator(statements.getAttestationsColumn()));
        add(FIELD_RULES, new JsonFieldTranslator(RULES_ALIAS));
        add(FIELD_MAPPINGS, new JsonFieldTranslator(MAPPING_ALIAS));
    }
}