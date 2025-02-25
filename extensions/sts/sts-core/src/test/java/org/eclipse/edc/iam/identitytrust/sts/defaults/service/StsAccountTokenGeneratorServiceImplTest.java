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

package org.eclipse.edc.iam.identitytrust.sts.defaults.service;


import org.eclipse.edc.iam.identitytrust.sts.service.StsClientTokenGeneratorServiceImpl;
import org.eclipse.edc.iam.identitytrust.sts.spi.model.StsAccountTokenAdditionalParams;
import org.eclipse.edc.identityhub.spi.authentication.ParticipantSecureTokenService;
import org.eclipse.edc.jwt.validation.jti.JtiValidationStore;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceFailure;
import org.eclipse.edc.spi.result.StoreResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.identitytrust.sts.spi.store.fixtures.TestFunctions.createClient;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StsAccountTokenGeneratorServiceImplTest {

    public static final long TOKEN_EXPIRATION = 60 * 5;
    private final JtiValidationStore jtiValidationStore = mock();
    private final ParticipantSecureTokenService sts = mock();
    private StsClientTokenGeneratorServiceImpl clientTokenService;

    @BeforeEach
    void setup() {
        when(jtiValidationStore.storeEntry(any())).thenReturn(StoreResult.success());
        when(sts.createToken(anyString(), anyMap(), anyString())).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().build()));
        clientTokenService = new StsClientTokenGeneratorServiceImpl(TOKEN_EXPIRATION, sts);
    }

    @Test
    void tokenFor() {
        var client = createClient("clientId");
        var token = TokenRepresentation.Builder.newInstance().token("token").expiresIn(TOKEN_EXPIRATION).build();
        when(sts.createToken(anyString(), anyMap(), isNull())).thenReturn(Result.success(token));

        var inserted = clientTokenService.tokenFor(client, StsAccountTokenAdditionalParams.Builder.newInstance().audience("aud").build());

        assertThat(inserted).isSucceeded().usingRecursiveComparison().isEqualTo(token);
    }

    @Test
    void tokenFor_error_whenGeneratorFails() {
        var client = createClient("clientId");
        when(sts.createToken(anyString(), anyMap(), isNull())).thenReturn(Result.failure("failure"));

        var inserted = clientTokenService.tokenFor(client, StsAccountTokenAdditionalParams.Builder.newInstance().audience("aud").build());

        assertThat(inserted).isFailed()
                .satisfies(serviceFailure -> {
                    assertThat(serviceFailure.getReason()).isEqualTo(ServiceFailure.Reason.BAD_REQUEST);
                    assertThat(serviceFailure.getFailureDetail()).isEqualTo("failure");
                });
    }

}
