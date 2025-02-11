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

package org.eclipse.edc.issuerservice.spi.issuance.attestation;

import java.util.Set;

/**
 * A registry of factories that validate attestation definitions and create sources.
 */
public interface AttestationSourceFactoryRegistry {

    /**
     * Returns the registered factory types.
     */
    Set<String> registeredTypes();

    /**
     * Registers a factory for the given type.
     */
    void registerFactory(String type, AttestationSourceFactory factory);

}
