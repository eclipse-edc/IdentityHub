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
import org.eclipse.edc.sql.translation.TranslationMapping;


/**
 * Provides a mapping from the canonical format to SQL column names for a {@code VerifiableCredentialResource}
 */
public class CredentialDefinitionMapping extends TranslationMapping {

    public static final String FIELD_ID = "id";
    public static final String FIELD_CREDENTIAL_TYPE = "credentialType";
    public static final String FIELD_CREATE_TIMESTAMP = "createdAt";
    public static final String FIELD_LASTMODIFIED_TIMESTAMP = "lastModified";


    public CredentialDefinitionMapping(CredentialDefinitionStoreStatements statements) {
        add(FIELD_ID, statements.getIdColumn());
        add(FIELD_CREDENTIAL_TYPE, statements.getCredentialTypeColumn());
        add(FIELD_CREATE_TIMESTAMP, statements.getCreateTimestampColumn());
        add(FIELD_LASTMODIFIED_TIMESTAMP, statements.getLastModifiedTimestampColumn());
    }
}