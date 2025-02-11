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

import org.eclipse.edc.issuerservice.spi.issuance.model.AttestationDefinition;

import java.util.Map;

import static java.util.Collections.emptyMap;

/**
 * Performs validation and creates attestation sources for an attestation definition.
 */
public interface AttestationSourceFactory {

    /**
     * Creates an executable source for the attestation definition.
     */
    AttestationSource createSource(AttestationDefinition definition);

    /**
     * Returns metadata that describes an attestation requirement.
     *
     * @param metadataType the type of metadata
     * @param definition   the attestation definition
     * @return the metadata
     */
    default Map<String, Object> createMetadata(String metadataType, AttestationDefinition definition) {
        return emptyMap();
    }

}
