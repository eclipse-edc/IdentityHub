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

import org.eclipse.edc.identityhub.protocols.dcp.spi.DcpHolderTokenVerifier;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.DcpRequestContext;
import org.eclipse.edc.identityhub.spi.participantcontext.model.IdentityHubParticipantContext;
import org.eclipse.edc.identityhub.verification.SelfIssuedTokenPrecheck;
import org.eclipse.edc.issuerservice.spi.holder.model.Holder;
import org.eclipse.edc.issuerservice.spi.holder.store.HolderStore;
import org.eclipse.edc.keys.spi.PublicKeyResolver;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.token.rules.AudienceValidationRule;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.eclipse.edc.verifiablecredentials.jwt.rules.IssuerKeyIdValidationRule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.eclipse.edc.identityhub.protocols.dcp.issuer.DcpIssuerCoreExtension.DCP_ISSUER_SELF_ISSUED_TOKEN_CONTEXT;
import static org.eclipse.edc.identityhub.spi.verification.SelfIssuedTokenConstants.TOKEN_CLAIM;

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
        // the signing key is resolved from the DID of the sender, so the sender must be known (or anonymous holders be
        // allowed) before that happens
        var precheck = SelfIssuedTokenPrecheck.check(tokenRepresentation.getToken());
        if (precheck.failed()) {
            return ServiceResult.unauthorized(precheck.getFailureDetail());
        }
        var unverified = precheck.getContent();
        var participantContextId = issuerContext.getParticipantContextId();

        return findHolder(participantContextId, unverified.issuer())
                .compose(holder -> {
                    if (holder.isEmpty() && !allowAnonymous) {
                        return ServiceResult.unauthorized("Participant not found");
                    }
                    return validateToken(issuerContext, tokenRepresentation, unverified.keyId())
                            // an anonymous Holder is only recorded once it has proven control over its DID
                            .compose(accessToken -> holder.map(ServiceResult::success)
                                    .orElseGet(() -> createAnonymousHolder(participantContextId, unverified.issuer()))
                                    .map(h -> new DcpRequestContext(h, Map.of(), accessToken)));
                });
    }

    private ServiceResult<Optional<Holder>> findHolder(String participantContextId, String holderDid) {
        // a Holder belongs to the Issuer it was registered with, so only Holders of the addressed Issuer are eligible
        var query = QuerySpec.Builder.newInstance().filter(List.of(
                Criterion.criterion("did", "=", holderDid),
                Criterion.criterion("participantContextId", "=", participantContextId))).build();
        var holdersResult = store.query(query);
        if (holdersResult.failed()) {
            return ServiceResult.from(holdersResult).mapFailure();
        }

        return ServiceResult.success(holdersResult.getContent().stream()
                .filter(holder -> participantContextId.equals(holder.getParticipantContextId()))
                .findFirst());
    }

    private ServiceResult<Holder> createAnonymousHolder(String participantContextId, String holderDid) {
        var newHolder = Holder.Builder.newInstance()
                .holderId(UUID.randomUUID().toString())
                .did(holderDid)
                .participantContextId(participantContextId)
                .isAnonymous(true)
                .build();
        return ServiceResult.from(store.create(newHolder)).map(u -> newHolder);
    }

    private ServiceResult<String> validateToken(IdentityHubParticipantContext issuerContext, TokenRepresentation token, String kid) {

        var rules = rulesRegistry.getRules(DCP_ISSUER_SELF_ISSUED_TOKEN_CONTEXT);
        var newRules = new ArrayList<>(rules);
        newRules.add(new AudienceValidationRule(issuerContext.getDid()));
        newRules.add(new IssuerKeyIdValidationRule(kid));
        var res = tokenValidationService.validate(token.getToken(), publicKeyResolver, newRules);
        if (res.failed()) {
            return ServiceResult.unauthorized("Token validation failed: " + res.getFailureDetail());
        }
        // the Holder may grant access to its Credential Service by embedding an access token, which must be presented
        // back to it when the credentials are delivered
        return ServiceResult.success(res.getContent().getStringClaim(TOKEN_CLAIM));
    }

}
