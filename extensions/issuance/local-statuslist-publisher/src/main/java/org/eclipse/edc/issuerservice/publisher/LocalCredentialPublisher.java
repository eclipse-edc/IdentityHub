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

package org.eclipse.edc.issuerservice.publisher;

import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.issuerservice.spi.credentials.statuslist.StatusListCredentialPublisher;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.HashMap;

public class LocalCredentialPublisher implements StatusListCredentialPublisher {
    private final CredentialStore credentialStore;
    private final String baseUrl;
    private final TransactionContext transactionContext;

    public LocalCredentialPublisher(CredentialStore credentialStore, String baseUrl, TransactionContext transactionContext) {
        this.credentialStore = credentialStore;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        this.transactionContext = transactionContext;
    }

    @Override
    public Result<String> publish(String participantContextId, String statusListCredentialResourceId) {
        return transactionContext.execute(() -> {
            var res = credentialStore.findById(statusListCredentialResourceId);
            if (res.failed()) {
                return Result.failure(res.getFailureDetail());
            }

            var resource = res.getContent();

            // update resource with URL
            var url = baseUrl + resource.getVerifiableCredential().credential().getId();

            resource = setPublished(resource);
            var updateResult = credentialStore.update(resource);
            if (updateResult.succeeded()) {
                return Result.success(url);
            }
            return Result.failure(updateResult.getFailureDetail());
        });
    }

    @Override
    public boolean canHandle(String participantContextId, String statusListCredentialId) {
        return credentialStore.findById(statusListCredentialId)
                .map(res -> !res.getMetadata().isEmpty())
                .orElse(f -> false); // only status list credentials have metadata
    }

    @Override
    public Result<Void> unpublish(String participantContextId, String statusListCredentialId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private VerifiableCredentialResource setPublished(VerifiableCredentialResource resource) {
        var meta = new HashMap<>(resource.getMetadata());
        meta.put("published", Boolean.TRUE);
        return resource.toBuilder().metadata(meta).build();
    }
}
