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

package org.eclipse.edc.identityhub.store.sql.credentialrequest.schema;

import org.eclipse.edc.identityhub.spi.credential.request.model.HolderCredentialRequest;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.lease.StatefulEntityStatements;
import org.eclipse.edc.sql.statement.SqlStatements;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

/**
 * Defines SQL-statements and column names for use with a SQL-based {@link HolderCredentialRequest}
 */
public interface HolderCredentialRequestStoreStatements extends StatefulEntityStatements, SqlStatements {

    default String getHolderCredentialRequestTable() {
        return "edc_holder_credentialrequest";
    }

    @Override
    default String getIdColumn() {
        return "id";
    }

    default String getCredentialFormatsColumn() {
        return "ids_and_formats";
    }

    default String getParticipantContextIdColumn() {
        return "participant_context_id";
    }

    default String getIssuerDidColumn() {
        return "issuer_did";
    }

    /**
     * Inserts a new request with its version. The request is not inserted if one with the same ID exists.
     */
    String getInsertTemplate();

    /**
     * Updates a request by its ID, if its version is still the one it was read with and its lease is owned by the caller
     * or free. The parameters are the columns to update, the ID, the version, the lease holder and the current time.
     */
    String getUpdateTemplate();

    String getDeleteByIdTemplate();

    String getFindByIdTemplate();


    SqlQueryStatement createQuery(QuerySpec query);

    SqlQueryStatement createNextNotLeaseQuery(QuerySpec querySpec);

    String getSelectStatement();

    default String getissuerPidColumn() {
        return "issuer_pid";
    }

    default String getPendingColumn() {
        return "pending";
    }
}
