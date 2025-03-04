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

package org.eclipse.edc.iam.identitytrust.sts.service;

import org.eclipse.edc.iam.identitytrust.sts.spi.service.StsAccountService;
import org.eclipse.edc.identityhub.spi.authentication.ParticipantSecureTokenService;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.spi.KeyIdDecorator;
import org.eclipse.edc.token.spi.TokenGenerationService;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.Nullable;

import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.AUDIENCE;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUER;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.SCOPE;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.SUBJECT;
import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;

public class EmbeddedSecureTokenService implements ParticipantSecureTokenService {
    public static final String PRESENTATION_TOKEN_CLAIM = "token";
    public static final String BEARER_ACCESS_SCOPE = "bearer_access_scope";
    private static final List<String> ACCESS_TOKEN_INHERITED_CLAIMS = List.of(ISSUER);
    private final TransactionContext transactionContext;
    private final long tokenValiditySeconds;
    private final TokenGenerationService tokenGenerationService;
    private final Clock clock;
    private final StsAccountService stsAccountService;

    public EmbeddedSecureTokenService(TransactionContext transactionContext,
                                      long tokenValiditySeconds,
                                      TokenGenerationService tokenGenerationService,
                                      Clock clock,
                                      StsAccountService stsAccountService) {
        this.transactionContext = transactionContext;
        this.tokenValiditySeconds = tokenValiditySeconds;
        this.tokenGenerationService = tokenGenerationService;
        this.clock = clock;
        this.stsAccountService = stsAccountService;
    }

    @Override
    public Result<TokenRepresentation> createToken(String participantContextId, Map<String, String> claims, @Nullable String bearerAccessScope) {
        return transactionContext.execute(() -> {
            var result = stsAccountService.findById(participantContextId);
            if (result.failed()) {
                return Result.failure(result.getFailureDetail());
            }


            var account = result.getContent();

            var keyId = account.getPublicKeyReference();
            var privateKeyAlias = account.getPrivateKeyAlias();
            var selfIssuedClaims = new HashMap<>(claims);

            return ofNullable(bearerAccessScope)
                    .map(scope -> createAndAcceptAccessToken(claims, scope, selfIssuedClaims::put, keyId, privateKeyAlias))
                    .orElse(success())
                    .compose(v -> {
                        var keyIdDecorator = new KeyIdDecorator(keyId);
                        return tokenGenerationService.generate(privateKeyAlias, keyIdDecorator, new SelfIssuedTokenDecorator(selfIssuedClaims, clock, tokenValiditySeconds));
                    });
        });
    }

    private Result<Void> createAndAcceptAccessToken(Map<String, String> claims, String scope, BiConsumer<String, String> consumer, String keyId, String privateKeyAlias) {
        return createAccessToken(claims, scope, keyId, privateKeyAlias)
                .compose(tokenRepresentation -> success(tokenRepresentation.getToken()))
                .onSuccess(withClaim(PRESENTATION_TOKEN_CLAIM, consumer))
                .mapEmpty();
    }

    private Result<TokenRepresentation> createAccessToken(Map<String, String> claims, String bearerAccessScope, String keyId, String privateKeyAlias) {
        var accessTokenClaims = new HashMap<>(accessTokenInheritedClaims(claims));
        var now = clock.instant();
        var exp = now.plusSeconds(tokenValiditySeconds);
        var jti = "accesstoken-%s".formatted(UUID.randomUUID());

        accessTokenClaims.put(SCOPE, bearerAccessScope);

        return addClaim(claims, ISSUER, withClaim(AUDIENCE, accessTokenClaims::put))
                .compose(v -> addClaim(claims, AUDIENCE, withClaim(SUBJECT, accessTokenClaims::put)))
                .compose(v -> {
                    var keyIdDecorator = new KeyIdDecorator(keyId);
                    return tokenGenerationService.generate(privateKeyAlias, keyIdDecorator, new AccessTokenDecorator(jti, now, exp, accessTokenClaims));
                });

    }

    private Result<Void> addClaim(Map<String, String> claims, String claim, Consumer<String> consumer) {
        var claimValue = claims.get(claim);
        if (claimValue != null) {
            consumer.accept(claimValue);
            return success();
        } else {
            return failure(format("Missing %s in the input claims", claim));
        }
    }

    private Consumer<String> withClaim(String key, BiConsumer<String, String> consumer) {
        return (value) -> consumer.accept(key, value);
    }

    private Map<String, String> accessTokenInheritedClaims(Map<String, String> claims) {
        return claims.entrySet().stream()
                .filter(entry -> ACCESS_TOKEN_INHERITED_CLAIMS.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
