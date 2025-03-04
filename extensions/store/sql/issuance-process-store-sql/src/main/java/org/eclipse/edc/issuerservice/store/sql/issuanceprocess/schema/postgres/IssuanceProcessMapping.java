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

import org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcessStates;
import org.eclipse.edc.issuerservice.store.sql.issuanceprocess.IssuanceProcessStoreStatements;
import org.eclipse.edc.sql.lease.StatefulEntityMapping;
import org.eclipse.edc.sql.translation.JsonArrayTranslator;
import org.eclipse.edc.sql.translation.JsonFieldTranslator;


/**
 * Provides a mapping from the canonical format to SQL column names for a {@code IssuanceProcess}.
 */
public class IssuanceProcessMapping extends StatefulEntityMapping {

    public static final String FIELD_ID = "id";
    public static final String FIELD_HOLDER_ID = "holderId";
    public static final String FIELD_PARTICIPANT_CONTEXT_ID = "participantContextId";
    public static final String FIELD_CLAIMS = "claims";
    public static final String FIELD_CREDENTIAL_DEFINITIONS = "credentialDefinitions";
    public static final String FIELD_CREDENTIAL_FORMATS = "credentialFormats";
    private static final String FIELD_HOLDER_PID = "holderPid";
    private static final String FIELD_PENDING = "pending";

    public IssuanceProcessMapping(IssuanceProcessStoreStatements statements) {
        super(statements, state -> IssuanceProcessStates.valueOf(state).code());
        add(FIELD_ID, statements.getIdColumn());
        add(FIELD_HOLDER_ID, statements.getHolderIdColumn());
        add(FIELD_PARTICIPANT_CONTEXT_ID, statements.getParticipantContextIdColumn());
        add(FIELD_CLAIMS, new JsonFieldTranslator(FIELD_CLAIMS));
        add(FIELD_PENDING, statements.getPendingColumn());
        add(FIELD_HOLDER_PID, statements.getHolderPidColumn());
        add(FIELD_CREDENTIAL_DEFINITIONS, new JsonArrayTranslator(statements.getCredentialDefinitionsColumn()));
        add(FIELD_CREDENTIAL_FORMATS, new JsonFieldTranslator(statements.getCredentialFormatsColumn()));
    }
}