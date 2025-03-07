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

import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.identityhub.publickey.KeyPairResourcePublicKeyResolver;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.verification.SelfIssuedTokenVerifier;
import org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames;
import org.eclipse.edc.keys.spi.PublicKeyResolver;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.rules.AudienceValidationRule;
import org.eclipse.edc.token.spi.TokenValidationRule;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.eclipse.edc.verifiablecredentials.jwt.rules.IssuerKeyIdValidationRule;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
    private final ParticipantContextService participantContextService;

    public SelfIssuedTokenVerifierImpl(TokenValidationService tokenValidationService,
                                       KeyPairResourcePublicKeyResolver localPublicKeyService,
                                       TokenValidationRulesRegistry tokenValidationRulesRegistry,
                                       PublicKeyResolver publicKeyResolver,
                                       ParticipantContextService participantContextService) {
        this.tokenValidationService = tokenValidationService;
        this.localPublicKeyService = localPublicKeyService;
        this.tokenValidationRulesRegistry = tokenValidationRulesRegistry;
        this.publicKeyResolver = publicKeyResolver;
        this.participantContextService = participantContextService;
    }

    @Override
    public Result<List<String>> verify(String token, String participantContextId) {
        Objects.requireNonNull(participantContextId, "Participant Context ID is mandatory.");

        var participantDidResult = participantContextService.getParticipantContext(participantContextId);

        if (participantDidResult.failed()) {
            return Result.failure(participantDidResult.getFailureDetail());
        }
        var pcDid = participantDidResult.getContent().getDid();

        var res = getKid(token).compose(kid -> {
            var rules = new ArrayList<>(tokenValidationRulesRegistry.getRules(DCP_PRESENTATION_SELF_ISSUED_TOKEN_CONTEXT));
            rules.add(new IssuerKeyIdValidationRule(kid));
            rules.add(new AudienceValidationRule(pcDid));
            return tokenValidationService.validate(token, publicKeyResolver, rules);
        });

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

            return aud.contains(pcDid) ?
                    Result.success() :
                    Result.failure("The DID associated with the Participant Context ID of this request ('%s') must match 'aud' claim in 'access_token' (%s).".formatted(pcDid, aud));
        };

        TokenValidationRule subClaimsMatch = (at, additional) -> {
            var atSub = at.getStringClaim(JwtRegisteredClaimNames.SUBJECT);
            // correlate sub and access_token.sub
            if (!Objects.equals(subClaim, atSub)) {
                return Result.failure("ID token [sub] claim is not equal to [%s.sub] claim: expected '%s', got '%s'.".formatted(TOKEN_CLAIM, subClaim, atSub));
            }
            return Result.success();
        };

        // verify the correctness of the 'access_token'
        var rules = new ArrayList<>(tokenValidationRulesRegistry.getRules(DCP_PRESENTATION_ACCESS_TOKEN_CONTEXT));
        rules.add(subClaimsMatch);
        rules.add(audMustMatchParticipantIdRule);
        // todo: verify that the resolved public key belongs to the participant ID
        var result = tokenValidationService.validate(accessTokenString, keyId -> localPublicKeyService.resolveKey(keyId, participantContextId), rules);
        if (result.failed()) {
            return result.mapFailure();
        }

        // verify that the access_token contains a scope claim
        var scope = result.getContent().getStringClaim(ACCESS_TOKEN_SCOPE_CLAIM);
        return Result.success(Arrays.asList(scope.split(SCOPE_SEPARATOR)));
    }

    private Result<String> getKid(String token) {
        try {
            return Optional.ofNullable(SignedJWT.parse(token).getHeader().getKeyID())
                    .map(Result::success)
                    .orElseGet(() -> Result.failure("Kid not present"));
        } catch (ParseException e) {
            return Result.failure("Failed to decode token");
        }
    }
}
