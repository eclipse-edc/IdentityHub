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
import org.eclipse.edc.spi.iam.PublicKeyResolver;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.spi.TokenValidationRule;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.eclipse.edc.token.spi.TokenValidationService;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import static com.nimbusds.jwt.JWTClaimNames.AUDIENCE;
import static com.nimbusds.jwt.JWTClaimNames.SUBJECT;
import static org.eclipse.edc.identityhub.DefaultServicesExtension.ACCESS_TOKEN_SCOPE_CLAIM;
import static org.eclipse.edc.identityhub.DefaultServicesExtension.IATP_ACCESS_TOKEN_CONTEXT;
import static org.eclipse.edc.identityhub.DefaultServicesExtension.IATP_SELF_ISSUED_TOKEN_CONTEXT;
import static org.eclipse.edc.identityhub.DefaultServicesExtension.TOKEN_CLAIM;
import static org.eclipse.edc.spi.result.Result.success;

/**
 * Default implementation used to verify Self-Issued tokens. The public key is expected to be found in the
 * issuer's DID
 */
public class AccessTokenVerifierImpl implements AccessTokenVerifier {

    private static final String SCOPE_SEPARATOR = " ";
    private final TokenValidationService tokenValidationService;
    private final TokenValidationRulesRegistry tokenValidationRulesRegistry;
    private final Supplier<PublicKey> stsPublicKey;
    private final Monitor monitor;
    private final PublicKeyResolver publicKeyResolver;

    public AccessTokenVerifierImpl(TokenValidationService tokenValidationService, Supplier<PublicKey> publicKeySupplier, TokenValidationRulesRegistry tokenValidationRulesRegistry, Monitor monitor,
                                   PublicKeyResolver publicKeyResolver) {
        this.tokenValidationService = tokenValidationService;
        this.tokenValidationRulesRegistry = tokenValidationRulesRegistry;
        this.monitor = monitor;
        this.stsPublicKey = publicKeySupplier;
        this.publicKeyResolver = publicKeyResolver;
    }

    @Override
    public Result<List<String>> verify(String token, String participantId) {
        Objects.requireNonNull(participantId, "Participant ID is mandatory.");
        var res = tokenValidationService.validate(token, publicKeyResolver, tokenValidationRulesRegistry.getRules(IATP_SELF_ISSUED_TOKEN_CONTEXT));
        if (res.failed()) {
            return res.mapTo();
        }

        var claimToken = res.getContent();
        var accessTokenString = claimToken.getStringClaim(TOKEN_CLAIM);
        var subClaim = claimToken.getStringClaim(SUBJECT);

        TokenValidationRule audMustMatchParticipantIdRule = (at, additional) -> {
            var aud = at.getListClaim(AUDIENCE);
            if (aud == null || aud.isEmpty()) {
                return Result.failure("Mandatory claim 'aud' on 'token' was null.");
            }
            return aud.contains(participantId) ? Result.success() : Result.failure("Participant Context ID must match 'aud' claim in 'access_token'");
        };

        TokenValidationRule subClaimsMatch = (at, additional) -> {
            var atSub = at.getStringClaim(SUBJECT);
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
        var result = tokenValidationService.validate(accessTokenString, id -> Result.success(stsPublicKey.get()), rules);
        if (result.failed()) {
            return result.mapTo();
        }

        // verify that the access_token contains a scope claim
        var scope = result.getContent().getStringClaim(ACCESS_TOKEN_SCOPE_CLAIM);
        return success(Arrays.asList(scope.split(SCOPE_SEPARATOR)));
    }
}
