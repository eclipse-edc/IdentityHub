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

import org.eclipse.edc.identityhub.did.store.sql.CredentialStoreStatements;
import org.eclipse.edc.sql.translation.TranslationMapping;

public class VerifiableCredentialContainerMapping extends TranslationMapping {
    public VerifiableCredentialContainerMapping(CredentialStoreStatements statements) {
        add("rawVc", statements.getRawVcColumn());
        add("format", statements.getVcFormatColumn());
        add("credential", new CredentialJsonMapping("verifiable_credential"));
    }

}
