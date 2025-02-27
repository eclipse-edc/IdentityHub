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
    ServiceResult<Void> write(String holderPid, String issuerPid, Collection<CredentialWriteRequest> credentials, String participantContextId);
}
