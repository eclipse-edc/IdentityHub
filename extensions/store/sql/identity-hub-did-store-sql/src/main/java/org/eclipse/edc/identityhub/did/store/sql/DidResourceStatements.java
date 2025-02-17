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

package org.eclipse.edc.identityhub.did.store.sql;

import org.eclipse.edc.identityhub.spi.did.store.DidResourceStore;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.statement.SqlStatements;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

/**
 * Defines SQL-statements and column names for use with a SQL-based {@link DidResourceStore}
 */
public interface DidResourceStatements extends SqlStatements {
    default String getDidResourceTableName() {
        return "did_resources";
    }

    default String getIdColumn() {
        return "did";
    }

    default String getStateColumn() {
        return "state";
    }

    default String getStateTimestampColumn() {
        return "state_timestamp";
    }

    default String getCreateTimestampColumn() {
        return "create_timestamp";
    }

    default String getDidDocumentColumn() {
        return "did_document";
    }

    default String getParticipantContextId() {
        return "participant_context_id";
    }

    String getInsertTemplate();

    String getUpdateTemplate();

    String getDeleteByIdTemplate();

    String getFindByIdTemplate();

    SqlQueryStatement createQuery(QuerySpec query);

    String getSelectStatement();
}
