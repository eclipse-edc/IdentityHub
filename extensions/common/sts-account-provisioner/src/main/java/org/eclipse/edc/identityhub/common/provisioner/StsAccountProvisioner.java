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
import org.eclipse.edc.identityhub.spi.keypair.events.KeyPairEvent;
import org.eclipse.edc.identityhub.spi.keypair.events.KeyPairRevoked;
import org.eclipse.edc.identityhub.spi.keypair.events.KeyPairRotated;
import org.eclipse.edc.identityhub.spi.participantcontext.AccountInfo;
import org.eclipse.edc.identityhub.spi.participantcontext.AccountProvisioner;
import org.eclipse.edc.identityhub.spi.participantcontext.events.ParticipantContextDeleted;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.security.Vault;

public class StsAccountProvisioner implements EventSubscriber, AccountProvisioner {

    private final Monitor monitor;
    private final StsClientStore stsClientStore;
    private final Vault vault;
    private final StsClientSecretGenerator stsClientSecretGenerator;

    public StsAccountProvisioner(Monitor monitor,
                                 StsClientStore stsClientStore,
                                 Vault vault,
                                 StsClientSecretGenerator stsClientSecretGenerator) {
        this.monitor = monitor;
        this.stsClientStore = stsClientStore;
        this.vault = vault;
        this.stsClientSecretGenerator = stsClientSecretGenerator;
    }

    @Override
    public <E extends Event> void on(EventEnvelope<E> event) {
        var payload = event.getPayload();
        ServiceResult<Void> result;
        if (payload instanceof ParticipantContextDeleted deletedEvent) {
            result = deleteAccount(deletedEvent.getParticipantId());
        } else if (payload instanceof KeyPairRevoked || payload instanceof KeyPairRotated) {
            result = setKeyAliases(((KeyPairEvent) payload).getParticipantId(), "", "");
        } else {
            result = ServiceResult.badRequest("Received event with unexpected payload type: %s".formatted(payload.getClass()));
        }

        result.onFailure(f -> monitor.warning(f.getFailureDetail()));
    }

    @Override
    public ServiceResult<AccountInfo> create(ParticipantManifest manifest) {
        return ServiceResult.from(createAccount(manifest));
    }

    private ServiceResult<Void> deleteAccount(String participantId) {
        var result = stsClientStore.deleteById(participantId);
        return ServiceResult.from(result).mapEmpty();
    }

    private ServiceResult<Void> setKeyAliases(String participantId, String privateKeyAlias, String publicKeyReference) {
        return ServiceResult.from(stsClientStore.findById(participantId)
                .compose(stsClient -> {
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
                }));
    }

    private Result<AccountInfo> createAccount(ParticipantManifest manifest) {
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
    }
}
