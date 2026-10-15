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

package org.eclipse.edc.identityhub.verification;

import org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.spi.TokenValidationRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Asserts that the {@code kid} header of a JWT refers to a key of the {@code iss}, i.e. that it has the form
 * <pre>
 *     kid := iss # key-id
 * </pre>
 * where {@code iss} must be a DID and {@code key-id} a non-empty fragment. The comparison is literal: the part of the
 * {@code kid} before the first {@code #} must be equal to {@code iss}.
 */
public class IssuerKeyIdRule implements TokenValidationRule {

    /**
     * Upper bound for the length of the {@code iss} claim and the {@code kid} header.
     */
    public static final int MAX_LENGTH = 2048;

    // did:<method-name>:<method-specific-id>, see https://www.w3.org/TR/did-core/#did-syntax
    private static final Pattern DID_PATTERN = Pattern.compile("did:[a-z0-9]+:[A-Za-z0-9._:%-]+");
    private static final char FRAGMENT_SEPARATOR = '#';

    private final String keyId;

    public IssuerKeyIdRule(String tokenKeyIdHeader) {
        this.keyId = tokenKeyIdHeader;
    }

    /**
     * Checks that the {@code kid} names a key of the {@code iss}, i.e. that the part of the {@code kid} before the first
     * {@code #} is equal to the {@code iss}, that it is followed by a non-empty fragment, and that the {@code iss} is a
     * syntactically valid DID. Neither value may exceed {@link #MAX_LENGTH}.
     *
     * @param keyId  the {@code kid} header
     * @param issuer the {@code iss} claim
     */
    public static Result<Void> checkKeyBinding(@Nullable String keyId, @Nullable String issuer) {
        if (keyId == null || keyId.isBlank()) {
            return Result.failure("Header 'kid' is missing.");
        }
        if (keyId.length() > MAX_LENGTH) {
            return Result.failure("Header 'kid' exceeds the maximum length of %d characters.".formatted(MAX_LENGTH));
        }
        var issuerResult = validateIssuer(issuer);
        if (issuerResult.failed()) {
            return issuerResult;
        }

        var separatorIndex = keyId.indexOf(FRAGMENT_SEPARATOR);
        if (separatorIndex < 0 || separatorIndex == keyId.length() - 1 || !keyId.substring(0, separatorIndex).equals(issuer)) {
            return Result.failure("kid header '%s' expected to correlate to 'iss' claim ('%s'), but it did not.".formatted(keyId, issuer));
        }
        return Result.success();
    }

    private static Result<Void> validateIssuer(@Nullable String issuer) {
        if (issuer == null || issuer.isBlank()) {
            return Result.failure("Claim '%s' is missing.".formatted(JwtRegisteredClaimNames.ISSUER));
        }
        if (issuer.length() > MAX_LENGTH) {
            return Result.failure("Claim '%s' exceeds the maximum length of %d characters.".formatted(JwtRegisteredClaimNames.ISSUER, MAX_LENGTH));
        }
        if (!DID_PATTERN.matcher(issuer).matches()) {
            return Result.failure("Claim '%s' is not a valid DID.".formatted(JwtRegisteredClaimNames.ISSUER));
        }
        return Result.success();
    }

    @Override
    public Result<Void> checkRule(@NotNull ClaimToken toVerify, @Nullable Map<String, Object> additional) {
        return checkKeyBinding(keyId, toVerify.getStringClaim(JwtRegisteredClaimNames.ISSUER));
    }
}
