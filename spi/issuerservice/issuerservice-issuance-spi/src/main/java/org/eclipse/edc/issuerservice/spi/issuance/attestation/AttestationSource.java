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
 * Sources data when an attestation pipeline is executed for a credential issuance request.
 * <p>
 * Attestation data may be obtained from verifiable presentations, trusted registries such as a database, or from other sources.
 */
@FunctionalInterface
public interface AttestationSource {

    /**
     * Sources and returns the data if found. Attestation sources may be required or optional. If the source data is required and not found,
     * a failure will be returned. Otherwise, if the source is optional and data is not found, an empty map will be returned as a result.
     */
    Result<Map<String, Object>> execute(AttestationContext context);

}
