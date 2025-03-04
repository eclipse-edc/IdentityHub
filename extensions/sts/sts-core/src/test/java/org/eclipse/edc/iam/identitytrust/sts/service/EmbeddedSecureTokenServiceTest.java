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

import org.eclipse.edc.iam.identitytrust.sts.spi.model.StsAccount;
import org.eclipse.edc.iam.identitytrust.sts.spi.service.StsAccountService;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.token.spi.KeyIdDecorator;
import org.eclipse.edc.token.spi.TokenDecorator;
import org.eclipse.edc.token.spi.TokenGenerationService;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.util.Map;

import static com.nimbusds.jwt.JWTClaimNames.AUDIENCE;
import static com.nimbusds.jwt.JWTClaimNames.ISSUER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmbeddedSecureTokenServiceTest {
    public static final String TEST_PRIVATEKEY_ID = "test-privatekey-id";
    public static final String TEST_PARTICIPANT = "test-participant";
    private final TokenGenerationService tokenGenerationService = mock();
    private final StsAccountService stsAccountService = mock();
    private final EmbeddedSecureTokenService sts = new EmbeddedSecureTokenService(new NoopTransactionContext(), 10 * 60, tokenGenerationService, Clock.systemUTC(), stsAccountService);

    @BeforeEach
    void setup() {
        when(stsAccountService.findById(anyString())).thenReturn(
                ServiceResult.success(StsAccount.Builder.newInstance()
                        .id("key-pair-id")
                        .privateKeyAlias(TEST_PRIVATEKEY_ID)
                        .publicKeyReference("key-id")
                        .clientId(TEST_PRIVATEKEY_ID)
                        .secretAlias(TEST_PARTICIPANT + "-alias")
                        .name(TEST_PARTICIPANT)
                        .did("did:web:" + TEST_PARTICIPANT)
                        .build()));
    }

    @Test
    void createToken_withoutBearerAccessScope() {
        var token = TokenRepresentation.Builder.newInstance().token("test").build();

        when(tokenGenerationService.generate(eq(TEST_PRIVATEKEY_ID), any(TokenDecorator[].class))).thenReturn(Result.success(token));
        var result = sts.createToken(TEST_PARTICIPANT, Map.of(), null);

        assertThat(result.succeeded()).isTrue();
        var captor = ArgumentCaptor.forClass(TokenDecorator[].class);

        verify(tokenGenerationService).generate(any(), captor.capture());

        assertThat(captor.getAllValues()).hasSize(1)
                .allSatisfy(decorators -> {
                    assertThat(decorators).hasSize(2)
                            .hasOnlyElementsOfTypes(KeyIdDecorator.class, SelfIssuedTokenDecorator.class);
                });

    }

    @Test
    void createToken_withBearerAccessScope() {

        var claims = Map.of(ISSUER, "testIssuer", AUDIENCE, "aud");
        var token = TokenRepresentation.Builder.newInstance().token("test").build();

        when(tokenGenerationService.generate(eq(TEST_PRIVATEKEY_ID), any(TokenDecorator[].class)))
                .thenReturn(Result.success(token))
                .thenReturn(Result.success(token));


        var result = sts.createToken(TEST_PARTICIPANT, claims, "scope:test");

        assertThat(result.succeeded()).isTrue();
        var captor = ArgumentCaptor.forClass(TokenDecorator[].class);

        verify(tokenGenerationService, times(2)).generate(any(), captor.capture());

        assertThat(captor.getAllValues()).hasSize(2)
                .satisfies(decorators -> {
                    assertThat(decorators.get(0))
                            .hasSize(2)
                            .hasOnlyElementsOfTypes(KeyIdDecorator.class, AccessTokenDecorator.class, SelfIssuedTokenDecorator.class);

                    assertThat(decorators.get(1))
                            .hasSize(2)
                            .hasOnlyElementsOfTypes(KeyIdDecorator.class, AccessTokenDecorator.class, SelfIssuedTokenDecorator.class);
                });

    }

    @Test
    void createToken_error_whenAccessTokenFails() {

        var claims = Map.of(ISSUER, "testIssuer", AUDIENCE, "aud");

        var token = TokenRepresentation.Builder.newInstance().token("test").build();

        when(tokenGenerationService.generate(eq(TEST_PRIVATEKEY_ID), any(TokenDecorator[].class)))
                .thenReturn(Result.failure("Failed to create access token"))
                .thenReturn(Result.success(token));

        var result = sts.createToken(TEST_PARTICIPANT, claims, "scope:test");

        assertThat(result.failed()).isTrue();
        var captor = ArgumentCaptor.forClass(TokenDecorator[].class);

        verify(tokenGenerationService, times(1)).generate(any(), captor.capture());

        assertThat(captor.getValue())
                .hasSize(2)
                .hasOnlyElementsOfTypes(SelfIssuedTokenDecorator.class, AccessTokenDecorator.class, KeyIdDecorator.class);

    }

    @Test
    void createToken_error_whenSelfTokenFails() {
        var claims = Map.of(ISSUER, "testIssuer", AUDIENCE, "aud");

        var token = TokenRepresentation.Builder.newInstance().token("test").build();

        when(tokenGenerationService.generate(eq(TEST_PRIVATEKEY_ID), any(TokenDecorator[].class)))
                .thenReturn(Result.success(token))
                .thenReturn(Result.failure("Failed to create access token"));


        var result = sts.createToken(TEST_PARTICIPANT, claims, "scope:test");

        assertThat(result.failed()).isTrue();

        verify(tokenGenerationService, times(2)).generate(eq(TEST_PRIVATEKEY_ID), any(TokenDecorator[].class));

    }

    @Test
    void createToken_whenStsAccountNotfound_expectFailure() {
        when(stsAccountService.findById(anyString())).thenReturn(ServiceResult.notFound("foobar"));

        var result = sts.createToken(TEST_PARTICIPANT, Map.of(), null);
        assertThat(result).isFailed()
                .detail()
                .isEqualTo("foobar");
    }
}