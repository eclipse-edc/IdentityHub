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

package org.eclipse.edc.issuerservice.store.sql.attestationdefinition.schema.postgres;

import org.eclipse.edc.issuerservice.store.sql.attestationdefinition.AttestationDefinitionStoreStatements;
import org.eclipse.edc.sql.translation.JsonFieldTranslator;
import org.eclipse.edc.sql.translation.TranslationMapping;


/**
 * Provides a mapping from the canonical format to SQL column names for a {@code CredentialDefinition}
 */
public class AttestationDefinitionMapping extends TranslationMapping {

    public static final String FIELD_ID = "id";
    public static final String FIELD_CREDENTIAL_TYPE = "attestationType";
    public static final String FIELD_PARTICIPANT_CONTEXT_ID = "participantContextId";
    public static final String FIELD_CREATE_TIMESTAMP = "createdAt";
    public static final String FIELD_LASTMODIFIED_TIMESTAMP = "lastModified";
    public static final String FIELD_CONFIGURATION = "configuration";


    public AttestationDefinitionMapping(AttestationDefinitionStoreStatements statements) {
        add(FIELD_ID, statements.getIdColumn());
        add(FIELD_PARTICIPANT_CONTEXT_ID, statements.getParticipantIdColumn());
        add(FIELD_CREDENTIAL_TYPE, statements.getAttestationTypeColumn());
        add(FIELD_CONFIGURATION, new JsonFieldTranslator(statements.getConfigurationColumn()));
        add(FIELD_CREATE_TIMESTAMP, statements.getCreateTimestampColumn());
        add(FIELD_LASTMODIFIED_TIMESTAMP, statements.getLastModifiedTimestampColumn());
    }
}