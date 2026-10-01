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

    public CredentialDefinitionMapping(CredentialDefinitionStoreStatements statements) {
        add("id", statements.getIdColumn());
        add("participantContextId", statements.getParticipantContextIdColumn());
        add("credentialType", statements.getCredentialTypeColumn());
        add("createdAt", statements.getCreateTimestampColumn());
        add("lastModified", statements.getLastModifiedTimestampColumn());
        add("jsonSchema", new JsonFieldTranslator(statements.getJsonSchemaColumn()));
        add("jsonSchemaUrl", statements.getJsonSchemaUrlColumn());
        add("validity", statements.getValidityColumn());
        add("format", statements.getFormatsColumn());
        add("attestations", new JsonArrayTranslator(statements.getAttestationsColumn()));
        add("rules", new JsonFieldTranslator(RULES_ALIAS));
        add("mappings", new JsonFieldTranslator(MAPPING_ALIAS));
        add("additionalContext", new JsonArrayTranslator(statements.getAdditionalContextColumn()));
        add("privateProperties", new JsonFieldTranslator(statements.getPrivatePropertiesColumn()));
    }
}