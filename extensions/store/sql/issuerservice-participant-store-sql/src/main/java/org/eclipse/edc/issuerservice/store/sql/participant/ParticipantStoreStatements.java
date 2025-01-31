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

package org.eclipse.edc.issuerservice.store.sql.participant;

import org.eclipse.edc.issuerservice.spi.participant.models.Participant;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.statement.SqlStatements;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

/**
 * Defines SQL-statements and column names for use with a SQL-based {@link Participant}
 */
public interface ParticipantStoreStatements extends SqlStatements {
    default String getParticipantsTable() {
        return "participants";
    }

    default String getIdColumn() {
        return "participant_id";
    }

    default String getCreateTimestampColumn() {
        return "created_date";
    }

    default String getLastModifiedTimestampColumn() {
        return "last_modified_date";
    }

    default String getParticipantNameColumn() {
        return "participant_name";
    }

    default String getDidColumn() {
        return "did";
    }


    String getInsertTemplate();

    String getUpdateTemplate();

    String getDeleteByIdTemplate();

    String getFindByIdTemplate();

    SqlQueryStatement createQuery(QuerySpec query);

    String getSelectStatement();
}
