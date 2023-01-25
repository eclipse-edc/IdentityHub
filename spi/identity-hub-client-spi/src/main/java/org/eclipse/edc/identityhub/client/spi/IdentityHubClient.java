/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.client.spi;

import org.eclipse.edc.identityhub.spi.credentials.model.CredentialEnvelope;
import org.eclipse.edc.spi.result.Result;

import java.util.Collection;

/**
 * This client is used to call the IdentityHub endpoints in order query and write VerifiableCredentials.
 * Eventually, this may be expanded to handle other types of objects and operations.
 */
public interface IdentityHubClient {

    /**
     * Get VerifiableCredentials provided by an Identity Hub instance.
     *
     * @param hubBaseUrl Base URL of the IdentityHub instance.
     * @return result containing VerifiableCredentials if request successful.
     */
    Result<Collection<CredentialEnvelope>> getVerifiableCredentials(String hubBaseUrl);

    /**
     * Write a VerifiableCredential.
     *
     * @param hubBaseUrl           Base URL of the IdentityHub instance.
     * @param verifiableCredential A verifiable credential to be saved.
     * @return result.
     */
    Result<Void> addVerifiableCredential(String hubBaseUrl, CredentialEnvelope verifiableCredential);
}
