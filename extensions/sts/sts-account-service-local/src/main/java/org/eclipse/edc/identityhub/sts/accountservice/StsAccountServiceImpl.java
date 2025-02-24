/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.sts.accountservice;

import org.eclipse.edc.iam.identitytrust.sts.spi.model.StsAccount;
import org.eclipse.edc.iam.identitytrust.sts.spi.store.StsAccountStore;
import org.eclipse.edc.identityhub.spi.participantcontext.StsAccountService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transaction.spi.TransactionContext;

/**
 * Manages {@link StsAccount} objects by directly interacting with a (local) storage. This is useful if the STS is directly
 * embedded into IdentityHub.
 */
class StsAccountServiceImpl implements StsAccountService {

    private final StsAccountStore stsAccountStore;
    private final TransactionContext transactionContext;

    StsAccountServiceImpl(StsAccountStore accountStore, TransactionContext transactionContext) {
        this.stsAccountStore = accountStore;
        this.transactionContext = transactionContext;
    }

    @Override
    public ServiceResult<Void> createAccount(ParticipantManifest manifest, String secretAlias) {
        return transactionContext.execute(() -> {

            var client = StsAccount.Builder.newInstance()
                    .id(manifest.getParticipantId())
                    .name(manifest.getParticipantId())
                    .clientId(manifest.getDid())
                    .did(manifest.getDid())
                    .privateKeyAlias(manifest.getKey().getPrivateKeyAlias())
                    .publicKeyReference(manifest.getKey().getKeyId())
                    .secretAlias(secretAlias)
                    .build();

            return ServiceResult.from(stsAccountStore.create(client).mapEmpty());
        });
    }

    @Override
    public ServiceResult<Void> deleteAccount(String id) {
        return transactionContext.execute(() -> ServiceResult.from(stsAccountStore.deleteById(id)).mapEmpty());
    }

    @Override
    public ServiceResult<Void> updateAccount(StsAccount updatedAccount) {
        return transactionContext.execute(() -> ServiceResult.from(stsAccountStore.update(updatedAccount)));
    }

    @Override
    public ServiceResult<StsAccount> findById(String id) {
        return ServiceResult.from(stsAccountStore.findById(id));
    }
}
