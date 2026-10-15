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

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.spi.result.Result;

import java.text.ParseException;
import java.util.regex.Pattern;

/**
 * Checks that can be run on a self-issued token before its signing key is resolved. Resolving the key means resolving
 * the DID named in the {@code kid} header, which for {@code did:web} is an outbound HTTP request, so it should only be
 * done for tokens that pass these checks.
 * <p>
 * The claims returned by {@link #check(String)} are <em>not</em> verified. They must only be used to decide whether a
 * token is worth verifying, never to grant access.
 */
public final class SelfIssuedTokenPrecheck {

    private static final String DID_WEB_PREFIX = "did:web:";
    // a domain name, optionally followed by a percent-encoded port, then optional colon-separated path segments
    private static final Pattern DID_WEB = Pattern.compile("did:web:[A-Za-z0-9._-]+(%3[Aa][0-9]{1,5})?(:[A-Za-z0-9._-]+)*");

    private SelfIssuedTokenPrecheck() {
    }

    /**
     * Parses the token without verifying it and checks that the {@code kid} header names a key of the DID in the
     * {@code iss} claim (see {@link IssuerKeyIdRule#checkKeyBinding(String, String)}), and that a {@code did:web} issuer
     * is well-formed.
     *
     * @param token the serialized JWT
     * @return the unverified header and claims, or a failure if the token must not be verified any further
     */
    public static Result<UnverifiedToken> check(String token) {
        SignedJWT jwt;
        JWTClaimsSet claims;
        try {
            jwt = SignedJWT.parse(token);
            claims = jwt.getJWTClaimsSet();
        } catch (ParseException e) {
            return Result.failure("Failed to decode token");
        }

        var kid = jwt.getHeader().getKeyID();
        if (kid == null) {
            return Result.failure("Kid not present");
        }
        var issuer = claims.getIssuer();
        if (issuer == null) {
            return Result.failure("Issuer claim not present");
        }
        return IssuerKeyIdRule.checkKeyBinding(kid, issuer)
                .compose(v -> checkDid(issuer))
                .map(v -> new UnverifiedToken(kid, issuer, claims));
    }

    /**
     * Checks that a {@code did:web} identifier only contains what the did:web method allows, so that it maps to a
     * single, predictable URL. Other DID methods are not checked.
     */
    public static Result<Void> checkDid(String did) {
        if (did.startsWith(DID_WEB_PREFIX) && !DID_WEB.matcher(did).matches()) {
            return Result.failure("Malformed did:web identifier '%s'".formatted(did));
        }
        return Result.success();
    }

    /**
     * The header and claims of a token that passed {@link #check(String)}, read without verifying the signature.
     */
    public record UnverifiedToken(String keyId, String issuer, JWTClaimsSet claims) {
    }
}
