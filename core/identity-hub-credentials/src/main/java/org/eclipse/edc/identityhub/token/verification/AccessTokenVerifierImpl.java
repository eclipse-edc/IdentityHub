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

import org.eclipse.edc.identityhub.spi.verification.AccessTokenVerifier;
import org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames;
import org.eclipse.edc.spi.iam.PublicKeyResolver;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.eclipse.edc.token.spi.TokenValidationService;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import static com.nimbusds.jwt.JWTClaimNames.SUBJECT;
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
    private final TokenValidationService tokenValidationService;
    private final TokenValidationRulesRegistry tokenValidationRulesRegistry;
    private final String audience;
    private final Supplier<PublicKey> stsPublicKey;
    private final Monitor monitor;
    private final PublicKeyResolver publicKeyResolver;

    public AccessTokenVerifierImpl(TokenValidationService tokenValidationService, Supplier<PublicKey> publicKeySupplier, TokenValidationRulesRegistry tokenValidationRulesRegistry, String audience, Monitor monitor,
                                   PublicKeyResolver publicKeyResolver) {
        this.tokenValidationService = tokenValidationService;
        this.tokenValidationRulesRegistry = tokenValidationRulesRegistry;
        this.audience = audience;
        this.monitor = monitor;
        this.stsPublicKey = publicKeySupplier;
        this.publicKeyResolver = publicKeyResolver;
    }

    @Override
    public Result<List<String>> verify(String token) {
        // verify cryptographic integrity
        var res = tokenValidationService.validate(token, publicKeyResolver, tokenValidationRulesRegistry.getRules("iatp-si"));
        if (res.failed()) {
            return res.mapTo();
        }

        // make sure an access_token claim exists
        var claimToken = res.getContent();
        if (claimToken.getClaim(ACCES_TOKEN_CLAIM) == null) {
            return failure("No 'access_token' claim was found on ID Token.");
        }

        var accessTokenString = claimToken.getClaim(ACCES_TOKEN_CLAIM).toString();

        // verify the correctness of the 'access_token'
        var result = tokenValidationService.validate(accessTokenString, id -> Result.success(stsPublicKey.get()), tokenValidationRulesRegistry.getRules("iatp-access-token"));
        if (result.failed()) {
            return result.mapTo();
        }
        var accessToken = result.getContent();
        var atSub = accessToken.getClaim(JwtRegisteredClaimNames.SUBJECT);

        // correlate sub and access_token.sub
        var sub = claimToken.getStringClaim(SUBJECT);
        if (!Objects.equals(sub, atSub)) {
            monitor.warning("ID token [sub] claim is not equal to [%s.sub] claim: expected '%s', got '%s'. Proof-of-possession could not be established!".formatted(ACCES_TOKEN_CLAIM, sub, atSub));
            // return failure("ID token 'sub' claim is not equal to '%s.sub' claim.".formatted(ACCES_TOKEN_CLAIM));
        }

        // verify that the access_token contains a scope claim
        var scope = accessToken.getStringClaim(ACCESS_TOKEN_SCOPE_CLAIM);
        if (scope == null) {
            return failure("No %s claim was found on the %s".formatted(ACCESS_TOKEN_SCOPE_CLAIM, ACCES_TOKEN_CLAIM));
        }
        return success(Arrays.asList(scope.split(SCOPE_SEPARATOR)));
    }
}
