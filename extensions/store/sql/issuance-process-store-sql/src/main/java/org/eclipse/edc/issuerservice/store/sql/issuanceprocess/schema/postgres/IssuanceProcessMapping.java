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

package org.eclipse.edc.issuerservice.store.sql.issuanceprocess.schema.postgres;

import org.eclipse.edc.issuerservice.store.sql.issuanceprocess.IssuanceProcessStoreStatements;
import org.eclipse.edc.sql.lease.StatefulEntityMapping;
import org.eclipse.edc.sql.translation.JsonArrayTranslator;
import org.eclipse.edc.sql.translation.JsonFieldTranslator;


/**
 * Provides a mapping from the canonical format to SQL column names for a {@code IssuanceProcess}.
 */
public class IssuanceProcessMapping extends StatefulEntityMapping {

    public static final String FIELD_ID = "id";
    public static final String FIELD_PARTICIPANT_ID = "participantId";
    public static final String FIELD_ISSUER_CONTEXT_ID = "issuerContextId";
    public static final String FIELD_CLAIMS = "claims";
    public static final String FIELD_CREDENTIAL_DEFINITIONS = "credentialDefinitions";

    public IssuanceProcessMapping(IssuanceProcessStoreStatements statements) {
        super(statements);
        add(FIELD_ID, statements.getIdColumn());
        add(FIELD_PARTICIPANT_ID, statements.getParticipantIdColumn());
        add(FIELD_ISSUER_CONTEXT_ID, statements.getIssuerContextIdColumn());
        add(FIELD_CLAIMS, new JsonFieldTranslator(FIELD_CLAIMS));
        add(FIELD_CREDENTIAL_DEFINITIONS, new JsonArrayTranslator(statements.getCredentialDefinitionsColumn()));
    }
}