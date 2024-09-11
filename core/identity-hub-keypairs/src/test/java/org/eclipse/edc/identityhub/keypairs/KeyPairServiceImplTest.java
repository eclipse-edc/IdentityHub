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

package org.eclipse.edc.identityhub.keypairs;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.gen.OctetKeyPairGenerator;
import org.eclipse.edc.identityhub.spi.keypair.events.KeyPairObservable;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairResource;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairState;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.store.KeyPairResourceStore;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.result.StoreResult.success;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class KeyPairServiceImplTest {

    private final KeyPairResourceStore keyPairResourceStore = mock(i -> StoreResult.success());
    private final Vault vault = mock();
    private final KeyPairObservable observableMock = mock();
    private final KeyPairServiceImpl keyPairService = new KeyPairServiceImpl(keyPairResourceStore, vault, mock(), observableMock, new NoopTransactionContext());


    @ParameterizedTest(name = "make default: {0}")
    @ValueSource(booleans = { true, false })
    void addKeyPair_publicKeyGiven(boolean makeDefault) {

        when(keyPairResourceStore.create(any())).thenReturn(success());
        var key = createKey().publicKeyJwk(createJwk()).publicKeyPem(null).keyGeneratorParams(null).build();

        assertThat(keyPairService.addKeyPair("some-participant", key, makeDefault)).isSucceeded();

        verify(keyPairResourceStore).create(argThat(kpr -> kpr.isDefaultPair() == makeDefault && kpr.getParticipantId().equals("some-participant")));
        // new key is set to active - expect an update in the DB
        verify(keyPairResourceStore).update(argThat(kpr -> !kpr.getId().equals(key.getKeyId()) && kpr.getState() == KeyPairState.ACTIVATED.code()));
        verify(observableMock, times(2)).invokeForEach(any());
        verifyNoMoreInteractions(keyPairResourceStore, vault, observableMock);
    }

    @ParameterizedTest(name = "make default: {0}")
    @ValueSource(booleans = { true, false })
    void addKeyPair_shouldGenerate_storesInVault(boolean makeDefault) {
        when(keyPairResourceStore.create(any())).thenReturn(success());

        var key = createKey().publicKeyJwk(null).publicKeyPem(null).keyGeneratorParams(Map.of(
                "algorithm", "EdDSA",
                "curve", "Ed25519"
        )).build();

        assertThat(keyPairService.addKeyPair("some-participant", key, makeDefault)).isSucceeded();

        verify(vault).storeSecret(eq(key.getPrivateKeyAlias()), anyString());
        verify(keyPairResourceStore).create(argThat(kpr -> kpr.isDefaultPair() == makeDefault &&
                kpr.getParticipantId().equals("some-participant") &&
                kpr.getState() == KeyPairState.ACTIVATED.code()));
        // new key is set to active - expect an update in the DB
        verify(keyPairResourceStore).update(argThat(kpr -> !kpr.getId().equals(key.getKeyId()) && kpr.getState() == KeyPairState.ACTIVATED.code()));
        verify(observableMock, times(2)).invokeForEach(any());
        verifyNoMoreInteractions(keyPairResourceStore, vault, observableMock);
    }

    @Test
    void addKeyPair_assertActiveState_whenKeyActive() {
        var isActive = true;
        when(keyPairResourceStore.query(any())).thenReturn(success(Collections.emptySet()));
        when(keyPairResourceStore.create(any())).thenReturn(success());

        var key = createKey().publicKeyJwk(null).publicKeyPem(null)
                .active(isActive)
                .keyGeneratorParams(Map.of(
                        "algorithm", "EdDSA",
                        "curve", "Ed25519"
                )).build();

        assertThat(keyPairService.addKeyPair("some-participant", key, true)).isSucceeded();

        verify(vault).storeSecret(eq(key.getPrivateKeyAlias()), anyString());
        //expect the query for other active keys at least once, if the new key is inactive
        verify(keyPairResourceStore, never()).query(any());
        verify(keyPairResourceStore).create(argThat(kpr -> kpr.isDefaultPair() && kpr.getParticipantId().equals("some-participant") && kpr.getState() == KeyPairState.ACTIVATED.code()));
        // new key is set to active - expect an update in the DB
        verify(keyPairResourceStore).update(argThat(kpr -> !kpr.getId().equals(key.getKeyId()) && kpr.getState() == KeyPairState.ACTIVATED.code()));
        verify(observableMock, times(2)).invokeForEach(any());
        verifyNoMoreInteractions(keyPairResourceStore, vault, observableMock);
    }

    @Test
    void addKeyPair_assertActiveState_whenKeyNotActive() {
        var isActive = false;
        when(keyPairResourceStore.query(any())).thenReturn(success(Collections.emptySet()));
        when(keyPairResourceStore.create(any())).thenReturn(success());

        var key = createKey().publicKeyJwk(null).publicKeyPem(null)
                .active(isActive)
                .keyGeneratorParams(Map.of(
                        "algorithm", "EdDSA",
                        "curve", "Ed25519"
                )).build();

        assertThat(keyPairService.addKeyPair("some-participant", key, true)).isSucceeded();

        verify(vault).storeSecret(eq(key.getPrivateKeyAlias()), anyString());
        //expect the query for other active keys at least once, if the new key is inactive
        verify(keyPairResourceStore, times(1)).query(any());
        verify(keyPairResourceStore).create(argThat(kpr -> kpr.isDefaultPair() && kpr.getParticipantId().equals("some-participant") && kpr.getState() == KeyPairState.CREATED.code()));
        verify(observableMock, times(1)).invokeForEach(any());
        verifyNoMoreInteractions(keyPairResourceStore, vault, observableMock);
    }

    @Test
    void addKeyPair_participantNotFound() {
        // can be implemented once events are used https://github.com/eclipse-edc/IdentityHub/issues/232
    }

    @Test
    void rotateKeyPair_withNewKey() {
        var oldId = "old-id";
        var oldKey = createKeyPairResource().id(oldId).build();

        when(keyPairResourceStore.query(any())).thenReturn(success(List.of(oldKey)));
        when(keyPairResourceStore.create(any())).thenReturn(success());

        var newKey = createKey().publicKeyPem("foobarpem").publicKeyJwk(null).keyGeneratorParams(null).build();

        assertThat(keyPairService.rotateKeyPair(oldId, newKey, Duration.ofDays(100).toMillis())).isSucceeded();

        verify(keyPairResourceStore).query(any());
        verify(keyPairResourceStore).update(argThat(kpr -> kpr.getId().equals(oldId)));
        verify(keyPairResourceStore).create(any());
        // new key is set to active - expect an update in the DB
        verify(keyPairResourceStore).update(argThat(kpr -> !kpr.getId().equals(oldId) && kpr.getState() == KeyPairState.ACTIVATED.code()));
        verify(vault).deleteSecret(eq(oldKey.getPrivateKeyAlias())); //deletes old private key
        verify(observableMock, times(3)).invokeForEach(any()); // 1 for rotate, 1 for add, 1 for update
        verifyNoMoreInteractions(keyPairResourceStore, vault, observableMock);
    }

    @Test
    void rotateKeyPair_withoutNewKey() {
        var oldId = "old-id";
        var oldKey = createKeyPairResource().id(oldId).build();

        when(keyPairResourceStore.query(any())).thenReturn(success(List.of(oldKey)));
        when(keyPairResourceStore.create(any())).thenReturn(success());

        assertThat(keyPairService.rotateKeyPair(oldId, null, Duration.ofDays(100).toMillis())).isSucceeded();

        verify(keyPairResourceStore).query(any());
        verify(keyPairResourceStore).update(argThat(kpr -> kpr.getId().equals(oldId)));
        verify(vault).deleteSecret(eq(oldKey.getPrivateKeyAlias())); //deletes old private key
        verify(observableMock).invokeForEach(any());
        verifyNoMoreInteractions(keyPairResourceStore, vault, observableMock);
    }

    @Test
    void rotateKeyPair_withNewKeyGenerate() {
        var oldId = "old-id";
        var oldKey = createKeyPairResource().id(oldId).build();

        when(keyPairResourceStore.query(any())).thenReturn(success(List.of(oldKey)));
        when(keyPairResourceStore.create(any())).thenReturn(success());

        var newKey = createKey().publicKeyPem(null).publicKeyJwk(null).keyGeneratorParams(Map.of(
                "algorithm", "EdDSA",
                "curve", "Ed25519"
        )).build();

        assertThat(keyPairService.rotateKeyPair(oldId, newKey, Duration.ofDays(100).toMillis())).isSucceeded();

        verify(keyPairResourceStore).query(any());
        verify(keyPairResourceStore).update(argThat(kpr -> kpr.getId().equals(oldId)));
        verify(keyPairResourceStore).create(any());
        // new key is set to active - expect an update in the DB
        verify(keyPairResourceStore).update(argThat(kpr -> !kpr.getId().equals(oldId) && kpr.getState() == KeyPairState.ACTIVATED.code()));
        verify(vault).deleteSecret(eq(oldKey.getPrivateKeyAlias())); //deletes old private key
        verify(vault).storeSecret(eq(newKey.getPrivateKeyAlias()), anyString());
        verify(observableMock, times(3)).invokeForEach(any()); // 1 for rotate, 1 for add
        verifyNoMoreInteractions(keyPairResourceStore, vault, observableMock);
    }

    @Test
    void rotateKeyPair_oldKeyWasDefault_withNewKey() {
        var oldId = "old-id";
        var oldKey = createKeyPairResource().isDefaultPair(true).id(oldId).build();

        when(keyPairResourceStore.query(any())).thenReturn(success(List.of(oldKey)));
        when(keyPairResourceStore.create(any())).thenReturn(success());

        var newKey = createKey().publicKeyPem(null).publicKeyJwk(null).keyGeneratorParams(Map.of(
                "algorithm", "EdDSA",
                "curve", "Ed25519"
        )).build();

        assertThat(keyPairService.rotateKeyPair(oldId, newKey, Duration.ofDays(100).toMillis())).isSucceeded();

        verify(keyPairResourceStore).query(any());
        verify(keyPairResourceStore).update(argThat(kpr -> kpr.getId().equals(oldId)));
        verify(keyPairResourceStore).create(argThat(KeyPairResource::isDefaultPair));
        // new key is set to active - expect an update in the DB
        verify(keyPairResourceStore).update(argThat(kpr -> !kpr.getId().equals(oldId) && kpr.getState() == KeyPairState.ACTIVATED.code()));
        verify(vault).deleteSecret(eq(oldKey.getPrivateKeyAlias())); //deletes old private key
        verify(vault).storeSecret(eq(newKey.getPrivateKeyAlias()), anyString());
        verify(observableMock, times(3)).invokeForEach(any()); //1 for revoke, 1 for add
        verifyNoMoreInteractions(keyPairResourceStore, vault, observableMock);
    }

    @Test
    void rotateKeyPair_oldKeyNotFound() {
        when(keyPairResourceStore.query(any())).thenReturn(success(List.of()));
        when(keyPairResourceStore.create(any())).thenReturn(success());

        var newKey = createKey().build();

        assertThat(keyPairService.rotateKeyPair("not-exist", newKey, Duration.ofDays(100).toMillis())).isFailed()
                .detail().isEqualTo("A KeyPairResource with ID 'not-exist' does not exist.");

        verify(keyPairResourceStore).query(any());
        verifyNoMoreInteractions(keyPairResourceStore, vault, observableMock);
    }

    @Test
    void revokeKey_withNewKey() {
        var oldId = "old-id";
        var oldKey = createKeyPairResource().id(oldId).build();
        when(keyPairResourceStore.query(any())).thenReturn(success(List.of(oldKey)));
        when(keyPairResourceStore.create(any())).thenReturn(success());

        var newKey = createKey().build();
        assertThat(keyPairService.revokeKey(oldId, newKey)).isSucceeded();

        verify(keyPairResourceStore).query(any());
        verify(keyPairResourceStore).update(argThat(kpr -> kpr.getId().equals(oldId) && kpr.getState() == KeyPairState.REVOKED.code()));
        verify(vault).deleteSecret(oldKey.getPrivateKeyAlias());
        verify(keyPairResourceStore).create(argThat(kpr -> !kpr.isDefaultPair()));
        // new key is set to active - expect an update in the DB
        verify(keyPairResourceStore).update(argThat(kpr -> !kpr.getId().equals(oldId) && kpr.getState() == KeyPairState.ACTIVATED.code()));
        verifyNoMoreInteractions(vault, keyPairResourceStore);
    }

    @Test
    void revokeKey_withoutNewKey() {
        var oldId = "old-id";
        var oldKey = createKeyPairResource().isDefaultPair(true).id(oldId).build();
        when(keyPairResourceStore.query(any())).thenReturn(success(List.of(oldKey)));

        assertThat(keyPairService.revokeKey(oldId, null)).isSucceeded();

        verify(keyPairResourceStore).query(any());
        verify(keyPairResourceStore).update(argThat(kpr -> kpr.getId().equals(oldId) && kpr.getState() == KeyPairState.REVOKED.code()));
        verify(vault).deleteSecret(oldKey.getPrivateKeyAlias());
        verify(observableMock).invokeForEach(any());
        verifyNoMoreInteractions(keyPairResourceStore, vault, observableMock);
    }

    @Test
    void revokeKey_oldKeyWasDefault_withNewKey() {
        var oldId = "old-id";
        var oldKey = createKeyPairResource().isDefaultPair(true).id(oldId).build();
        when(keyPairResourceStore.query(any())).thenReturn(success(List.of(oldKey)));
        when(keyPairResourceStore.create(any())).thenReturn(success());

        var newKey = createKey().build();
        assertThat(keyPairService.revokeKey(oldId, newKey)).isSucceeded();

        verify(keyPairResourceStore).query(any());
        verify(keyPairResourceStore).update(argThat(kpr -> kpr.getId().equals(oldId) && kpr.getState() == KeyPairState.REVOKED.code()));
        verify(vault).deleteSecret(oldKey.getPrivateKeyAlias());
        verify(keyPairResourceStore).create(argThat(KeyPairResource::isDefaultPair));
        // new key is set to active - expect an update in the DB
        verify(keyPairResourceStore).update(argThat(kpr -> !kpr.getId().equals(oldId) && kpr.getState() == KeyPairState.ACTIVATED.code()));
        verify(observableMock, times(3)).invokeForEach(any()); // 1 for revoke, 1 for add
        verifyNoMoreInteractions(keyPairResourceStore, vault, observableMock);
    }

    @Test
    void revokeKey_oldKeyWasDefault_withNewKeyNotActive() {
        var oldId = "old-id";
        var oldKey = createKeyPairResource().isDefaultPair(true).id(oldId).build();
        when(keyPairResourceStore.query(any())).thenReturn(success(List.of(oldKey)));
        when(keyPairResourceStore.create(any())).thenReturn(success());

        var newKey = createKey().active(false).build();
        assertThat(keyPairService.revokeKey(oldId, newKey)).isSucceeded();

        // queries twice, because the key is not active, tries to determine whether there are other active keys
        verify(keyPairResourceStore, times(2)).query(any());
        verify(keyPairResourceStore).update(argThat(kpr -> kpr.getId().equals(oldId) && kpr.getState() == KeyPairState.REVOKED.code()));
        verify(vault).deleteSecret(oldKey.getPrivateKeyAlias());
        verify(keyPairResourceStore).create(argThat(KeyPairResource::isDefaultPair));
        // new key is set to inactive, do not expect a DB update
        verify(observableMock, times(2)).invokeForEach(any()); // 1 for revoke, 1 for add
        verifyNoMoreInteractions(keyPairResourceStore, vault, observableMock);
    }

    @Test
    void revokeKey_oldKeyWasDefault_withoutNewKey() {
        var oldId = "old-id";
        var oldKey = createKeyPairResource().isDefaultPair(true).id(oldId).build();
        when(keyPairResourceStore.query(any())).thenReturn(success(List.of(oldKey)));

        assertThat(keyPairService.revokeKey(oldId, null)).isSucceeded();

        verify(keyPairResourceStore).query(any());
        verify(keyPairResourceStore).update(argThat(kpr -> kpr.getId().equals(oldId) && kpr.getState() == KeyPairState.REVOKED.code()));
        verify(vault).deleteSecret(oldKey.getPrivateKeyAlias());
        verify(observableMock).invokeForEach(any());
        verifyNoMoreInteractions(keyPairResourceStore, vault, observableMock);
    }

    @Test
    void revokeKey_notfound() {
        when(keyPairResourceStore.query(any())).thenReturn(success(List.of()));

        var newKey = createKey().build();

        assertThat(keyPairService.revokeKey("not-exist", newKey)).isFailed()
                .detail().isEqualTo("A KeyPairResource with ID 'not-exist' does not exist.");

        verify(keyPairResourceStore).query(any());
        verifyNoMoreInteractions(keyPairResourceStore, vault, observableMock);
    }

    @ParameterizedTest(name = "Valid state = {0}")
    // cannot use enum literals and the .code() method -> needs to be compile constant
    @ValueSource(ints = { 100, 200 })
    void activate(int validState) {
        var oldId = "old-id";
        var oldKey = createKeyPairResource().id(oldId).state(validState).build();

        when(keyPairResourceStore.query(any())).thenReturn(success(List.of(oldKey)));
        when(keyPairResourceStore.update(any())).thenReturn(success());

        assertThat(keyPairService.activate(oldId)).isSucceeded();
    }

    @ParameterizedTest(name = "Valid state = {0}")
    // cannot use enum literals and the .code() method -> needs to be compile constant
    @ValueSource(ints = { 0, 30, 400, -10 })
    void activate_invalidState(int validState) {
        var oldId = "old-id";
        var oldKey = createKeyPairResource().id(oldId).state(validState).build();

        when(keyPairResourceStore.query(any())).thenReturn(success(List.of(oldKey)));
        when(keyPairResourceStore.update(any())).thenReturn(success());

        assertThat(keyPairService.activate(oldId))
                .isFailed()
                .detail()
                .isEqualTo("The key pair resource is expected to be in [200, 100], but was %s".formatted(validState));
    }

    @Test
    void activate_notExists() {

        when(keyPairResourceStore.query(any())).thenReturn(success(List.of()));

        assertThat(keyPairService.activate("notexists"))
                .isFailed()
                .detail()
                .isEqualTo("A KeyPairResource with ID 'notexists' does not exist.");
    }

    private KeyPairResource.Builder createKeyPairResource() {
        return KeyPairResource.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .keyId("test-key-1")
                .privateKeyAlias("private-key-alias")
                .participantId("test-participant")
                .serializedPublicKey("this-is-a-pem-string")
                .useDuration(Duration.ofDays(6).toMillis());
    }

    @NotNull
    private KeyDescriptor.Builder createKey() {
        return KeyDescriptor.Builder.newInstance().keyId("test-kie")
                .privateKeyAlias("private-alias")
                .publicKeyJwk(createJwk());
    }

    private Map<String, Object> createJwk() {
        try {
            return new OctetKeyPairGenerator(Curve.Ed25519)
                    .generate()
                    .toJSONObject();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }
}