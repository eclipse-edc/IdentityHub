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
import java.util.Set;

/**
 * Evaluates the given attestations, returning any resolved data if executed successfully.
 */
public interface AttestationPipeline {

    /**
     * Performs the evaluation.
     */
    Result<Map<String, Object>> evaluate(Set<String> attestations, AttestationContext context);

}
