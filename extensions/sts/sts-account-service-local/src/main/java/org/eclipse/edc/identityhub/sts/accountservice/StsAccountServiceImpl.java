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

import org.eclipse.edc.iam.decentralizedclaims.sts.spi.model.StsAccount;
import org.eclipse.edc.iam.decentralizedclaims.sts.spi.service.StsAccountService;
import org.eclipse.edc.iam.decentralizedclaims.sts.spi.service.StsClientSecretGenerator;
import org.eclipse.edc.iam.decentralizedclaims.sts.spi.store.StsAccountStore;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.Nullable;

import java.security.MessageDigest;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Optional.ofNullable;
import static org.eclipse.edc.spi.result.ServiceResult.from;
import static org.eclipse.edc.spi.result.ServiceResult.fromFailure;
import static org.eclipse.edc.spi.result.ServiceResult.notFound;
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
    private final Monitor monitor;

    public StsAccountServiceImpl(StsAccountStore accountStore, TransactionContext transactionContext, Vault vault, StsClientSecretGenerator secretGenerator, Monitor monitor) {
        this.stsAccountStore = accountStore;
        this.transactionContext = transactionContext;
        this.vault = vault;
        this.secretGenerator = secretGenerator;
        this.monitor = monitor;
    }

    @Override
    public ServiceResult<Void> createAccount(ParticipantManifest manifest, String secretAlias) {
        return transactionContext.execute(() -> {

            var client = StsAccount.Builder.newInstance()
                    .id(manifest.getParticipantContextId())
                    .name(manifest.getParticipantContextId())
                    .clientId(manifest.getDid())
                    .did(manifest.getDid())
                    .secretAlias(secretAlias)
                    .participantContextId(manifest.getParticipantContextId())
                    .build();

            return from(stsAccountStore.create(client).mapEmpty());
        });
    }

    @Override
    public ServiceResult<Void> deleteAccount(String participantContextId) {
        return transactionContext.execute(() -> from(stsAccountStore.deleteById(participantContextId)).compose(acct -> from(vault.deleteSecret(participantContextId, acct.getSecretAlias()))));
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
        return ofNullable(vault.resolveSecret(client.getParticipantContextId(), client.getSecretAlias()))
                .filter(vaultSecret -> secret != null && MessageDigest.isEqual(vaultSecret.getBytes(UTF_8), secret.getBytes(UTF_8)))
                .map(s -> success(client))
                .orElseGet(() -> unauthorized(format("Failed to authenticate client with id %s", client.getId())));
    }

    @Override
    public ServiceResult<String> updateSecret(String participantContextId, String id, String newSecretAlias, @Nullable String newSecret) {

        Objects.requireNonNull(newSecretAlias, "Secret alias cannot be null");

        var secret = ofNullable(newSecret).orElseGet(() -> secretGenerator.generateClientSecret(null));
        var oldAlias = new AtomicReference<String>();

        ServiceResult<Void> updateResult = transactionContext.execute(() -> {
            var account = stsAccountStore.findById(id).orElse(f -> null);
            if (account == null || !participantContextId.equals(account.getParticipantContextId())) {
                return notFound(format("An STS account with ID '%s' does not exist.", id));
            }
            oldAlias.set(account.getSecretAlias());

            // the new secret is stored before the account refers to it, so that the account always has a secret
            var storeResult = vault.storeSecret(participantContextId, newSecretAlias, secret);
            if (storeResult.failed()) {
                return unexpected(format("Failed to store the new client secret: %s", storeResult.getFailureDetail()));
            }
            if (newSecretAlias.equals(account.getSecretAlias())) {
                return success();
            }

            account.updateSecretAlias(newSecretAlias);
            var accountUpdateResult = stsAccountStore.update(account);
            if (accountUpdateResult.failed()) {
                // the account still refers to the old secret
                vault.deleteSecret(participantContextId, newSecretAlias);
                return fromFailure(accountUpdateResult);
            }
            return success();
        });

        if (updateResult.failed()) {
            return updateResult.mapFailure();
        }
        // the old secret is only deleted once the account refers to the new one
        if (!newSecretAlias.equals(oldAlias.get())) {
            vault.deleteSecret(participantContextId, oldAlias.get())
                    .onFailure(f -> monitor.warning(format("Failed to delete the previous client secret of STS account '%s'. It must be deleted manually: %s", id, f.getFailureDetail())));
        }
        return success(secret);
    }

    @Override
    public ServiceResult<StsAccount> findByClientId(String clientId) {
        return transactionContext.execute(() -> from(stsAccountStore.findByClientId(clientId)));
    }

}
