/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.accesstoken.verification;

import org.eclipse.edc.identityhub.spi.verification.AccessTokenVerifier;
import org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames;
import org.eclipse.edc.keys.spi.LocalPublicKeyService;
import org.eclipse.edc.keys.spi.PublicKeyResolver;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.spi.TokenValidationRule;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.eclipse.edc.token.spi.TokenValidationService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.eclipse.edc.identityhub.accesstoken.verification.AccessTokenConstants.ACCESS_TOKEN_SCOPE_CLAIM;
import static org.eclipse.edc.identityhub.accesstoken.verification.AccessTokenConstants.IATP_ACCESS_TOKEN_CONTEXT;
import static org.eclipse.edc.identityhub.accesstoken.verification.AccessTokenConstants.IATP_SELF_ISSUED_TOKEN_CONTEXT;
import static org.eclipse.edc.identityhub.accesstoken.verification.AccessTokenConstants.TOKEN_CLAIM;

/**
 * Default implementation used to verify Self-Issued tokens. The public key is expected to be found in the
 * issuer's DID
 */
public class AccessTokenVerifierImpl implements AccessTokenVerifier {


    private static final String SCOPE_SEPARATOR = " ";
    private final TokenValidationService tokenValidationService;
    private final LocalPublicKeyService localPublicKeyService;
    private final TokenValidationRulesRegistry tokenValidationRulesRegistry;
    private final Monitor monitor;
    private final PublicKeyResolver publicKeyResolver;

    public AccessTokenVerifierImpl(TokenValidationService tokenValidationService, LocalPublicKeyService localPublicKeyService, TokenValidationRulesRegistry tokenValidationRulesRegistry, Monitor monitor,
                                   PublicKeyResolver publicKeyResolver) {
        this.tokenValidationService = tokenValidationService;
        this.localPublicKeyService = localPublicKeyService;
        this.tokenValidationRulesRegistry = tokenValidationRulesRegistry;
        this.monitor = monitor;
        this.publicKeyResolver = publicKeyResolver;
    }

    @Override
    public Result<List<String>> verify(String token, String participantId) {
        Objects.requireNonNull(participantId, "Participant ID is mandatory.");
        var res = tokenValidationService.validate(token, publicKeyResolver, tokenValidationRulesRegistry.getRules(IATP_SELF_ISSUED_TOKEN_CONTEXT));
        if (res.failed()) {
            return res.mapFailure();
        }

        var claimToken = res.getContent();
        var accessTokenString = claimToken.getStringClaim(TOKEN_CLAIM);
        var subClaim = claimToken.getStringClaim(JwtRegisteredClaimNames.SUBJECT);

        TokenValidationRule audMustMatchParticipantIdRule = (at, additional) -> {
            var aud = at.getListClaim(JwtRegisteredClaimNames.AUDIENCE);
            if (aud == null || aud.isEmpty()) {
                return Result.failure("Mandatory claim 'aud' on 'token' was null.");
            }
            return aud.contains(participantId) ? Result.success() : Result.failure("Participant Context ID must match 'aud' claim in 'access_token'");
        };

        TokenValidationRule subClaimsMatch = (at, additional) -> {
            var atSub = at.getStringClaim(JwtRegisteredClaimNames.SUBJECT);
            // correlate sub and access_token.sub
            if (!Objects.equals(subClaim, atSub)) {
                monitor.warning("ID token [sub] claim is not equal to [%s.sub] claim: expected '%s', got '%s'. Proof-of-possession could not be established!".formatted(TOKEN_CLAIM, subClaim, atSub));
                // return failure("ID token 'sub' claim is not equal to '%s.sub' claim.".formatted(ACCES_TOKEN_CLAIM));
            }
            return Result.success();
        };

        // verify the correctness of the 'access_token'
        var rules = new ArrayList<>(tokenValidationRulesRegistry.getRules(IATP_ACCESS_TOKEN_CONTEXT));
        rules.add(subClaimsMatch);
        rules.add(audMustMatchParticipantIdRule);
        // todo: verify that the resolved public key belongs to the participant ID
        var result = tokenValidationService.validate(accessTokenString, localPublicKeyService, rules);
        if (result.failed()) {
            return result.mapFailure();
        }

        // verify that the access_token contains a scope claim
        var scope = result.getContent().getStringClaim(ACCESS_TOKEN_SCOPE_CLAIM);
        return Result.success(Arrays.asList(scope.split(SCOPE_SEPARATOR)));
    }
}
