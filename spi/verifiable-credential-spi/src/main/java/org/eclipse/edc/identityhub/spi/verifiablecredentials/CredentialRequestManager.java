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

import org.eclipse.edc.identityhub.spi.credential.request.model.HolderCredentialRequest;
import org.eclipse.edc.identityhub.spi.credential.request.model.RequestedCredential;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.entity.StateEntityManager;
import org.eclipse.edc.spi.result.ServiceResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Makes credential requests to a given issuer and manages the state on the holder side
 */
@ExtensionPoint
public interface CredentialRequestManager extends StateEntityManager {
    /**
     * Service type for entries in a DID document that contain the fully-qualified Credential Request API endpoint
     */
    String ISSUER_SERVICE_ENDPOINT_TYPE = "IssuerService";

    /**
     * Initiates the holder-side credential request by sending the DCP message to the issuer
     *
     * @param participantContextId The Participant Context ID of the requestor
     * @param issuerDid            The DID of the issuer
     * @param holderPid            The holder-defined request ID.
     * @param typesAndFormats      A map containing credential-type - credential-format entries
     * @return A ServiceResult containing the database ID of the {@code HolderCredentialRequest}.
     */
    ServiceResult<String> initiateRequest(String participantContextId, String issuerDid, String holderPid, List<RequestedCredential> typesAndFormats);

    /**
     * Finds a {@link HolderCredentialRequest} for the given participant context, with the given ID
     *
     * @param holderPid The (holder-side) ID of the request. Not to be confused with issuerPid.
     * @return the holder request, or null if not found
     */
    @Nullable HolderCredentialRequest findById(String holderPid);
}
