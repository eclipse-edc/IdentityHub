/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.issuerservice.spi.credentials.statuslist;

/**
 * Maintains specific implementations for statuslist factories.
 */
public interface StatusListInfoFactoryRegistry {
    /**
     * Register a {@link StatusListInfoFactory} for a particular "type". This type must be the {@code credentialStatus.type}
     * field of a holder verifiable credential, for example {@code "BitStringStatusListEntry}
     *
     * @param type the {@code credentialStatus.type} value of the holder credential
     * @return returns the specific factory for that type
     */
    StatusListInfoFactory getInfoFactory(String type);

    /**
     * Adds a {@link StatusListInfoFactory} for a specific status list type.
     *
     * @param type    the type, i.e. the value of the {@code credentialStatus.type} field of the holder credential, e.g. {@code "BitStringStatusListEntry}
     * @param factory the factory for that type, or null
     */
    void register(String type, StatusListInfoFactory factory);
}
