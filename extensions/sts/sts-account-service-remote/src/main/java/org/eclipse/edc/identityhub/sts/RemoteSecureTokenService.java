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

package org.eclipse.edc.identityhub.sts;

import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2CredentialsRequest;
import org.eclipse.edc.iam.oauth2.spi.client.SharedSecretOauth2CredentialsRequest;
import org.eclipse.edc.identityhub.spi.authentication.ParticipantSecureTokenService;
import org.eclipse.edc.identityhub.spi.participantcontext.StsAccountService;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.stream.Collectors;

import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.AUDIENCE;

public class RemoteSecureTokenService implements ParticipantSecureTokenService {
    public static final String PRESENTATION_TOKEN_CLAIM = "token";
    public static final String BEARER_ACCESS_SCOPE = "bearer_access_scope";
    public static final String GRANT_TYPE = "client_credentials";
    public static final String AUDIENCE_PARAM = "audience";

    private static final Map<String, String> CLAIM_MAPPING = Map.of(
            AUDIENCE, AUDIENCE_PARAM,
            PRESENTATION_TOKEN_CLAIM, PRESENTATION_TOKEN_CLAIM);


    private final Oauth2Client oauth2Client;
    private final TransactionContext transactionContext;
    private final Vault vault;
    private final String tokenUrl;
    private final StsAccountService stsAccountService;

    public RemoteSecureTokenService(Oauth2Client oauth2Client, TransactionContext transactionContext, Vault vault, String tokenUrl, StsAccountService stsAccountService) {
        this.oauth2Client = oauth2Client;
        this.transactionContext = transactionContext;
        this.vault = vault;
        this.tokenUrl = tokenUrl;
        this.stsAccountService = stsAccountService;
    }

    @Override
    public Result<TokenRepresentation> createToken(String participantContextId, Map<String, String> claims, @Nullable String bearerAccessScope) {
        return transactionContext.execute(() -> {
            var result = stsAccountService.findById(participantContextId);
            if (result.failed()) {
                return Result.failure(result.getFailureDetail());
            }
            var stsAccount = result.getContent();
            return createRequest(stsAccount.getClientId(), stsAccount.getSecretAlias(), claims, bearerAccessScope)
                    .compose(oauth2Client::requestToken);
        });
    }

    @NotNull
    private Result<Oauth2CredentialsRequest> createRequest(String clientId, String clientSecretAlias, Map<String, String> claims, @Nullable String bearerAccessScope) {

        var secret = vault.resolveSecret(clientSecretAlias);
        if (secret != null) {
            var builder = SharedSecretOauth2CredentialsRequest.Builder.newInstance()
                    .url(tokenUrl)
                    .clientId(clientId)
                    .clientSecret(secret)
                    .grantType(GRANT_TYPE);

            var additionalParams = claims.entrySet().stream()
                    .filter(entry -> CLAIM_MAPPING.containsKey(entry.getKey()))
                    .map(entry -> Map.entry(CLAIM_MAPPING.get(entry.getKey()), entry.getValue()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            if (bearerAccessScope != null) {
                additionalParams.put(BEARER_ACCESS_SCOPE, bearerAccessScope);
            }

            builder.params(additionalParams);
            return Result.success(builder.build());
        } else {
            return Result.failure("Failed to fetch client secret from the vault with alias: %s".formatted(clientSecretAlias));
        }
    }
}
