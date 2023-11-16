/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.publickey.resolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import org.eclipse.edc.iam.did.crypto.key.EcPublicKeyWrapper;
import org.eclipse.edc.iam.did.crypto.key.RsaPublicKeyWrapper;
import org.eclipse.edc.iam.did.spi.key.PublicKeyWrapper;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;

/**
 * This extension reads a public key either from a {@link Vault} or from a file located on the file system, and
 * attempts to interpret the contents either as PEM or as JWK (JSON).
 * If either no secret alias or file path is configured, or if the contents are neither PEM nor JWK, an {@link EdcException} is thrown.
 */
@Extension(value = "PublicKey Provider extension")
public class PublicKeyWrapperExtension implements ServiceExtension {

    @Setting(value = "Key alias, which was used to store the public key in the vaule", required = true)
    public static final String PUBLIC_KEY_VAULT_ALIAS_PROPERTY = "edc.ih.iam.publickey.alias";

    @Setting(value = "Path to a file that holds the public key, e.g. a PEM file. Do not use in production!")
    public static final String PUBLIC_KEY_PATH_PROPERTY = "edc.ih.iam.publickey.path";

    @Setting(value = "Public key in PEM format")
    public static final String PUBLIC_KEY_PEM = "edc.ih.iam.publickey.pem";

    @Inject
    private Vault vault;


    @Provider
    public PublicKeyWrapper createPublicKey(ServiceExtensionContext context) {
        var alias = context.getSetting(PUBLIC_KEY_VAULT_ALIAS_PROPERTY, null);
        if (alias != null) {
            return getPublicKeyFromVault(alias);
        }

        var path = context.getSetting(PUBLIC_KEY_PATH_PROPERTY, null);
        if (path != null) {
            return getPublicKeyFromFile(path);
        }

        var pem = context.getSetting(PUBLIC_KEY_PEM, null);
        if (pem != null) {
            return parseRawPublicKey(pem);
        }

        throw new EdcException("No public key was configured! Please either configure '%s' or '%s'.".formatted(PUBLIC_KEY_PATH_PROPERTY, PUBLIC_KEY_VAULT_ALIAS_PROPERTY));
    }

    /**
     * Retrieves a public key from a PEM file specified by the given path.
     *
     * @param path The path to the PEM file containing the public key.
     * @return A {@link PublicKeyWrapper} object representing the public key.
     * @throws EdcException If an error occurs while reading the file or parsing the public key.
     */
    private PublicKeyWrapper getPublicKeyFromFile(String path) {
        try {
            var raw = Files.readString(Path.of(path));
            return parseRawPublicKey(raw);
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

    /**
     * Retrieves a public key from the vault using the given alias. Public keys can be stored either in PEM or JWK format (JSON)
     *
     * @param alias The alias of the public key in the vault.
     * @return A {@link PublicKeyWrapper} object representing the public key.
     */
    private PublicKeyWrapper getPublicKeyFromVault(String alias) {
        var raw = vault.resolveSecret(alias);
        return parseRawPublicKey(raw);
    }

    /**
     * Parses the raw public key string and converts it into a PublicKeyWrapper object. The Public Key can either be in PEM or JWK format.
     *
     * @param raw The raw public key string to be parsed.
     * @return A PublicKeyWrapper object representing the parsed public key.
     * @throws EdcException If an error occurs while parsing the public key.
     */
    private PublicKeyWrapper parseRawPublicKey(String raw) {
        try {
            if (isJson(raw)) {
                return jwkToPublicKey(JWK.parse(raw));
            } else {
                return jwkToPublicKey(JWK.parseFromPEMEncodedObjects(raw));
            }
        } catch (ParseException | JOSEException ex) {
            throw new EdcException(ex);
        }
    }

    /**
     * Checks if the given raw string represents a JSON structure.
     *
     * @param raw The raw string to be checked.
     * @return true if the raw string is a valid JSON object, false otherwise.
     */
    private boolean isJson(String raw) {
        try (var parser = new ObjectMapper().createParser(raw)) {
            parser.nextToken();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Converts a JWK (JSON Web Key) to a corresponding PublicKeyWrapper object by casting it either to a {@link ECKey} or a {@link RSAKey}
     *
     * @param key The JWK key to convert.
     * @return A PublicKeyWrapper object representing the converted public key.
     * @throws EdcException If the JWK public key type is not supported.
     */
    @NotNull
    private PublicKeyWrapper jwkToPublicKey(JWK key) {
        if (key instanceof ECKey) {
            return new EcPublicKeyWrapper(key.toECKey());
        } else if (key instanceof RSAKey) {
            return new RsaPublicKeyWrapper(key.toRSAKey());
        }
        throw new EdcException("Jwk public key type not supported: " + key.getClass().getName());
    }
}
