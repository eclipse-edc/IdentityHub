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
import org.eclipse.edc.iam.identitytrust.sts.spi.store.StsAccountStore;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class StsAccountServiceImplTest {

    private static final String PARTICIPANT_CONTEXT_ID = "test-participant";
    private static final String PARTICIPANT_DID = "did:web:" + PARTICIPANT_CONTEXT_ID;
    private static final String KEY_ID = "test-key-id";
    private final StsAccountStore stsAccountStore = mock();
    private final Vault vault = mock();
    private final StsAccountServiceImpl stsAccountService = new StsAccountServiceImpl(stsAccountStore, new NoopTransactionContext(), vault, new RandomStringGenerator());

    @Test
    void create() {
        when(stsAccountStore.create(any())).thenReturn(StoreResult.success(createClient().build()));

        assertThat(stsAccountService.createAccount(createManifest().build(), "test-alias")).isSucceeded();

        verify(stsAccountStore).create(any());
    }

    @Test
    void create_withCustomSecretAlias() {
        when(stsAccountStore.create(any())).thenReturn(StoreResult.success(createClient().build()));

        assertThat(stsAccountService.createAccount(createManifest().build(), "test-alias")).isSucceeded();

        verify(stsAccountStore).create(any());
    }

    @Test
    void create_whenClientAlreadyExists() {
        when(stsAccountStore.create(any())).thenReturn(StoreResult.alreadyExists("foo"));

        var res = stsAccountService.createAccount(createManifest().build(), "test-alias");
        assertThat(res).isFailed()
                .detail().isEqualTo("foo");

        verify(stsAccountStore).create(any());
    }

    @Test
    void update_succeeds() {
        var account = createClient().build();
        when(stsAccountStore.update(any())).thenAnswer(a -> StoreResult.success(a.getArguments()[0]));

        assertThat(stsAccountService.updateAccount(account)).isSucceeded();

        verify(stsAccountStore).update(any());
        verifyNoMoreInteractions(stsAccountStore);
    }

    @Test
    void update_storageFailure() {
        var account = createClient().build();
        when(stsAccountStore.update(any())).thenAnswer(a -> StoreResult.notFound("foo"));

        assertThat(stsAccountService.updateAccount(account)).isFailed().detail().isEqualTo("foo");

        verify(stsAccountStore).update(any());
        verifyNoMoreInteractions(stsAccountStore);
    }


    @Test
    void findByClientId() {
        var clientId = "clientId";
        var client = createClient(clientId);

        when(stsAccountStore.findByClientId(clientId)).thenReturn(StoreResult.success(client));

        var inserted = stsAccountService.findByClientId(clientId);

        assertThat(inserted).isSucceeded().isEqualTo(client);
        verify(stsAccountStore).findByClientId(clientId);
        verifyNoMoreInteractions(stsAccountStore);
    }

    @Test
    void authenticate() {
        var clientId = "clientId";
        var secret = "secret";
        var client = createClient(clientId);
        when(vault.resolveSecret(client.getSecretAlias())).thenReturn(secret);

        var inserted = stsAccountService.authenticate(client, secret);

        assertThat(inserted).isSucceeded();
        verify(vault).resolveSecret(client.getSecretAlias());
    }

    @Test
    void update() {
        when(stsAccountStore.update(any())).thenReturn(StoreResult.success());

        var client = createClient("clientId");
        assertThat(stsAccountService.updateAccount(client)).isSucceeded();
        verify(stsAccountStore).update(client);
        verifyNoInteractions(vault);
    }

    @Test
    void update_whenNotExists() {
        when(stsAccountStore.update(any())).thenReturn(StoreResult.notFound("foo"));

        var client = createClient("clientId");
        assertThat(stsAccountService.updateAccount(client)).isFailed()
                .detail().contains("foo");
        verify(stsAccountStore).update(client);
        verifyNoMoreInteractions(vault, stsAccountStore);
    }

    @Test
    void updateSecret() {
        var client = createClient("clientId");
        var oldAlias = client.getSecretAlias();
        when(stsAccountStore.findById(any())).thenReturn(StoreResult.success(client));
        when(stsAccountStore.update(any())).thenReturn(StoreResult.success());
        when(vault.deleteSecret(eq(oldAlias))).thenReturn(Result.success());
        when(vault.storeSecret(eq("new-alias"), eq("new-secret"))).thenReturn(Result.success());

        assertThat(stsAccountService.updateSecret(client.getId(), "new-alias", "new-secret")).isSucceeded();


        verify(stsAccountStore).findById(client.getId());
        verify(vault).deleteSecret(oldAlias);
        verify(vault).storeSecret("new-alias", "new-secret");
        verify(stsAccountStore).update(any());
        verifyNoMoreInteractions(stsAccountStore, vault);
    }

    @Test
    void updateSecret_aliasNull() {
        var client = createClient("clientId");

        assertThatThrownBy(() -> stsAccountService.updateSecret(client.getId(), null, "some-secret"))
                .isInstanceOf(NullPointerException.class);
        verifyNoInteractions(stsAccountStore, vault);
    }

    @Test
    void updateSecret_secretNull() {
        var client = createClient("clientId");
        var oldAlias = client.getSecretAlias();
        when(stsAccountStore.findById(any())).thenReturn(StoreResult.success(client));
        when(stsAccountStore.update(any())).thenReturn(StoreResult.success());
        when(vault.deleteSecret(eq(oldAlias))).thenReturn(Result.success());
        when(vault.storeSecret(eq("new-alias"), anyString())).thenReturn(Result.success());

        assertThat(stsAccountService.updateSecret(client.getId(), "new-alias")).isSucceeded();

        verify(stsAccountStore).findById(client.getId());
        verify(vault).deleteSecret(oldAlias);
        verify(vault).storeSecret(eq("new-alias"), anyString());
        verify(stsAccountStore).update(any());
        verifyNoMoreInteractions(stsAccountStore, vault);
    }

    @Test
    void updateSecret_whenNotExists() {
        var client = createClient("clientId");
        when(stsAccountStore.findById(any())).thenReturn(StoreResult.notFound("foo"));

        assertThat(stsAccountService.updateSecret(client.getId(), "new-alias")).isFailed().detail()
                .isEqualTo("foo");

        verify(stsAccountStore).findById(client.getId());
        verifyNoMoreInteractions(stsAccountStore, vault);
    }

    @Test
    void updateSecret_vaultFailsToDelete() {
        var client = createClient("clientId");
        var oldAlias = client.getSecretAlias();
        when(stsAccountStore.findById(any())).thenReturn(StoreResult.success(client));
        when(stsAccountStore.update(any())).thenReturn(StoreResult.success());
        when(vault.deleteSecret(eq(oldAlias))).thenReturn(Result.failure("foo"));

        assertThat(stsAccountService.updateSecret(client.getId(), "new-alias")).isFailed();

        verify(stsAccountStore).findById(client.getId());
        verify(vault).deleteSecret(oldAlias);
        verify(stsAccountStore).update(any());
        verifyNoMoreInteractions(stsAccountStore, vault);
    }

    @Test
    void deleteById() {
        when(stsAccountStore.deleteById(any())).thenReturn(StoreResult.success(createClient("test-id")));
        when(vault.deleteSecret(any())).thenReturn(Result.success());
        assertThat(stsAccountService.deleteAccount("test-id")).isSucceeded();
        verify(stsAccountStore).deleteById("test-id");
        verify(vault).deleteSecret(any());
        verifyNoMoreInteractions(stsAccountStore, vault);
    }

    @Test
    void deleteById_whenNotExists() {
        when(stsAccountStore.deleteById(any())).thenReturn(StoreResult.notFound("foo"));
        assertThat(stsAccountService.deleteAccount("test-id")).isFailed().detail().isEqualTo("foo");
        verify(stsAccountStore).deleteById("test-id");
        verifyNoMoreInteractions(stsAccountStore, vault);
    }

    @Test
    void query() {
        var id1 = createClient("id1");
        var id2 = createClient("id2");
        when(stsAccountStore.findAll(any())).thenReturn(Stream.of(id1, id2));

        assertThat(stsAccountService.queryAccounts(QuerySpec.max()))
                .containsExactlyInAnyOrder(id1, id2);
    }

    @Test
    void query_noResults() {
        when(stsAccountStore.findAll(any())).thenReturn(Stream.of());
        assertThat(stsAccountService.queryAccounts(QuerySpec.max()))
                .isEmpty();
    }

    @Test
    void findById() {
        var client = createClient("test-id");
        when(stsAccountStore.findById(anyString())).thenReturn(StoreResult.success(client));
        assertThat(stsAccountService.findById("test-id")).isSucceeded()
                .usingRecursiveComparison()
                .isEqualTo(client);
    }

    @Test
    void findById_whenNotExists() {
        when(stsAccountStore.findById(anyString())).thenReturn(StoreResult.notFound("foo"));
        assertThat(stsAccountService.findById("test-id")).isFailed()
                .detail().isEqualTo("foo");
    }

    private StsAccount.Builder createClient() {
        return StsAccount.Builder.newInstance()
                .id("test-id")
                .name("test-name")
                .did("did:web:" + PARTICIPANT_CONTEXT_ID)
                .secretAlias("test-secret")
                .publicKeyReference("public-key-ref")
                .privateKeyAlias("private-key-alias")
                .clientId("client-id");
    }

    private StsAccount createClient(String clientId) {
        return StsAccount.Builder.newInstance()
                .id(clientId)
                .name("test-name")
                .did("did:web:" + PARTICIPANT_CONTEXT_ID)
                .secretAlias("test-secret")
                .publicKeyReference("public-key-ref")
                .privateKeyAlias("private-key-alias")
                .clientId(clientId)
                .build();
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