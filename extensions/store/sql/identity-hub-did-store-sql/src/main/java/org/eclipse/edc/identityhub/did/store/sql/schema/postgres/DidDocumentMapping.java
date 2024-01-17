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
import org.eclipse.edc.sql.translation.JsonFieldTranslator;
import org.eclipse.edc.sql.translation.TranslationMapping;

/**
 * Provides a mapping from the canonical format to SQL column names for a {@link org.eclipse.edc.iam.did.spi.document.DidDocument}
 */
public class DidDocumentMapping extends TranslationMapping {

    public static final String FIELD_ID = "id";
    public static final String FIELD_SERVICE = "service";
    public static final String FIELD_VERIFICATION_METHOD = "verificationMethod";
    public static final String FIELD_AUTHENTICATION = "authentication";

    public DidDocumentMapping(DidResourceStatements statements) {
        add(FIELD_ID, statements.getIdColumn());
        add(FIELD_SERVICE, new JsonFieldTranslator(FIELD_SERVICE));
        add(FIELD_VERIFICATION_METHOD, new JsonFieldTranslator(FIELD_VERIFICATION_METHOD));
        add(FIELD_AUTHENTICATION, FIELD_AUTHENTICATION);
    }
}
