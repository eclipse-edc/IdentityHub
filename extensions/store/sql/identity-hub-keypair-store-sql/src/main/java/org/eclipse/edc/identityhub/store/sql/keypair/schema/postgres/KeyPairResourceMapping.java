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

package org.eclipse.edc.identityhub.store.sql.keypair.schema.postgres;

import org.eclipse.edc.identityhub.store.sql.keypair.BaseSqlDialectStatements;
import org.eclipse.edc.sql.translation.TranslationMapping;

public class KeyPairResourceMapping extends TranslationMapping {
    public KeyPairResourceMapping(BaseSqlDialectStatements stmt) {
        add("id", stmt.getIdColumn());
        add("participantContextId", stmt.getParticipantIdColumn());
        add("timestamp", stmt.getTimestampColumn());
        add("keyId", stmt.getKeyIdColumn());
        add("groupName", stmt.getGroupNameColumn());
        add("isDefaultPair", stmt.getIsDefaultKeyPairColumn());
        add("useDuration", stmt.getUseDurationColumn());
        add("rotationDuration", stmt.getRotationDurationColumn());
        add("serializedPublicKey", stmt.getSerializedPublicKeyColumn());
        add("privateKeyAlias", stmt.getPrivateKeyAliasColumn());
        add("state", stmt.getStateColumn());
    }
}
