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

package org.eclipse.edc.identityhub.spi.verifiablecredentials.generator;

import org.eclipse.edc.spi.result.ServiceResult;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Creates a {@code VerifiableCredentialResource} in the database
 * after a <a href="https://eclipse-dataspace-dcp.github.io/decentralized-claims-protocol/HEAD/#credential-message">CredentialMessage</a>
 * was received. Credentials can be in several formats, thus the {@link CredentialWriter} uses delegate credential parsers to extract metadata.
 */
public interface CredentialWriter {
    /**
     * Writes a credential object to storage received by an Issuer when issuing credentials
     *
     * @param holderPid            identifies the Holder's credential request the credentials were issued for
     * @param holderDid            the DID of the Holder the credentials must be bound to
     * @param issuerPid            the issuance process ID as reported by the Issuer
     * @param issuerDid            the DID of the Issuer that delivered the credentials, as authenticated from its Self-Issued ID token
     * @param credentials          the credentials to store
     * @param participantContextId the participant context the credentials belong to
     */
    ServiceResult<Void> write(String holderPid, String holderDid, String issuerPid, String issuerDid, Collection<CredentialWriteRequest> credentials, String participantContextId);

    /**
     * Records that an Issuer rejected a credential request. The request is failed, so the Holder stops waiting for
     * credentials that will never arrive. Nothing is stored.
     *
     * @param holderPid            identifies the Holder's credential request that was rejected
     * @param issuerPid            the issuance process ID as reported by the Issuer
     * @param issuerDid            the DID of the Issuer that reported the rejection, as authenticated from its Self-Issued ID token
     * @param rejectionReason      why the Issuer rejected the request, or {@code null} if it did not say
     * @param participantContextId the participant context the request belongs to
     */
    ServiceResult<Void> reject(String holderPid, String issuerPid, String issuerDid, @Nullable String rejectionReason, String participantContextId);
}
