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
import org.eclipse.edc.iam.identitytrust.sts.spi.service.StsAccountService;
import org.eclipse.edc.iam.identitytrust.sts.spi.service.StsClientSecretGenerator;
import org.eclipse.edc.iam.identitytrust.sts.spi.store.StsAccountStore;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.eclipse.edc.spi.result.ServiceResult.from;
import static org.eclipse.edc.spi.result.ServiceResult.fromFailure;
import static org.eclipse.edc.spi.result.ServiceResult.success;
import static org.eclipse.edc.spi.result.ServiceResult.unauthorized;
import static org.eclipse.edc.spi.result.ServiceResult.unexpected;

/**
 * Manages {@link StsAccount} objects by directly interacting with a (local) storage. This is useful if the STS is directly
 * embedded into IdentityHub.
 */
public class StsAccountServiceImpl implements StsAccountService {

    private final StsAccountStore stsAccountStore;
    private final TransactionContext transactionContext;
    private final Vault vault;
    private final StsClientSecretGenerator secretGenerator;

    public StsAccountServiceImpl(StsAccountStore accountStore, TransactionContext transactionContext, Vault vault, StsClientSecretGenerator secretGenerator) {
        this.stsAccountStore = accountStore;
        this.transactionContext = transactionContext;
        this.vault = vault;
        this.secretGenerator = secretGenerator;
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

            return from(stsAccountStore.create(client).mapEmpty());
        });
    }

    @Override
    public ServiceResult<Void> deleteAccount(String id) {
        return transactionContext.execute(() -> from(stsAccountStore.deleteById(id)).compose(acct -> from(vault.deleteSecret(acct.getSecretAlias()))));
    }

    @Override
    public ServiceResult<Void> updateAccount(StsAccount updatedAccount) {
        return transactionContext.execute(() -> from(stsAccountStore.update(updatedAccount)));
    }

    @Override
    public ServiceResult<StsAccount> findById(String id) {
        return from(stsAccountStore.findById(id));
    }

    @Override
    public Collection<StsAccount> queryAccounts(QuerySpec querySpec) {
        return transactionContext.execute(() -> stsAccountStore.findAll(querySpec).toList());
    }

    @Override
    public ServiceResult<StsAccount> authenticate(StsAccount client, String secret) {
        return ofNullable(vault.resolveSecret(client.getSecretAlias()))
                .filter(vaultSecret -> vaultSecret.equals(secret))
                .map(s -> success(client))
                .orElseGet(() -> unauthorized(format("Failed to authenticate client with id %s", client.getId())));
    }

    @Override
    public ServiceResult<String> updateSecret(String id, String newSecretAlias, @Nullable String newSecret) {

        Objects.requireNonNull(newSecretAlias, "Secret alias cannot be null");

        var oldAlias = new AtomicReference<String>();
        // generate new secret if needed
        newSecret = ofNullable(newSecret).orElseGet(() -> secretGenerator.generateClientSecret(null));

        var updateResult = transactionContext.execute(() -> stsAccountStore.findById(id)
                .compose(stsAccount -> {
                    oldAlias.set(stsAccount.getSecretAlias());
                    stsAccount.updateSecretAlias(newSecretAlias);
                    return stsAccountStore.update(stsAccount);
                }));

        if (updateResult.succeeded()) {
            var oldSecretAlias = oldAlias.get();
            Result<Void> vaultInteractionResult = Result.success();

            if (!oldSecretAlias.equals(newSecretAlias)) {
                vaultInteractionResult = vaultInteractionResult.merge(vault.deleteSecret(oldSecretAlias));
            }

            var finalNewSecret = newSecret;
            vaultInteractionResult = vaultInteractionResult.compose(v -> vault.storeSecret(newSecretAlias, finalNewSecret));
            return vaultInteractionResult.succeeded()
                    ? success(newSecretAlias)
                    : unexpected(vaultInteractionResult.getFailureDetail());
        }
        return fromFailure(updateResult);
    }

    @Override
    public ServiceResult<StsAccount> findByClientId(String clientId) {
        return transactionContext.execute(() -> from(stsAccountStore.findByClientId(clientId)));
    }

}
