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

import org.eclipse.edc.spi.iam.ClaimToken;
import org.jetbrains.annotations.Nullable;

/**
 * Provides access to context data for attestation evaluation.
 */
public interface AttestationContext {

    /**
     * Returns validated token claims for the current request.
     */
    @Nullable
    ClaimToken getClaimToken(String type);

    /**
     * Returns the participant ID associated with the current request.
     */
    String participantId();

}
