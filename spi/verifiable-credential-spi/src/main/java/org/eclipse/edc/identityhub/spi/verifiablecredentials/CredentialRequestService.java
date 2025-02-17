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

package org.eclipse.edc.identityhub.spi.verifiablecredentials;

import org.eclipse.edc.spi.result.ServiceResult;

import java.util.Map;

/**
 * Makes credential requests to a given issuer
 */
public interface CredentialRequestService {
    /**
     * Initiates the holder-side credential request by sending the DCP message to the issuer
     *
     * @param participantContext The Participant Context ID of the requestor
     * @param issuerDid          The DID of the issuer
     * @param requestId          The holder-defined request ID.
     * @param typesAndFormats    A map containing credential-type - credential-format entries
     * @return A ServiceResult containing the issuer-assigned process ID, or a failure otherwise.
     */
    ServiceResult<String> initiateRequest(String participantContext, String issuerDid, String requestId, Map<String, String> typesAndFormats);
}
