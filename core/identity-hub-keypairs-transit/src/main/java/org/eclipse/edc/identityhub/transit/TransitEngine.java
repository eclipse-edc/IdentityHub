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
     * Generates a new key with the given name.
     *
     * @param keyName The name of the key. Should not contain spaces or special characters.
     * @return ServiceResult containing the generated TransitKeyDescriptor or error details.
     */
    Result<TransitKeyDescriptor> generateKey(String keyName);

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
}
