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

package org.eclipse.edc.identityhub.store.sql.credentials;

import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.statement.SqlStatements;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

/**
 * Defines SQL-statements and column names for use with a SQL-based {@link CredentialStore}
 */
public interface CredentialStoreStatements extends SqlStatements {
    default String getCredentialResourceTable() {
        return "credential_resource";
    }

    default String getIdColumn() {
        return "id";
    }

    default String getCreateTimestampColumn() {
        return "create_timestamp";
    }

    default String getIssuerIdColumn() {
        return "issuer_id";
    }

    default String getHolderIdColumn() {
        return "holder_id";
    }

    default String getVcStateColumn() {
        return "vc_state";
    }

    default String getIssuancePolicyColumn() {
        return "issuance_policy";
    }

    default String getReissuancePolicyColumn() {
        return "reissuance_policy";
    }

    default String getVcFormatColumn() {
        return "vc_format";
    }

    default String getRawVcColumn() {
        return "raw_vc";
    }

    default String getVerifiableCredentialColumn() {
        return "verifiable_credential";
    }

    default String getParticipantContextIdColumn() {
        return "participant_context_id";
    }

    default String getMetadataColumn() {
        return "metadata";
    }

    String getInsertTemplate();

    String getUpdateTemplate();

    String getDeleteByIdTemplate();

    String getFindByIdTemplate();

    SqlQueryStatement createQuery(QuerySpec query);

    String getSelectStatement();
}
