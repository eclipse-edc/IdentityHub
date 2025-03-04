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

package org.eclipse.edc.identityhub.protocols.dcp.issuer;


import org.eclipse.edc.identityhub.protocols.dcp.spi.model.DcpRequestContext;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationContext;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.jetbrains.annotations.Nullable;

/**
 * Attestation context for DCP.
 */
public record DcpAttestationContext(DcpRequestContext context) implements AttestationContext {
    @Override
    public @Nullable ClaimToken getClaimToken(String type) {
        return context.claims().get(type);
    }

    @Override
    public String participantId() {
        return context.holder().getHolderId();
    }
}
