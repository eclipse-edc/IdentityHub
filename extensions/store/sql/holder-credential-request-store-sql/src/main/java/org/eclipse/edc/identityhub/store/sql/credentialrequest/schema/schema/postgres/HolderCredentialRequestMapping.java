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

package org.eclipse.edc.identityhub.store.sql.credentialrequest.schema.schema.postgres;

import org.eclipse.edc.identityhub.store.sql.credentialrequest.schema.HolderCredentialRequestStoreStatements;
import org.eclipse.edc.sql.lease.StatefulEntityMapping;
import org.eclipse.edc.sql.translation.JsonFieldTranslator;


/**
 * Provides a mapping from the canonical format to SQL column names for a {@code IssuanceProcess}.
 */
public class HolderCredentialRequestMapping extends StatefulEntityMapping {

    public static final String FIELD_PARTICIPANT_ID = "participantContextId";
    public static final String FIELD_ISSUER_DID = "issuerDid";
    public static final String FIELD_CREDENTIAL_TYPES = "credentialTypes";
    public static final String FIELD_ISSUANCE_PROCESS = "issuanceProcessId";

    public HolderCredentialRequestMapping(HolderCredentialRequestStoreStatements statements) {
        super(statements);
        add(FIELD_PARTICIPANT_ID, statements.getParticipantIdColumn());
        add(FIELD_ISSUER_DID, statements.getIssuerDidColumn());
        add(FIELD_CREDENTIAL_TYPES, new JsonFieldTranslator(statements.getCredentialTypesColumn()));
        add(FIELD_ISSUANCE_PROCESS, statements.getIssuanceProcessIdColumn());
    }
}