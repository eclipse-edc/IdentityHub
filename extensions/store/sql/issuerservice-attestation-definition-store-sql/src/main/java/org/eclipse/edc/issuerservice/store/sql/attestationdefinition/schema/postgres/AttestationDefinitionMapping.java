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

    public AttestationDefinitionMapping(AttestationDefinitionStoreStatements statements) {
        add("id", statements.getIdColumn());
        add("participantContextId", statements.getParticipantContextIdColumn());
        add("attestationType", statements.getAttestationTypeColumn());
        add("configuration", new JsonFieldTranslator(statements.getConfigurationColumn()));
        add("privateProperties", new JsonFieldTranslator(statements.getPrivatePropertiesColumn()));
        add("createdAt", statements.getCreateTimestampColumn());
        add("lastModified", statements.getLastModifiedTimestampColumn());
    }
}