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

import java.util.Collection;

/**
 * Creates a {@code VerifiableCredentialResource} in the database
 * after a <a href="https://eclipse-dataspace-dcp.github.io/decentralized-claims-protocol/HEAD/#credential-message">CredentialMessage</a>
 * was received. Credentials can be in several formats, thus the {@link CredentialWriter} uses delegate credential parsers to extract metadata.
 */
@FunctionalInterface
public interface CredentialWriter {
    /**
     * @param holderPid            identifies the Holder's credential request the credentials were issued for
     * @param holderDid            the DID of the Holder the credentials must be bound to
     * @param issuerPid            the issuance process ID as reported by the Issuer
     * @param issuerDid            the DID of the Issuer that delivered the credentials, as authenticated from its Self-Issued ID token
     * @param credentials          the credentials to store
     * @param participantContextId the participant context the credentials belong to
     */
    ServiceResult<Void> write(String holderPid, String holderDid, String issuerPid, String issuerDid, Collection<CredentialWriteRequest> credentials, String participantContextId);
}
