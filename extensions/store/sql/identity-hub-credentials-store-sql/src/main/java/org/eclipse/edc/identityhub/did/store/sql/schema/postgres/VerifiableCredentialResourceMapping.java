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

package org.eclipse.edc.identityhub.did.store.sql.schema.postgres;

import org.eclipse.edc.identityhub.did.store.sql.CredentialStoreStatements;
import org.eclipse.edc.sql.translation.TranslationMapping;


/**
 * Provides a mapping from the canonical format to SQL column names for a {@link org.eclipse.edc.identityhub.spi.store.model.VerifiableCredentialResource}
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

    public VerifiableCredentialResourceMapping(CredentialStoreStatements statements) {
        add(FIELD_ID, statements.getIdColumn());
        add(FIELD_CREATE_TIMESTAMP, statements.getCreateTimestampColumn());
        add(FIELD_ISSUER_ID, statements.getIssuerIdColumn());
        add(FIELD_HOLDER_ID, statements.getHolderIdColumn());
        add(FIELD_STATE, statements.getVcStateColumn());
        add(FIELD_ISSUANCE_POLICY, statements.getIssuancePolicyColumn());
        add(FIELD_REISSUANCE_POLICY, statements.getReissuancePolicyColumn());
        add(FIELD_VERIFIABLE_CREDENTIAL, new VerifiableCredentialContainerMapping(statements));
    }
}