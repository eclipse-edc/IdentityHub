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

import org.eclipse.edc.identityhub.security.KeyPairGenerator;
import org.eclipse.edc.identityhub.spi.KeyPairService;
import org.eclipse.edc.identityhub.spi.events.keypair.KeyPairObservable;
import org.eclipse.edc.identityhub.spi.events.participant.ParticipantContextCreated;
import org.eclipse.edc.identityhub.spi.events.participant.ParticipantContextDeleted;
import org.eclipse.edc.identityhub.spi.model.KeyPairResource;
import org.eclipse.edc.identityhub.spi.model.KeyPairState;
import org.eclipse.edc.identityhub.spi.model.participant.KeyDescriptor;
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
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

public class KeyPairServiceImpl implements KeyPairService, EventSubscriber {
    private final KeyPairResourceStore keyPairResourceStore;
    private final Vault vault;
    private final Monitor monitor;
    private final KeyPairObservable observable;

    public KeyPairServiceImpl(KeyPairResourceStore keyPairResourceStore, Vault vault, Monitor monitor, KeyPairObservable observable) {
        this.keyPairResourceStore = keyPairResourceStore;
        this.vault = vault;
        this.monitor = monitor;
        this.observable = observable;
    }

    @Override
    public ServiceResult<Void> addKeyPair(String participantId, KeyDescriptor keyDescriptor, boolean makeDefault) {

        var key = generateOrGetKey(keyDescriptor);
        if (key.failed()) {
            return ServiceResult.badRequest(key.getFailureDetail());
        }

        var newResource = KeyPairResource.Builder.newInstance()
                .id(keyDescriptor.getKeyId())
                .keyId(keyDescriptor.getKeyId())
                .state(KeyPairState.CREATED)
                .isDefaultPair(makeDefault)
                .privateKeyAlias(keyDescriptor.getPrivateKeyAlias())
                .serializedPublicKey(key.getContent())
                .timestamp(Instant.now().toEpochMilli())
                .participantId(participantId)
                .build();

        return ServiceResult.from(keyPairResourceStore.create(newResource)).onSuccess(v -> observable.invokeForEach(l -> l.added(newResource)));
    }

    @Override
    public ServiceResult<Void> rotateKeyPair(String oldId, @Nullable KeyDescriptor newKeySpec, long duration) {

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
    }

    @Override
    public ServiceResult<Void> revokeKey(String id, @Nullable KeyDescriptor newKeySpec) {
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
    }

    @Override
    public ServiceResult<Collection<KeyPairResource>> query(QuerySpec querySpec) {
        return ServiceResult.from(keyPairResourceStore.query(querySpec));
    }

    @Override
    public <E extends Event> void on(EventEnvelope<E> eventEnvelope) {
        var payload = eventEnvelope.getPayload();
        if (payload instanceof ParticipantContextCreated created) {
            created(created);
        } else if (payload instanceof ParticipantContextDeleted deleted) {
            deleted(deleted);
        } else {
            monitor.warning("KeyPairServiceImpl Received event with unexpected payload type: %s".formatted(payload.getClass()));
        }
    }

    private void created(ParticipantContextCreated event) {
        addKeyPair(event.getParticipantId(), event.getManifest().getKey(), true)
                .onFailure(f -> monitor.warning("Adding the key pair to a new ParticipantContext failed: %s".formatted(f.getFailureDetail())));
    }

    private void deleted(ParticipantContextDeleted event) {
        //hard-delete all keypairs that are associated with the deleted participant
        var query = QuerySpec.Builder.newInstance().filter(new Criterion("participantId", "=", event.getParticipantId())).build();
        keyPairResourceStore.query(query)
                .compose(list -> {
                    var x = list.stream().map(r -> keyPairResourceStore.deleteById(r.getId()))
                            .filter(StoreResult::failed)
                            .map(AbstractResult::getFailureDetail)
                            .collect(Collectors.joining(","));

                    if (x.isEmpty()) {
                        return StoreResult.success();
                    }
                    // not-found is not necessarily correct, but we only care about the error message
                    return StoreResult.notFound("An error occurred when deleting KeyPairResources: %s".formatted(x));
                })
                .onFailure(f -> monitor.warning("Removing key pairs from a deleted ParticipantContext failed: %s".formatted(f.getFailureDetail())));
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
                return keyPair.mapTo();
            }
            var privateJwk = CryptoConverter.createJwk(keyPair.getContent());
            publicKeySerialized = privateJwk.toPublicJWK().toJSONString();
            vault.storeSecret(keyDescriptor.getPrivateKeyAlias(), privateJwk.toJSONString());
        } else {
            // either take the public key from the JWK structure or the PEM field
            publicKeySerialized = Optional.ofNullable(keyDescriptor.getPublicKeyJwk())
                    .map(m -> CryptoConverter.create(m).toJSONString())
                    .orElseGet(keyDescriptor::getPublicKeyPem);
        }
        return Result.success(publicKeySerialized);
    }

    //    private ServiceResult<JWK> createOrUpdateKey(KeyDescriptor key) {
    //        // do we need to generate the key?
    //        var keyGeneratorParams = key.getKeyGeneratorParams();
    //        JWK publicKeyJwk;
    //        if (keyGeneratorParams != null) {
    //            var kp = KeyPairGenerator.generateKeyPair(keyGeneratorParams);
    //            if (kp.failed()) {
    //                return badRequest("Could not generate KeyPair from generator params: %s".formatted(kp.getFailureDetail()));
    //            }
    //            var alias = key.getPrivateKeyAlias();
    //            var storeResult = vault.storeSecret(alias, CryptoConverter.createJwk(kp.getContent()).toJSONString());
    //            if (storeResult.failed()) {
    //                return badRequest(storeResult.getFailureDetail());
    //            }
    //            publicKeyJwk = CryptoConverter.createJwk(kp.getContent()).toPublicJWK();
    //        } else if (key.getPublicKeyJwk() != null) {
    //            publicKeyJwk = CryptoConverter.create(key.getPublicKeyJwk());
    //        } else if (key.getPublicKeyPem() != null) {
    //            var pubKey = keyParserRegistry.parse(key.getPublicKeyPem());
    //            if (pubKey.failed()) {
    //                return badRequest("Cannot parse public key from PEM: %s".formatted(pubKey.getFailureDetail()));
    //            }
    //            publicKeyJwk = CryptoConverter.createJwk(new KeyPair((PublicKey) pubKey.getContent(), null));
    //        } else {
    //            return badRequest("No public key information found in KeyDescriptor.");
    //        }
    //        // insert the "kid" parameter
    //        var json = publicKeyJwk.toJSONObject();
    //        json.put(JWKParameterNames.KEY_ID, key.getKeyId());
    //        try {
    //            publicKeyJwk = JWK.parse(json);
    //            return success(publicKeyJwk);
    //        } catch (ParseException e) {
    //            return badRequest("Could not create JWK: %s".formatted(e.getMessage()));
    //        }
    //    }
}
