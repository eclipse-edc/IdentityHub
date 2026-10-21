/*
 *  Copyright (c) 2026 Metaform Systems Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.core.services.verifiablepresentation.generators;

import org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames;
import org.eclipse.edc.token.spi.TokenDecorator;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Registered JWT claims that tie a presentation token to its holder and its audience, and limit its lifetime.
 */
public final class PresentationTokenClaims {

    private PresentationTokenClaims() {
    }

    /**
     * Creates a decorator that adds the {@code iss}, {@code iat}, {@code exp} and a random {@code jti} claim,
     * and the {@code aud} claim if an audience is given.
     *
     * @param now      the time the token is issued at
     * @param lifetime how long the token is valid after it was issued
     * @param issuerId the holder of the presentation, usually a DID
     * @param audience the verifier the presentation is intended for, may be null
     */
    public static TokenDecorator presentationTokenClaims(Instant now, Duration lifetime, String issuerId, @Nullable Object audience) {
        return builder -> {
            builder.claims(JwtRegisteredClaimNames.ISSUER, issuerId)
                    .claims(JwtRegisteredClaimNames.ISSUED_AT, Date.from(now))
                    .claims(JwtRegisteredClaimNames.EXPIRATION_TIME, Date.from(now.plus(lifetime)))
                    .claims(JwtRegisteredClaimNames.JWT_ID, UUID.randomUUID().toString());
            if (audience != null) {
                builder.claims(JwtRegisteredClaimNames.AUDIENCE, audience);
            }
            return builder;
        };
    }
}
