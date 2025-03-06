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

package org.eclipse.edc.issuerservice.spi.credentials.statuslist;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialStatus;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;

public interface StatusListCredentialEntry {
    /**
     * Generate a {@link CredentialStatus} object from the status list credential, that can be added to a holder credential
     */
    CredentialStatus createCredentialStatus();

    VerifiableCredentialResource statusListCredential();

    int statusListIndex();

    String credentialUrl();
}

