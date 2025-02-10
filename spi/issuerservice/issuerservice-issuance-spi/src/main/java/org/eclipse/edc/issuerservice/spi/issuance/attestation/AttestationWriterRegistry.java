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

/**
 * Performs validation and creates attestation sources for an attestation definition.
 */
public interface AttestationWriterRegistry {

    /**
     * Registers the writer for the given attestation type.
     */
    void registerWriter(String type, AttestationWriter writer);

    /**
     * Returns the writer for the given attestation type.
     */
    AttestationWriter resolveWriter(String type);

}
