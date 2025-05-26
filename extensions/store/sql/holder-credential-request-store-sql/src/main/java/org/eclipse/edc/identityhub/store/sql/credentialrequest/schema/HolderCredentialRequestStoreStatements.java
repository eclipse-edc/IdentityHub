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
import org.eclipse.edc.sql.lease.LeaseStatements;
import org.eclipse.edc.sql.lease.StatefulEntityStatements;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

/**
 * Defines SQL-statements and column names for use with a SQL-based {@link HolderCredentialRequest}
 */
public interface HolderCredentialRequestStoreStatements extends StatefulEntityStatements, LeaseStatements {

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

    default String getParticipantIdColumn() {
        return "participant_context_id";
    }

    default String getIssuerDidColumn() {
        return "issuer_did";
    }

    String getInsertTemplate();

    String getUpdateTemplate();

    String getDeleteByIdTemplate();

    String getFindByIdTemplate();


    SqlQueryStatement createQuery(QuerySpec query);

    String getSelectStatement();

    default String getissuerPidColumn() {
        return "issuer_pid";
    }

    default String getPendingColumn() {
        return "pending";
    }
}
