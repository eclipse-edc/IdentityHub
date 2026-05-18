/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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

package org.eclipse.edc.identityhub.transit;

import org.eclipse.edc.spi.result.Result;

public interface TransitEngine {
    /**
     * Generates a new key with the given name. Note that keys are created with the {@code deletion_allowed} flag set to {@code true}.
     *
     * @param keyName The name of the key. Should not contain spaces or special characters.
     * @param keyType The type of key, i.e., the key algorithm and curve etc. Supported types are <a href="https://developer.hashicorp.com/vault/api-docs/secret/transit#type">here</a>
     * @return ServiceResult containing the generated TransitKeyDescriptor or error details.
     */
    Result<TransitKeyDescriptor> generateKey(String keyName, String keyType);

    /**
     * Rotates the key with the given name by adding a new "version" of the key. Note that previous versions of the key are not deleted,
     * and they can still be used for encryption and decryption.
     * Use {@link TransitEngine#setMinEncryptionKeyVersion(String, int)} and {@link TransitEngine#setMinDecryptionKeyVersion(String, int)} to
     * control which versions of the key are used for encryption and decryption.
     *
     * @param keyName The name of the key. Should not contain spaces or special characters.
     */
    Result<Void> rotateKey(String keyName);

    /**
     * Retrieves the key information with the given name.
     *
     * @param keyName The name of the key. Should not contain spaces or special characters.
     * @return ServiceResult containing the generated TransitKeyDescriptor or error details.
     */
    Result<TransitKeyDescriptor> getKey(String keyName);

    /**
     * Sets the minimum version of the key that can be used for encryption.
     *
     * @param keyName    The name of the key. Should not contain spaces or special characters.
     * @param minVersion The minimum version number. Must be positive.
     * @return ServiceResult indicating success or failure.
     */
    Result<Void> setMinEncryptionKeyVersion(String keyName, int minVersion);

    /**
     * Sets the minimum version of the key that can be used for decryption. A version of a key might not be usable for
     * <em>encryption</em> anymore, but it might be useful to allow decryption of older versions of the key.
     *
     * @param keyName    The name of the key. Should not contain spaces or special characters.
     * @param minVersion The minimum version number. Must be positive.
     * @return ServiceResult indicating success or failure.
     */
    Result<Void> setMinDecryptionKeyVersion(String keyName, int minVersion);

    /**
     * Sets the minimum version of the key that is still stored by the Transit secret engine. Old keys might get deleted.
     * <p>
     * Note that the min-available version cannot be set if the min encryption version is not set.
     *
     * @param keyName    The name of the key. Should not contain spaces or special characters.
     * @param minVersion The minimum version number. Must be positive.
     * @return ServiceResult indicating success or failure.
     */
    Result<Void> setMinAvailableVersion(String keyName, int minVersion);

    /**
     * Signs the given payload using the specified key.
     *
     * @param keyName The name of the key to use for signing.
     * @param payload The payload to be signed. The implementation will base64-encode the payload before sending it to Vault.
     * @return ServiceResult indicating success or failure, carrying the signature in the result.
     */
    Result<String> sign(String keyName, String payload);

    /**
     * Verifies the given signature against the given payload using the specified key.
     *
     * @param keyName   The name of the key to use for verification.
     * @param payload   The payload to be verified. The implementation will base64-encode the payload before sending it to Vault.
     * @param signature The signature to be verified.
     * @return ServiceResult indicating success or failure.
     */
    Result<Void> verify(String keyName, String payload, String signature);

    /**
     * Deletes the key with the given name.
     *
     * @param keyName the name of the key to delete
     * @return ServiceResult indicating success or failure.
     */
    Result<Void> deleteKey(String keyName);
}
