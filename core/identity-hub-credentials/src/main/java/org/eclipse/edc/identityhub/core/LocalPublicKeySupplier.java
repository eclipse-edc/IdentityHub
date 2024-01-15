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

package org.eclipse.edc.identityhub.core;

import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.KeyParserRegistry;
import org.eclipse.edc.spi.security.Vault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PublicKey;
import java.util.Objects;
import java.util.function.Supplier;

import static org.eclipse.edc.identityhub.core.CoreServicesExtension.PUBLIC_KEY_PATH_PROPERTY;
import static org.eclipse.edc.identityhub.core.CoreServicesExtension.PUBLIC_KEY_VAULT_ALIAS_PROPERTY;


/**
 * Provides a public key, that is resolved from the vault, a file (using the path) or a raw string, in that sequence.
 * Typically, we use this when we have a public key configured for the STS service, so we can verify access tokens created by it.
 * <p>
 * It is NOT intended for general use when resolving arbitrary public keys!
 */
public class LocalPublicKeySupplier implements Supplier<PublicKey> {
    public static final String NO_PUBLIC_KEY_CONFIGURED_ERROR = "No public key was configured! Please either configure '%s', '%s' or '%s'."
            .formatted(PUBLIC_KEY_VAULT_ALIAS_PROPERTY, PUBLIC_KEY_PATH_PROPERTY, PUBLIC_KEY_VAULT_ALIAS_PROPERTY);
    private String vaultAlias;
    private String publicKeyPath;
    private String publicKeyRaw;
    private KeyParserRegistry keyParserRegistry;
    private Vault vault;

    private LocalPublicKeySupplier() {
    }

    @Override
    public PublicKey get() {
        Result<PublicKey> result = Result.failure(NO_PUBLIC_KEY_CONFIGURED_ERROR);
        if (vaultAlias != null) {
            result = getPublicKeyFromVault(vaultAlias);
        }

        if (publicKeyPath != null) {
            result = getPublicKeyFromFile(publicKeyPath);
        }

        if (publicKeyRaw != null) {
            result = parseRawPublicKey(publicKeyRaw);
        }

        return result.orElseThrow(f -> new EdcException(f.getFailureDetail()));
    }

    /**
     * Retrieves a public key from a PEM file specified by the given path.
     *
     * @param path The path to the PEM file containing the public key.
     * @return A {@link PublicKey} object representing the public key.
     * @throws EdcException If an error occurs while reading the file or parsing the public key.
     */
    private Result<PublicKey> getPublicKeyFromFile(String path) {
        try {
            var raw = Files.readString(Path.of(path));
            return parseRawPublicKey(raw);
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

    private Result<PublicKey> parseRawPublicKey(String encodedPublicKey) {
        return keyParserRegistry.parse(encodedPublicKey)
                .map(key -> (PublicKey) key);
    }

    /**
     * Retrieves a public key from the vault using the given alias. Public keys can be stored either in PEM or JWK format (JSON)
     *
     * @param alias The alias of the public key in the vault.
     * @return A {@link Result} object representing the public key.
     */
    private Result<PublicKey> getPublicKeyFromVault(String alias) {
        var raw = vault.resolveSecret(alias);
        return parseRawPublicKey(raw);
    }


    public static class Builder {
        private final LocalPublicKeySupplier instance;

        private Builder() {
            this.instance = new LocalPublicKeySupplier();
        }

        public Builder vaultAlias(@Nullable String vaultAlias) {
            this.instance.vaultAlias = vaultAlias;
            return this;
        }

        public Builder publicKeyPath(@Nullable String publicKeyPath) {
            this.instance.publicKeyPath = publicKeyPath;
            return this;
        }

        public Builder rawString(@Nullable String publicKeyRawContents) {
            this.instance.publicKeyRaw = publicKeyRawContents;
            return this;
        }

        public Builder keyParserRegistry(KeyParserRegistry registry) {
            this.instance.keyParserRegistry = registry;
            return this;
        }

        public LocalPublicKeySupplier build() {
            if (this.instance.vaultAlias != null) {
                Objects.requireNonNull(this.instance.vault);
            }

            if (instance.vaultAlias == null && instance.publicKeyPath == null && instance.publicKeyRaw == null) {
                throw new EdcException(NO_PUBLIC_KEY_CONFIGURED_ERROR);
            }

            Objects.requireNonNull(this.instance.keyParserRegistry, "KeyParserRegistry is mandatory");

            return instance;
        }

        public Builder vault(Vault vault) {
            this.instance.vault = vault;
            return this;
        }

        public static Builder newInstance() {
            return new Builder();
        }
    }
}
