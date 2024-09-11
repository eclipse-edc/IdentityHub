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

package org.eclipse.edc.identityhub.keypairs;

import org.eclipse.edc.identityhub.spi.keypair.KeyPairService;
import org.eclipse.edc.identityhub.spi.keypair.events.KeyPairObservable;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairResource;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairState;
import org.eclipse.edc.identityhub.spi.participantcontext.events.ParticipantContextCreated;
import org.eclipse.edc.identityhub.spi.participantcontext.events.ParticipantContextDeleted;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantResource;
import org.eclipse.edc.identityhub.spi.store.KeyPairResourceStore;
import org.eclipse.edc.security.token.jwt.CryptoConverter;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.AbstractResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class KeyPairServiceImpl implements KeyPairService, EventSubscriber {
    private final KeyPairResourceStore keyPairResourceStore;
    private final Vault vault;
    private final Monitor monitor;
    private final KeyPairObservable observable;
    private final TransactionContext transactionContext;

    public KeyPairServiceImpl(KeyPairResourceStore keyPairResourceStore, Vault vault, Monitor monitor, KeyPairObservable observable, TransactionContext transactionContext) {
        this.keyPairResourceStore = keyPairResourceStore;
        this.vault = vault;
        this.monitor = monitor;
        this.observable = observable;
        this.transactionContext = transactionContext;
    }

    @Override
    public ServiceResult<Void> addKeyPair(String participantId, KeyDescriptor keyDescriptor, boolean makeDefault) {

        return transactionContext.execute(() -> {
            var key = generateOrGetKey(keyDescriptor);
            if (key.failed()) {
                return ServiceResult.badRequest(key.getFailureDetail());
            }

            // check if the new key is not active, and no other active key exists
            if (!keyDescriptor.isActive()) {

                //todo: replace this with invocation to activateKeyPair()
                var hasActiveKeys = keyPairResourceStore.query(ParticipantResource.queryByParticipantId(participantId).build())
                        .orElse(failure -> Collections.emptySet())
                        .stream().filter(kpr -> kpr.getState() == KeyPairState.ACTIVATED.code())
                        .findAny()
                        .isEmpty();

                if (!hasActiveKeys) {
                    monitor.warning("Participant '%s' has no active key pairs, and adding an inactive one will prevent the participant from becoming operational.");
                }
            }

            var newResource = KeyPairResource.Builder.newInstance()
                    .id(keyDescriptor.getResourceId())
                    .keyId(keyDescriptor.getKeyId())
                    .state(keyDescriptor.isActive() ? KeyPairState.ACTIVATED : KeyPairState.CREATED)
                    .isDefaultPair(makeDefault)
                    .privateKeyAlias(keyDescriptor.getPrivateKeyAlias())
                    .serializedPublicKey(key.getContent())
                    .timestamp(Instant.now().toEpochMilli())
                    .participantId(participantId)
                    .keyContext(keyDescriptor.getType())
                    .build();

            return ServiceResult.from(keyPairResourceStore.create(newResource))
                    .onSuccess(v -> observable.invokeForEach(l -> l.added(newResource, keyDescriptor.getType())))
                    .compose(v -> {
                        if (keyDescriptor.isActive()) {
                            return activateKeyPair(newResource);
                        }
                        return ServiceResult.success();
                    });
        });
    }

    @Override
    public ServiceResult<Void> rotateKeyPair(String oldId, @Nullable KeyDescriptor newKeySpec, long duration) {
        return transactionContext.execute(() -> {
            var oldKey = findById(oldId);
            if (oldKey == null) {
                return ServiceResult.notFound("A KeyPairResource with ID '%s' does not exist.".formatted(oldId));
            }

            var participantId = oldKey.getParticipantId();
            boolean wasDefault = oldKey.isDefaultPair();

            // deactivate the old key
            var oldAlias = oldKey.getPrivateKeyAlias();
            vault.deleteSecret(oldAlias);
            oldKey.rotate(duration);
            var updateResult = ServiceResult.from(keyPairResourceStore.update(oldKey))
                    .onSuccess(v -> observable.invokeForEach(l -> l.rotated(oldKey)));

            if (newKeySpec != null) {
                return updateResult.compose(v -> addKeyPair(participantId, newKeySpec, wasDefault));
            }
            monitor.warning("Rotating keys without a successor key may leave the participant without an active keypair.");
            return updateResult;
        });
    }

    @Override
    public ServiceResult<Void> revokeKey(String id, @Nullable KeyDescriptor newKeySpec) {
        return transactionContext.execute(() -> {
            var oldKey = findById(id);
            if (oldKey == null) {
                return ServiceResult.notFound("A KeyPairResource with ID '%s' does not exist.".formatted(id));
            }

            var participantId = oldKey.getParticipantId();
            boolean wasDefault = oldKey.isDefaultPair();

            // deactivate the old key
            var oldAlias = oldKey.getPrivateKeyAlias();
            vault.deleteSecret(oldAlias);
            oldKey.revoke();
            var updateResult = ServiceResult.from(keyPairResourceStore.update(oldKey))
                    .onSuccess(v -> observable.invokeForEach(l -> l.revoked(oldKey)));

            if (newKeySpec != null) {
                return updateResult.compose(v -> addKeyPair(participantId, newKeySpec, wasDefault));
            }
            monitor.warning("Revoking keys without a successor key may leave the participant without an active keypair.");
            return updateResult;
        });
    }

    @Override
    public ServiceResult<Collection<KeyPairResource>> query(QuerySpec querySpec) {
        return ServiceResult.from(keyPairResourceStore.query(querySpec));
    }

    @Override
    public ServiceResult<Void> activate(String keyPairResourceId) {
        return transactionContext.execute(() -> {
            var existingKeyPair = findById(keyPairResourceId);
            if (existingKeyPair == null) {
                return ServiceResult.notFound("A KeyPairResource with ID '%s' does not exist.".formatted(keyPairResourceId));
            }

            return activateKeyPair(existingKeyPair);
        });
    }

    @Override
    public <E extends Event> void on(EventEnvelope<E> eventEnvelope) {
        var payload = eventEnvelope.getPayload();
        if (payload instanceof ParticipantContextDeleted deleted) {
            deleted(deleted);
        } else {
            monitor.warning("Received event with unexpected payload type: %s".formatted(payload.getClass()));
        }
    }

    private @NotNull ServiceResult<Void> activateKeyPair(KeyPairResource existingKeyPair) {
        var allowedStates = List.of(KeyPairState.ACTIVATED.code(), KeyPairState.CREATED.code());
        if (!allowedStates.contains(existingKeyPair.getState())) {
            return ServiceResult.badRequest("The key pair resource is expected to be in %s, but was %s".formatted(allowedStates, existingKeyPair.getState()));
        }
        existingKeyPair.activate();

        return ServiceResult.from(keyPairResourceStore.update(existingKeyPair)
                .onSuccess(u -> observable.invokeForEach(l -> l.activated(existingKeyPair, existingKeyPair.getKeyContext()))));
    }

    private void created(ParticipantContextCreated event) {
        addKeyPair(event.getParticipantId(), event.getManifest().getKey(), true)
                .onFailure(f -> monitor.warning("Adding the key pair to a new ParticipantContext failed: %s".formatted(f.getFailureDetail())));
    }

    private void deleted(ParticipantContextDeleted event) {
        //hard-delete all keypairs that are associated with the deleted participant
        var query = ParticipantResource.queryByParticipantId(event.getParticipantId()).build();
        transactionContext.execute(() -> {
            keyPairResourceStore.query(query)
                    .compose(list -> {
                        var errors = list.stream()
                                .map(r -> keyPairResourceStore.deleteById(r.getId()))
                                .filter(StoreResult::failed)
                                .map(AbstractResult::getFailureDetail)
                                .collect(Collectors.joining(","));

                        if (errors.isEmpty()) {
                            return StoreResult.success();
                        }
                        return StoreResult.generalError("An error occurred when deleting KeyPairResources: %s".formatted(errors));
                    })
                    .onFailure(f -> monitor.warning("Removing key pairs from a deleted ParticipantContext failed: %s".formatted(f.getFailureDetail())));
        });
    }

    private KeyPairResource findById(String oldId) {
        var q = QuerySpec.Builder.newInstance()
                .filter(new Criterion("id", "=", oldId)).build();
        return keyPairResourceStore.query(q).map(list -> list.stream().findFirst().orElse(null)).orElse(f -> null);
    }

    private Result<String> generateOrGetKey(KeyDescriptor keyDescriptor) {
        String publicKeySerialized;
        if (keyDescriptor.getKeyGeneratorParams() != null) {
            var keyPair = KeyPairGenerator.generateKeyPair(keyDescriptor.getKeyGeneratorParams());
            if (keyPair.failed()) {
                return keyPair.mapFailure();
            }
            var privateJwk = CryptoConverter.createJwk(keyPair.getContent(), keyDescriptor.getKeyId());
            publicKeySerialized = privateJwk.toPublicJWK().toJSONString();
            vault.storeSecret(keyDescriptor.getPrivateKeyAlias(), privateJwk.toJSONString());
        } else {
            // either take the public key from the JWK structure or the PEM field
            publicKeySerialized = Optional.ofNullable(keyDescriptor.getPublicKeyJwk())
                    .map(m -> CryptoConverter.create(m).toJSONString())
                    .orElseGet(() -> keyDescriptor.getPublicKeyPem().replace("\\n", "\n"));
        }
        return Result.success(publicKeySerialized);
    }
}
