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

package org.eclipse.edc.identityhub.common.provisioner;

import org.eclipse.edc.iam.identitytrust.sts.spi.model.StsClient;
import org.eclipse.edc.iam.identitytrust.sts.spi.store.StsClientStore;
import org.eclipse.edc.identityhub.spi.keypair.events.KeyPairRevoked;
import org.eclipse.edc.identityhub.spi.keypair.events.KeyPairRotated;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairResource;
import org.eclipse.edc.identityhub.spi.participantcontext.AccountInfo;
import org.eclipse.edc.identityhub.spi.participantcontext.AccountProvisioner;
import org.eclipse.edc.identityhub.spi.participantcontext.events.ParticipantContextDeleted;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * AccountProvisioner, that synchronizes the {@link org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext} object
 * to {@link StsClient} entries. That means, when a participant is created, this provisioner takes care of creating a corresponding
 * {@link StsClient}, if the embedded STS is used.
 * When key pairs are revoked or rotated, the corresponding {@link StsClient} entry is updated.
 */
public class StsAccountProvisioner implements EventSubscriber, AccountProvisioner {

    private final Monitor monitor;
    private final StsClientStore stsClientStore;
    private final Vault vault;
    private final StsClientSecretGenerator stsClientSecretGenerator;
    private final TransactionContext transactionContext;

    public StsAccountProvisioner(Monitor monitor,
                                 StsClientStore stsClientStore,
                                 Vault vault,
                                 StsClientSecretGenerator stsClientSecretGenerator,
                                 TransactionContext transactionContext) {
        this.monitor = monitor;
        this.stsClientStore = stsClientStore;
        this.vault = vault;
        this.stsClientSecretGenerator = stsClientSecretGenerator;
        this.transactionContext = transactionContext;
    }

    @Override
    public <E extends Event> void on(EventEnvelope<E> event) {
        var payload = event.getPayload();
        ServiceResult<Void> result;
        if (payload instanceof ParticipantContextDeleted deletedEvent) {
            result = deleteAccount(deletedEvent.getParticipantId());
        } else if (payload instanceof KeyPairRevoked kpe) {
            result = updateStsClient(kpe.getKeyPairResource(), kpe.getParticipantId(), kpe.getNewKeyDescriptor());
        } else if (payload instanceof KeyPairRotated kpr) {
            result = updateStsClient(kpr.getKeyPairResource(), kpr.getParticipantId(), kpr.getNewKeyDescriptor());
        } else {
            result = ServiceResult.badRequest("Received event with unexpected payload type: %s".formatted(payload.getClass()));
        }

        result.onFailure(f -> monitor.warning(f.getFailureDetail()));
    }

    @Override
    public ServiceResult<AccountInfo> create(ParticipantManifest manifest) {
        return ServiceResult.from(createAccount(manifest));
    }

    private Result<AccountInfo> createAccount(ParticipantManifest manifest) {
        return transactionContext.execute(() -> {
            var secretAlias = manifest.getParticipantId() + "-sts-client-secret";

            var client = StsClient.Builder.newInstance()
                    .id(manifest.getParticipantId())
                    .name(manifest.getParticipantId())
                    .clientId(manifest.getDid())
                    .did(manifest.getDid())
                    .privateKeyAlias(manifest.getKey().getPrivateKeyAlias())
                    .publicKeyReference(manifest.getKey().getKeyId())
                    .secretAlias(secretAlias)
                    .build();

            var createResult = stsClientStore.create(client)
                    .map(stsClient -> {
                        var clientSecret = stsClientSecretGenerator.generateClientSecret(null);
                        return new AccountInfo(stsClient.getClientId(), clientSecret);
                    })
                    .onSuccess(accountInfo -> {
                        // the vault's result does not influence the service result, since that may cause the transaction to roll back,
                        // but vaults aren't transactional resources
                        vault.storeSecret(secretAlias, accountInfo.clientSecret())
                                .onFailure(e -> monitor.severe(e.getFailureDetail()));
                    });

            return createResult.succeeded() ? Result.success(createResult.getContent()) : Result.failure(createResult.getFailureDetail());
        });
    }

    private ServiceResult<Void> updateStsClient(KeyPairResource oldKeyResource, String participantId, @Nullable KeyDescriptor newKeyDescriptor) {
        return transactionContext.execute(() -> {
            var findResult = stsClientStore.findById(participantId);
            if (findResult.failed()) {
                return ServiceResult.from(findResult).mapEmpty();
            }

            var existingClient = findResult.getContent();

            if (Objects.equals(oldKeyResource.getPrivateKeyAlias(), existingClient.getPrivateKeyAlias())) {
                return ServiceResult.success(); // the revoked/rotated key pair does not pertain to this STS Client
            }

            if (newKeyDescriptor == null) {
                // no "successor" key was given, will only reset
                return setKeyAliases(existingClient, "", "");
            }

            var publicKeyRef = newKeyDescriptor.getKeyId();
            // check that key-id contains the DID
            if (!publicKeyRef.startsWith(existingClient.getDid())) {
                publicKeyRef = existingClient.getDid() + "#" + publicKeyRef;
            }
            return setKeyAliases(existingClient, newKeyDescriptor.getPrivateKeyAlias(), publicKeyRef);
        });
    }

    private ServiceResult<Void> deleteAccount(String participantId) {
        var result = transactionContext.execute(() -> stsClientStore.deleteById(participantId));
        return ServiceResult.from(result).mapEmpty();
    }

    private ServiceResult<Void> setKeyAliases(StsClient stsClient, String privateKeyAlias, String publicKeyReference) {
        var updatedClient = transactionContext.execute(() -> {
            var newClient = StsClient.Builder.newInstance()
                    .id(stsClient.getId())
                    .clientId(stsClient.getClientId())
                    .did(stsClient.getDid())
                    .name(stsClient.getName())
                    .secretAlias(stsClient.getSecretAlias())
                    .privateKeyAlias(privateKeyAlias)
                    .publicKeyReference(publicKeyReference)
                    .build();
            return stsClientStore.update(newClient);
        });
        return ServiceResult.from(updatedClient);
    }
}
