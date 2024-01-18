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

import org.eclipse.edc.identityhub.did.store.sql.DidResourceStatements;
import org.eclipse.edc.sql.translation.TranslationMapping;


/**
 * Provides a mapping from the canonical format to SQL column names for a {@link org.eclipse.edc.identithub.did.spi.model.DidResource}
 */
public class DidResourceMapping extends TranslationMapping {

    public static final String FIELD_DID = "did";
    public static final String FIELD_STATE = "state";
    public static final String FIELD_CREATE_TIMESTAMP = "create_timestamp";
    public static final String FIELD_STATE_TIMESTAMP = "state_timestamp";
    public static final String FIELD_DOCUMENT = "document";

    public DidResourceMapping(DidResourceStatements statements) {
        add(FIELD_DID, statements.getIdColumn());
        add(FIELD_STATE, statements.getStateColumn());
        add(FIELD_CREATE_TIMESTAMP, statements.getCreateTimestampColumn());
        add(FIELD_STATE_TIMESTAMP, statements.getStateTimestampColumn());
        add(FIELD_DOCUMENT, new DidDocumentMapping(statements));
    }
}