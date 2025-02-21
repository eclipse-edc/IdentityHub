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

import org.assertj.core.api.Assertions;
import org.eclipse.edc.iam.identitytrust.sts.spi.model.StsAccount;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.iam.oauth2.spi.client.SharedSecretOauth2CredentialsRequest;
import org.eclipse.edc.identityhub.spi.participantcontext.StsAccountService;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.UUID;

import static java.lang.String.format;
import static org.eclipse.edc.identityhub.sts.RemoteSecureTokenService.AUDIENCE_PARAM;
import static org.eclipse.edc.identityhub.sts.RemoteSecureTokenService.BEARER_ACCESS_SCOPE;
import static org.eclipse.edc.identityhub.sts.RemoteSecureTokenService.GRANT_TYPE;
import static org.eclipse.edc.identityhub.sts.RemoteSecureTokenService.PRESENTATION_TOKEN_CLAIM;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.AUDIENCE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RemoteSecureTokenServiceTest {
    public static final String TEST_SECRET_ALIAS = "test-secret-alias";
    public static final String TOKEN_URL = "http://foo.com/auth/token";
    private static final String PARTICIPANT_ID = "test-participant";
    private final Oauth2Client oauth2Client = mock();
    private final Vault vault = mock();
    private final StsAccountService stsAccountService = mock();
    private RemoteSecureTokenService secureTokenService;

    @BeforeEach
    void setup() {
        secureTokenService = new RemoteSecureTokenService(oauth2Client, new NoopTransactionContext(), vault, TOKEN_URL, stsAccountService);
        when(stsAccountService.findById(PARTICIPANT_ID)).thenReturn(ServiceResult.success(StsAccount.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .name(PARTICIPANT_ID)
                .did("did:web:test")
                .privateKeyAlias("privateKeyAlias")
                .publicKeyReference("public-key")
                .clientId(PARTICIPANT_ID)
                .secretAlias(TEST_SECRET_ALIAS)
                .build()));
    }

    @Test
    void createToken() {
        var audience = "aud";
        var secret = "secret";
        when(oauth2Client.requestToken(any())).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().build()));
        when(vault.resolveSecret(eq(TEST_SECRET_ALIAS))).thenReturn(secret);

        assertThat(secureTokenService.createToken(PARTICIPANT_ID, Map.of(AUDIENCE, audience), null)).isSucceeded();

        var captor = ArgumentCaptor.forClass(SharedSecretOauth2CredentialsRequest.class);
        verify(oauth2Client).requestToken(captor.capture());

        Assertions.assertThat(captor.getValue()).satisfies(request -> {
            Assertions.assertThat(request.getUrl()).isEqualTo(TOKEN_URL);
            Assertions.assertThat(request.getClientId()).isEqualTo(PARTICIPANT_ID);
            Assertions.assertThat(request.getGrantType()).isEqualTo(GRANT_TYPE);
            Assertions.assertThat(request.getClientSecret()).isEqualTo(secret);
            Assertions.assertThat(request.getParams())
                    .containsEntry(AUDIENCE_PARAM, audience);
        });
    }

    @Test
    void createToken_withAccessScope() {
        var audience = "aud";
        var bearerAccessScope = "scope";
        var secret = "secret";

        when(oauth2Client.requestToken(any())).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().build()));
        when(vault.resolveSecret(eq(TEST_SECRET_ALIAS))).thenReturn(secret);

        assertThat(secureTokenService.createToken(PARTICIPANT_ID, Map.of(AUDIENCE, audience), bearerAccessScope)).isSucceeded();

        var captor = ArgumentCaptor.forClass(SharedSecretOauth2CredentialsRequest.class);
        verify(oauth2Client).requestToken(captor.capture());

        Assertions.assertThat(captor.getValue()).satisfies(request -> {
            Assertions.assertThat(request.getUrl()).isEqualTo(TOKEN_URL);
            Assertions.assertThat(request.getClientId()).isEqualTo(PARTICIPANT_ID);
            Assertions.assertThat(request.getGrantType()).isEqualTo(GRANT_TYPE);
            Assertions.assertThat(request.getClientSecret()).isEqualTo(secret);
            Assertions.assertThat(request.getParams())
                    .containsEntry(AUDIENCE_PARAM, audience)
                    .containsEntry(BEARER_ACCESS_SCOPE, bearerAccessScope);
        });
    }

    @Test
    void createToken_withAccessToken() {
        var audience = "aud";
        var accessToken = "accessToken";
        var secret = "secret";

        when(oauth2Client.requestToken(any())).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().build()));
        when(vault.resolveSecret(eq(TEST_SECRET_ALIAS))).thenReturn(secret);

        assertThat(secureTokenService.createToken(PARTICIPANT_ID, Map.of(AUDIENCE, audience, PRESENTATION_TOKEN_CLAIM, accessToken), null)).isSucceeded();

        var captor = ArgumentCaptor.forClass(SharedSecretOauth2CredentialsRequest.class);
        verify(oauth2Client).requestToken(captor.capture());

        Assertions.assertThat(captor.getValue()).satisfies(request -> {
            Assertions.assertThat(request.getUrl()).isEqualTo(TOKEN_URL);
            Assertions.assertThat(request.getClientId()).isEqualTo(PARTICIPANT_ID);
            Assertions.assertThat(request.getGrantType()).isEqualTo(GRANT_TYPE);
            Assertions.assertThat(request.getClientSecret()).isEqualTo(secret);
            Assertions.assertThat(request.getParams())
                    .containsEntry(AUDIENCE_PARAM, audience)
                    .containsEntry(PRESENTATION_TOKEN_CLAIM, accessToken);
        });
    }

    @Test
    void createToken_shouldFail_whenSecretIsNotPresent() {
        var audience = "aud";
        var accessToken = "accessToken";

        when(oauth2Client.requestToken(any())).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().build()));
        when(vault.resolveSecret(eq(TEST_SECRET_ALIAS))).thenReturn(null);

        assertThat(secureTokenService.createToken(PARTICIPANT_ID, Map.of(AUDIENCE, audience, PRESENTATION_TOKEN_CLAIM, accessToken), null))
                .isFailed()
                .detail().isEqualTo(format("Failed to fetch client secret from the vault with alias: %s", TEST_SECRET_ALIAS));

    }

    @Test
    void createToken_whenNoStsAccount_expectFailure() {
        when(stsAccountService.findById(PARTICIPANT_ID)).thenReturn(ServiceResult.notFound("foo"));

        assertThat(secureTokenService.createToken(PARTICIPANT_ID, Map.of(AUDIENCE, "audience"), null)).isFailed()
                .detail()
                .isEqualTo("foo");
    }
}