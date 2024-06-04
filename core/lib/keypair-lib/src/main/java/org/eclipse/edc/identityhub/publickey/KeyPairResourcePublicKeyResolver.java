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

import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantResource;
import org.eclipse.edc.identityhub.spi.store.KeyPairResourceStore;
import org.eclipse.edc.keys.LocalPublicKeyServiceImpl;
import org.eclipse.edc.keys.spi.KeyParserRegistry;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;

import java.security.PublicKey;

/**
 * This {@link org.eclipse.edc.keys.spi.LocalPublicKeyService} resolves this IdentityHub's own public keys by querying the {@link KeyPairResourceStore}.
 * The rationale being that public keys should be represented as a {@link org.eclipse.edc.identityhub.spi.keypair.model.KeyPairResource}.
 * <p>
 * If no such {@link org.eclipse.edc.identityhub.spi.keypair.model.KeyPairResource} is found, this service will fall back to looking up the key in the vault. Note that this
 * would be a strong indication of a data inconsistency.
 */
public class KeyPairResourcePublicKeyResolver extends LocalPublicKeyServiceImpl {

    private final KeyPairResourceStore keyPairResourceStore;
    private final KeyParserRegistry keyParserRegistry;
    private final Monitor monitor;

    public KeyPairResourcePublicKeyResolver(Vault vault, KeyPairResourceStore keyPairResourceStore, KeyParserRegistry registry, Monitor monitor) {
        super(vault, registry);
        this.keyPairResourceStore = keyPairResourceStore;
        this.keyParserRegistry = registry;
        this.monitor = monitor;
    }

    @Override
    public Result<PublicKey> resolveKey(String id) {
        return resolveFromDbOrVault(id);
    }

    private Result<PublicKey> resolveFromDbOrVault(String publicKeyId) {
        var query = ParticipantResource.queryByParticipantId("").filter(new Criterion("keyId", "=", publicKeyId)).build();
        var result = keyPairResourceStore.query(query);
        // store failed, e.g. data model does not match query, etc.
        if (result.failed()) {
            monitor.warning("Error querying database for KeyPairResource with key ID '%s': %s".formatted(publicKeyId, result.getFailureDetail()));
            return Result.failure(result.getFailureDetail());
        }

        var resources = result.getContent();
        if (resources.size() > 1) {
            monitor.warning("Expected exactly 1 KeyPairResource with keyId '%s' but found '%d'. This indicates a database inconsistency. Will return the first one.".formatted(publicKeyId, resources.size()));
        }
        return resources.stream().findAny()
                .map(kpr -> parseKey(kpr.getSerializedPublicKey()))
                .orElseGet(() -> {
                    monitor.warning("No KeyPairResource with keyId '%s' was found in the store. Will attempt to resolve from the Vault. This could be an indication of a data inconsistency, it is recommended to revoke and regenerate keys!");
                    return super.resolveKey(publicKeyId); // attempt to resolve from vault
                });
    }

    // super-class's method is private, simply temporarily copy-pasted here
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
