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
import org.eclipse.edc.identityhub.spi.keypair.events.KeyPairEventListener;
import org.eclipse.edc.identityhub.spi.keypair.events.KeyPairObservable;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairResource;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairState;
import org.eclipse.edc.identityhub.spi.keypair.store.KeyPairResourceStore;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.identityhub.transaction.TrackingTransactionContext;
import org.eclipse.edc.participantcontext.spi.store.ParticipantContextStore;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContextState;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceFailure;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.security.Vault;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatcher;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.identityhub.spi.participantcontext.model.IdentityHubParticipantContext.API_TOKEN_ALIAS;
import static org.eclipse.edc.identityhub.spi.participantcontext.model.KeyPairUsage.CREDENTIAL_SIGNING;
import static org.eclipse.edc.identityhub.spi.participantcontext.model.KeyPairUsage.PRESENTATION_SIGNING;
import static org.eclipse.edc.identityhub.spi.participantcontext.model.KeyPairUsage.TOKEN_SIGNING;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.result.StoreResult.success;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class KeyPairServiceImplTest {

    public static final String PARTICIPANT_ID = "test-participant";
    private static final String NEW_KEY_ID = "test-kid";
    private final KeyPairResourceStore keyPairResourceStore = mock(i -> StoreResult.success());
    private final Vault vault = mock();
    private final KeyPairObservable observableMock = mock();
    private final ParticipantContextStore participantContextServiceMock = mock();
    private final Monitor monitor = mock();
    private final TrackingTransactionContext transactionContext = new TrackingTransactionContext();
    private final KeyPairServiceImpl keyPairService = new KeyPairServiceImpl(keyPairResourceStore, vault, monitor, observableMock, transactionContext, participantContextServiceMock);


    @BeforeEach
    void setup() {
        when(keyPairResourceStore.query(any())).thenReturn(success(List.of()));
        when(vault.storeSecret(anyString(), anyString(), anyString())).thenReturn(Result.success());
        when(vault.deleteSecret(anyString(), anyString())).thenReturn(Result.success());
        when(participantContextServiceMock.findById(anyString()))
                .thenReturn(StoreResult.success(ParticipantContext.Builder.newInstance()
                        .participantContextId(PARTICIPANT_ID)
                        .identity("did:example:123")
                        .property(API_TOKEN_ALIAS, "apitoken-alias").build()));
    }

    @ParameterizedTest(name = "make default: {0}")
    @ValueSource(booleans = {true, false})
    void addKeyPair_publicKeyGiven(boolean makeDefault) {

        when(keyPairResourceStore.create(any())).thenReturn(success());
        var key = createKey().publicKeyJwk(createJwk()).publicKeyPem(null).keyGeneratorParams(null).build();

        assertThat(keyPairService.addKeyPair(PARTICIPANT_ID, key, makeDefault)).isSucceeded();

        // the key ID of the new key is checked to not be in use already
        verify(keyPairResourceStore).query(argThat(isKeyIdQuery(key.getKeyId())));
        verify(keyPairResourceStore).create(argThat(kpr -> kpr.isDefaultPair() == makeDefault && kpr.getParticipantContextId().equals(PARTICIPANT_ID) &&
                kpr.getState() == KeyPairState.ACTIVATED.code()));
        // the key pair is written in its final state, and announced as added and activated at once
        verify(observableMock).invokeForEach(any());
        verifyNoMoreInteractions(keyPairResourceStore, vault, observableMock);
    }

    @ParameterizedTest(name = "make default: {0}")
    @ValueSource(booleans = {true, false})
    void addKeyPair_shouldGenerate_storesInVault(boolean makeDefault) {
        when(keyPairResourceStore.create(any())).thenReturn(success());

        var key = createKey().publicKeyJwk(null).publicKeyPem(null).keyGeneratorParams(Map.of(
                "algorithm", "EdDSA",
                "curve", "Ed25519"
        )).build();

        assertThat(keyPairService.addKeyPair(PARTICIPANT_ID, key, makeDefault)).isSucceeded();

        // an existing secret is never overwritten
        verify(vault).resolveSecret(PARTICIPANT_ID, key.getPrivateKeyAlias());
        verify(vault).storeSecret(anyString(), eq(key.getPrivateKeyAlias()), anyString());
        // the key ID of the new key is checked to not be in use already
        verify(keyPairResourceStore).query(argThat(isKeyIdQuery(key.getKeyId())));
        verify(keyPairResourceStore).create(argThat(kpr -> kpr.isDefaultPair() == makeDefault &&
                kpr.getParticipantContextId().equals(PARTICIPANT_ID) &&
                kpr.getState() == KeyPairState.ACTIVATED.code()));
        // the key pair is written in its final state, and announced as added and activated at once
        verify(observableMock).invokeForEach(any());
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

        assertThat(keyPairService.addKeyPair(PARTICIPANT_ID, key, true)).isSucceeded();

        // an existing secret is never overwritten
        verify(vault).resolveSecret(PARTICIPANT_ID, key.getPrivateKeyAlias());
        verify(vault).storeSecret(anyString(), eq(key.getPrivateKeyAlias()), anyString());
        // only the key ID of the new key is checked, other active keys are only looked for if the new key is inactive
        verify(keyPairResourceStore).query(argThat(isKeyIdQuery(key.getKeyId())));
        verify(keyPairResourceStore).create(argThat(kpr -> kpr.isDefaultPair() && kpr.getParticipantContextId().equals(PARTICIPANT_ID) && kpr.getState() == KeyPairState.ACTIVATED.code()));
        // the key pair is written in its final state, and announced as added and activated at once
        verify(observableMock).invokeForEach(any());
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

        assertThat(keyPairService.addKeyPair(PARTICIPANT_ID, key, true)).isSucceeded();

        // an existing secret is never overwritten
        verify(vault).resolveSecret(PARTICIPANT_ID, key.getPrivateKeyAlias());
        verify(vault).storeSecret(anyString(), eq(key.getPrivateKeyAlias()), anyString());
        // the key ID of the new key is checked, and other active keys are looked for, because the new key is inactive
        verify(keyPairResourceStore, times(2)).query(any());
        verify(keyPairResourceStore).query(argThat(isKeyIdQuery(key.getKeyId())));
        verify(keyPairResourceStore).create(argThat(kpr -> kpr.isDefaultPair() && kpr.getParticipantContextId().equals(PARTICIPANT_ID) && kpr.getState() == KeyPairState.CREATED.code()));
        verify(observableMock, times(1)).invokeForEach(any());
        verifyNoMoreInteractions(keyPairResourceStore, vault, observableMock);
    }

    @Test
    void addKeyPair_participantNotFound() {
        when(participantContextServiceMock.findById(anyString()))
                .thenReturn(StoreResult.notFound("A ParticipantContext with ID '%s' does not exist.".formatted(PARTICIPANT_ID)));

        assertThat(keyPairService.addKeyPair(PARTICIPANT_ID, createKey().build(), false)).isFailed()
                .detail().isEqualTo("A ParticipantContext with ID '%s' does not exist.".formatted(PARTICIPANT_ID));
    }


    @Test
    void addKeyPair_whenParticipantDeactivated_shouldFail() {
        var pc = ParticipantContext.Builder.newInstance()
                .participantContextId(PARTICIPANT_ID)
                .identity("did:example:123")
                .property(API_TOKEN_ALIAS, "apitoken-alias")
                .state(ParticipantContextState.DEACTIVATED)
                .build();
        when(participantContextServiceMock.findById(anyString())).thenReturn(StoreResult.success(pc));

        assertThat(keyPairService.addKeyPair(PARTICIPANT_ID, createKey().build(), false))
                .isFailed()
                .detail()
                .isEqualTo("To add a key pair, the ParticipantContext with ID '%s' must be in state ACTIVATED or CREATED but was DEACTIVATED.".formatted(PARTICIPANT_ID));
    }


    @Test
    void rotateKeyPair_withNewKey() {
        var oldId = "old-id";
        var oldKey = createKeyPairResource().id(oldId).build();

        storeFinds(oldKey);
        when(keyPairResourceStore.create(any())).thenReturn(success());

        var newKey = createKey().publicKeyPem("foobarpem").publicKeyJwk(null).keyGeneratorParams(null).build();

        assertThat(keyPairService.rotateKeyPair(oldId, newKey, Duration.ofDays(100).toMillis())).isSucceeded();

        // the old key is looked up, and the successor's key ID is checked before anything is changed
        verify(keyPairResourceStore, times(2)).query(any());
        verify(keyPairResourceStore).query(argThat(isKeyIdQuery(newKey.getKeyId())));
        verify(keyPairResourceStore).create(argThat(kpr -> !kpr.getId().equals(oldId) && kpr.getState() == KeyPairState.ACTIVATED.code()));
        verify(keyPairResourceStore).update(argThat(kpr -> kpr.getId().equals(oldId) && kpr.getState() == KeyPairState.ROTATED.code()));
        verify(vault).deleteSecret(anyString(), eq(oldKey.getPrivateKeyAlias())); //deletes old private key
        verify(observableMock, times(2)).invokeForEach(any()); // 1 for rotate, 1 for add and activate
        verifyNoMoreInteractions(keyPairResourceStore, vault, observableMock);
    }

    @Test
    void rotateKeyPair_withoutNewKey() {
        var oldId = "old-id";
        var oldKey = createKeyPairResource().id(oldId).build();

        storeFinds(oldKey);
        when(keyPairResourceStore.create(any())).thenReturn(success());

        assertThat(keyPairService.rotateKeyPair(oldId, null, Duration.ofDays(100).toMillis())).isSucceeded();

        verify(keyPairResourceStore).query(any());
        verify(keyPairResourceStore).update(argThat(kpr -> kpr.getId().equals(oldId)));
        verify(vault).deleteSecret(anyString(), eq(oldKey.getPrivateKeyAlias())); //deletes old private key
        verify(observableMock).invokeForEach(any());
        verifyNoMoreInteractions(keyPairResourceStore, vault, observableMock);
    }

    @Test
    void rotateKeyPair_withNewKeyGenerate() {
        var oldId = "old-id";
        var oldKey = createKeyPairResource().id(oldId).build();

        storeFinds(oldKey);
        when(keyPairResourceStore.create(any())).thenReturn(success());

        var newKey = createKey().publicKeyPem(null).publicKeyJwk(null).keyGeneratorParams(Map.of(
                "algorithm", "EdDSA",
                "curve", "Ed25519"
        )).build();

        assertThat(keyPairService.rotateKeyPair(oldId, newKey, Duration.ofDays(100).toMillis())).isSucceeded();

        // the old key is looked up, and the successor's key ID is checked before anything is changed
        verify(keyPairResourceStore, times(2)).query(any());
        verify(keyPairResourceStore).query(argThat(isKeyIdQuery(newKey.getKeyId())));
        verify(keyPairResourceStore).create(argThat(kpr -> !kpr.getId().equals(oldId) && kpr.getState() == KeyPairState.ACTIVATED.code()));
        verify(keyPairResourceStore).update(argThat(kpr -> kpr.getId().equals(oldId) && kpr.getState() == KeyPairState.ROTATED.code()));
        verify(vault).deleteSecret(anyString(), eq(oldKey.getPrivateKeyAlias())); //deletes old private key
        verify(vault).resolveSecret(PARTICIPANT_ID, newKey.getPrivateKeyAlias());
        verify(vault).storeSecret(anyString(), eq(newKey.getPrivateKeyAlias()), anyString());
        verify(observableMock, times(2)).invokeForEach(any()); // 1 for rotate, 1 for add and activate
        verifyNoMoreInteractions(keyPairResourceStore, vault, observableMock);
    }

    @Test
    void rotateKeyPair_oldKeyWasDefault_withNewKey() {
        var oldId = "old-id";
        var oldKey = createKeyPairResource().isDefaultPair(true).id(oldId).build();

        storeFinds(oldKey);
        when(keyPairResourceStore.create(any())).thenReturn(success());

        var newKey = createKey().publicKeyPem(null).publicKeyJwk(null).keyGeneratorParams(Map.of(
                "algorithm", "EdDSA",
                "curve", "Ed25519"
        )).build();

        assertThat(keyPairService.rotateKeyPair(oldId, newKey, Duration.ofDays(100).toMillis())).isSucceeded();

        // the old key is looked up, and the successor's key ID is checked before anything is changed
        verify(keyPairResourceStore, times(2)).query(any());
        verify(keyPairResourceStore).query(argThat(isKeyIdQuery(newKey.getKeyId())));
        verify(keyPairResourceStore).create(argThat(kpr -> !kpr.getId().equals(oldId) && kpr.getState() == KeyPairState.ACTIVATED.code() && kpr.isDefaultPair()));
        verify(keyPairResourceStore).update(argThat(kpr -> kpr.getId().equals(oldId) && kpr.getState() == KeyPairState.ROTATED.code()));
        verify(vault).deleteSecret(anyString(), eq(oldKey.getPrivateKeyAlias())); //deletes old private key
        verify(vault).resolveSecret(PARTICIPANT_ID, newKey.getPrivateKeyAlias());
        verify(vault).storeSecret(anyString(), eq(newKey.getPrivateKeyAlias()), anyString());
        verify(observableMock, times(2)).invokeForEach(any()); // 1 for rotate, 1 for add and activate
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
        storeFinds(oldKey);
        when(keyPairResourceStore.create(any())).thenReturn(success());

        var newKey = createKey().build();
        assertThat(keyPairService.revokeKey(oldId, newKey)).isSucceeded();

        // the old key is looked up, and the successor's key ID is checked before anything is changed
        verify(keyPairResourceStore, times(2)).query(any());
        verify(keyPairResourceStore).query(argThat(isKeyIdQuery(newKey.getKeyId())));
        verify(keyPairResourceStore).create(argThat(kpr -> !kpr.isDefaultPair() && kpr.getState() == KeyPairState.ACTIVATED.code()));
        verify(keyPairResourceStore).update(argThat(kpr -> kpr.getId().equals(oldId) && kpr.getState() == KeyPairState.REVOKED.code()));
        verify(vault).deleteSecret(anyString(), eq(oldKey.getPrivateKeyAlias()));
        verifyNoMoreInteractions(vault, keyPairResourceStore);
    }

    @Test
    void revokeKey_withoutNewKey() {
        var oldId = "old-id";
        var oldKey = createKeyPairResource().isDefaultPair(true).id(oldId).build();
        storeFinds(oldKey);

        assertThat(keyPairService.revokeKey(oldId, null)).isSucceeded();

        verify(keyPairResourceStore).query(any());
        verify(keyPairResourceStore).update(argThat(kpr -> kpr.getId().equals(oldId) && kpr.getState() == KeyPairState.REVOKED.code()));
        verify(vault).deleteSecret(anyString(), eq(oldKey.getPrivateKeyAlias()));
        verify(observableMock).invokeForEach(any());
        verifyNoMoreInteractions(keyPairResourceStore, vault, observableMock);
    }

    @Test
    void revokeKey_oldKeyWasDefault_withNewKey() {
        var oldId = "old-id";
        var oldKey = createKeyPairResource().isDefaultPair(true).id(oldId).build();
        storeFinds(oldKey);
        when(keyPairResourceStore.create(any())).thenReturn(success());

        var newKey = createKey().build();
        assertThat(keyPairService.revokeKey(oldId, newKey)).isSucceeded();

        // the old key is looked up, and the successor's key ID is checked before anything is changed
        verify(keyPairResourceStore, times(2)).query(any());
        verify(keyPairResourceStore).query(argThat(isKeyIdQuery(newKey.getKeyId())));
        verify(keyPairResourceStore).create(argThat(kpr -> kpr.isDefaultPair() && kpr.getState() == KeyPairState.ACTIVATED.code()));
        verify(keyPairResourceStore).update(argThat(kpr -> kpr.getId().equals(oldId) && kpr.getState() == KeyPairState.REVOKED.code()));
        verify(vault).deleteSecret(anyString(), eq(oldKey.getPrivateKeyAlias()));
        verify(observableMock, times(2)).invokeForEach(any()); // 1 for revoke, 1 for add and activate
        verifyNoMoreInteractions(keyPairResourceStore, vault, observableMock);
    }

    @Test
    void revokeKey_oldKeyWasDefault_withNewKeyNotActive() {
        var oldId = "old-id";
        var oldKey = createKeyPairResource().isDefaultPair(true).id(oldId).build();
        storeFinds(oldKey);
        when(keyPairResourceStore.create(any())).thenReturn(success());

        var newKey = createKey().active(false).build();
        assertThat(keyPairService.revokeKey(oldId, newKey)).isSucceeded();

        // looks up the old key, checks the successor's key ID, and, because the successor is not active, looks for other
        // active keys once the old one is revoked
        verify(keyPairResourceStore, times(3)).query(any());
        verify(keyPairResourceStore).query(argThat(isKeyIdQuery(newKey.getKeyId())));
        verify(keyPairResourceStore).create(argThat(kpr -> kpr.isDefaultPair() && kpr.getState() == KeyPairState.CREATED.code()));
        verify(keyPairResourceStore).update(argThat(kpr -> kpr.getId().equals(oldId) && kpr.getState() == KeyPairState.REVOKED.code()));
        verify(vault).deleteSecret(anyString(), eq(oldKey.getPrivateKeyAlias()));
        verify(observableMock, times(2)).invokeForEach(any()); // 1 for revoke, 1 for add
        verify(monitor).warning(contains("has no active key pairs"));
        verifyNoMoreInteractions(keyPairResourceStore, vault, observableMock);
    }

    @Test
    void revokeKey_oldKeyWasDefault_withoutNewKey() {
        var oldId = "old-id";
        var oldKey = createKeyPairResource().isDefaultPair(true).id(oldId).build();
        storeFinds(oldKey);

        assertThat(keyPairService.revokeKey(oldId, null)).isSucceeded();

        verify(keyPairResourceStore).query(any());
        verify(keyPairResourceStore).update(argThat(kpr -> kpr.getId().equals(oldId) && kpr.getState() == KeyPairState.REVOKED.code()));
        verify(vault).deleteSecret(anyString(), eq(oldKey.getPrivateKeyAlias()));
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

    @Test
    void addKeyPair_whenKeyIdInUse_shouldFailWithoutStoringKeyMaterial() {
        var existingKey = createKeyPairResource().keyId(NEW_KEY_ID).state(KeyPairState.ACTIVATED).build();
        when(keyPairResourceStore.query(argThat(isKeyIdQuery(NEW_KEY_ID)))).thenReturn(success(List.of(existingKey)));

        var key = createKey().publicKeyJwk(null).publicKeyPem(null).keyGeneratorParams(Map.of(
                "algorithm", "EdDSA",
                "curve", "Ed25519"
        )).build();

        // the key ID identifies the verification method in the DID document, which must be unique
        assertThat(keyPairService.addKeyPair(PARTICIPANT_ID, key, true)).isFailed()
                .extracting(ServiceFailure::getReason).isEqualTo(ServiceFailure.Reason.CONFLICT);
        verify(vault, never()).storeSecret(anyString(), anyString(), anyString());
        verify(keyPairResourceStore, never()).create(any());
        verifyNoInteractions(observableMock);
    }

    @Test
    void addKeyPair_whenKeyIdOnlyUsedByRevokedKey_shouldSucceed() {
        // a revoked key is not in the DID document anymore
        var revokedKey = createKeyPairResource().keyId(NEW_KEY_ID).state(KeyPairState.REVOKED).build();
        when(keyPairResourceStore.query(argThat(isKeyIdQuery(NEW_KEY_ID)))).thenReturn(success(List.of(revokedKey)));
        when(keyPairResourceStore.create(any())).thenReturn(success());

        var key = createKey().publicKeyJwk(createJwk()).publicKeyPem(null).keyGeneratorParams(null).build();

        assertThat(keyPairService.addKeyPair(PARTICIPANT_ID, key, true)).isSucceeded();
        verify(keyPairResourceStore).create(any());
    }

    @Test
    void rotateKeyPair_whenSuccessorReusesKeyId_shouldFailWithoutRotating() {
        var oldId = "old-id";
        var oldKey = createKeyPairResource().id(oldId).keyId(NEW_KEY_ID).state(KeyPairState.ACTIVATED).build();
        // the store finds the old key both by its ID and by its key ID
        when(keyPairResourceStore.query(any())).thenReturn(success(List.of(oldKey)));

        var newKey = createKey().build();

        // a rotated key stays in the DID document, so its successor cannot use the same key ID
        assertThat(keyPairService.rotateKeyPair(oldId, newKey, Duration.ofDays(100).toMillis())).isFailed()
                .extracting(ServiceFailure::getReason).isEqualTo(ServiceFailure.Reason.CONFLICT);
        verify(vault, never()).deleteSecret(anyString(), anyString());
        verify(keyPairResourceStore, never()).update(any());
        verify(keyPairResourceStore, never()).create(any());
        verifyNoInteractions(observableMock);
    }

    @Test
    void revokeKey_whenSuccessorReusesKeyIdOfRevokedKey_shouldSucceed() {
        var oldId = "old-id";
        var oldKey = createKeyPairResource().id(oldId).keyId(NEW_KEY_ID).state(KeyPairState.ACTIVATED).build();
        // the store finds the old key both by its ID and by its key ID
        when(keyPairResourceStore.query(any())).thenReturn(success(List.of(oldKey)));
        when(keyPairResourceStore.create(any())).thenReturn(success());

        var newKey = createKey().build();

        // a revoked key is removed from the DID document, so its successor may use the same key ID
        assertThat(keyPairService.revokeKey(oldId, newKey)).isSucceeded();
        verify(keyPairResourceStore).create(argThat(kpr -> kpr.getKeyId().equals(NEW_KEY_ID)));
    }

    @Test
    void addKeyPair_whenSecretWithAliasExists_shouldFailWithoutOverwritingIt() {
        // e.g. the private key of another key pair
        when(vault.resolveSecret(PARTICIPANT_ID, "private-alias")).thenReturn("existing-secret");
        var key = createKey().publicKeyJwk(null).publicKeyPem(null).keyGeneratorParams(Map.of(
                "algorithm", "EdDSA",
                "curve", "Ed25519"
        )).build();

        assertThat(keyPairService.addKeyPair(PARTICIPANT_ID, key, true)).isFailed()
                .extracting(ServiceFailure::getReason).isEqualTo(ServiceFailure.Reason.CONFLICT);
        verify(vault, never()).storeSecret(anyString(), anyString(), anyString());
        verify(keyPairResourceStore, never()).create(any());
        verifyNoInteractions(observableMock);
    }

    @Test
    void addKeyPair_whenStoringPrivateKeyFails_shouldFail() {
        when(vault.storeSecret(anyString(), anyString(), anyString())).thenReturn(Result.failure("vault unavailable"));
        var key = createKey().publicKeyJwk(null).publicKeyPem(null).keyGeneratorParams(Map.of(
                "algorithm", "EdDSA",
                "curve", "Ed25519"
        )).build();

        assertThat(keyPairService.addKeyPair(PARTICIPANT_ID, key, true)).isFailed()
                .detail().contains("vault unavailable");
        verify(keyPairResourceStore, never()).create(any());
        verifyNoInteractions(observableMock);
    }

    @Test
    void addKeyPair_whenCreateFails_shouldDeletePrivateKeyWithoutRollingBack() {
        when(keyPairResourceStore.create(any())).thenReturn(StoreResult.alreadyExists("key pair exists"));
        var key = createKey().publicKeyJwk(null).publicKeyPem(null).keyGeneratorParams(Map.of(
                "algorithm", "EdDSA",
                "curve", "Ed25519"
        )).build();

        assertThat(keyPairService.addKeyPair(PARTICIPANT_ID, key, true)).isFailed()
                .detail().isEqualTo("key pair exists");

        var inOrder = inOrder(vault, keyPairResourceStore);
        inOrder.verify(vault).storeSecret(anyString(), eq(key.getPrivateKeyAlias()), anyString());
        inOrder.verify(keyPairResourceStore).create(any());
        inOrder.verify(vault).deleteSecret(PARTICIPANT_ID, key.getPrivateKeyAlias());
        verifyNoInteractions(observableMock);
        // nothing was written, so the caller decides whether a surrounding transaction, e.g. the one creating a participant
        // context, is rolled back
        assertThat(transactionContext.isRolledBack(1)).isFalse();
    }

    @Test
    void discardKeyMaterial_whenGenerated_shouldDeletePrivateKey() {
        var key = createKey().publicKeyJwk(null).publicKeyPem(null).keyGeneratorParams(Map.of(
                "algorithm", "EdDSA",
                "curve", "Ed25519"
        )).build();

        keyPairService.discardKeyMaterial(PARTICIPANT_ID, key);

        verify(vault).deleteSecret(PARTICIPANT_ID, key.getPrivateKeyAlias());
        verifyNoMoreInteractions(vault);
        verifyNoInteractions(keyPairResourceStore, observableMock);
    }

    @Test
    void discardKeyMaterial_whenImported_shouldNotDeletePrivateKey() {
        var key = createKey().publicKeyJwk(createJwk()).publicKeyPem(null).keyGeneratorParams(null).build();

        keyPairService.discardKeyMaterial(PARTICIPANT_ID, key);

        // the private key of an imported key pair was stored by someone else
        verifyNoInteractions(vault, keyPairResourceStore, observableMock);
    }

    @Test
    void discardKeyMaterial_whenDeletingFails_shouldWarn() {
        when(vault.deleteSecret(anyString(), anyString())).thenReturn(Result.failure("vault down"));
        var key = createKey().publicKeyJwk(null).publicKeyPem(null).keyGeneratorParams(Map.of(
                "algorithm", "EdDSA",
                "curve", "Ed25519"
        )).build();

        keyPairService.discardKeyMaterial(PARTICIPANT_ID, key);

        verify(monitor).warning(contains(key.getPrivateKeyAlias()));
    }

    @Test
    void revokeKey_withNewKey_shouldStoreSuccessorFirstAndDeleteOldPrivateKeyLast() {
        var oldId = "old-id";
        var oldKey = createKeyPairResource().id(oldId).build();
        storeFinds(oldKey);
        var transactionsOfDeletes = new ArrayList<Integer>();
        when(vault.deleteSecret(anyString(), anyString())).thenAnswer(i -> {
            transactionsOfDeletes.add(transactionContext.currentTransaction());
            return Result.success();
        });
        var listener = mock(KeyPairEventListener.class);
        doAnswer(i -> {
            i.<Consumer<KeyPairEventListener>>getArgument(0).accept(listener);
            return null;
        }).when(observableMock).invokeForEach(any());

        var newKey = createKey().publicKeyJwk(null).publicKeyPem(null).keyGeneratorParams(Map.of(
                "algorithm", "EdDSA",
                "curve", "Ed25519"
        )).build();
        assertThat(keyPairService.revokeKey(oldId, newKey)).isSucceeded();

        // the successor's private key is stored before anything is written, and the events, which may take locks, come
        // last. The old key pair is announced first, because its successor may have the same key ID
        var inOrder = inOrder(vault, keyPairResourceStore, listener);
        inOrder.verify(vault).storeSecret(anyString(), eq(newKey.getPrivateKeyAlias()), anyString());
        inOrder.verify(keyPairResourceStore).create(argThat(kpr -> kpr.getKeyId().equals(NEW_KEY_ID)));
        inOrder.verify(keyPairResourceStore).update(argThat(kpr -> kpr.getId().equals(oldId)));
        inOrder.verify(listener).revoked(argThat(kpr -> kpr.getId().equals(oldId)), eq(newKey));
        inOrder.verify(listener).added(argThat(kpr -> kpr.getKeyId().equals(NEW_KEY_ID)), any());
        inOrder.verify(listener).activated(argThat(kpr -> kpr.getKeyId().equals(NEW_KEY_ID)), any());
        inOrder.verify(vault).deleteSecret(PARTICIPANT_ID, oldKey.getPrivateKeyAlias());
        // the old private key is only deleted once the transaction is completed, so that a failure leaves the old key usable
        assertThat(transactionsOfDeletes).containsExactly(0);
    }

    @Test
    void revokeKey_withNewKey_whenUpdatingOldKeyFails_shouldRollBack() {
        var oldId = "old-id";
        var oldKey = createKeyPairResource().id(oldId).build();
        storeFinds(oldKey);
        when(keyPairResourceStore.update(any())).thenReturn(StoreResult.generalError("update failed"));
        var newKey = createKey().publicKeyJwk(null).publicKeyPem(null).keyGeneratorParams(Map.of(
                "algorithm", "EdDSA",
                "curve", "Ed25519"
        )).build();

        assertThat(keyPairService.revokeKey(oldId, newKey)).isFailed().detail().isEqualTo("update failed");

        // the successor was written already, and must not stay without the old key being revoked
        assertThat(transactionContext.isRolledBack(1)).isTrue();
        verify(vault).deleteSecret(PARTICIPANT_ID, newKey.getPrivateKeyAlias());
        // the old key stays usable
        verify(vault, never()).deleteSecret(anyString(), eq(oldKey.getPrivateKeyAlias()));
        verifyNoInteractions(observableMock);
    }

    @Test
    void revokeKey_withNewKey_whenCreatingSuccessorFails_shouldNotRevoke() {
        var oldId = "old-id";
        var oldKey = createKeyPairResource().id(oldId).build();
        storeFinds(oldKey);
        when(keyPairResourceStore.create(any())).thenReturn(StoreResult.alreadyExists("key pair exists"));
        var newKey = createKey().publicKeyJwk(null).publicKeyPem(null).keyGeneratorParams(Map.of(
                "algorithm", "EdDSA",
                "curve", "Ed25519"
        )).build();

        assertThat(keyPairService.revokeKey(oldId, newKey)).isFailed().detail().isEqualTo("key pair exists");

        verify(keyPairResourceStore, never()).update(any());
        verify(vault).deleteSecret(PARTICIPANT_ID, newKey.getPrivateKeyAlias());
        verify(vault, never()).deleteSecret(anyString(), eq(oldKey.getPrivateKeyAlias()));
        verifyNoInteractions(observableMock);
    }

    @Test
    void rotateKeyPair_withNewKey_whenParticipantDeactivated_shouldFailWithoutRotating() {
        var oldId = "old-id";
        storeFinds(createKeyPairResource().id(oldId).build());
        when(participantContextServiceMock.findById(anyString())).thenReturn(StoreResult.success(ParticipantContext.Builder.newInstance()
                .participantContextId(PARTICIPANT_ID)
                .identity("did:example:123")
                .state(ParticipantContextState.DEACTIVATED)
                .build()));

        // the successor is checked before anything is changed, so that the old key is not left rotated without it
        assertThat(keyPairService.rotateKeyPair(oldId, createKey().build(), Duration.ofDays(100).toMillis())).isFailed();
        verify(keyPairResourceStore, never()).update(any());
        verify(keyPairResourceStore, never()).create(any());
        verifyNoInteractions(vault, observableMock);
    }

    @Test
    void revokeKey_whenKeyWasRotated_shouldNotDeletePrivateKeyAgain() {
        var oldId = "old-id";
        storeFinds(createKeyPairResource().id(oldId).state(KeyPairState.ROTATED).build());

        assertThat(keyPairService.revokeKey(oldId, null)).isSucceeded();

        // its private key was deleted when it was rotated
        verify(keyPairResourceStore).update(argThat(kpr -> kpr.getState() == KeyPairState.REVOKED.code()));
        verifyNoInteractions(vault);
    }

    @Test
    void activate_whenAlreadyActive_shouldNotActivateAgain() {
        var keyPair = createKeyPairResource().state(KeyPairState.ACTIVATED).build();
        when(keyPairResourceStore.query(any())).thenReturn(success(List.of(keyPair)));

        // e.g. a client retrying the request: the key pair must not be added to the DID document a second time
        assertThat(keyPairService.activate(keyPair.getId())).isSucceeded();

        verify(keyPairResourceStore, never()).update(any());
        verifyNoInteractions(observableMock);
    }

    @ParameterizedTest(name = "Valid state = {0}")
    // cannot use enum literals and the .code() method -> needs to be compile constant
    @ValueSource(ints = {100, 200})
    void activate(int validState) {
        var oldId = "old-id";
        var oldKey = createKeyPairResource().id(oldId).state(validState).build();

        storeFinds(oldKey);
        when(keyPairResourceStore.update(any())).thenReturn(success());

        assertThat(keyPairService.activate(oldId)).isSucceeded();
    }

    @ParameterizedTest(name = "Valid state = {0}")
    // cannot use enum literals and the .code() method -> needs to be compile constant
    @ValueSource(ints = {0, 30, 400, -10})
    void activate_invalidState(int validState) {
        var oldId = "old-id";
        var oldKey = createKeyPairResource().id(oldId).state(validState).build();

        storeFinds(oldKey);
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

    @Test
    void getActiveKeyPairForUsage_singleKeyPair() {
        var keyPair = createKeyPairResource()
                .usage(PRESENTATION_SIGNING)
                .state(KeyPairState.ACTIVATED.code())
                .build();

        when(keyPairResourceStore.query(any())).thenReturn(success(List.of(keyPair)));

        var result = keyPairService.getActiveKeyPairForUsage(PARTICIPANT_ID, PRESENTATION_SIGNING);

        assertThat(result).isSucceeded()
                .satisfies(kp -> assertThat(kp.getId()).isEqualTo(keyPair.getId()));
    }

    @Test
    void getActiveKeyPairForUsage_multipleKeyPairs_oneIsDefault() {
        var defaultKeyPair = createKeyPairResource()
                .usage(CREDENTIAL_SIGNING)
                .state(KeyPairState.ACTIVATED.code())
                .isDefaultPair(true)
                .build();

        var nonDefaultKeyPair = createKeyPairResource()
                .usage(CREDENTIAL_SIGNING)
                .state(KeyPairState.ACTIVATED.code())
                .isDefaultPair(false)
                .build();

        when(keyPairResourceStore.query(any())).thenReturn(success(List.of(defaultKeyPair, nonDefaultKeyPair)));

        var result = keyPairService.getActiveKeyPairForUsage(PARTICIPANT_ID, CREDENTIAL_SIGNING);

        assertThat(result).isSucceeded()
                .satisfies(kp -> assertThat(kp.getId()).isEqualTo(defaultKeyPair.getId()));
    }

    @Test
    void getActiveKeyPairForUsage_multipleKeyPairs_noneIsDefault() {
        var keyPair1 = createKeyPairResource()
                .usage((TOKEN_SIGNING))
                .state(KeyPairState.ACTIVATED.code())
                .isDefaultPair(false)
                .build();

        var keyPair2 = createKeyPairResource()
                .usage((TOKEN_SIGNING))
                .state(KeyPairState.ACTIVATED.code())
                .isDefaultPair(false)
                .build();

        when(keyPairResourceStore.query(any())).thenReturn(success(List.of(keyPair1, keyPair2)));

        var result = keyPairService.getActiveKeyPairForUsage(PARTICIPANT_ID, TOKEN_SIGNING);

        assertThat(result).isFailed()
                .detail().isEqualTo("Multiple key-pairs found for signing credentials, but none was marked as 'default'");
    }

    @Test
    void getActiveKeyPairForUsage_noMatchingKeyPairs() {
        var keyPair = createKeyPairResource()
                .usage(Set.of(PRESENTATION_SIGNING))
                .state(KeyPairState.ACTIVATED.code())
                .build();

        when(keyPairResourceStore.query(any())).thenReturn(success(List.of(keyPair)));

        var result = keyPairService.getActiveKeyPairForUsage(PARTICIPANT_ID, CREDENTIAL_SIGNING);

        assertThat(result).isFailed()
                .detail().isEqualTo("No active key pair found for participant '%s' with usage 'CREDENTIAL_SIGNING'".formatted(PARTICIPANT_ID));
    }

    @Test
    void getActiveKeyPairForUsage_storeQueryFails() {
        when(keyPairResourceStore.query(any())).thenReturn(StoreResult.notFound("Store error"));

        var result = keyPairService.getActiveKeyPairForUsage(PARTICIPANT_ID, TOKEN_SIGNING);

        assertThat(result).isFailed()
                .detail().contains("Error obtaining private key for participant '%s'".formatted(PARTICIPANT_ID));
    }

    @Test
    void getActiveKeyPairForUsage_keyPairWithMultipleUsages() {
        var keyPair = createKeyPairResource()
                .usage(Set.of(PRESENTATION_SIGNING, CREDENTIAL_SIGNING))
                .state(KeyPairState.ACTIVATED.code())
                .build();

        when(keyPairResourceStore.query(any())).thenReturn(success(List.of(keyPair)));

        var result = keyPairService.getActiveKeyPairForUsage(PARTICIPANT_ID, CREDENTIAL_SIGNING);

        assertThat(result).isSucceeded()
                .satisfies(kp -> assertThat(kp.getId()).isEqualTo(keyPair.getId()));
    }

    /**
     * Lets the store find the given key pair, but no key pair that uses the key ID of the new key.
     */
    private void storeFinds(KeyPairResource keyPair) {
        when(keyPairResourceStore.query(any())).thenReturn(success(List.of(keyPair)));
        when(keyPairResourceStore.query(argThat(isKeyIdQuery(NEW_KEY_ID)))).thenReturn(success(List.of()));
    }

    private ArgumentMatcher<QuerySpec> isKeyIdQuery(String keyId) {
        return query -> query != null && query.getFilterExpression().contains(new Criterion("keyId", "=", keyId));
    }

    private KeyPairResource.Builder createKeyPairResource() {
        return KeyPairResource.Builder.newTokenSigning()
                .id(UUID.randomUUID().toString())
                .keyId("test-key-1")
                .privateKeyAlias("private-key-alias")
                .participantContextId(PARTICIPANT_ID)
                .serializedPublicKey("this-is-a-pem-string")
                .useDuration(Duration.ofDays(6).toMillis());
    }

    @NotNull
    private KeyDescriptor.Builder createKey() {
        return KeyDescriptor.Builder.newInstance()
                .keyId(NEW_KEY_ID)
                .usage(Set.of(PRESENTATION_SIGNING))
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