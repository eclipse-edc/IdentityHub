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

package org.eclipse.edc.issuerservice.spi.issuance.delivery;


import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcess;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * Client to deliver credentials to a credential service.
 */
@ExtensionPoint
public interface CredentialStorageClient {

    Result<Void> deliverCredentials(IssuanceProcess issuanceProcess, Collection<VerifiableCredentialContainer> credentials);

    /**
     * Delivers the credentials like {@link #deliverCredentials(IssuanceProcess, Collection)}, but without blocking the
     * caller while waiting for the credential service, so that the issuance state machine can go on with other
     * processes. The default implementation delivers synchronously.
     *
     * @return a future containing the result of the delivery, which does not complete exceptionally
     */
    default CompletableFuture<Result<Void>> deliverCredentialsAsync(IssuanceProcess issuanceProcess, Collection<VerifiableCredentialContainer> credentials) {
        return CompletableFuture.completedFuture(deliverCredentials(issuanceProcess, credentials));
    }

    /**
     * Tells the Holder that an issuance it was told had been accepted will not produce any credentials, so it can stop
     * waiting for them. No credentials are sent.
     *
     * @param issuanceProcess the failed issuance process, supplying the pids the Holder correlates on
     * @param rejectionReason why the request was rejected, or {@code null}. Must not disclose anything confidential.
     */
    Result<Void> deliverRejection(IssuanceProcess issuanceProcess, @Nullable String rejectionReason);

    /**
     * Delivers the rejection like {@link #deliverRejection(IssuanceProcess, String)}, but without blocking the caller
     * while waiting for the credential service. The default implementation delivers synchronously.
     *
     * @return a future containing the result of the delivery, which does not complete exceptionally
     */
    default CompletableFuture<Result<Void>> deliverRejectionAsync(IssuanceProcess issuanceProcess, @Nullable String rejectionReason) {
        return CompletableFuture.completedFuture(deliverRejection(issuanceProcess, rejectionReason));
    }
}
