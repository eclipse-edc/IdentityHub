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

package org.eclipse.edc.issuerservice.spi.credentials;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.issuerservice.spi.credentials.statuslist.StatusListInfo;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Service to revoke, suspend, resume and query the status of VerifiableCredentials. This is agnostic of the status list
 * implementation, as it delegates down to {@link StatusListInfo} objects that handle the concrete status list implementation.
 * This service handles various operations on a high level.
 */
public interface CredentialStatusService {

    /**
     * Adds a newly created credential to the status list credential. This should be done when the issuer service issues a new credential.
     * This method will modify the credential by adding a {@code credentialStatus} object to it. This means that callers
     * <em>must</em> re-load the credential from storage after this method, before delivering it to the holder!
     *
     * @param participantContextId The ID of the participant context (=tenant) within the IssuerService
     * @param credential           The credential, which should be added to the status list credential. Will be modified.
     * @return The modified verifiable credential, including the credentialStatus entry.
     */
    ServiceResult<VerifiableCredential> addCredential(String participantContextId, VerifiableCredential credential);

    /**
     * Revokes a credential by adding its ID to the revocation list credential. Implementations may choose to also track
     * the status in the internal database. This operation is irreversible.
     * <p>
     * Note that the specific revocation credential is determined by inspecting the user credentials
     * {@code credentialSubject.statusListCredential} field.
     *
     * @param credentialId The ID of the credential.
     * @return a service result indicating success or failure
     */
    ServiceResult<Void> revokeCredential(String credentialId);

    /**
     * Suspends a credential by adding its ID to the revocation list credential. Implementations may choose to also track
     * the status in the internal database
     *
     * @param credentialId The ID of the credential.
     * @param reason       An optional string indicating the reason for suspension, e.g. "temporary account suspension", etc.
     * @return a service result indicating success or failure
     * @throws UnsupportedOperationException if this credential status service does not support suspension
     */
    ServiceResult<Void> suspendCredential(String credentialId, @Nullable String reason);

    /**
     * Removes the "suspended" state from the revocation credential.
     *
     * @param credentialId The ID of the credential.
     * @param reason       An optional string indicating the reason for resuming, e.g. "account reactivation", etc.
     * @return a service result indicating success or failure
     * @throws UnsupportedOperationException if this credential status service does not support suspension/resuming
     */
    ServiceResult<Void> resumeCredential(String credentialId, @Nullable String reason);

    /**
     * Obtains the status for a given credential. This is done by parsing the StatusList credential, decoding the bitstring
     * and interpreting the status purpose.
     * <p>
     * Alternatively, users can inspect {@code VerifiableCredentialResource#getState()}
     *
     * @param credentialId The ID of the credential.
     * @return A string containing the credential status, null if the status is not set, or a failure to indicate an error.
     */
    ServiceResult<String> getCredentialStatus(String credentialId);

    ServiceResult<Collection<VerifiableCredentialResource>> queryCredentials(QuerySpec query);

    /**
     * Obtains a single credential by its ID.
     *
     * @param credentialId The ID of the credential.
     * @return The credential resource, or a failure if the credential does not exist.
     */
    ServiceResult<VerifiableCredentialResource> getCredentialById(String credentialId);
}
