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

package org.eclipse.edc.identityhub.sts.accountservice;

import org.eclipse.edc.iam.identitytrust.sts.spi.model.StsAccount;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.spi.result.StoreResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RemoteStsAccountServiceTest {

    private static final String PARTICIPANT_CONTEXT_ID = "test-participant";
    private static final String PARTICIPANT_DID = "did:web:" + PARTICIPANT_CONTEXT_ID;
    private static final String KEY_ID = "test-key-id";
    private final RemoteStsAccountService accountServiceMock = new RemoteStsAccountService();

    @Test
    void create() {
        when(stsAccountStore.create(any())).thenReturn(StoreResult.success(createStsClient().build()));

        assertThat(accountServiceMock.createAccount(createManifest().build(), "test-alias")).isSucceeded();

        verify(stsAccountStore).create(any());
    }

    @Test
    void create_withCustomSecretAlias() {
        when(stsAccountStore.create(any())).thenReturn(StoreResult.success(createStsClient().build()));

        assertThat(accountServiceMock.createAccount(createManifest().build(), "test-alias")).isSucceeded();

        verify(stsAccountStore).create(any());
    }

    @Test
    void create_whenClientAlreadyExists() {
        when(stsAccountStore.create(any())).thenReturn(StoreResult.alreadyExists("foo"));

        var res = accountServiceMock.createAccount(createManifest().build(), "test-alias");
        assertThat(res).isFailed()
                .detail().isEqualTo("foo");

        verify(stsAccountStore).create(any());
    }

    @Test
    void update_succeeds() {
        var account = createStsClient().build();
        when(stsAccountStore.update(any())).thenAnswer(a -> StoreResult.success(a.getArguments()[0]));

        assertThat(accountServiceMock.updateAccount(account)).isSucceeded();

        verify(stsAccountStore).update(any());
        verifyNoMoreInteractions(stsAccountStore);
    }

    @Test
    void update_storageFailure() {
        var account = createStsClient().build();
        when(stsAccountStore.update(any())).thenAnswer(a -> StoreResult.notFound("foo"));

        assertThat(accountServiceMock.updateAccount(account)).isFailed().detail().isEqualTo("foo");

        verify(stsAccountStore).update(any());
        verifyNoMoreInteractions(stsAccountStore);
    }


    private StsAccount.Builder createStsClient() {
        return StsAccount.Builder.newInstance()
                .id("test-id")
                .name("test-name")
                .did("did:web:" + PARTICIPANT_CONTEXT_ID)
                .secretAlias("test-secret")
                .publicKeyReference("public-key-ref")
                .privateKeyAlias("private-key-alias")
                .clientId("client-id");
    }

    private ParticipantManifest.Builder createManifest() {
        return ParticipantManifest.Builder.newInstance()
                .participantId(PARTICIPANT_CONTEXT_ID)
                .active(true)
                .did(PARTICIPANT_DID)
                .key(KeyDescriptor.Builder.newInstance()
                        .privateKeyAlias(KEY_ID + "-alias")
                        .keyGeneratorParams(Map.of("algorithm", "EdDSA", "curve", "Ed25519"))
                        .keyId(KEY_ID)
                        .build()
                );
    }
}