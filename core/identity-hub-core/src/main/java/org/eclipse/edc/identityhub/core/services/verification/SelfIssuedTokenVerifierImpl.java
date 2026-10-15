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

package org.eclipse.edc.identityhub.core.services.verification;

import org.eclipse.edc.identityhub.publickey.KeyPairResourcePublicKeyResolver;
import org.eclipse.edc.identityhub.spi.participantcontext.IdentityHubParticipantContextService;
import org.eclipse.edc.identityhub.spi.verification.SelfIssuedTokenVerifier;
import org.eclipse.edc.identityhub.verification.IssuerKeyIdRule;
import org.eclipse.edc.identityhub.verification.SelfIssuedTokenPrecheck;
import org.eclipse.edc.identityhub.verification.SelfIssuedTokenPrecheck.UnverifiedToken;
import org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames;
import org.eclipse.edc.keys.spi.PublicKeyResolver;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.rules.AudienceValidationRule;
import org.eclipse.edc.token.rules.ExpirationIssuedAtValidationRule;
import org.eclipse.edc.token.spi.TokenValidationRule;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.eclipse.edc.verifiablecredentials.jwt.rules.IssuerEqualsSubjectRule;
import org.eclipse.edc.verifiablecredentials.jwt.rules.SubJwkIsNullRule;

import java.text.ParseException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.eclipse.edc.identityhub.spi.verification.SelfIssuedTokenConstants.ACCESS_TOKEN_SCOPE_CLAIM;
import static org.eclipse.edc.identityhub.spi.verification.SelfIssuedTokenConstants.DCP_PRESENTATION_ACCESS_TOKEN_CONTEXT;
import static org.eclipse.edc.identityhub.spi.verification.SelfIssuedTokenConstants.DCP_PRESENTATION_SELF_ISSUED_TOKEN_CONTEXT;
import static org.eclipse.edc.identityhub.spi.verification.SelfIssuedTokenConstants.TOKEN_CLAIM;

/**
 * Default implementation used to verify Self-Issued tokens. The public key is expected to be found in the
 * issuer's DID
 */
public class SelfIssuedTokenVerifierImpl implements SelfIssuedTokenVerifier {


    private static final String SCOPE_SEPARATOR = " ";
    private final TokenValidationService tokenValidationService;
    private final KeyPairResourcePublicKeyResolver localPublicKeyService;
    private final TokenValidationRulesRegistry tokenValidationRulesRegistry;
    private final PublicKeyResolver publicKeyResolver;
    private final IdentityHubParticipantContextService participantContextService;
    private final Clock clock;

    public SelfIssuedTokenVerifierImpl(TokenValidationService tokenValidationService,
                                       KeyPairResourcePublicKeyResolver localPublicKeyService,
                                       TokenValidationRulesRegistry tokenValidationRulesRegistry,
                                       PublicKeyResolver publicKeyResolver,
                                       IdentityHubParticipantContextService participantContextService,
                                       Clock clock) {
        this.tokenValidationService = tokenValidationService;
        this.localPublicKeyService = localPublicKeyService;
        this.tokenValidationRulesRegistry = tokenValidationRulesRegistry;
        this.publicKeyResolver = publicKeyResolver;
        this.participantContextService = participantContextService;
        this.clock = clock;
    }

    @Override
    public Result<List<String>> verify(String token, String participantContextId) {
        Objects.requireNonNull(participantContextId, "Participant Context ID is mandatory.");

        var participantDidResult = participantContextService.getParticipantContext(participantContextId);

        if (participantDidResult.failed()) {
            return Result.failure(participantDidResult.getFailureDetail());
        }
        var participantDid = participantDidResult.getContent().getDid();

        // resolving the signing key of the ID token means resolving the DID of its sender, so that is only done for
        // tokens that carry an access token this participant issued, which can be checked with a local key
        var precheck = SelfIssuedTokenPrecheck.check(token);
        if (precheck.failed()) {
            return precheck.mapFailure();
        }
        var unverified = precheck.getContent();
        var accessTokenOrigin = checkAccessTokenOrigin(unverified, participantDid, participantContextId);
        if (accessTokenOrigin.failed()) {
            return accessTokenOrigin.mapFailure();
        }

        var idTokenRules = new ArrayList<TokenValidationRule>();
        idTokenRules.add(new IssuerKeyIdRule(unverified.keyId()));
        idTokenRules.addAll(tokenValidationRulesRegistry.getRules(DCP_PRESENTATION_SELF_ISSUED_TOKEN_CONTEXT));
        idTokenRules.add(new IssuerEqualsSubjectRule());
        idTokenRules.add(new SubJwkIsNullRule());
        idTokenRules.add(new AudienceValidationRule(participantDid));
        var res = tokenValidationService.validate(token, publicKeyResolver, idTokenRules);

        if (res.failed()) {
            return res.mapFailure();
        }

        var claimToken = res.getContent();
        var accessTokenString = claimToken.getStringClaim(TOKEN_CLAIM);
        var issClaim = claimToken.getStringClaim(JwtRegisteredClaimNames.ISSUER);

        TokenValidationRule audMustMatchParticipantDidRule = (at, additional) -> {
            var aud = at.getListClaim(JwtRegisteredClaimNames.AUDIENCE);
            if (aud == null || aud.isEmpty()) {
                return Result.failure("Mandatory claim 'aud' on 'token' was null.");
            }

            return aud.contains(participantDid) ?
                    Result.success() :
                    Result.failure("The DID associated with the Participant Context ID of this request ('%s') must match 'aud' claim in 'access_token' (%s).".formatted(participantDid, aud));
        };

        TokenValidationRule subClaimsMatch = (at, additional) -> {
            var atSub = at.getStringClaim(JwtRegisteredClaimNames.SUBJECT);
            // correlate the verified issuer of the ID token (iss == sub) with access_token.sub
            if (atSub == null || !atSub.equals(issClaim)) {
                return Result.failure("ID token [sub] claim is not equal to [%s.sub] claim: expected '%s', got '%s'.".formatted(TOKEN_CLAIM, issClaim, atSub));
            }
            return Result.success();
        };

        // verify the correctness of the 'access_token'
        var rules = new ArrayList<>(tokenValidationRulesRegistry.getRules(DCP_PRESENTATION_ACCESS_TOKEN_CONTEXT));
        rules.add(subClaimsMatch);
        rules.add(audMustMatchParticipantDidRule);
        // todo: verify that the resolved public key belongs to the participant ID
        var result = tokenValidationService.validate(accessTokenString, keyId -> localPublicKeyService.resolveKey(keyId, participantContextId), rules);
        if (result.failed()) {
            return result.mapFailure();
        }

        // verify that the access_token contains a scope claim
        var scope = result.getContent().getStringClaim(ACCESS_TOKEN_SCOPE_CLAIM);
        return Result.success(Arrays.asList(scope.split(SCOPE_SEPARATOR)));
    }

    /**
     * Checks that the access token embedded in the (not yet verified) ID token was issued by this participant, for the
     * sender of the ID token, and has not expired. Rules with side effects, such as the JTI check, are left to the full
     * validation, which still runs afterwards.
     */
    private Result<Void> checkAccessTokenOrigin(UnverifiedToken idToken, String participantDid, String participantContextId) {
        String accessToken;
        try {
            accessToken = idToken.claims().getStringClaim(TOKEN_CLAIM);
        } catch (ParseException e) {
            return Result.failure("Failed to decode token");
        }
        if (accessToken == null) {
            return Result.failure("Required claim '%s' not present on token.".formatted(TOKEN_CLAIM));
        }

        // the access token must have been issued to the sender of the ID token
        TokenValidationRule subjectMatches = (at, additional) -> {
            var atSub = at.getStringClaim(JwtRegisteredClaimNames.SUBJECT);
            return idToken.issuer().equals(atSub) ?
                    Result.success() :
                    Result.failure("ID token [sub] claim is not equal to [%s.sub] claim: expected '%s', got '%s'.".formatted(TOKEN_CLAIM, idToken.issuer(), atSub));
        };
        var rules = List.of(new AudienceValidationRule(participantDid), subjectMatches, new ExpirationIssuedAtValidationRule(clock, 5, false));
        return tokenValidationService.validate(accessToken, keyId -> localPublicKeyService.resolveKey(keyId, participantContextId), rules)
                .mapEmpty();
    }
}
