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

package org.eclipse.edc.issuerservice.store.sql.participant.schema.postgres;

import org.eclipse.edc.issuerservice.store.sql.participant.ParticipantStoreStatements;
import org.eclipse.edc.sql.translation.JsonArrayTranslator;
import org.eclipse.edc.sql.translation.TranslationMapping;


/**
 * Provides a mapping from the canonical format to SQL column names for a {@code VerifiableCredentialResource}
 */
public class ParticipantMapping extends TranslationMapping {

    public static final String FIELD_ID = "participantId";
    public static final String FIELD_CREATE_TIMESTAMP = "createdAt";
    public static final String FIELD_LASTMODIFIED_TIMESTAMP = "lastModified";
    public static final String FIELD_DID = "did";
    public static final String FIELD_NAME = "participantName";
    public static final String FIELD_ATTESTATIONS = "attestations";

    public ParticipantMapping(ParticipantStoreStatements statements) {
        add(FIELD_ID, statements.getIdColumn());
        add(FIELD_CREATE_TIMESTAMP, statements.getCreateTimestampColumn());
        add(FIELD_LASTMODIFIED_TIMESTAMP, statements.getLastModifiedTimestampColumn());
        add(FIELD_NAME, statements.getParticipantNameColumn());
        add(FIELD_DID, statements.getDidColumn());
        add(FIELD_ATTESTATIONS, new JsonArrayTranslator());
    }
}