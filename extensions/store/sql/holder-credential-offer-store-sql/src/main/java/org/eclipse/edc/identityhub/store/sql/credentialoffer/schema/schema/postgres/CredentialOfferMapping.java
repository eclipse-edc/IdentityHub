/*
 *  Copyright (c) 2025 Metaform Systems Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.store.sql.credentialoffer.schema.schema.postgres;

import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialOfferStatus;
import org.eclipse.edc.identityhub.store.sql.credentialoffer.schema.CredentialOfferStoreStatements;
import org.eclipse.edc.sql.lease.StatefulEntityMapping;
import org.eclipse.edc.sql.translation.JsonFieldTranslator;


/**
 * Provides a mapping from the canonical format to SQL column names for a {@code IssuanceProcess}.
 */
public class CredentialOfferMapping extends StatefulEntityMapping {

    public static final String FIELD_PARTICIPANT_ID = "participantContextId";
    public static final String FIELD_ISSUER_DID = "issuer";
    public static final String FIELD_CREDENTIAL_TYPES = "credentials";

    public CredentialOfferMapping(CredentialOfferStoreStatements statements) {
        super(statements, state -> CredentialOfferStatus.valueOf(state).code());
        add(FIELD_PARTICIPANT_ID, statements.getParticipantIdColumn());
        add(FIELD_ISSUER_DID, statements.getIssuerColumn());
        add(FIELD_CREDENTIAL_TYPES, new JsonFieldTranslator(statements.getCredentialsColumn()));
    }
}