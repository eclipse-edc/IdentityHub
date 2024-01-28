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
import org.eclipse.edc.identityhub.spi.model.KeyPairResource;
import org.eclipse.edc.identityhub.spi.model.KeyPairState;
import org.eclipse.edc.identityhub.spi.model.participant.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.store.KeyPairResourceStore;
import org.eclipse.edc.security.token.jwt.CryptoConverter;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.security.Vault;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

public class KeyPairServiceImpl implements KeyPairService {
    private final KeyPairResourceStore keyPairResourceStore;
    private final Vault vault;

    public KeyPairServiceImpl(KeyPairResourceStore keyPairResourceStore, Vault vault) {
        this.keyPairResourceStore = keyPairResourceStore;
        this.vault = vault;
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

        return ServiceResult.from(keyPairResourceStore.create(newResource));
    }

    @Override
    public ServiceResult<Void> rotateKeyPair(String oldId, KeyDescriptor newKeySpec, long duration) {
        Objects.requireNonNull(newKeySpec);
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
        keyPairResourceStore.update(oldKey);

        return addKeyPair(participantId, newKeySpec, wasDefault);
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
        keyPairResourceStore.update(oldKey);
        //todo: emit event for the did service, which should update the did document

        if (newKeySpec != null) {
            return addKeyPair(participantId, newKeySpec, wasDefault);
        }
        return ServiceResult.success();
    }

    @Override
    public ServiceResult<Collection<KeyPairResource>> query(QuerySpec querySpec) {
        return ServiceResult.from(keyPairResourceStore.query(querySpec));
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
}
