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

package org.eclipse.edc.issuerservice.issuance.attestations.database;

import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationContext;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public record TestAttestationContext(String participantId,
                                     Map<String, ClaimToken> claims) implements AttestationContext {
    @Override
    public @Nullable ClaimToken getClaimToken(String type) {
        return claims.get(type);
    }
}
