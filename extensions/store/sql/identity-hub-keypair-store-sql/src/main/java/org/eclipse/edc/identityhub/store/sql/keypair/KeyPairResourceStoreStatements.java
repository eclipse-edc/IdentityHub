/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
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

package org.eclipse.edc.identityhub.store.sql.keypair;

import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.statement.SqlStatements;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

/**
 * Defines SQL-statements and column names for use with a SQL-based {@link ParticipantContext}
 */
public interface KeyPairResourceStoreStatements extends SqlStatements {
    default String getTableName() {
        return "keypair_resource";
    }

    default String getIdColumn() {
        return "id";
    }

    default String getParticipantIdColumn() {
        return "participant_id";
    }

    default String getTimestampColumn() {
        return "timestamp";
    }

    default String getKeyIdColumn() {
        return "key_id";
    }

    default String getGroupNameColumn() {
        return "group_name";
    }

    default String getIsDefaultKeyPairColumn() {
        return "is_default_pair";
    }

    default String getUseDurationColumn() {
        return "use_duration";
    }

    default String getRotationDurationColumn() {
        return "rotation_duration";
    }

    default String getSerializedPublicKeyColumn() {
        return "serialized_public_key";
    }

    default String getPrivateKeyAliasColumn() {
        return "private_key_alias";
    }

    default String getStateColumn() {
        return "state";
    }

    default String getKeyContextColumn() {
        return "key_context";
    }

    String getInsertTemplate();

    String getUpdateTemplate();

    String getDeleteByIdTemplate();

    String getFindByIdTemplate();

    SqlQueryStatement createQuery(QuerySpec query);

    String getSelectStatement();
}
