/*
 *  Copyright (c) 2023 Metaform Systems, Inc.
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

package org.eclipse.edc.identityhub.store.sql.credentials.schema.postgres;

import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.store.sql.credentials.CredentialStoreStatements;
import org.eclipse.edc.sql.translation.JsonFieldTranslator;
import org.eclipse.edc.sql.translation.TranslationMapping;


/**
 * Provides a mapping from the canonical format to SQL column names for a {@link VerifiableCredentialResource}
 */
public class VerifiableCredentialResourceMapping extends TranslationMapping {

    public static final String FIELD_ID = "id";
    public static final String FIELD_CREATE_TIMESTAMP = "timestamp";
    public static final String FIELD_ISSUER_ID = "issuerId";
    public static final String FIELD_HOLDER_ID = "holderId";
    public static final String FIELD_STATE = "state";
    public static final String FIELD_ISSUANCE_POLICY = "issuancePolicy";
    public static final String FIELD_REISSUANCE_POLICY = "reissuancePolicy";
    public static final String FIELD_VERIFIABLE_CREDENTIAL = "verifiableCredential";
    public static final String FIELD_PARTICIPANT_CONTEXT_ID = "participantContextId";
    public static final String FIELD_METADATA = "metadata";

    public VerifiableCredentialResourceMapping(CredentialStoreStatements statements) {
        add(FIELD_ID, statements.getIdColumn());
        add(FIELD_CREATE_TIMESTAMP, statements.getCreateTimestampColumn());
        add(FIELD_ISSUER_ID, statements.getIssuerIdColumn());
        add(FIELD_HOLDER_ID, statements.getHolderIdColumn());
        add(FIELD_STATE, statements.getVcStateColumn());
        add(FIELD_ISSUANCE_POLICY, statements.getIssuancePolicyColumn());
        add(FIELD_REISSUANCE_POLICY, statements.getReissuancePolicyColumn());
        add(FIELD_VERIFIABLE_CREDENTIAL, new VerifiableCredentialContainerMapping(statements));
        add(FIELD_PARTICIPANT_CONTEXT_ID, statements.getParticipantContextIdColumn());
        add(FIELD_METADATA, new JsonFieldTranslator(statements.getMetadataColumn()));
    }
}