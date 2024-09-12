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
import org.eclipse.edc.identithub.spi.did.DidDocumentService;
import org.eclipse.edc.identithub.spi.did.events.DidDocumentPublished;
import org.eclipse.edc.identithub.spi.did.model.DidResource;
import org.eclipse.edc.identityhub.spi.keypair.KeyPairService;
import org.eclipse.edc.identityhub.spi.keypair.events.KeyPairEvent;
import org.eclipse.edc.identityhub.spi.keypair.events.KeyPairRevoked;
import org.eclipse.edc.identityhub.spi.keypair.events.KeyPairRotated;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairResource;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairState;
import org.eclipse.edc.identityhub.spi.participantcontext.events.ParticipantContextCreated;
import org.eclipse.edc.identityhub.spi.participantcontext.events.ParticipantContextDeleted;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantResource;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static java.util.Optional.ofNullable;

public class StsAccountProvisioner implements EventSubscriber {

    private final Monitor monitor;
    private final KeyPairService keyPairService;
    private final DidDocumentService didDocumentService;
    private final StsClientStore stsClientStore;
    private final Vault vault;

    public StsAccountProvisioner(Monitor monitor, KeyPairService keyPairService, DidDocumentService didDocumentService, StsClientStore stsClientStore, Vault vault) {
        this.monitor = monitor;
        this.keyPairService = keyPairService;
        this.didDocumentService = didDocumentService;
        this.stsClientStore = stsClientStore;
        this.vault = vault;
    }

    @Override
    public <E extends Event> void on(EventEnvelope<E> event) {
        var payload = event.getPayload();
        Result<Void> result;
        if (payload instanceof ParticipantContextCreated createdEvent) {
            result = createAccount(createdEvent.getManifest());
        } else if (payload instanceof ParticipantContextDeleted deletedEvent) {
            result = deleteAccount(deletedEvent.getParticipantId());
        } else if (payload instanceof KeyPairRevoked || payload instanceof KeyPairRotated) {
            result = setKeyAliases(((KeyPairEvent) payload).getParticipantId(), null, null);
        } else if (payload instanceof DidDocumentPublished didDocumentPublished) {
            result = didDocumentPublished(didDocumentPublished);
        } else {
            result = Result.failure("Received event with unexpected payload type: %s".formatted(payload.getClass()));
        }

        result.onFailure(f -> monitor.warning(f.getFailureDetail()));
    }

    private Result<Void> deleteAccount(String participantId) {
        return Result.failure("Deleting StsClients is not yet implemented");
    }

    private @NotNull Result<Void> didDocumentPublished(DidDocumentPublished didDocumentPublished) {
        Result<Void> result;
        var participantId = didDocumentPublished.getParticipantId();
        result = getDefaultKeyPair(participantId)
                .map(kpr -> {
                    var alias = kpr.getPrivateKeyAlias();
                    var publicKeyReference = getVerificationMethodWithId(didDocumentPublished.getDid(), kpr.getKeyId());
                    return setKeyAliases(participantId, alias, publicKeyReference);
                })
                .orElse(Result.failure("No default keypair found for participant " + participantId));
        return result;
    }

    private String getVerificationMethodWithId(String did, String keyId) {
        return ofNullable(didDocumentService.findById(did))
                .map(DidResource::getDocument).flatMap(dd -> dd.getVerificationMethod()
                        .stream()
                        .filter(vm -> vm.getId().endsWith(keyId)).findFirst())
                .map(vm -> {
                    if (vm.getController() != null && !vm.getId().startsWith(vm.getController())) {
                        return vm.getController() + "#" + vm.getId();
                    }
                    return vm.getId();
                }).orElse(null);
    }

    private Optional<KeyPairResource> getDefaultKeyPair(String participantId) {
        return keyPairService.query(ParticipantResource.queryByParticipantId(participantId).build())
                .orElse(failure -> Collections.emptySet())
                .stream()
                .filter(kpr -> kpr.getState() == KeyPairState.ACTIVATED.code())
                .filter(KeyPairResource::isDefaultPair)
                .findAny();
    }

    private Result<Void> setKeyAliases(String participantId, String privateKeyAlias, String publicKeyReference) {
        return Result.failure("Updating StsClients is not yet implemented");
    }

    private Result<Void> createAccount(ParticipantManifest manifest) {
        var secretAlias = UUID.randomUUID().toString();

        //todo: generate random password and store in vault!

        var client = StsClient.Builder.newInstance()
                .id(manifest.getParticipantId())
                .name(manifest.getParticipantId())
                .clientId(manifest.getDid())
                .did(manifest.getDid())
                .privateKeyAlias(manifest.getKey().getPrivateKeyAlias())
                .publicKeyReference(manifest.getKey().getKeyId())
                .secretAlias(secretAlias)
                .build();
        var createResult = stsClientStore.create(client);
        return createResult.succeeded() ? Result.success() : Result.failure(createResult.getFailureDetail());
    }
}
