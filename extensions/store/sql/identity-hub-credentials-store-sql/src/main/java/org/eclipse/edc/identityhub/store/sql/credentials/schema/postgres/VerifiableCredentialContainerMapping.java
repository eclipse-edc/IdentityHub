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

package org.eclipse.edc.identityhub.store.sql.credentials.schema.postgres;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.identityhub.store.sql.credentials.CredentialStoreStatements;
import org.eclipse.edc.sql.translation.TranslationMapping;

/**
 * Maps the canonical format of a {@link VerifiableCredentialContainer} to SQL column names
 */
public class VerifiableCredentialContainerMapping extends TranslationMapping {
    public VerifiableCredentialContainerMapping(CredentialStoreStatements statements) {
        add("rawVc", statements.getRawVcColumn());
        add("format", statements.getVcFormatColumn());
        add("credential", new CredentialJsonMapping("verifiable_credential"));
    }

}
