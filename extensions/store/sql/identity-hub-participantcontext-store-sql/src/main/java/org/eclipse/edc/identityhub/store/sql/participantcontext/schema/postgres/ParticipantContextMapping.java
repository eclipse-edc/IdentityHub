/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
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

package org.eclipse.edc.identityhub.store.sql.participantcontext.schema.postgres;

import org.eclipse.edc.identityhub.store.sql.participantcontext.ParticipantContextStoreStatements;
import org.eclipse.edc.sql.translation.TranslationMapping;


/**
 * Provides a mapping from the canonical format to SQL column names for a {@code VerifiableCredentialResource}
 */
public class ParticipantContextMapping extends TranslationMapping {

    public static final String FIELD_ID = "participantContextId";
    public static final String FIELD_CREATE_TIMESTAMP = "createdAt";
    public static final String FIELD_LASTMODIFIED_TIMESTAMP = "lastModified";
    public static final String FIELD_STATE = "state";
    public static final String FIELD_API_TOKEN_ALIAS = "apiTokenAlias";
    public static final String FIELD_DID = "did";

    public ParticipantContextMapping(ParticipantContextStoreStatements statements) {
        add(FIELD_ID, statements.getIdColumn());
        add(FIELD_CREATE_TIMESTAMP, statements.getCreateTimestampColumn());
        add(FIELD_STATE, statements.getStateColumn());
        add(FIELD_LASTMODIFIED_TIMESTAMP, statements.getLastModifiedTimestampColumn());
        add(FIELD_API_TOKEN_ALIAS, statements.getApiTokenAliasColumn());
        add(FIELD_DID, statements.getDidColumn());
    }
}