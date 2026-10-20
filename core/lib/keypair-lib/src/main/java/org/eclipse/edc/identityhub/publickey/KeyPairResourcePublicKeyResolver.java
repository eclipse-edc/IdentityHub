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

package org.eclipse.edc.identityhub.publickey;

import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairState;
import org.eclipse.edc.identityhub.spi.keypair.store.KeyPairResourceStore;
import org.eclipse.edc.identityhub.spi.participantcontext.model.IdentityHubParticipantContext;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyPairUsage;
import org.eclipse.edc.keys.spi.KeyParserRegistry;
import org.eclipse.edc.keys.spi.LocalPublicKeyService;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.Nullable;

import java.security.PublicKey;

import static org.eclipse.edc.participantcontext.spi.types.ParticipantResource.queryByParticipantContextId;


/**
 * Resolves this IdentityHub's own public keys by querying the {@link KeyPairResourceStore}. The rationale being that public keys
 * should be represented as a {@link org.eclipse.edc.identityhub.spi.keypair.model.KeyPairResource}.
 * <p>
 * If no such {@link org.eclipse.edc.identityhub.spi.keypair.model.KeyPairResource} is found, resolution fails, unless a fallback
 * {@link LocalPublicKeyService} is configured, which e.g. looks up the keys from the vault. That lookup is not scoped to the participant
 * context, and a missing {@link org.eclipse.edc.identityhub.spi.keypair.model.KeyPairResource} is a strong indication of a data inconsistency.
 */
public class KeyPairResourcePublicKeyResolver {

    private final KeyPairResourceStore keyPairResourceStore;
    private final KeyParserRegistry keyParserRegistry;
    private final Monitor monitor;
    private final @Nullable LocalPublicKeyService fallbackResolver;

    /**
     * Creates the resolver.
     *
     * @param fallbackResolver resolves keys that are not found in the {@link KeyPairResourceStore}, or {@code null} if those keys must not be resolved.
     */
    public KeyPairResourcePublicKeyResolver(KeyPairResourceStore keyPairResourceStore, KeyParserRegistry registry, Monitor monitor, @Nullable LocalPublicKeyService fallbackResolver) {

        this.keyPairResourceStore = keyPairResourceStore;
        this.keyParserRegistry = registry;
        this.monitor = monitor;
        this.fallbackResolver = fallbackResolver;
    }

    /**
     * Resolves a {@link PublicKey} with a given key-ID from the internal {@link KeyPairResourceStore}. Note that this only works for public keys
     * that are known to this runtime, i.e. this only works for public keys that belong to one of the {@link IdentityHubParticipantContext} objects
     * that are managed by this IdentityHub!
     * <p>
     * Only key pairs with the {@link KeyPairUsage#TOKEN_SIGNING} usage are considered. Revoked key pairs are rejected, as are ambiguous matches,
     * i.e. if more than one non-revoked key pair has the same key ID.
     * <p>
     * If the PublicKey is not found in storage, resolution fails, unless a fallback {@link LocalPublicKeyService} is configured.
     *
     * @param publicKeyId          The fully-qualified ID of the public key. For example: {@code did:web:someparticipant#key-123}.
     * @param participantContextId The participant context ID of the requestor
     * @return A result with the public key, resolved from storage, or a failed result.
     */
    public Result<PublicKey> resolveKey(String publicKeyId, String participantContextId) {
        var query = queryByParticipantContextId(participantContextId).filter(new Criterion("keyId", "=", publicKeyId)).build();
        var result = keyPairResourceStore.query(query);
        // store failed, e.g. data model does not match query, etc.
        if (result.failed()) {
            monitor.warning("Error querying database for KeyPairResource with key ID '%s': %s".formatted(publicKeyId, result.getFailureDetail()));
            return Result.failure(result.getFailureDetail());
        }

        var resources = result.getContent();
        if (resources.isEmpty()) {
            if (fallbackResolver == null) {
                return Result.failure("No KeyPairResource with keyId '%s' was found.".formatted(publicKeyId));
            }
            monitor.warning("No KeyPairResource with keyId '%s' was found for participant '%s' in the store. Will attempt to resolve from the Vault. ".formatted(publicKeyId, participantContextId) +
                    "This could be an indication of a data inconsistency, it is recommended to revoke and regenerate keys!");
            return fallbackResolver.resolveKey(publicKeyId); // attempt to resolve from vault
        }

        var candidates = resources.stream()
                .filter(kpr -> kpr.getState() != KeyPairState.REVOKED.code())
                .toList();
        if (candidates.isEmpty()) {
            return Result.failure("The KeyPairResource with keyId '%s' is revoked.".formatted(publicKeyId));
        }
        if (candidates.size() > 1) {
            monitor.warning("Expected exactly 1 non-revoked KeyPairResource with keyId '%s' but found '%d'. This indicates a database inconsistency.".formatted(publicKeyId, candidates.size()));
            return Result.failure("Multiple KeyPairResources with keyId '%s' were found.".formatted(publicKeyId));
        }

        var keyPair = candidates.get(0);
        if (!keyPair.getUsage().contains(KeyPairUsage.TOKEN_SIGNING)) {
            return Result.failure("The KeyPairResource with keyId '%s' is not intended for token signing.".formatted(publicKeyId));
        }
        return parseKey(keyPair.getSerializedPublicKey());
    }

    private Result<PublicKey> parseKey(String encodedKey) {
        return keyParserRegistry.parse(encodedKey).compose(pk -> {
            if (pk instanceof PublicKey publicKey) {
                return Result.success(publicKey);
            } else {
                return Result.failure("The specified resource did not contain public key material.");
            }
        });
    }
}
