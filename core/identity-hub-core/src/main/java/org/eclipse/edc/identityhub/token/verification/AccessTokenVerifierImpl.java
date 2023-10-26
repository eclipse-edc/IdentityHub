/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.token.verification;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.iam.did.spi.key.PublicKeyWrapper;
import org.eclipse.edc.identityhub.spi.verification.AccessTokenVerifier;
import org.eclipse.edc.identitytrust.validation.JwtValidator;
import org.eclipse.edc.identitytrust.verification.JwtVerifier;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;

import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;

/**
 * Default implementation used to verify Self-Issued tokens. The public key is expected to be found in the
 * issuer's DID
 */
public class AccessTokenVerifierImpl implements AccessTokenVerifier {
    public static final String ACCES_TOKEN_CLAIM = "access_token";
    public static final String ACCESS_TOKEN_SCOPE_CLAIM = "scope";
    private static final String SCOPE_SEPARATOR = " ";
    private final JwtVerifier jwtVerifier;
    private final JwtValidator jwtValidator;
    private final String audience;
    private final PublicKeyWrapper stsPublicKey;

    public AccessTokenVerifierImpl(JwtVerifier jwtVerifier, JwtValidator jwtValidator, String audience, PublicKeyWrapper stsPublicKey) {
        this.jwtVerifier = jwtVerifier;
        this.jwtValidator = jwtValidator;
        this.audience = audience;
        this.stsPublicKey = stsPublicKey;
    }

    @Override
    public Result<List<String>> verify(String token) {
        // verify cryptographic integrity
        var res = jwtVerifier.verify(token, audience);
        if (res.failed()) {
            return res.mapTo();
        }

        // assert valid structure
        var tokenRep = TokenRepresentation.Builder.newInstance().token(token).build();
        var validationResult = jwtValidator.validateToken(tokenRep, audience);
        if (validationResult.failed()) {
            return validationResult.mapTo();
        }

        // make sure an access_token claim exists
        var claimToken = validationResult.getContent();
        if (claimToken.getClaim("access_token") == null) {
            return failure("No 'access_token' claim was found on ID Token.");
        }

        var accessTokenString = claimToken.getClaim(ACCES_TOKEN_CLAIM).toString();

        // verify the correctness of the 'access_token'
        try {
            var accessTokenJwt = SignedJWT.parse(accessTokenString);
            if (!accessTokenJwt.verify(stsPublicKey.verifier())) {
                return failure("Could not verify %s: Invalid Signature".formatted(ACCES_TOKEN_CLAIM));
            }
            var accessTokenClaims = accessTokenJwt.getJWTClaimsSet();
            var atSub = accessTokenClaims.getSubject();

            // correlate sub and access_token.sub
            if (!correlate(claimToken.getStringClaim("sub"), atSub)) {
                return failure("ID token 'sub' claim is not equal to '%s.sub' claim.".formatted(ACCES_TOKEN_CLAIM));
            }

            // verify that the access_token contains a scope claim
            var scope = accessTokenClaims.getStringClaim(ACCESS_TOKEN_SCOPE_CLAIM);
            if (scope == null) {
                return failure("No %s claim was found on the %s".formatted(ACCESS_TOKEN_SCOPE_CLAIM, ACCES_TOKEN_CLAIM));
            }
            return success(Arrays.asList(scope.split(SCOPE_SEPARATOR)));
        } catch (ParseException e) {
            return failure("Error parsing %s: %s".formatted(ACCES_TOKEN_CLAIM, e.getMessage()));
        } catch (JOSEException e) {
            return failure("Could not verify %s with STS public key: %s".formatted(ACCES_TOKEN_CLAIM, e.getMessage()));
        }
    }

    private boolean correlate(String sub, String accessTokenSub) {
        // todo: make this overridable (and bit more generic), so that cases, where the DID is different from the participant ID
        //  can be handled in a flexible way, because then access_token.sub != sub
        return Objects.equals(sub, accessTokenSub);
    }
}
