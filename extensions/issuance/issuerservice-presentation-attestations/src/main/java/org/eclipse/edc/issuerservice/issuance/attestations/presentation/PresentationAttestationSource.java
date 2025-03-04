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

package org.eclipse.edc.issuerservice.issuance.attestations.presentation;

import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationContext;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationSource;
import org.eclipse.edc.spi.result.Result;

import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;
import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;

/**
 * Resolves an attestation that is a verifiable presentation.
 */
public class PresentationAttestationSource implements AttestationSource {
    private final String claimType;
    private final String outputClaim;
    private final boolean required;

    public PresentationAttestationSource(String claimType, String outputClaim, boolean required) {
        this.claimType = requireNonNull(claimType, "claimType");
        this.outputClaim = requireNonNull(outputClaim, "outputClaim");
        this.required = required;
    }

    @Override
    public Result<Map<String, Object>> execute(AttestationContext context) {
        var claimToken = context.getClaimToken(claimType);
        if (claimToken == null) {
            return !required ? success(emptyMap()) : failure("Claim token not found");
        }
        return success(Map.of(outputClaim, claimToken.getClaims()));
    }
}
