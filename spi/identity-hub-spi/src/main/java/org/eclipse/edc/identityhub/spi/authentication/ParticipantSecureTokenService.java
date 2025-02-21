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

package org.eclipse.edc.identityhub.spi.authentication;

import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Multi-tenant (participant-aware) variant of a SecureTokenService
 */
public interface ParticipantSecureTokenService {


    /**
     * Creates a self-issued ID token ("SI token")
     *
     * @param participantContextId The ID of the participant context on behalf of whom the token is generated
     * @param claims               a set of claims, that are to be included in the SI token. MUST include {@code iss}, {@code sub} and {@code aud}.
     * @param bearerAccessScope    if non-null, must be a space-separated list of scopes as per <a href="https://github.com/eclipse-tractusx/identity-trust/blob/main/specifications/M1/verifiable.presentation.protocol.md#31-access-scopes">DCP specification</a>
     *                             if bearerAccessScope != null -> creates a {@code token} claim, which is another JWT containing the scope as claims.
     *                             if bearerAccessScope == null -> creates a normal JWT using all the claims in the map
     * @return A result containing the token representation, or a failure
     */
    Result<TokenRepresentation> createToken(String participantContextId, Map<String, String> claims, @Nullable String bearerAccessScope);
}
