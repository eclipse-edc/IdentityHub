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

package org.eclipse.edc.identityhub.protocols.dcp.issuer;

import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.identityhub.protocols.dcp.spi.DcpHolderTokenVerifier;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.DcpRequestContext;
import org.eclipse.edc.identityhub.spi.participantcontext.model.IdentityHubParticipantContext;
import org.eclipse.edc.issuerservice.spi.holder.model.Holder;
import org.eclipse.edc.issuerservice.spi.holder.store.HolderStore;
import org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames;
import org.eclipse.edc.keys.spi.PublicKeyResolver;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.token.rules.AudienceValidationRule;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.eclipse.edc.verifiablecredentials.jwt.rules.IssuerKeyIdValidationRule;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.eclipse.edc.identityhub.protocols.dcp.issuer.DcpIssuerCoreExtension.DCP_ISSUER_SELF_ISSUED_TOKEN_CONTEXT;

public class DcpHolderTokenVerifierImpl implements DcpHolderTokenVerifier {

    private final TokenValidationRulesRegistry rulesRegistry;
    private final TokenValidationService tokenValidationService;
    private final PublicKeyResolver publicKeyResolver;
    private final HolderStore store;
    private final boolean allowAnonymous;

    public DcpHolderTokenVerifierImpl(TokenValidationRulesRegistry rulesRegistry, TokenValidationService tokenValidationService, PublicKeyResolver publicKeyResolver, HolderStore store, boolean allowAnonymous) {
        this.rulesRegistry = rulesRegistry;
        this.tokenValidationService = tokenValidationService;
        this.publicKeyResolver = publicKeyResolver;
        this.store = store;
        this.allowAnonymous = allowAnonymous;
    }


    @Override
    public ServiceResult<DcpRequestContext> verify(IdentityHubParticipantContext issuerContext, TokenRepresentation tokenRepresentation) {
        return getTokenIssuer(tokenRepresentation.getToken())
                .compose(token -> getParticipant(issuerContext.getParticipantContextId(), token))
                .compose(participant ->
                        getKid(tokenRepresentation.getToken())
                                .compose(kid -> validateToken(issuerContext, tokenRepresentation, participant, kid))
                );
    }

    private ServiceResult<String> getTokenIssuer(String token) {
        try {
            return Optional.ofNullable(SignedJWT.parse(token).getJWTClaimsSet().getClaim(JwtRegisteredClaimNames.ISSUER))
                    .map(Object::toString)
                    .map(ServiceResult::success)
                    .orElseGet(() -> ServiceResult.unauthorized("Issuer claim not present"));
        } catch (ParseException e) {
            return ServiceResult.badRequest("Failed to decode token");
        }
    }

    private ServiceResult<String> getKid(String token) {
        try {
            return Optional.ofNullable(SignedJWT.parse(token).getHeader().getKeyID())
                    .map(ServiceResult::success)
                    .orElseGet(() -> ServiceResult.unauthorized("Kid not present"));
        } catch (ParseException e) {
            return ServiceResult.badRequest("Failed to decode token");
        }
    }

    private ServiceResult<Holder> getParticipant(String participantContextId, String holderDid) {
        var query = QuerySpec.Builder.newInstance().filter(Criterion.criterion("did", "=", holderDid)).build();
        var holdersResult = store.query(query);
        if (holdersResult.failed()) {
            return ServiceResult.from(holdersResult).mapFailure();
        }

        var holders = holdersResult.getContent();

        if (holders.isEmpty() && allowAnonymous) {
            var newHolder = Holder.Builder.newInstance()
                    .holderId(UUID.randomUUID().toString())
                    .did(holderDid)
                    .participantContextId(participantContextId)
                    .isAnonymous(true)
                    .build();
            return ServiceResult.from(store.create(newHolder)).map(u -> newHolder);
        }
        return holders.stream().findFirst().map(ServiceResult::success).orElseGet(() -> ServiceResult.unauthorized("Participant not found"));
    }

    private ServiceResult<DcpRequestContext> validateToken(IdentityHubParticipantContext issuerContext, TokenRepresentation token, Holder holder, String kid) {

        var rules = rulesRegistry.getRules(DCP_ISSUER_SELF_ISSUED_TOKEN_CONTEXT);
        var newRules = new ArrayList<>(rules);
        newRules.add(new AudienceValidationRule(issuerContext.getDid()));
        newRules.add(new IssuerKeyIdValidationRule(kid));
        var res = tokenValidationService.validate(token.getToken(), publicKeyResolver, newRules);
        if (res.failed()) {
            return ServiceResult.unauthorized("Token validation failed: " + res.getFailureDetail());
        }
        return ServiceResult.success(new DcpRequestContext(holder, Map.of()));
    }

}
