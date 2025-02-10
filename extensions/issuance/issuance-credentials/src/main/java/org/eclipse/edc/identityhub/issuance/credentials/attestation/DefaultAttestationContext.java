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

package org.eclipse.edc.identityhub.issuance.credentials.attestation;

import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationContext;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Default context.
 */
public class DefaultAttestationContext implements AttestationContext {
    private final Map<String, ClaimToken> claims;
    private final String participantId;

    public DefaultAttestationContext(String participantId, Map<String, ClaimToken> claims) {
        this.participantId = requireNonNull(participantId, "participantId");
        this.claims = requireNonNull(claims, "claims");
    }

    @Override
    public @Nullable ClaimToken getClaimToken(String type) {
        return claims.get(type);
    }

    @Override
    public String participantId() {
        return participantId;
    }

    public Map<String, ClaimToken> getClaims() {
        return claims;
    }
}
