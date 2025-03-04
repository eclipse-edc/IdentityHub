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
import org.eclipse.edc.sql.lease.LeaseStatements;
import org.eclipse.edc.sql.lease.StatefulEntityStatements;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

/**
 * Defines SQL-statements and column names for use with a SQL-based {@link IssuanceProcess}
 */
public interface IssuanceProcessStoreStatements extends StatefulEntityStatements, LeaseStatements {

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

    String getInsertTemplate();

    String getUpdateTemplate();

    String getDeleteByIdTemplate();

    String getFindByIdTemplate();


    SqlQueryStatement createQuery(QuerySpec query);

    String getSelectStatement();
}
