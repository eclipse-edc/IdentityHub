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

import org.eclipse.edc.iam.identitytrust.sts.spi.model.StsAccount;
import org.eclipse.edc.iam.identitytrust.sts.spi.service.StsAccountService;
import org.eclipse.edc.iam.identitytrust.sts.spi.service.StsClientSecretGenerator;
import org.eclipse.edc.identityhub.spi.keypair.events.KeyPairRevoked;
import org.eclipse.edc.identityhub.spi.keypair.events.KeyPairRotated;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairResource;
import org.eclipse.edc.identityhub.spi.participantcontext.AccountInfo;
import org.eclipse.edc.identityhub.spi.participantcontext.StsAccountProvisioner;
import org.eclipse.edc.identityhub.spi.participantcontext.events.ParticipantContextDeleted;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.security.Vault;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * AccountProvisioner, that synchronizes the {@link org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext} object
 * to {@link StsAccount} entries. That means, when a participant is created, this provisioner takes care of creating a corresponding
 * {@link StsAccount}, if the embedded STS is used.
 * When key pairs are revoked or rotated, the corresponding {@link StsAccount} entry is updated.
 */
public class StsAccountProvisionerImpl implements EventSubscriber, StsAccountProvisioner {

    private final Monitor monitor;
    private final Vault vault;
    private final StsClientSecretGenerator stsClientSecretGenerator;
    private final StsAccountService stsAccountService;

    public StsAccountProvisionerImpl(Monitor monitor,
                                     Vault vault,
                                     StsClientSecretGenerator stsClientSecretGenerator,
                                     StsAccountService stsAccountService) {
        this.monitor = monitor;
        this.vault = vault;
        this.stsClientSecretGenerator = stsClientSecretGenerator;
        this.stsAccountService = stsAccountService;
    }

    @Override
    public <E extends Event> void on(EventEnvelope<E> event) {
        var payload = event.getPayload();
        ServiceResult<Void> result;
        if (payload instanceof ParticipantContextDeleted deletedEvent) {
            result = stsAccountService.deleteAccount(deletedEvent.getParticipantContextId());
        } else if (payload instanceof KeyPairRevoked kpe) {
            result = updateStsClient(kpe.getKeyPairResource(), kpe.getParticipantContextId(), kpe.getNewKeyDescriptor());
        } else if (payload instanceof KeyPairRotated kpr) {
            result = updateStsClient(kpr.getKeyPairResource(), kpr.getParticipantContextId(), kpr.getNewKeyDescriptor());
        } else {
            result = ServiceResult.badRequest("Received event with unexpected payload type: %s".formatted(payload.getClass()));
        }

        result.onFailure(f -> monitor.warning(f.getFailureDetail()));
    }

    @Override
    public ServiceResult<AccountInfo> create(ParticipantManifest manifest) {

        var secretAlias = manifest.clientSecretAlias();
        var createResult = stsAccountService.createAccount(manifest, secretAlias)
                .map(v -> stsClientSecretGenerator.generateClientSecret(null))
                .map(secret -> new AccountInfo(manifest.getDid(), secret))
                .onSuccess(accountInfo -> {
                    // the vault's result does not influence the service result, since that may cause the transaction to roll back,
                    // but vaults aren't transactional resources
                    vault.storeSecret(secretAlias, accountInfo.clientSecret())
                            .onFailure(e -> monitor.severe(e.getFailureDetail()));
                });

        return createResult.succeeded() ? ServiceResult.success(createResult.getContent()) : ServiceResult.badRequest(createResult.getFailureDetail());
    }


    private ServiceResult<Void> updateStsClient(KeyPairResource oldKeyResource, String participantId, @Nullable KeyDescriptor newKeyDescriptor) {

        var findResult = stsAccountService.findById(participantId);

        return findResult.compose(existingAccount -> {
            if (Objects.equals(oldKeyResource.getPrivateKeyAlias(), existingAccount.getPrivateKeyAlias())) {
                return ServiceResult.success(); // the revoked/rotated key pair does not belong to this STS Client
            }
            String newAlias = "";
            String newReference = "";

            if (newKeyDescriptor != null) {
                var publicKeyRef = newKeyDescriptor.getKeyId();
                // check that key-id contains the DID
                if (!publicKeyRef.startsWith(existingAccount.getDid())) {
                    publicKeyRef = existingAccount.getDid() + "#" + publicKeyRef;
                }
                newAlias = newKeyDescriptor.getPrivateKeyAlias();
                newReference = publicKeyRef;
            }
            var updatedAccount = setKeyAliases(existingAccount, newAlias, newReference);
            return stsAccountService.updateAccount(updatedAccount);
        });
    }


    private StsAccount setKeyAliases(StsAccount stsClient, String privateKeyAlias, String publicKeyReference) {
        return StsAccount.Builder.newInstance()
                .id(stsClient.getId())
                .clientId(stsClient.getClientId())
                .did(stsClient.getDid())
                .name(stsClient.getName())
                .secretAlias(stsClient.getSecretAlias())
                .privateKeyAlias(privateKeyAlias)
                .publicKeyReference(publicKeyReference)
                .build();
    }
}
