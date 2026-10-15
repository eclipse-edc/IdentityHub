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

package org.eclipse.edc.issuerservice.store.sql.issuanceprocess;

import org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcess;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.lease.StatefulEntityStatements;
import org.eclipse.edc.sql.statement.SqlStatements;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

/**
 * Defines SQL-statements and column names for use with a SQL-based {@link IssuanceProcess}
 */
public interface IssuanceProcessStoreStatements extends StatefulEntityStatements, SqlStatements {

    default String getIssuanceProcessTable() {
        return "edc_issuance_process";
    }

    @Override
    default String getIdColumn() {
        return "id";
    }

    default String getClaimsColumn() {
        return "claims";
    }


    default String getCredentialDefinitionsColumn() {
        return "credential_definitions";
    }

    default String getCredentialFormatsColumn() {
        return "credential_formats";
    }

    default String getHolderIdColumn() {
        return "holder_id";
    }

    default String getParticipantContextIdColumn() {
        return "participant_context_id";
    }

    default String getPendingColumn() {
        return "pending";
    }

    default String getHolderPidColumn() {
        return "holder_pid";
    }

    /**
     * Inserts a new process with its version. The process is not inserted if it conflicts with an existing one, either by
     * its ID or by its holderPid.
     */
    String getInsertTemplate();

    /**
     * Updates a process by its ID, if its version is still the one it was read with and its lease is owned by the caller
     * or free. The parameters are the columns to update, the ID, the version, the lease holder and the current time.
     */
    String getUpdateTemplate();

    String getDeleteByIdTemplate();

    String getFindByIdTemplate();


    SqlQueryStatement createQuery(QuerySpec query);

    SqlQueryStatement createNextNotLeaseQuery(QuerySpec querySpec);

    String getSelectStatement();
}
