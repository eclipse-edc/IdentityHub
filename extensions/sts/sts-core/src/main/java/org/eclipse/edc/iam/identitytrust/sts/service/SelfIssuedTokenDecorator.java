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

package org.eclipse.edc.iam.identitytrust.sts.service;

import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.token.spi.TokenDecorator;

import java.time.Clock;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.EXPIRATION_TIME;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUED_AT;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.JWT_ID;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.NOT_BEFORE;

/**
 * Decorator for Self-Issued ID token and Access Token. It appends input claims and
 * generic claims like iat, exp, and jti
 */
class SelfIssuedTokenDecorator implements TokenDecorator {
    private final Map<String, String> claims;
    private final Clock clock;
    private final long validity;

    SelfIssuedTokenDecorator(Map<String, String> claims, Clock clock, long validity) {
        this.claims = claims;
        this.clock = clock;
        this.validity = validity;
    }

    @Override
    public TokenParameters.Builder decorate(TokenParameters.Builder tokenParameters) {
        this.claims.forEach(tokenParameters::claims);
        return tokenParameters.claims(ISSUED_AT, Date.from(clock.instant()))
                .claims(NOT_BEFORE, Date.from(clock.instant()))
                .claims(EXPIRATION_TIME, Date.from(clock.instant().plusSeconds(validity)))
                .claims(JWT_ID, UUID.randomUUID().toString());
    }
}
