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

package org.eclipse.dataspaceconnector.identityhub.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspaceconnector.spi.response.StatusResult;

import java.util.Collection;

/**
 * IdentityHub Client
 * This client is used to call the IdentityHub endpoints in order query and write VerifiableCredentials, and display the
 * Self-Description document.
 * Eventually, this may be expanded to handle other types of objects and operations.
 */
public interface IdentityHubClient {

    /**
     * Display the Self-Description document.
     *
     * @param hubBaseUrl Base URL of the IdentityHub instance.
     * @return status result containing the Self-Description document if request successful.
     */
    StatusResult<JsonNode> getSelfDescription(String hubBaseUrl);

    /**
     * Get VerifiableCredentials provided by an Identity Hub instance.
     *
     * @param hubBaseUrl Base URL of the IdentityHub instance.
     * @return status result containing VerifiableCredentials if request successful.
     */
    StatusResult<Collection<SignedJWT>> getVerifiableCredentials(String hubBaseUrl);

    /**
     * Write a VerifiableCredential.
     *
     * @param hubBaseUrl           Base URL of the IdentityHub instance.
     * @param verifiableCredential A verifiable credential to be saved.
     * @return status result.
     */
    StatusResult<Void> addVerifiableCredential(String hubBaseUrl, SignedJWT verifiableCredential);

}
