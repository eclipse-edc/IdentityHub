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

import org.eclipse.edc.spi.result.Result;

import java.util.Map;

/**
 * Writes an attestation to persistent storage.
 */
@FunctionalInterface
public interface AttestationWriter {

    /**
     * Writes the attestation data for the participant.
     */
    Result<Void> write(String participantId, Map<String, Object> data);

}
